# 大屏首页重构同步文档（给 Cursor）

本次同步只覆盖大屏首页和首页加载壳层，不动第二页及后续舱页。

## 需要直接替换的文件

1. `test(vue)/bigScene/src/pages/CommandCenter.vue`
2. `test(vue)/bigScene/src/App.vue`

## 本次重构目标

- 首页骨架改为：顶部四张横向总量卡 + 左中右主体区 + 底部整行趋势区 + 最底部状态带
- 首页所有展示文案全部中文
- 左侧排行面板改为 DOM 信息面板，不再依赖旧的 ECharts 排行图
- 右侧改为“大处置环 + 双小环完成率”结构
- 事件流移到底部状态带
- 首页继续使用真实数据接口：
  - `fetchHomeStats`
  - `fetchMonthSummary`
  - `fetchPatientSummary`
  - `fetchAlerts`
  - `fetchHardwareAlerts`
  - `fetchPatientRiskList`
- 首页保留分级加载和 KeepAlive 生命周期处理
- App 壳层恢复为异步加载各舱页

## 说明

- 不需要回退旧版 `ScreenPanel / StatCard / EventTicker` 的首页用法
- 如果后续还要继续对齐 Stitch，只在首页内继续精修，不要把第二页以后一起改掉
- 若接口字段实际名称与页面里兜底逻辑不同，优先补字段映射，不要改回假数据
