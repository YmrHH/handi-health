"use strict";
const utils_request = require("../utils/request.js");
const utils_session = require("../utils/session.js");
function getLoginInfo() {
  const u = utils_session.getUser() || {};
  return {
    username: u.username || "",
    password: u.password || "",
    backendRole: (u.backendRole || u.role || "").toUpperCase(),
    userId: u.id
  };
}
function toText(v) {
  return v == null ? "" : String(v);
}
function normalizeGender(v) {
  const s = toText(v).toUpperCase();
  if (!s)
    return "";
  if (s === "M" || s === "MALE" || s === "男" || s === "1")
    return "男";
  if (s === "F" || s === "FEMALE" || s === "女" || s === "0" || s === "2")
    return "女";
  return toText(v);
}
function mapTaskTypeToText(v) {
  const s = toText(v).toUpperCase();
  if (!s)
    return "随访任务";
  if (s.includes("PHONE") || s.includes("TEL") || s === "电话")
    return "电话随访";
  if (s.includes("HOME") || s.includes("VISIT") || s === "上门")
    return "上门随访";
  if (s.includes("VIDEO") || s === "视频")
    return "视频随访";
  if (s.includes("CLINIC") || s.includes("OUTPATIENT") || s === "门诊")
    return "门诊随访";
  return toText(v);
}
function fmtDateTime(v) {
  if (!v)
    return "";
  const s = toText(v);
  return s.replace("T", " ").replace(/\.\d+$/, "");
}
function mapRiskToUi(risk) {
  const r = toText(risk).toUpperCase();
  if (r === "HIGH" || r === "高")
    return { level: "high", text: "高风险", cn: "高" };
  if (r === "LOW" || r === "低")
    return { level: "low", text: "低风险", cn: "低" };
  return { level: "mid", text: "中风险", cn: "中" };
}
async function fetchPatientBasic(patientId) {
  if (!patientId)
    return null;
  const tryUrls = [
    `/api/patient/detail/${encodeURIComponent(patientId)}`,
    `/api/patient/${encodeURIComponent(patientId)}`,
    `/patient/detail/${encodeURIComponent(patientId)}`,
    `/patient/${encodeURIComponent(patientId)}`
  ];
  for (const url of tryUrls) {
    try {
      const data = await utils_request.request({ url, method: "GET" });
      const root = data && (data.data || data.result) || data || {};
      const basic = root.basicInfo || root.patient || root.profile || root || {};
      const name = basic.name || basic.patientName || basic.realName || basic.nickname || "";
      const age = basic.age || basic.patientAge || basic.years || "";
      const gender = normalizeGender(basic.gender || basic.sex || basic.genderText || basic.sexText);
      return {
        id: basic.id || basic.patientId || patientId,
        name,
        age,
        gender
      };
    } catch (e) {
    }
  }
  return { id: patientId, name: `患者#${patientId}`, age: "", gender: "" };
}
function getTasks(params) {
  const { username, password, backendRole } = getLoginInfo();
  const filter = params && params.filter || "";
  if (backendRole === "DOCTOR") {
    const now = /* @__PURE__ */ new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, "0");
    const dd = String(now.getDate()).padStart(2, "0");
    const startAt = `${yyyy}-${mm}-${dd}T00:00:00`;
    const endAt = `${yyyy}-${mm}-${dd}T23:59:59`;
    const q = {
      doctorUsername: username,
      doctorPassword: password,
      page: 1,
      pageSize: 200
    };
    if (filter === "done")
      q.status = "COMPLETED";
    if (filter === "urgent")
      q.riskLevel = "HIGH";
    if (filter === "today" || filter === "urgent") {
      q.startAt = startAt;
      q.endAt = endAt;
    }
    return utils_request.request({ url: "/api/followup/tasks", method: "GET", data: q }).then(async (data) => {
      const rows = data && data.rows || [];
      const needEnrich = rows.length <= 15;
      const patientCache = /* @__PURE__ */ new Map();
      async function getP(pid) {
        if (!needEnrich)
          return null;
        if (!pid)
          return null;
        if (patientCache.has(pid))
          return patientCache.get(pid);
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
        const done = statusRaw === "COMPLETED" || statusRaw === "DONE" || statusRaw === "FINISHED";
        let overdue = false;
        if (!done && planTime) {
          const planTs = Date.parse(planTime.replace(" ", "T"));
          if (!Number.isNaN(planTs))
            overdue = planTs < Date.now();
        }
        const p = await getP(r.patientId);
        const patientName = r.patientName || r.name || p && p.name || (r.patientId ? `患者#${r.patientId}` : "未知患者");
        const age = r.age || r.patientAge || p && p.age || "";
        const gender = normalizeGender(r.gender || r.sex || p && p.gender);
        const typeText = r.typeText || r.taskTypeText || mapTaskTypeToText(r.taskType || r.type || r.followupType);
        const aiSummary = toText(r.aiSummary || r.summary || r.remark || "");
        const fromAlert = !!(r.fromAlert || r.alertId || r.relatedAlertId);
        const statusText = done ? "已完成" : overdue ? "已超期" : "待处理";
        list.push({
          id: r.taskId || r.id,
          patientId: r.patientId,
          patientName,
          age,
          gender,
          type: r.taskType || r.type || "",
          typeText,
          desc: aiSummary,
          planTime,
          doneTime: finishTime,
          aiSummary,
          fromAlert,
          overdue,
          riskLevel: risk.level,
          riskText: risk.text,
          status: done ? "done" : "todo",
          statusText
        });
      }
      return list;
    });
  }
  if (backendRole === "FOLLOW_UP") {
    return utils_request.request({
      url: "/followUpTask/followup/my",
      method: "GET",
      data: { followUpUsername: username, followUpPassword: password }
    }).then(async (tasks) => {
      const arr = Array.isArray(tasks) ? tasks : [];
      const needEnrich = arr.length <= 15;
      const patientCache = /* @__PURE__ */ new Map();
      async function getP(pid) {
        if (!needEnrich)
          return { id: pid, name: `患者#${pid}` };
        if (patientCache.has(pid))
          return patientCache.get(pid);
        const p = await fetchPatientBasic(pid);
        patientCache.set(pid, p);
        return p;
      }
      const list = [];
      for (const t of arr) {
        const statusRaw = toText(t.status).toUpperCase();
        const done = ["COMPLETED", "DONE", "FINISHED", "1"].includes(statusRaw);
        const p = await getP(t.patientId);
        const typeText = t.typeText || t.taskTypeText || mapTaskTypeToText(t.taskType || t.type || t.followupType);
        const planTime = fmtDateTime(t.planTime || t.scheduledAt || t.ext1 || t.planDate || t.createdAt);
        const aiSummary = toText(t.aiSummary || t.description || t.remark || "");
        const patientName = p && p.name || t.patientName || (t.patientId ? `患者#${t.patientId}` : "未知患者");
        const age = t.age || t.patientAge || p && p.age || "";
        const gender = normalizeGender(t.gender || t.sex || p && p.gender);
        let overdue = false;
        if (!done && planTime) {
          const planTs = Date.parse(String(planTime).replace(" ", "T"));
          if (!Number.isNaN(planTs))
            overdue = planTs < Date.now();
        }
        const statusText = done ? "已完成" : overdue ? "已超期" : "待处理";
        list.push({
          id: t.id,
          patientId: t.patientId,
          patientName,
          age,
          gender,
          type: t.taskType || t.type || "",
          typeText,
          desc: aiSummary,
          planTime,
          doneTime: "",
          aiSummary,
          fromAlert: !!(t.fromAlert || t.alertId),
          riskLevel: "mid",
          riskText: "中风险",
          status: done ? "done" : "todo",
          statusText,
          overdue
        });
      }
      if (filter === "done")
        return list.filter((x) => x.status === "done");
      return list;
    });
  }
  return Promise.reject({ message: "当前账号角色暂无可用的随访任务接口" });
}
async function getTaskDetail(taskId) {
  const { username, password, backendRole } = getLoginInfo();
  if (!taskId)
    throw { message: "缺少 taskId" };
  if (backendRole === "DOCTOR") {
    const data = await utils_request.request({
      url: "/api/followup/tasks",
      method: "GET",
      data: { doctorUsername: username, doctorPassword: password, page: 1, pageSize: 200 }
    });
    const rows = data && data.rows || [];
    const row = rows.find((r) => toText(r.taskId) === toText(taskId) || toText(r.id) === toText(taskId));
    if (!row)
      throw { message: "未找到任务" };
    const p = await fetchPatientBasic(row.patientId);
    const risk = mapRiskToUi(row.riskLevel);
    return {
      patient: {
        id: row.patientId,
        name: p && p.name || row.patientName || `患者#${row.patientId}`,
        age: p && p.age,
        gender: p && p.gender
      },
      task: {
        id: row.taskId,
        typeText: "随访任务",
        planTime: fmtDateTime(row.planTime),
        aiSummary: "",
        riskLevel: risk.level,
        riskText: risk.text
      }
    };
  }
  if (backendRole === "FOLLOW_UP") {
    const tasks = await utils_request.request({
      url: "/followUpTask/followup/my",
      method: "GET",
      data: { followUpUsername: username, followUpPassword: password }
    });
    const arr = Array.isArray(tasks) ? tasks : [];
    const t = arr.find((x) => toText(x.id) === toText(taskId));
    if (!t)
      throw { message: "未找到任务" };
    const p = await fetchPatientBasic(t.patientId);
    return {
      patient: {
        id: t.patientId,
        name: p && p.name || `患者#${t.patientId}`,
        age: p && p.age,
        gender: p && p.gender
      },
      task: {
        id: t.id,
        typeText: "随访任务",
        planTime: fmtDateTime(t.createdAt),
        aiSummary: t.description || "",
        riskLevel: "mid",
        riskText: "中风险"
      }
    };
  }
  throw { message: "当前账号角色暂无可用的任务详情接口" };
}
function getAlerts(params) {
  const { username, password, backendRole } = getLoginInfo();
  if (backendRole !== "DOCTOR") {
    return Promise.reject({ message: "当前账号角色暂无可用的告警列表接口" });
  }
  const q = {
    doctorUsername: username,
    doctorPassword: password,
    page: 1,
    pageSize: 50,
    ...params || {}
  };
  return utils_request.request({ url: "/api/alert/alerts", method: "GET", data: q }).then(async (data) => {
    const rows = data && data.rows || [];
    const needEnrich = rows.length <= 15;
    const patientCache = /* @__PURE__ */ new Map();
    const detectFlags = (summary) => {
      const s = toText(summary);
      const flags = [];
      if (/血压/.test(s))
        flags.push("bp");
      if (/(体温|体重)/.test(s))
        flags.push("weight");
      if (/症状|胸闷|憋醒|气促|头晕|乏力/.test(s))
        flags.push("symptom");
      return flags;
    };
    async function getP(pid) {
      if (!needEnrich)
        return null;
      if (patientCache.has(pid))
        return patientCache.get(pid);
      const p = await fetchPatientBasic(pid);
      patientCache.set(pid, p);
      return p;
    }
    const list = [];
    for (const r of rows) {
      const risk = mapRiskToUi(r.riskLevel);
      const statusRaw = toText(r.status).toUpperCase();
      const done = statusRaw === "HANDLED" || statusRaw === "DONE" || statusRaw === "RESOLVED";
      const p = await getP(r.patientId);
      const summary = toText(r.summary);
      const reasons = summary ? summary.split(/；|;|。|\n/).map((x) => x.trim()).filter(Boolean).slice(0, 3) : [];
      const typeFlags = detectFlags(summary);
      list.push({
        id: r.patientId ?? r.id,
        // 形如 HEALTH-1 / DEVICE-2
        source: r.source,
        alertId: r.alertId,
        patientId: r.patientId,
        patientName: r.patientName || p && p.name || (r.patientId ? `患者#${r.patientId}` : ""),
        age: p && p.age,
        gender: p && p.gender,
        mainDisease: "—",
        reasons,
        typeFlags,
        latestBP: "",
        latestWeight: "",
        planText: summary ? "请评估处理" : "",
        nextVisitDate: "",
        aiSummary: summary,
        planTime: fmtDateTime(r.alertTime),
        riskLevel: risk.level,
        riskText: risk.text,
        status: done ? "done" : "todo",
        overdue: false
      });
    }
    return list;
  });
}
async function getAlertDetail(alertId) {
  const { username, password, backendRole } = getLoginInfo();
  if (backendRole !== "DOCTOR")
    throw { message: "当前账号角色暂无可用的告警详情接口" };
  if (!alertId)
    throw { message: "缺少 alertId" };
  const idText = toText(alertId);
  const m = idText.match(/^(HEALTH|DEVICE)\-(\d+)$/i);
  const source = m ? m[1].toUpperCase() : "";
  const numericId = m ? Number(m[2]) : Number(idText);
  if (source === "DEVICE") {
    throw { message: "设备告警暂无 form 详情接口（可先使用 Demo 兜底）" };
  }
  if (!numericId || Number.isNaN(numericId)) {
    throw { message: "alertId 格式不正确" };
  }
  const data = await utils_request.request({
    url: "/alert/form",
    method: "GET",
    data: { doctorUsername: username, doctorPassword: password, alertId: numericId }
  });
  const patient = data && data.patient || {};
  const event = data && data.event || {};
  const abnormal = data && data.abnormalItems || [];
  const risk = mapRiskToUi(patient.riskLevel);
  const summary = abnormal.map((x) => `${x.name || ""}${x.currentValue != null ? `:${x.currentValue}` : ""}${x.unit ? x.unit : ""}`).filter(Boolean).join("；");
  return {
    patient: {
      id: patient.patientId,
      name: patient.name,
      age: "",
      gender: ""
    },
    task: {
      id: `alert-${numericId}`,
      typeText: "预警随访",
      planTime: fmtDateTime(event.createdAt),
      aiSummary: summary,
      riskLevel: risk.level,
      riskText: risk.text
    }
  };
}
function submitFollowup(data) {
  return utils_request.request({ url: "/staff/followups", method: "POST", data });
}
function getFollowupByTaskId(taskId) {
  return utils_request.request({ url: `/staff/followups/by-task/${encodeURIComponent(taskId)}`, method: "GET" });
}
function listPatients(params) {
  const q = {
    page: 1,
    pageSize: 200,
    ...params || {}
  };
  return utils_request.request({ url: "/api/patient/summary", method: "GET", data: q }).then((data) => {
    const rows = data && data.rows || [];
    return rows.map((r) => {
      const risk = mapRiskToUi(r.riskLevel);
      return {
        id: r.patientId ?? r.id,
        name: r.name,
        age: r.age,
        disease: r.mainDisease || r.disease || "—",
        constitution: "—",
        tcmPattern: r.syndrome || "—",
        riskLevel: risk.cn
      };
    });
  });
}
function getPatients(params) {
  return listPatients(params);
}
function getProfile() {
  return utils_request.request({ url: "/staff/profile", method: "GET" });
}
function updateProfile(data) {
  return utils_request.request({ url: "/staff/profile", method: "PUT", data });
}
function getStaffProfile() {
  return getProfile();
}
function updateStaffProfile(data) {
  return updateProfile(data);
}
function getPatientData(patientId, params) {
  const q = params || {};
  return utils_request.request({
    url: `/api/staff/patients/${encodeURIComponent(patientId)}/data`,
    method: "GET",
    data: q
  });
}
function sendPatientAdvice(patientId, content) {
  return utils_request.request({
    url: `/api/staff/patients/${encodeURIComponent(patientId)}/advice`,
    method: "POST",
    data: { content }
  });
}
exports.getAlertDetail = getAlertDetail;
exports.getAlerts = getAlerts;
exports.getFollowupByTaskId = getFollowupByTaskId;
exports.getPatientData = getPatientData;
exports.getPatients = getPatients;
exports.getStaffProfile = getStaffProfile;
exports.getTaskDetail = getTaskDetail;
exports.getTasks = getTasks;
exports.listPatients = listPatients;
exports.sendPatientAdvice = sendPatientAdvice;
exports.submitFollowup = submitFollowup;
exports.updateStaffProfile = updateStaffProfile;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/staff.js.map
