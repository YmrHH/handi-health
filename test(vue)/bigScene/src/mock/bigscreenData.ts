export type BigscreenEventItem = { id: string | number; title: string; time: string }

export type BigscreenDemoData = {
  meta: {
    orgName: string
    updatedAt: string
  }
  overview: {
    patientsTotal: number
    managedTotal: number
    todayNew: number
    weekNew: number
    risk: { high: number; mid: number; low: number }
    alertsToday: { total: number; handled: number; pending: number }
    followToday: { total: number; done: number; pending: number; overdue: number }
  }
  diseasesTop: Array<{ name: string; count: number }>
  tcm: {
    constitution: Array<{ name: string; count: number }>
    syndrome: Array<{ name: string; count: number }>
    symptomHot: Array<{ name: string; score: number }>
  }
  trends: {
    last7Days: Array<{ day: string; alerts: number; followups: number; highRisk: number }>
    last12Months: Array<{ month: string; alerts: number; highRisk: number }>
  }
  vitals: {
    items: Array<{ name: string; unit: string; value: number; status: 'normal' | 'warn' | 'danger' }>
  }
  doctorsTop: Array<{ name: string; tasks: number }>
  lists: {
    recentEvents: BigscreenEventItem[]
    recentAlerts: Array<{ id: string; patient: string; type: string; level: '高' | '中' | '低'; time: string; status: '待处理' | '处理中' | '已关闭' }>
    recentFollowups: Array<{ id: string; patient: string; type: string; time: string; result: string; risk: '高' | '中' | '低' }>
    aiInsights: Array<{ id: string; title: string; content: string; tag: string }>
    interventions: Array<{ id: string; patient: string; plan: string; status: '待执行' | '执行中' | '已完成'; time: string }>
  }
}

function pad2(n: number) {
  return String(n).padStart(2, '0')
}

function lastNDays(n: number) {
  const out: string[] = []
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(Date.now() - i * 86400000)
    out.push(`${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`)
  }
  return out
}

function last12Months() {
  const d = new Date()
  const out: string[] = []
  for (let i = 11; i >= 0; i--) {
    const t = new Date(d.getFullYear(), d.getMonth() - i, 1)
    out.push(`${t.getFullYear()}-${pad2(t.getMonth() + 1)}`)
  }
  return out
}

