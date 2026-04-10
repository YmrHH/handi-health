# 智能随访后端接口文档

## 基础信息

- 服务地址：`http://localhost:8081`
- 端口配置：`src/main/resources/application.properties` 中 `server.port=8081`
- 跨域（CORS）：已允许 `http://localhost:8080`（见 `ZhinengsuifangApplication#corsConfigurer`）

## 通用响应结构（多数接口）

多数接口返回 `Map<String,Object>`，常见字段：

- `success`：`true/false`
- `code`：数字错误码（见文末 ApiCode）
- `message`：提示信息
- `data`：数据体（部分接口无此字段）

> 少数接口直接返回实体或字符串：
>
>- `GET /user` 返回 `List<User>`

---

## 14. AI 分析看板 / 报表 Board & Export

### 14.1 AI 分析看板数据

- 方法：`GET`
- 路径：`/api/report/board`

请求参数（Query）：

- `days`：integer，可选（默认 180，用于趋势窗口）

响应：`Map<String,Object>`（`data` 为看板数据结构）

说明：

- `latestMonth` 为 `yyyy-MM`（例如 `2026-01`）
- `latestNewPatients/latestAlerts/latestFollowups` 为“本月”口径统计
- `alertChangeRate` 为（本月告警数 - 上月告警数）/ 上月告警数（上月为 0 时返回 0）
- 趋势 `months/alertsArr/followArr/highArr/otherArr` 为按月聚合，数组长度一致
- `diseaseAnalysis` 当前为简化结构：至少包含 `disease/patientCount/stableRate/improvementRate/deteriorationRate`，其余复杂字段先返回空数组，后续可逐步丰富

部分 AI 指标字段（`latestAuc/latestF1`）目前返回 0 占位，后续可接入真实模型评估。

### 14.2 报表导出

- 方法：`POST`
- 路径：`/api/report/export`
- Content-Type：`application/json`

请求体字段：

- `reportType`：string，必填（如 `patientList` / `followup` / `alert` / `diseaseSummary`）

响应：`Map<String,Object>`（`data.fileName` + `data.downloadUrl`）

### 14.3 下载导出文件

- 方法：`GET`
- 路径：`/api/report/download/{id}`

说明：当前实现为最小闭环版（内存临时存储 CSV），重启服务后下载链接会失效。

---

## 15. 系统与基础配置 System

### 15.1 系统规则

- 方法：`GET`
- 路径：`/api/system/rules`

响应：`Map<String,Object>`（`data.seasonRules[]`）

### 15.2 医生建议

- 方法：`GET`
- 路径：`/api/system/doctor-advice`

请求参数（Query）：

- `page` / `pageSize`：可选
- `disease`：可选
- `tag`：可选

### 15.3 系统分析汇总

- 方法：`GET`
- 路径：`/api/system/analysis-summary`

### 15.4 系统修改日志

- 方法：`GET`
- 路径：`/api/system/change-logs`

请求参数（Query）：

- `page` / `pageSize`：可选
- `module`：可选
- `operatorId`：可选
- `startAt` / `endAt`：可选

---

## 0. 首页 Home

### 0.1 首页统计卡片

- 方法：`GET`
- 路径：`/api/home/stats`
- 请求参数：无

响应：`Map<String,Object>`（`success/code/message/data`）

`data` 字段：

- `totalPatients`：在管患者总数（role = `PATIENT`）
- `totalPatientsChange`：本周新增患者数较上周的差值文本（如 `+12人` / `-5人` / `0人`）
- `weekFollowRate`：本周随访完成率（0–1）
- `weekFollowDone`：本周完成随访人数
- `processingAlerts`：当前“处理中/待处理”告警数（统计 `health_alert` + `device_alert`，status in `NEW/IN_PROGRESS/FOLLOWUP_CREATED`）
- `redAlerts`：当前红色/高危告警数（在 processingAlerts 的口径基础上，severity in `RED/HIGH`）

示例：

```http
GET /api/home/stats
```

---

## 数据库更新（MySQL）

如果你的数据库中 `user` 表已存在但没有 `created_at/updated_at` 字段，需要先执行：

