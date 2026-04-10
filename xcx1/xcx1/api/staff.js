// api/staff.js
//
// 说明：本项目最初的接口约定为 /api/staff/**（token 鉴权）。
// 但当前提供的后端代码（智能随访）接口形态不同：
// - 登录：/auth/login（返回 userId/role/name，通常不返回 token）
// - 医生侧：大量接口需要 doctorUsername/doctorPassword 作为参数二次校验
// - 随访员侧：部分接口需要 followUpUsername/followUpPassword 二次校验
//
// 因此这里做一层“适配器”：
// - 若后端确实有对应能力，就把小程序的数据结构转换成页面期望的结构
// - 若后端没有，对外仍保持原函数名，页面可以继续走 USE_MOCK_FALLBACK 的 demo 兜底

import { request } from '@/utils/request.js';
import { getUser } from '@/utils/session.js';

function getLoginInfo() {
  const u = getUser() || {};
  return {
    username: u.username || '',
    password: u.password || '',
    backendRole: (u.backendRole || u.role || '').toUpperCase(),
    userId: u.id
  };
}

function toText(v) {
  return v == null ? '' : String(v);
}function normalizeGender(v) {
  const s = toText(v).toUpperCase();
  if (!s) return '';
  if (s === 'M' || s === 'MALE' || s === '男' || s === '1') return '男';
  if (s === 'F' || s === 'FEMALE' || s === '女' || s === '0' || s === '2') return '女';
  return toText(v);
}

function mapTaskTypeToText(v) {
  const s = toText(v).toUpperCase();
  if (!s) return '随访任务';
  if (s.includes('PHONE') || s.includes('TEL') || s === '电话') return '电话随访';
  if (s.includes('HOME') || s.includes('VISIT') || s === '上门') return '上门随访';
  if (s.includes('VIDEO') || s === '视频') return '视频随访';
  if (s.includes('CLINIC') || s.includes('OUTPATIENT') || s === '门诊') return '门诊随访';
  return toText(v);
}

function fmtDateTime(v) {
  if (!v) return '';
  const s = toText(v);
  // 兼容 LocalDateTime: 2026-01-01T09:30:00
  return s.replace('T', ' ').replace(/\.\d+$/, '');
}

function mapRiskToUi(risk) {
  const r = toText(risk).toUpperCase();
  if (r === 'HIGH' || r === '高') return { level: 'high', text: '高风险', cn: '高' };
  if (r === 'LOW' || r === '低') return { level: 'low', text: '低风险', cn: '低' };
  return { level: 'mid', text: '中风险', cn: '中' };
}

async function fetchPatientBasic(patientId) {
  if (!patientId) return null;

  const tryUrls = [
    `/api/patient/detail/${encodeURIComponent(patientId)}`,
    `/api/patient/${encodeURIComponent(patientId)}`,
    `/patient/detail/${encodeURIComponent(patientId)}`,
    `/patient/${encodeURIComponent(patientId)}`
  ];

  for (const url of tryUrls) {
    try {
      const data = await request({ url, method: 'GET' });
      // 兼容多种返回结构
      const root = (data && (data.data || data.result)) || data || {};
      const basic = root.basicInfo || root.patient || root.profile || root || {};
      const name = basic.name || basic.patientName || basic.realName || basic.nickname || '';
      const age = basic.age || basic.patientAge || basic.years || '';
      const gender = normalizeGender(basic.gender || basic.sex || basic.genderText || basic.sexText);
      return {
        id: basic.id || basic.patientId || patientId,
        name,
        age,
        gender
      };
    } catch (e) {
      // continue
    }
  }

  // 最后兜底
  return { id: patientId, name: `患者#${patientId}`, age: '', gender: '' };
}

// =======================
// 随访任务
// =======================

