<template>
  <view class="app-page msg">
    <view class="app-card app-card--glass msg-head">
      <view class="flex-row items-start justify-between">
        <view class="flex-col">
          <text class="msg-title">消息中心</text>
          <text class="msg-sub">医生建议与系统提醒会在这里统一汇总。</text>
        </view>
        <view class="msg-chip">
          <text class="msg-chip-text">{{ totalUnread }} 未读</text>
        </view>
      </view>
    </view>

    <view class="app-card msg-tabs">
      <view class="msg-tab" :class="tab === 'doctor' ? 'msg-tab--active' : ''" @tap="tab = 'doctor'">
        <text class="msg-tab-text" :class="tab === 'doctor' ? 'msg-tab-text--active' : ''">医生建议</text>
        <view v-if="doctorUnread > 0" class="msg-dot">
          <text class="msg-dot-text">{{ doctorUnread > 99 ? '99+' : doctorUnread }}</text>
        </view>
      </view>

      <view class="msg-tab" :class="tab === 'system' ? 'msg-tab--active' : ''" @tap="tab = 'system'">
        <text class="msg-tab-text" :class="tab === 'system' ? 'msg-tab-text--active' : ''">系统提醒</text>
        <view v-if="systemUnread > 0" class="msg-dot">
          <text class="msg-dot-text">{{ systemUnread > 99 ? '99+' : systemUnread }}</text>
        </view>
      </view>
    </view>

    <view class="msg-tools">
      <view class="msg-tool" @tap="markAllRead">
        <text class="msg-tool-text">全部已读</text>
      </view>
      <view class="msg-tool" @tap="clearRead">
        <text class="msg-tool-text">清空已读</text>
      </view>
    </view>

    <view class="app-card msg-list">
      <view v-if="currentList.length === 0" class="msg-empty">
        <text class="msg-empty-text">暂无消息</text>
        <text class="msg-empty-sub">医生建议会在医生端审核后下发；系统提醒来自任务、指标与设备状态。</text>
      </view>

      <view
        v-for="it in currentList"
        :key="it.id"
        class="msg-item"
        :class="it.read ? '' : 'msg-item--unread'"
        @tap="openMsg(it)"
      >
        <view class="flex-row items-center justify-between">
          <view class="flex-row items-center">
            <view class="msg-icon" :class="'icon-' + it.kind"></view>
            <text class="msg-item-title">{{ it.title }}</text>
          </view>
          <text class="msg-item-time">{{ it.time }}</text>
        </view>

        <text class="msg-item-preview">{{ it.preview }}</text>

        <view class="msg-item-foot flex-row items-center justify-between">
          <text class="msg-item-tag">{{ it.tag }}</text>
          <text class="msg-item-more">{{ it.read ? '查看' : '查看 · 未读' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { listMessages, markMessageRead, markAllMessagesRead } from '@/api/patient.js';
import { getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
import { getDailyMetricsLatest, parseBp } from '@/utils/metrics-store.js';

const DOCTOR_MESSAGE_TYPES = new Set(['DOCTOR', 'ADVICE', 'RECOMMENDATION']);

function uid() {
  return 'm_' + Date.now() + '_' + Math.floor(Math.random() * 10000);
}

function pad2(n) {
  return n < 10 ? '0' + n : '' + n;
}

function nowStr() {
  const d = new Date();
  return (
    d.getFullYear() +
    '-' +
    pad2(d.getMonth() + 1) +
    '-' +
    pad2(d.getDate()) +
    ' ' +
    pad2(d.getHours()) +
    ':' +
    pad2(d.getMinutes())
  );
}

function todayKey() {
  const d = new Date();
  return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
}

function clip(s, n) {
  const t = (s || '').toString().replace(/\s+/g, ' ').trim();
  return t.length > n ? t.slice(0, n) + '…' : t;
}

function safeNum(v) {
  if (v === null || v === undefined) return null;
  const s = String(v).trim();
  if (!s || s === '--') return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

function mergeListById(oldList, nextList) {
  const oldMap = new Map((Array.isArray(oldList) ? oldList : []).map((x) => [String(x.id), x]));
  return (Array.isArray(nextList) ? nextList : []).map((item) => {
    const old = oldMap.get(String(item.id));
    return old ? { ...item, read: !!old.read } : item;
  });
}

function getMsgType(it) {
  return String((it && (it.messageType || it.message_type || it.type || '')) || '')
    .trim()
    .toUpperCase();
}

function isDoctorMsg(it) {
  const t = getMsgType(it);
  return !t || DOCTOR_MESSAGE_TYPES.has(t);
}

function isReadMsg(it) {
  if (it && typeof it.read === 'boolean') return it.read;
  const status = String((it && it.status) || '')
    .trim()
    .toUpperCase();
  return status === 'READ';
}

function buildTaskReminder() {
  const taskState = getScopedStorageSync('rehab_tasks_today', null) || null
  const tasks = Array.isArray(taskState && taskState.tasks) ? taskState.tasks : []
  const currentMinutes = new Date().getHours() * 60 + new Date().getMinutes()
  const overdueTasks = tasks.filter((item) => {
    if (!item || item.status === 'done') return false
    if (item.overdue === true) return true
    const dueTime = String(item.dueTime || '')
    if (!/^\d{1,2}:\d{2}$/.test(dueTime)) return false
    const parts = dueTime.split(':')
    const dueMinutes = Number(parts[0]) * 60 + Number(parts[1])
    return currentMinutes > dueMinutes
  })
  if (overdueTasks.length > 0) {
    const t = nowStr()
    const pending = overdueTasks.length
    return {
      id: 'local_task_overdue_' + todayKey(),
      kind: 'system',
      title: '今日康养任务提醒',
      time: t,
      content:
        pending === 1
          ? '您今日仍有 1 项康养任务未完成，已超过建议完成时间。建议优先完成该项任务；若身体不适，请及时联系医生。'
          : `您今日仍有 ${pending} 项康养任务未完成，已超过建议完成时间。建议优先完成饮食或轻运动任务，并留意身体反应。`,
      preview:
        pending === 1
          ? '您今日仍有 1 项康养任务未完成，已超过建议完成时间。'
          : `您今日仍有 ${pending} 项康养任务未完成，已超过建议完成时间。`,
      tag: '任务提醒',
      read: false
    }
  }

  const summary =
    getScopedStorageSync('rehab_summary', null) ||
    getScopedStorageSync('followup_summary', null) ||
    null;
  if (!summary || typeof summary !== 'object') return null;

  const total = Number(summary.total || 0);
  const done = Number(summary.done || 0);
  const pending = Math.max(total - done, 0);
  if (pending <= 0) return null;

  const hour = new Date().getHours();
  if (hour < 18) return null;

  const t = nowStr();
  return {
    id: 'local_task_overdue_' + todayKey(),
    kind: 'system',
    title: '今日康养任务提醒',
    time: t,
    content:
      pending === 1
        ? '您今日仍有 1 项康养任务未完成，已超过建议完成时间。建议在身体允许情况下尽快补做，并从明天开始保持规律打卡。'
        : `您今日仍有 ${pending} 项康养任务未完成，已超过建议完成时间。建议优先完成耗时较短的任务，逐步恢复打卡节奏。`,
    preview:
      pending === 1
        ? '您今日仍有 1 项康养任务未完成，已超过建议完成时间。'
        : `您今日仍有 ${pending} 项康养任务未完成，已超过建议完成时间。`,
    tag: '任务提醒',
    read: false
  };
}

function buildMetricProblems() {
  const latest = getDailyMetricsLatest();
  const list = Array.isArray(latest && latest.metrics) ? latest.metrics : [];
  const map = {};
  list.forEach((m) => {
    if (m && m.key) map[m.key] = m;
  });

  const problems = [];

  const bp = map.bp;
  if (bp && bp.value && bp.value !== '--') {
    const { sbp, dbp } = parseBp(bp.value);
    if ((sbp != null && sbp >= 140) || (dbp != null && dbp >= 90)) {
      problems.push({
        key: 'bp',
        title: '血压偏高提醒',
        short: '血压',
        content:
          '系统检测到您最近一次血压偏高。建议先休息 10 分钟后再次测量，注意清淡饮食和避免情绪波动；若连续多次偏高，请联系医生。'
      });
    } else if ((sbp != null && sbp < 90) || (dbp != null && dbp < 60)) {
      problems.push({
        key: 'bp',
        title: '血压偏低提醒',
        short: '血压',
        content:
          '系统检测到您最近一次血压偏低。建议先休息观察，避免突然起身；如伴随头晕、乏力等不适，请及时就医或联系医生。'
      });
    }
  }

  const hr = safeNum(map.hr && map.hr.value);
  if (hr != null) {
    if (hr > 100) {
      problems.push({
        key: 'hr',
        title: '心率偏快提醒',
        short: '心率',
        content:
          '系统检测到您的心率偏快。建议先安静休息，避免剧烈活动和浓茶咖啡；若连续多次异常，请尽快咨询医生。'
      });
    } else if (hr < 50) {
      problems.push({
        key: 'hr',
        title: '心率偏慢提醒',
        short: '心率',
        content:
          '系统检测到您的心率偏慢。建议留意是否伴有头晕、胸闷等不适；如症状明显，请及时联系医生。'
      });
    }
  }

  const temp = safeNum((map.temp && map.temp.value) || null);
  if (temp != null) {
    if (temp >= 37.3) {
      problems.push({
        key: 'temp',
        title: '体温偏高提醒',
        short: '体温',
        content:
          '系统检测到您的体温偏高。建议注意休息、适量饮水，并继续观察体温变化；若持续升高或伴有明显不适，请及时就医。'
      });
    } else if (temp < 35.5) {
      problems.push({
        key: 'temp',
        title: '体温偏低提醒',
        short: '体温',
        content:
          '系统检测到您的体温偏低。建议注意保暖并重新测量；如反复偏低或伴有明显不适，请及时咨询医生。'
      });
    }
  }

  const spo2 = safeNum(map.spo2 && map.spo2.value);
  if (spo2 != null && spo2 < 95) {
    problems.push({
      key: 'spo2',
      title: '血氧偏低提醒',
      short: '血氧',
      content:
        '系统检测到您的血氧偏低。建议先静坐休息并再次测量；如连续偏低或伴有气短、胸闷等不适，请尽快就医。'
    });
  }

  const glucose = safeNum(map.glucose && map.glucose.value);
  if (glucose != null) {
    if (glucose > 7.8) {
      problems.push({
        key: 'glucose_high',
        title: '血糖偏高提醒',
        short: '血糖',
        content:
          '系统检测到您的血糖偏高。建议注意控制饮食，减少高糖食物摄入，并按时复测；若连续偏高，请联系医生。'
      });
    } else if (glucose < 3.9) {
      problems.push({
        key: 'glucose_low',
        title: '血糖偏低提醒',
        short: '血糖',
        content:
          '系统检测到您的血糖偏低。建议及时补充食物并注意观察身体反应；若出现心慌、出汗等情况，请尽快处理并联系医生。'
      });
    }
  }

  const sleep = safeNum(map.sleep && map.sleep.value);
  if (sleep != null && sleep < 6) {
    problems.push({
      key: 'sleep',
      title: '睡眠不足提醒',
      short: '睡眠',
      content:
        '系统检测到您最近睡眠时长偏少。建议尽量规律作息，减少熬夜，并关注近期精神状态和身体疲劳情况。'
    });
  }

  return problems;
}

function buildMetricReminder() {
  const problems = buildMetricProblems();
  if (!problems.length) return null;

  const t = nowStr();
  if (problems.length >= 2) {
    const names = problems
      .slice(0, 3)
      .map((x) => x.short)
      .join('、');
    return {
      id: 'local_metric_multi_' + todayKey(),
      kind: 'system',
      title: '多项指标异常提醒',
      time: t,
      content: `系统检测到您本次${names}存在异常。建议优先复测异常指标，并注意休息；若连续异常，请及时联系医生。`,
      preview: `系统检测到您本次${names}存在异常。建议优先复测异常指标。`,
      tag: '指标提醒',
      read: false
    };
  }

  const one = problems[0];
  return {
    id: 'local_metric_' + one.key + '_' + todayKey(),
    kind: 'system',
    title: one.title,
    time: t,
    content: one.content,
    preview: clip(one.content, 64),
    tag: '指标提醒',
    read: false
  };
}

function buildDeviceReminder() {
  const bound = getScopedStorageSync('bound_ble_device', null);
  const lastSync = getScopedStorageSync('device_last_sync_at', '');
  const t = nowStr();

  if (!bound) {
    return {
      id: 'local_device_unbound',
      kind: 'system',
      title: '设备绑定提醒',
      time: t,
      content:
        '您当前尚未绑定健康监测设备。绑定后可自动同步每日监测数据，便于在“身体数据”中查看最新指标。',
      preview: '您当前尚未绑定健康监测设备。绑定后可自动同步每日监测数据。',
      tag: '设备提醒',
      read: false
    };
  }

  if (!lastSync) {
    return {
      id: 'local_device_nosync',
      kind: 'system',
      title: '设备同步提醒',
      time: t,
      content: '设备已绑定，但今天还没有同步到新的监测数据。建议打开设备并保持蓝牙连接，完成一次测量。',
      preview: '设备已绑定，但今天还没有同步到新的监测数据。',
      tag: '同步提醒',
      read: false
    };
  }

  return {
    id: 'local_device_synced_' + todayKey(),
    kind: 'system',
    title: '今日监测已同步',
    time: t,
    content: '系统检测到今日监测数据已同步。您可前往“我的”或“健康档案”查看最新指标变化。',
    preview: '系统检测到今日监测数据已同步，可查看最新指标变化。',
    tag: '数据提醒',
    read: true
  };
}

function buildLocalSystemReminders() {
  const list = [];
  const taskMsg = buildTaskReminder();
  const metricMsg = buildMetricReminder();

  if (taskMsg) list.push(taskMsg);
  if (metricMsg) list.push(metricMsg);
  if (!taskMsg && !metricMsg) {
    const deviceMsg = buildDeviceReminder();
    if (deviceMsg) list.push(deviceMsg);
  }

  return list;
}

export default {
  data() {
    return {
      tab: 'doctor',
      doctorList: [],
      systemList: []
    };
  },

  computed: {
    doctorUnread() {
      return this.doctorList.filter((x) => !x.read).length;
    },
    systemUnread() {
      return this.systemList.filter((x) => !x.read).length;
    },
    totalUnread() {
      return this.doctorUnread + this.systemUnread;
    },
    currentList() {
      return this.tab === 'doctor' ? this.doctorList : this.systemList;
    }
  },

  onShow() {
    this.load();
  },

  methods: {
    load() {
      const d = getScopedStorageSync('message_doctor_list', []);
      const s = getScopedStorageSync('message_system_list', []);
      this.doctorList = Array.isArray(d) ? d : [];
      this.systemList = Array.isArray(s) ? s : [];

      const latest = getScopedStorageSync('doctor_advice_latest', null);
      if (latest && typeof latest === 'object') {
        const title = latest.title || '医生健康建议';
        const content = latest.content || '';
        const time = latest.time || nowStr();
        const key = 'doctor_advice_latest_time';
        const prev = getScopedStorageSync(key, '');
        if (time && time !== prev) {
          setScopedStorageSync(key, time);
          this.doctorList.unshift({
            id: uid(),
            kind: 'doctor',
            title,
            time,
            content,
            preview: clip(content, 64) || '点击查看医生建议详情。',
            tag: '医生建议',
            read: false
          });
        }
      }

      this.doctorList = mergeListById(d, this.doctorList);
      this.systemList = mergeListById(this.systemList, buildLocalSystemReminders());
      this.persist();
      this.fetchRemote();
    },

    fetchRemote() {
      listMessages()
        .then((res) => {
          const all = Array.isArray(res) ? res : (res && res.list) || [];

          const normalize = (arr, kind) =>
            (Array.isArray(arr) ? arr : []).map((it) => ({
              id: it.id,
              kind,
              title: it.title || (kind === 'doctor' ? '医生建议' : '系统提醒'),
              time: it.time || it.createdAt || it.updatedAt || nowStr(),
              content: it.content || '',
              preview: it.preview || clip(it.content, 64) || '点击查看详情',
              tag: kind === 'doctor' ? (it.tag || '医生建议') : (it.tag || '系统提醒'),
              read: isReadMsg(it)
            }));

          const doctorRemote = normalize(all.filter(isDoctorMsg), 'doctor');
          const systemRemote = normalize(all.filter((it) => !isDoctorMsg(it)), 'system');
          const localSystem = buildLocalSystemReminders();

          this.doctorList = mergeListById(this.doctorList, doctorRemote);
          this.systemList = mergeListById(this.systemList, [...systemRemote, ...localSystem]);

          this.persist();
        })
        .catch(() => {
          this.systemList = mergeListById(this.systemList, buildLocalSystemReminders());
          this.persist();
        });
    },

    persist() {
      setScopedStorageSync('message_doctor_list', this.doctorList);
      setScopedStorageSync('message_system_list', this.systemList);
    },

    openMsg(it) {
      if (!it.read) {
        it.read = true;
        this.persist();
        if (it.id && !String(it.id).startsWith('local_')) {
          markMessageRead(it.id).catch(() => {});
        }
      }

      uni.showModal({
        title: it.title,
        content: it.content || it.preview || '',
        showCancel: false,
        confirmText: '知道了'
      });
    },

    markAllRead() {
      const list = this.tab === 'doctor' ? this.doctorList : this.systemList;
      list.forEach((x) => {
        x.read = true;
      });
      this.persist();

      if (this.tab === 'doctor') {
        markAllMessagesRead({ type: 'doctor' }).catch(() => {
          if (!USE_MOCK_FALLBACK) uni.showToast({ title: '后端同步失败', icon: 'none' });
        });
      }

      uni.showToast({ title: '已全部标记', icon: 'success' });
    },

    clearRead() {
      if (this.tab === 'doctor') {
        this.doctorList = this.doctorList.filter((x) => !x.read);
      } else {
        this.systemList = this.systemList.filter((x) => !x.read);
      }
      this.persist();
      uni.showToast({ title: '已清空已读', icon: 'success' });
    }
  }
};
</script>

<style src="./message.css"></style>