```sql
ALTER TABLE user
  ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
```

## 1. 认证 Auth

### 1.1 注册

- 方法：`POST`
- 路径：`/auth/register`
- Content-Type：`application/json`
- 请求体：`RegisterRequest`

字段：

- `username`：string，必填
- `password`：string，必填
- `confirmPassword`：string，必填（必须与 `password` 相同）
- `phone`：string，可选

说明：

- 该接口用于 **Web 端医生注册**。
- 后端会 **自动将 role 固定为 `DOCTOR`**（请求体不需要也不支持传 role）。

响应：`Map<String,Object>`（`success/code/message`）

示例：

```http
POST /auth/register
Content-Type: application/json

{
  "username": "test001",
  "password": "123456",
  "confirmPassword": "123456",
  "phone": "13800000000"
}
```

### 1.2 医生创建用户（患者/随访员）

- 方法：`POST`
- 路径：`/auth/createUserByDoctor`
- Content-Type：`application/json`
- 请求体：`CreateUserByDoctorRequest`

字段：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `username`：string，必填
- `name`：string，必填
- `address`：string，可选
- `age`：integer，可选
- `sex`：string，可选
- `phone`：string，必填
- `riskLevel`：string，可选（当 `role=PATIENT` 时必填，取值约束以服务实现为准）
- `role`：string，必填（`PATIENT` / `FOLLOW_UP`）

响应：`Map<String,Object>`（常见 `success/code/message/userId`）

### 1.3 登录

- 方法：`POST`
- 路径：`/auth/login`
- Content-Type：`application/json`
- 请求体：`LoginRequest`

字段：

- `username`：string，必填
- `password`：string，必填

响应：`Map<String,Object>`（常见 `success/code/message/userId/role/name`）

说明：

- 小程序侧只需要使用该登录接口，不需要使用注册接口。

---

## 全局异常与错误码说明

### 全局异常处理（GlobalExceptionHandler）

项目通过 `GlobalExceptionHandler` 统一将常见异常转换为 `Map<String,Object>`，返回结构同上（包含 `success/code/message`）。

常见映射：

- `MissingServletRequestParameterException` -> `ApiCode.PARAM_ERROR`
- `HttpMessageNotReadableException` -> `ApiCode.PARAM_ERROR`
- `MethodArgumentTypeMismatchException` -> `ApiCode.PARAM_ERROR`
- `MethodArgumentNotValidException` / `BindException` -> `ApiCode.VALIDATION_ERROR`
- 其他未捕获异常 -> `ApiCode.INTERNAL_ERROR`

### ApiCode 列表

- `SUCCESS` = `0`
- `PARAM_ERROR` = `40001`
- `UNAUTHORIZED` = `40101`
- `FORBIDDEN` = `40301`
- `NOT_FOUND` = `40401`
- `CONFLICT` = `40901`
- `VALIDATION_ERROR` = `42201`
- `INTERNAL_ERROR` = `50000`

## 2. 用户 User

### 2.1 查询所有用户

- 方法：`GET`
- 路径：`/user`
- 请求参数：无
- 响应：`List<User>`

### 2.2 新增用户

说明：用户创建请使用 `POST /auth/createUserByDoctor`（医生创建患者/随访员账号），避免绕过业务校验。

### 2.3 查询患者总数

- 方法：`GET`
- 路径：`/user/patient/count`
- 请求参数：无
- 响应：`Map<String,Object>`（`data` 为患者数量）

### 2.4 查询患者风险等级统计

- 方法：`GET`
- 路径：`/user/patient/risk-stats`
- 请求参数：无
- 响应：`Map<String,Object>`（`data` 为统计数据）

---

## 9. 患者 Patient（管理端）

### 9.1 患者列表（分页 + 搜索 + 风险筛选）

- 方法：`GET`
- 路径：`/api/patient/summary`

请求参数（Query）：

- `page`：integer，可选，默认 1
- `pageSize`：integer，可选，默认 10，仅支持 1-200（超出范围直接返回参数错误）
- `keyword`：string，可选（模糊匹配姓名/手机号/账号；仅支持中文/字母/数字；最多 30 位；纯数字至少 4 位）
- `riskLevel`：string，可选（匹配 `risk_level`，不区分大小写，仅支持 `HIGH/MID/LOW`）