// 随访任务列表
export function getTasks(params) {
  const { username, password, backendRole } = getLoginInfo();
  const filter = (params && params.filter) || '';

  // 1) 医生侧：/api/followup/tasks（返回带 patientName / riskLevel / planTime 的 rows）
  if (backendRole === 'DOCTOR') {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const startAt = `${yyyy}-${mm}-${dd}T00:00:00`;
    const endAt = `${yyyy}-${mm}-${dd}T23:59:59`;

    const q = {
      doctorUsername: username,
      doctorPassword: password,
      page: 1,
      pageSize: 200
    };

    if (filter === 'done') q.status = 'COMPLETED';
    if (filter === 'urgent') q.riskLevel = 'HIGH';

    // “今日”筛选：后端支持按计划时间区间筛选
    if (filter === 'today' || filter === 'urgent') {
      q.startAt = startAt;
      q.endAt = endAt;
    }

    return request({ url: '/api/followup/tasks', method: 'GET', data: q }).then(async (data) => {
  const rows = (data && data.rows) || [];
  const needEnrich = rows.length <= 15;
  const patientCache = new Map();

  async function getP(pid) {
    if (!needEnrich) return null;
    if (!pid) return null;
    if (patientCache.has(pid)) return patientCache.get(pid);
    const p = await fetchPatientBasic(pid);
    patientCache.set(pid, p);
    return p;
  }

  const list = [];
  for (const r of rows) {
    const risk = mapRiskToUi(r.riskLevel || r.risk || r.level);
    const planTime = fmtDateTime(r.planTime || r.planAt || r.scheduledAt || r.createdAt);
    const finishTime = fmtDateTime(r.finishTime || r.completedAt || r.doneAt);

    const statusRaw = toText(r.status).toUpperCase();
    const done = statusRaw === 'COMPLETED' || statusRaw === 'DONE' || statusRaw === 'FINISHED';

    // “超期”粗略判断：计划时间早于当前且未完成
    let overdue = false;
    if (!done && planTime) {
      const planTs = Date.parse(planTime.replace(' ', 'T'));
      if (!Number.isNaN(planTs)) overdue = planTs < Date.now();
    }

    const p = await getP(r.patientId);
    const patientName = r.patientName || r.name || (p && p.name) || (r.patientId ? `患者#${r.patientId}` : '未知患者');
    const age = r.age || r.patientAge || (p && p.age) || '';
    const gender = normalizeGender(r.gender || r.sex || (p && p.gender));

    const typeText = r.typeText || r.taskTypeText || mapTaskTypeToText(r.taskType || r.type || r.followupType);
    const aiSummary = toText(r.aiSummary || r.summary || r.remark || '');
    const fromAlert = !!(r.fromAlert || r.alertId || r.relatedAlertId);

    const statusText = done ? '已完成' : overdue ? '已超期' : '待处理';

    list.push({
      id: r.taskId || r.id,
      patientId: r.patientId,
      patientName,
      age,
      gender,
      type: r.taskType || r.type || '',
      typeText,
      desc: aiSummary,
      planTime,
      doneTime: finishTime,
      aiSummary,
      fromAlert,
      overdue,
      riskLevel: risk.level,
      riskText: risk.text,
      status: done ? 'done' : 'todo',
      statusText
    });
  }

  return list;
});
  }

  // 2) 随访员侧：/followUpTask/followup/my（返回 FollowUpTask 列表，字段较少）
  if (backendRole === 'FOLLOW_UP') {
    return request({
      url: '/followUpTask/followup/my',
      method: 'GET',
      data: { followUpUsername: username, followUpPassword: password }
    }).then(async (tasks) => {
      const arr = Array.isArray(tasks) ? tasks : [];
      // 任务数量不多时补齐患者基本信息，避免页面全是 patientId
      const needEnrich = arr.length <= 15;
      const patientCache = new Map();

      async function getP(pid) {
        if (!needEnrich) return { id: pid, name: `患者#${pid}` };
        if (patientCache.has(pid)) return patientCache.get(pid);
        const p = await fetchPatientBasic(pid);
        patientCache.set(pid, p);
        return p;
      }

      const list = [];
      for (const t of arr) {
        const statusRaw = toText(t.status).toUpperCase();
        const done = ['COMPLETED','DONE','FINISHED','1'].includes(statusRaw);
        const p = await getP(t.patientId);
        const typeText = t.typeText || t.taskTypeText || mapTaskTypeToText(t.taskType || t.type || t.followupType);
const planTime = fmtDateTime(t.planTime || t.scheduledAt || t.ext1 || t.planDate || t.createdAt);
const aiSummary = toText(t.aiSummary || t.description || t.remark || '');
const patientName = (p && p.name) || t.patientName || (t.patientId ? `患者#${t.patientId}` : '未知患者');
const age = t.age || t.patientAge || (p && p.age) || '';
const gender = normalizeGender(t.gender || t.sex || (p && p.gender));

// “超期”粗略判断：计划时间早于当前且未完成
let overdue = false;
if (!done && planTime) {
  const planTs = Date.parse(String(planTime).replace(' ', 'T'));
  if (!Number.isNaN(planTs)) overdue = planTs < Date.now();
}

const statusText = done ? '已完成' : overdue ? '已超期' : '待处理';

list.push({
  id: t.id,
  patientId: t.patientId,
  patientName,
  age,
  gender,
  type: t.taskType || t.type || '',
  typeText,
  desc: aiSummary,
  planTime,
  doneTime: '',
  aiSummary,
  fromAlert: !!(t.fromAlert || t.alertId),
  riskLevel: 'mid',
  riskText: '中风险',
  status: done ? 'done' : 'todo',
  statusText,
  overdue
});
      }

      // 前端筛选：done/today/urgent/overdue
      if (filter === 'done') return list.filter((x) => x.status === 'done');
      return list;
    });
  }

  // 其它角色：让页面走 demo 兜底
  return Promise.reject({ message: '当前账号角色暂无可用的随访任务接口' });
}