export function getBigscreenDemoData(): BigscreenDemoData {
  const days = lastNDays(7)
  const months = last12Months()

  // 让趋势“有波动”，避免完全平直
  const daySeries = days.map((day, idx) => {
    const baseA = [24, 28, 19, 33, 26, 37, 29][idx] ?? 24
    const baseF = [46, 40, 52, 48, 44, 57, 50][idx] ?? 48
    const baseH = [82, 86, 84, 88, 90, 86, 92][idx] ?? 86
    return { day, alerts: baseA, followups: baseF, highRisk: baseH }
  })

  const monthSeries = months.map((m, i) => {
    const alerts = [132, 148, 125, 156, 171, 160, 182, 176, 165, 190, 204, 196][i] ?? 160
    const highRisk = [78, 82, 86, 84, 90, 92, 88, 94, 96, 98, 92, 100][i] ?? 86
    return { month: m, alerts, highRisk }
  })

  const demo: BigscreenDemoData = {
    meta: {
      orgName: '默认组织',
      updatedAt: '刚刚'
    },
    overview: {
      patientsTotal: 1286,
      managedTotal: 1234,
      todayNew: 12,
      weekNew: 86,
      risk: { high: 86, mid: 214, low: 986 },
      alertsToday: { total: 37, handled: 25, pending: 12 },
      followToday: { total: 48, done: 31, pending: 13, overdue: 4 }
    },
    diseasesTop: [
      { name: '高血压', count: 412 },
      { name: '2型糖尿病', count: 286 },
      { name: '冠心病', count: 214 },
      { name: '慢阻肺', count: 163 },
      { name: '脑卒中后', count: 98 }
    ],
    tcm: {
      constitution: [
        { name: '痰湿质', count: 268 },
        { name: '气虚质', count: 224 },
        { name: '阴虚质', count: 198 },
        { name: '血瘀质', count: 172 },
        { name: '平和质', count: 368 }
      ],
      syndrome: [
        { name: '气阴两虚', count: 246 },
        { name: '痰湿阻络', count: 212 },
        { name: '肝阳上亢', count: 186 },
        { name: '瘀血内阻', count: 162 },
        { name: '脾肾阳虚', count: 128 }
      ],
      symptomHot: [
        { name: '头晕', score: 92 },
        { name: '胸闷', score: 84 },
        { name: '乏力', score: 78 },
        { name: '失眠', score: 74 },
        { name: '口干', score: 69 },
        { name: '咳嗽', score: 63 }
      ]
    },
    trends: {
      last7Days: daySeries,
      last12Months: monthSeries
    },
    vitals: {
      items: [
        { name: '收缩压', unit: 'mmHg', value: 142, status: 'warn' },
        { name: '舒张压', unit: 'mmHg', value: 92, status: 'warn' },
        { name: '血糖', unit: 'mmol/L', value: 7.8, status: 'warn' },
        { name: '血氧', unit: '%', value: 96, status: 'normal' },
        { name: '心率', unit: 'bpm', value: 88, status: 'normal' },
        { name: '体温', unit: '°C', value: 36.6, status: 'normal' },
        { name: '睡眠', unit: 'h', value: 6.3, status: 'warn' }
      ]
    },
    doctorsTop: [
      { name: '王医生', tasks: 38 },
      { name: '林护士', tasks: 34 },
      { name: '郭医生', tasks: 31 },
      { name: '孙医生', tasks: 28 },
      { name: '陈护士', tasks: 26 }
    ],
    lists: {
      recentEvents: [
        { id: 'e1', title: '告警 · 张某某 · 血压连续3次偏高', time: `${days[6]} 09:12` },
        { id: 'e2', title: '随访 · 李某某 · 用药依从性下降', time: `${days[6]} 10:05` },
        { id: 'e3', title: '服务 · 王某某 · 上门评估已指派', time: `${days[6]} 11:20` },
        { id: 'e4', title: '告警 · 赵某某 · 血糖波动异常', time: `${days[6]} 13:48` },
        { id: 'e5', title: '随访 · 刘某某 · 复查提醒已下发', time: `${days[6]} 15:06` },
        { id: 'e6', title: 'AI 洞察 · 高危共性：夜间血压升高', time: `${days[6]} 16:30` },
        { id: 'e7', title: '告警 · 周某某 · 血氧下降（设备）', time: `${days[6]} 18:02` },
        { id: 'e8', title: '随访 · 孙某某 · 情绪评估需关注', time: `${days[6]} 19:10` }
      ],
      recentAlerts: [
        { id: 'a1', patient: '张某某', type: '血压异常', level: '高', time: `${days[6]} 09:12`, status: '待处理' },
        { id: 'a2', patient: '赵某某', type: '血糖异常', level: '中', time: `${days[6]} 13:48`, status: '处理中' },
        { id: 'a3', patient: '周某某', type: '血氧异常（设备）', level: '中', time: `${days[6]} 18:02`, status: '待处理' },
        { id: 'a4', patient: '王某某', type: '心率偏高', level: '低', time: `${days[5]} 16:40`, status: '已关闭' },
        { id: 'a5', patient: '李某某', type: '体温异常', level: '低', time: `${days[5]} 11:18`, status: '已关闭' }
      ],
      recentFollowups: [
        { id: 'f1', patient: '李某某', type: '电话随访', time: `${days[6]} 10:05`, result: '提醒用药并复查', risk: '中' },
        { id: 'f2', patient: '刘某某', type: '线上随访', time: `${days[6]} 15:06`, result: '复查提醒下发', risk: '低' },
        { id: 'f3', patient: '孙某某', type: '电话随访', time: `${days[6]} 19:10`, result: '建议情绪评估与睡眠管理', risk: '中' },
        { id: 'f4', patient: '张某某', type: '上门随访', time: `${days[5]} 14:22`, result: '血压管理方案调整', risk: '高' }
      ],
      aiInsights: [
        { id: 'i1', title: '高危共性', content: '近7日高危人群夜间血压上升明显，建议加强晚间监测与用药提醒。', tag: '风险' },
        { id: 'i2', title: '异常波动原因', content: '血糖波动多出现在餐后2小时段，建议结合饮食记录与运动处方进行干预。', tag: '监测' },
        { id: 'i3', title: '随访优先级', content: '逾期任务集中在慢阻肺与冠心病患者，优先安排电话随访并核对用药依从性。', tag: '随访' },
        { id: 'i4', title: '中医辨证提示', content: '痰湿质与气阴两虚人群占比偏高，建议增加运动与睡眠管理的综合干预。', tag: '中医' }
      ],
      interventions: [
        { id: 's1', patient: '王某某', plan: '健康评估 + 用药指导', status: '执行中', time: `${days[6]} 11:20` },
        { id: 's2', patient: '张某某', plan: '血压管理方案调整', status: '待执行', time: `${days[6]} 14:10` },
        { id: 's3', patient: '赵某某', plan: '饮食与运动处方', status: '待执行', time: `${days[6]} 16:05` },
        { id: 's4', patient: '刘某某', plan: '复查提醒 + 随访回访', status: '已完成', time: `${days[5]} 09:30` }
      ]
    }
  }

  return demo
}