响应：`Map<String,Object>`（`data.total` + `data.rows[]`）

`rows[]` 字段：

- `id`
- `name`
- `gender`
- `age`
- `mainDisease`（当前版本为 null）
- `syndrome`（当前版本为 null）
- `riskLevel`
- `doctorName`（当前版本为空字符串）
- `lastFollowTime`（来自 `follow_up_schedule` 的 `MAX(due_at)`）
- `status`（当前版本固定为 `在管`）

示例：

```http
GET /api/patient/summary?page=1&pageSize=10&keyword=张&riskLevel=HIGH
```

### 9.2 患者详情（档案与画像分块）

- 方法：`GET`
- 路径：`/api/patient/detail/{id}`

响应：`Map<String,Object>`（`data` 分块）

`data`：

- `basicInfo`
- `diagnosis`
- `extraInfo`
- `profile`
- `riskSummary`

其中：

- `riskSummary.lastFollowTime`：来自 `follow_up_schedule` 的 `MAX(due_at)`

示例：

```http
GET /api/patient/detail/1
```

---

## 3. 候选患者 Candidate

### 3.1 医生查询候选患者列表

- 方法：`GET`
- 路径：`/candidate/patients`

请求参数（Query）：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填

响应：`Map<String,Object>`

---

## 4. 健康指标 HealthMetric

### 4.1 上报健康指标

- 方法：`POST`
- 路径：`/healthMetric/add`
- Content-Type：`application/json`
- 请求体：`HealthMetric`
- 响应：`Map<String,Object>`

### 4.2 医生查询患者最新指标

- 方法：`GET`
- 路径：`/healthMetric/doctor/patient/latest`

请求参数（Query）：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `patientId`：long，必填
- `limit`：integer，可选，默认 50，最大 200

说明：仅校验医生账号密码与患者存在（role=PATIENT），不再要求该医生与患者已建立随访任务关联。

响应：`Map<String,Object>`

### 4.3 医生按时间范围查询患者指标

- 方法：`GET`
- 路径：`/healthMetric/doctor/patient/query`

请求参数（Query）：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `patientId`：long，必填
- `metricType`：string，可选（如 `BP/HR/PULSE/SPO2`；为空表示不过滤类型）
- `startAt`：datetime，必填（ISO-8601，不带时区，例如 `2026-01-21T10:00:00`）
- `endAt`：datetime，必填（ISO-8601，不带时区，例如 `2026-01-21T12:00:00`）

说明：仅校验医生账号密码与患者存在（role=PATIENT），不再要求该医生与患者已建立随访任务关联。

## 5. 健康预警 Alert

### 5.1 医生查询预警列表

- 方法：`GET`
- 路径：`/alert/list`

请求参数（Query）：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `status`：string，可选

响应：`Map<String,Object>`

### 5.2 根据预警生成随访单

- 方法：`POST`
- 路径：`/alert/createFollowUp`
- Content-Type：`application/json`
- 请求体：`CreateFollowUpFromAlertRequest`

字段：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `alertId`：long，必填
- `followUpUserId`：long，必填
- `description`：string，可选
- `dueAt`：datetime，必填（格式 `yyyy-MM-dd HH:mm:ss`，GMT+8）

响应：`Map<String,Object>`

### 5.3 医生标记预警状态

- 方法：`POST`
- 路径：`/alert/mark`
- Content-Type：`application/json`
- 请求体：`MarkHealthAlertRequest`

字段：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `alertId`：long，必填
- `status`：string，必填

响应：`Map<String,Object>`

---

## 10. 告警中心 Alert Center（管理端）

### 10.1 告警列表（分页 + 筛选）

- 方法：`GET`
- 路径：`/api/alert/list`