// 任务详情（尽力从现有接口拼出来）
export async function getTaskDetail(taskId) {
  const { username, password, backendRole } = getLoginInfo();
  if (!taskId) throw { message: '缺少 taskId' };

  if (backendRole === 'DOCTOR') {
    // /api/followup/tasks 无“详情”接口，先拉一页再按 taskId 过滤
    const data = await request({
      url: '/api/followup/tasks',
      method: 'GET',
      data: { doctorUsername: username, doctorPassword: password, page: 1, pageSize: 200 }
    });
    const rows = (data && data.rows) || [];
    const row = rows.find((r) => toText(r.taskId) === toText(taskId) || toText(r.id) === toText(taskId));
    if (!row) throw { message: '未找到任务' };
    const p = await fetchPatientBasic(row.patientId);
    const risk = mapRiskToUi(row.riskLevel);
    return {
      patient: {
        id: row.patientId,
        name: (p && p.name) || row.patientName || `患者#${row.patientId}`,
        age: p && p.age,
        gender: p && p.gender
      },
      task: {
        id: row.taskId,
        typeText: '随访任务',
        planTime: fmtDateTime(row.planTime),
        aiSummary: '',
        riskLevel: risk.level,
        riskText: risk.text
      }
    };
  }

  if (backendRole === 'FOLLOW_UP') {
    const tasks = await request({
      url: '/followUpTask/followup/my',
      method: 'GET',
      data: { followUpUsername: username, followUpPassword: password }
    });
    const arr = Array.isArray(tasks) ? tasks : [];
    const t = arr.find((x) => toText(x.id) === toText(taskId));
    if (!t) throw { message: '未找到任务' };
    const p = await fetchPatientBasic(t.patientId);
    return {
      patient: {
        id: t.patientId,
        name: (p && p.name) || `患者#${t.patientId}`,
        age: p && p.age,
        gender: p && p.gender
      },
      task: {
        id: t.id,
        typeText: '随访任务',
        planTime: fmtDateTime(t.createdAt),
        aiSummary: t.description || '',
        riskLevel: 'mid',
        riskText: '中风险'
      }
    };
  }

  throw { message: '当前账号角色暂无可用的任务详情接口' };
}

// =======================
// 告警/异常（医生侧）
// =======================

// 告警/异常列表
export function getAlerts(params) {
  const { username, password, backendRole } = getLoginInfo();
  if (backendRole !== 'DOCTOR') {
    return Promise.reject({ message: '当前账号角色暂无可用的告警列表接口' });
  }

  const q = {
    doctorUsername: username,
    doctorPassword: password,
    page: 1,
    pageSize: 50,
    ...(params || {})
  };

  return request({ url: '/api/alert/alerts', method: 'GET', data: q }).then(async (data) => {
    const rows = (data && data.rows) || [];
    const needEnrich = rows.length <= 15;
    const patientCache = new Map();

    const detectFlags = (summary) => {
      const s = toText(summary);
      const flags = [];
      if (/血压/.test(s)) flags.push('bp');
      if (/(体温|体重)/.test(s)) flags.push('weight');
      if (/症状|胸闷|憋醒|气促|头晕|乏力/.test(s)) flags.push('symptom');
      return flags;
    };

    async function getP(pid) {
      if (!needEnrich) return null;
      if (patientCache.has(pid)) return patientCache.get(pid);
      const p = await fetchPatientBasic(pid);
      patientCache.set(pid, p);
      return p;
    }

    const list = [];
    for (const r of rows) {
      const risk = mapRiskToUi(r.riskLevel);
      const statusRaw = toText(r.status).toUpperCase();
      const done = statusRaw === 'HANDLED' || statusRaw === 'DONE' || statusRaw === 'RESOLVED';
      const p = await getP(r.patientId);

      const summary = toText(r.summary);
      const reasons = summary
        ? summary
            .split(/；|;|。|\n/)
            .map((x) => x.trim())
            .filter(Boolean)
            .slice(0, 3)
        : [];
      const typeFlags = detectFlags(summary);

      list.push({
        id: r.patientId ?? r.id, // 形如 HEALTH-1 / DEVICE-2
        source: r.source,
        alertId: r.alertId,
        patientId: r.patientId,
        patientName: r.patientName || (p && p.name) || (r.patientId ? `患者#${r.patientId}` : ''),
        age: p && p.age,
        gender: p && p.gender,
        mainDisease: '—',
        reasons,
        typeFlags,
        latestBP: '',
        latestWeight: '',
        planText: summary ? '请评估处理' : '',
        nextVisitDate: '',
        aiSummary: summary,
        planTime: fmtDateTime(r.alertTime),
        riskLevel: risk.level,
        riskText: risk.text,
        status: done ? 'done' : 'todo',
        overdue: false
      });
    }
    return list;
  });
}