请求参数（Query）：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `page`：integer，可选，默认 1
- `pageSize`：integer，可选，默认 10，最大 200
- `keyword`：string，可选（模糊匹配患者姓名/手机号）
- `level`：string，可选（告警级别，对应表字段 severity，例如 `RED/HIGH` 等）
- `status`：string，可选（告警状态，例如 `NEW/IN_PROGRESS/READ/FOLLOWUP_CREATED/REVIEWED/IGNORED` 等）
- `source`：string，可选（告警来源：`HEALTH` 或 `DEVICE`）
- `patientId`：long，可选（按患者过滤）
- `startAt`：datetime，可选
- `endAt`：datetime，可选

响应：`Map<String,Object>`（`data.total` + `data.rows[]`）

`rows[]` 字段：

- `id`：字符串（形如 `HEALTH-123` / `DEVICE-456`，用于前端展示唯一行）
- `source`：`HEALTH` / `DEVICE`
- `alertId`：告警 id（数值）
- `patientId`
- `patientName`
- `mainDisease`（当前版本为 null）
- `riskLevel`
- `level`
- `alertTime`
- `status`
- `doctorName`（当前版本为空字符串）
- `handlerName`（当前版本为空字符串）
- `summary`（简要原因）

示例：

```http
GET /api/alert/list?doctorUsername=doc1&doctorPassword=123456&page=1&pageSize=10&status=NEW
```

### 10.2 标记已读

- 方法：`POST`
- 路径：`/api/alert/mark-read`
- Content-Type：`application/json`

请求体字段：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `source`：string，必填（`HEALTH` / `DEVICE`）
- `id`：long，必填（告警 id）

说明：当前实现为把对应记录的 `status` 更新为 `READ`。

### 10.3 更新处理状态

- 方法：`POST`
- 路径：`/api/alert/update-status`
- Content-Type：`application/json`

请求体字段：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `source`：string，必填（`HEALTH` / `DEVICE`）
- `id`：long，必填（告警 id）
- `newStatus`：string，必填（会被转为大写写入数据库）

---

## 6. 随访任务 FollowUpTask

## 11. 随访工作台 FollowUp Workbench（管理端）

### 11.0 鉴权说明（推荐）

- 推荐使用 Header：`Authorization: Bearer <userId>`
- 当 Header 可解析出 `userId` 且角色为：
  - `DOCTOR`：接口会自动将 `doctorId` 固定为该医生 id
  - `FOLLOW_UP`：可用于随访员的任务状态变更接口（start/complete/reschedule）
  - `ADMIN`：可用于列表查询（不会自动覆盖 doctorId）
- 为兼容旧版本调用，部分接口仍支持 `doctorUsername/doctorPassword` 或 `followUpUsername/followUpPassword` 回退校验。

### 11.1 随访任务列表（工作台）

- 方法：`GET`
- 路径：`/api/followup/tasks`

请求参数（Query）：

- `doctorUsername`：string，可选（当未使用 Header 鉴权时必填）
- `doctorPassword`：string，可选（当未使用 Header 鉴权时必填）
- `page`：integer，可选，默认 1
- `pageSize`：integer，可选，默认 10，仅支持 1-200（超出范围直接返回参数校验失败）
- `status`：string，可选（任务状态，来自 `follow_up_task.status`，如 `ASSIGNED/IN_PROGRESS/COMPLETED/CANCELED`）
- `staffId`：long，可选（按随访员过滤）
- `doctorId`：long，可选（按医生过滤）
- `riskLevel`：string，可选（按患者风险等级过滤）
- `startAt`：datetime，可选（按计划随访时间 `due_at` 起始过滤；建议 ISO-8601，例如 `2026-01-01T10:00:00`）
- `endAt`：datetime，可选（按计划随访时间 `due_at` 结束过滤；建议 ISO-8601，例如 `2026-01-31T23:59:59`）

响应：`Map<String,Object>`（`data.total` + `data.rows[]`）

`rows[]` 字段：

- `taskId`
- `patientId`
- `patientName`
- `riskLevel`
- `mainDisease`（当前版本为 null）
- `planTime`（来自 `follow_up_schedule.due_at`）
- `finishTime`（来自 `follow_up_schedule.completed_at`）
- `followMethod`（当前版本为 null）
- `status`
- `staffId`
- `staffName`

示例：