// 告警详情（仅健康预警支持 form；设备预警暂无详情接口）
export async function getAlertDetail(alertId) {
  const { username, password, backendRole } = getLoginInfo();
  if (backendRole !== 'DOCTOR') throw { message: '当前账号角色暂无可用的告警详情接口' };
  if (!alertId) throw { message: '缺少 alertId' };

  // alertId 可能是 HEALTH-123 这类复合 id
  const idText = toText(alertId);
  const m = idText.match(/^(HEALTH|DEVICE)\-(\d+)$/i);
  const source = m ? m[1].toUpperCase() : '';
  const numericId = m ? Number(m[2]) : Number(idText);

  if (source === 'DEVICE') {
    throw { message: '设备告警暂无 form 详情接口（可先使用 Demo 兜底）' };
  }
  if (!numericId || Number.isNaN(numericId)) {
    throw { message: 'alertId 格式不正确' };
  }

  const data = await request({
    url: '/alert/form',
    method: 'GET',
    data: { doctorUsername: username, doctorPassword: password, alertId: numericId }
  });

  const patient = (data && data.patient) || {};
  const event = (data && data.event) || {};
  const abnormal = (data && data.abnormalItems) || [];

  const risk = mapRiskToUi(patient.riskLevel);
  const summary = abnormal
    .map((x) => `${x.name || ''}${x.currentValue != null ? `:${x.currentValue}` : ''}${x.unit ? x.unit : ''}`)
    .filter(Boolean)
    .join('；');

  return {
    patient: {
      id: patient.patientId,
      name: patient.name,
      age: '',
      gender: ''
    },
    task: {
      id: `alert-${numericId}`,
      typeText: '预警随访',
      planTime: fmtDateTime(event.createdAt),
      aiSummary: summary,
      riskLevel: risk.level,
      riskText: risk.text
    }
  };
}

// =======================
// 随访记录（后端暂无对应接口，保持原样，让页面走 Demo 兜底）
// =======================

export function submitFollowup(data) {
  return request({ url: '/staff/followups', method: 'POST', data });
}

// 按 taskId 获取已填写的随访记录（用于回显；后端需实现 /staff/followups/by-task/{taskId}）
export function getFollowupByTaskId(taskId) {
  return request({ url: `/staff/followups/by-task/${encodeURIComponent(taskId)}`, method: 'GET' });
}


export function updateTaskStatus(taskId, status) {
  return request({
    url: `/staff/tasks/${encodeURIComponent(taskId)}/status`,
    method: 'PUT',
    data: { status }
  });
}

// =======================
// 患者列表（后端有 /api/patient/summary）
// =======================

export function listPatients(params) {
  const q = {
    page: 1,
    pageSize: 200,
    ...(params || {})
  };
  return request({ url: '/api/patient/summary', method: 'GET', data: q }).then((data) => {
    const rows = (data && data.rows) || [];
    return rows.map((r) => {
      const risk = mapRiskToUi(r.riskLevel);
      return {
        id: r.patientId ?? r.id,
        name: r.name,
        age: r.age,
        disease: r.mainDisease || r.disease || '—',
        constitution: '—',
        tcmPattern: r.syndrome || '—',
        riskLevel: risk.cn
      };
    });
  });
}

// 兼容旧页面命名：patients.vue 使用 getPatients
export function getPatients(params) {
  return listPatients(params);
}

// =======================
// 个人资料（后端暂无 /staff/profile，保持旧接口，页面会走本地存储兜底）
// =======================

export function getProfile() {
  return request({ url: '/staff/profile', method: 'GET' });
}

export function updateProfile(data) {
  return request({ url: '/staff/profile', method: 'PUT', data });
}

export function getStaffProfile() {
  return getProfile();
}

export function updateStaffProfile(data) {
  return updateProfile(data);
}


// =======================
// 医护端：查看患者数据（对标患者端）
// =======================

// 返回建议结构：{ profile, today, history, labs, rehabTasks, messages? }
export function getPatientData(patientId, params) {
  const q = params || {};
  return request({
    url: `/api/staff/patients/${encodeURIComponent(patientId)}/data`,
    method: 'GET',
    data: q
  });
}

// =======================
// 医护端：给患者发送建议（写入患者消息中心）
// =======================

export function sendPatientAdvice(patientId, content) {
  return request({
    url: `/api/staff/patients/${encodeURIComponent(patientId)}/advice`,
    method: 'POST',
    data: { content }
  });
}