```http
GET /api/followup/tasks?doctorUsername=doc1&doctorPassword=123456&page=1&pageSize=10&status=ASSIGNED
```

### 11.1.1 随访列表（旧版兼容）

- 方法：`GET`
- 路径：`/api/followup/list`

请求参数（Query）：

- `pageNo`：integer，可选，默认 1
- `pageSize`：integer，可选，默认 10（最大 200）
- `status`：string，可选
- `staffId`：long，可选
- `doctorId`：long，可选
- `riskLevel`：string，可选

响应：`Map<String,Object>`（`data.rows[]/pageNo/totalPages/total/stats`）。

说明：该接口返回字段为前端旧版工作台适配结构（例如 `id/followupDate/patientName/resultStatus` 等）。

### 11.1.2 随访详情（占位）

- 方法：`GET`
- 路径：`/api/followup/detail`

请求参数（Query）：

- `id`：long，可选（当前实现未使用）

响应：`Map<String,Object>`（当前实现返回占位结构：`record/patient/tcmPulseTypeList`）。

### 11.2 开始随访

- 方法：`POST`
- 路径：`/api/followup/task/start`
- Content-Type：`application/json`

请求体字段：

- `taskId`：long，必填
- `followUpUsername`：string，可选（当未使用 Header 鉴权时必填）
- `followUpPassword`：string，可选（当未使用 Header 鉴权时必填）

说明：

- 将 `follow_up_task.status` 更新为 `IN_PROGRESS`。
- 只能操作分配给自己的任务（`task.follow_up_user_id` 必须等于当前随访员 id）。

响应：`Map<String,Object>`（`success/code/message`，无 `data`）

示例：

```http
POST /api/followup/task/start
Content-Type: application/json

{
  "taskId": 1,
  "followUpUsername": "followup001",
  "followUpPassword": "123456"
}
```

### 11.3 完成随访

- 方法：`POST`
- 路径：`/api/followup/task/complete`
- Content-Type：`application/json`

请求体字段：

- `taskId`：long，必填
- `followUpUsername`：string，可选（当未使用 Header 鉴权时必填）
- `followUpPassword`：string，可选（当未使用 Header 鉴权时必填）

说明：完成后会同步将 `follow_up_schedule` 中该任务对应记录标记为 `COMPLETED` 并写入 `completed_at`。

响应：`Map<String,Object>`（`success/code/message`，无 `data`）

示例：

```http
POST /api/followup/task/complete
Content-Type: application/json

{
  "taskId": 1,
  "followUpUsername": "followup001",
  "followUpPassword": "123456"
}
```

### 11.4 重新预约（改期）

- 方法：`POST`
- 路径：`/api/followup/task/reschedule`
- Content-Type：`application/json`

请求体字段：

- `taskId`：long，必填
- `followUpUsername`：string，可选（当未使用 Header 鉴权时必填）
- `followUpPassword`：string，可选（当未使用 Header 鉴权时必填）
- `dueAt`：string，必填（ISO-8601 格式，例如 `2025-01-01T10:00:00`；后端使用 `LocalDateTime.parse` 解析）

说明：

- 将 `follow_up_schedule` 中对应任务（`related_task_id = taskId`）的 `due_at` 更新为新的时间，并将计划状态更新为 `DUE`。
- 只能操作分配给自己的任务。

响应：`Map<String,Object>`（`success/code/message`，无 `data`）

示例：

```http
POST /api/followup/task/reschedule
Content-Type: application/json

{
  "taskId": 1,
  "followUpUsername": "followup001",
  "followUpPassword": "123456",
  "dueAt": "2025-01-01T10:00:00"
}
```

---

## 12. 随访人员 FollowUp Staff（管理端）

### 12.1 随访人员列表

- 方法：`GET`
- 路径：`/api/followup/staff`

请求参数（Query）：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `page`：integer，可选，默认 1
- `pageSize`：integer，可选，默认 10，最大 200
- `keyword`：string，可选（模糊匹配姓名/手机号/账号）
- `status`：integer，可选（0=停用，1=启用）

响应：`Map<String,Object>`（`data.total` + `data.rows[]`）

`rows[]` 字段：

- `id`
- `name`
- `jobNo`（当前版本取 user.username）
- `mobile`
- `orgName`（当前版本为空字符串）
- `deptName`（当前版本为空字符串）
- `role`（固定 `FOLLOW_UP`）
- `status`
- `currentTaskCount`（ASSIGNED/IN_PROGRESS 的任务数量）

### 12.2 注册/新增随访人员

- 方法：`POST`
- 路径：`/api/followup/staff`
- Content-Type：`application/json`

请求体字段：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `username`：string，必填（新账号）
- `name`：string，必填
- `phone`：string，必填
- `address`：string，可选
- `age`：integer，可选
- `sex`：string，可选

说明：该接口复用 `POST /auth/createUserByDoctor` 的创建逻辑，role 固定为 `FOLLOW_UP`，默认密码为 `123456`。

示例：

```http
POST /api/followup/staff
Content-Type: application/json

{
  "doctorUsername": "doctor001",
  "doctorPassword": "123456",
  "username": "followup001",
  "name": "李四",
  "phone": "13900000000"
}
```

### 6.1 医生创建随访任务（派单）

## 13. 复查计划 Visit Plan（管理端）

### 13.1 复查计划列表

- 方法：`GET`
- 路径：`/api/intervention/visit-plan`

请求参数（Query）：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `page`：integer，可选，默认 1
- `pageSize`：integer，可选，默认 10，最大 200
- `status`：string，可选（当前版本复用 `follow_up_schedule.status`）
- `startDate`：datetime，可选（按 due_at 起始过滤）
- `endDate`：datetime，可选（按 due_at 结束过滤）
- `keyword`：string，可选（模糊匹配患者姓名/手机号）

响应：`Map<String,Object>`（`data.total` + `data.plans[]`）

`plans[]` 字段：

- `id`（计划ID，当前版本复用 `follow_up_schedule.id`）
- `patientId`
- `patientName`
- `visitDate`（当前版本复用 `follow_up_schedule.due_at`）
- `visitType`（当前版本为 null）
- `doctorName`（当前版本为空字符串）
- `status`
- `remark`（当前版本为空字符串）

示例：

```http
GET /api/intervention/visit-plan?doctorUsername=doctor001&doctorPassword=123456&page=1&pageSize=10&status=DUE
```

### 13.2 更新复查计划状态

- 方法：`POST`
- 路径：`/api/intervention/visit-plan/update-status`
- Content-Type：`application/json`

请求体字段：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `planId`：long，必填（当前版本为 follow_up_schedule.id）
- `newStatus`：string，必填

示例：

```http
POST /api/intervention/visit-plan/update-status
Content-Type: application/json

{
  "doctorUsername": "doctor001",
  "doctorPassword": "123456",
  "planId": 1,
  "newStatus": "COMPLETED"
}
```

---

- 方法：`POST`
- 路径：`/followUpTask/create`
- Content-Type：`application/json`
- 请求体：`CreateFollowUpTaskRequest`

字段：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `patientId`：long，必填
- `followUpUserId`：long，必填
- `triggerType`：string，可选
- `description`：string，可选
- `dueAt`：datetime，必填（格式 `yyyy-MM-dd HH:mm:ss`，GMT+8）

响应：`Map<String,Object>`

### 6.2 医生查询派单列表

- 方法：`GET`
- 路径：`/followUpTask/doctor/list`

请求参数（Query）：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填

响应：`Map<String,Object>`

### 6.3 随访员查询我的任务

- 方法：`GET`
- 路径：`/followUpTask/followup/my`

请求参数（Query）：

- `followUpUsername`：string，必填
- `followUpPassword`：string，必填

响应：`Map<String,Object>`

### 6.4 医生修改任务（视为重新发布）

- 方法：`POST`
- 路径：`/followUpTask/republish`
- Content-Type：`application/json`
- 请求体：`RepublishFollowUpTaskRequest`

字段：

- `taskId`：long，必填（原任务 id）
- `doctorUsername`：string，必填
- `doctorPassword`：string，必填
- `followUpUserId`：long，必填（新的随访员 id）
- `triggerType`：string，可选
- `description`：string，可选
- `dueAt`：datetime，可选（格式 `yyyy-MM-dd HH:mm:ss`，GMT+8）

说明：

- 会将原任务标记为 `CANCELED`，并创建一条新的任务（`ASSIGNED`）及对应计划（`DUE`）。

响应：`Map<String,Object>`（`data.oldTaskId` + `data.newTaskId`）

### 6.5 医生取消任务

- 方法：`POST`
- 路径：`/followUpTask/cancel`
- Content-Type：`application/json`
- 请求体：`CancelFollowUpTaskRequest`

字段：

- `taskId`：long，必填
- `doctorUsername`：string，必填
- `doctorPassword`：string，必填

响应：`Map<String,Object>`

---

## 7. 复诊记录 Revisit

### 7.1 医生新增患者复诊记录

- 方法：`POST`
- 路径：`/revisit/add`

请求参数（Query）：

- `doctorUsername`：string，必填
- `doctorPassword`：string，必填

请求体：`RevisitRecord`

必填字段与规则：

- `patientId`：必填，且必须为已存在的患者
- `revisitAt`：必填，只允许填写当前时间或过去时间（不允许未来时间）
- `hospital`：必填，不能为空
- `department`：必填，不能为空
- `notes`：必填，不能为空
- 额外校验：`patientId` 对应患者必须存在，且患者姓名（user.name）不能为空

后端写入/覆盖字段：

- `doctorName`：后端根据医生账号写入（请求体传入会被覆盖）
- `createdByDoctorId`：后端写入
- `createdAt`：后端写入

响应：`Map<String,Object>`

---

## 8. 数据看板 Dashboard

### 8.1 汇总信息

- 方法：`GET`
- 路径：`/dashboard/summary`

请求参数（Query）：

- `weekOffset`：integer，可选

响应：`Map<String,Object>`

### 8.2 风险等级趋势

- 方法：`GET`
- 路径：`/dashboard/riskLevel/trend`

请求参数（Query）：

- `days`：integer，可选

响应：`Map<String,Object>`

### 8.3 综合趋势（syndrome + alert）

- 方法：`GET`
- 路径：`/dashboard/syndromeAlert/trend`

请求参数（Query）：

- `days`：integer，可选

响应：`Map<String,Object>`

### 8.4 高风险待处理列表

- 方法：`GET`
- 路径：`/dashboard/highRisk/pending`

请求参数（Query）：

- `limit`：integer，可选

响应：`Map<String,Object>`

### 8.5 综合征异常患者

- 方法：`GET`
- 路径：`/dashboard/syndrome/abnormalPatients`

请求参数（Query）：

- `days`：integer，可选
- `limit`：integer，可选

响应：`Map<String,Object>`

---

## 附：常用对象字段

### User

- `id`：long
- `username`：string
- `password`：string（BCrypt 密文）
- `name`：string
- `address`：string
- `age`：integer
- `sex`：string
- `phone`：string
- `riskLevel`：string（数据库列名 `risk_level`）
- `role`：string（`DOCTOR` / `PATIENT` / `FOLLOW_UP`）
- `status`：integer（1=启用，0=停用）

### HealthMetric

- `id`：long
- `patientId`：long
- `metricType`：string
- `value1`：double
- `value2`：double
- `measuredAt`：datetime
- `createdByUserId`：long
- `createdAt`：datetime

### FollowUpTask

- `id`：long
- `patientId`：long
- `doctorId`：long
- `followUpUserId`：long
- `triggerType`：string
- `description`：string
- `status`：string
- `createdAt`：datetime
- `updatedAt`：datetime

### HealthAlert

- `id`：long
- `patientId`：long
- `metricType`：string
- `prevValue1`：double
- `prevValue2`：double
- `currValue1`：double
- `currValue2`：double
- `deltaValue1`：double
- `deltaValue2`：double
- `severity`：string
- `status`：string
- `createdAt`：datetime

### RevisitRecord

- `id`：long
- `patientId`：long
- `revisitAt`：datetime
- `hospital`：string
- `department`：string
- `doctorName`：string
- `notes`：string
- `createdByDoctorId`：long
- `createdAt`：datetime
