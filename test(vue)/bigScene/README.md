# bigScene（大屏驾驶舱 · v4 严格参考图版）

## 启动

在 `e:\智能康养系统\Complete system\test(vue)\bigScene` 目录：

```bash
npm install
npm run dev -- --host
```

默认地址：`http://localhost:5190/`

## 说明

- **只读展示**：不改后端接口路径与字段语义；缺失接口会优雅降级为 0/占位数据以保证驾驶舱结构完整。
- **布局骨架**：左中右三段式，中间主视觉最强。
- **弱表格强图形化**：用事件流、排行条、环图、趋势图替代大表格。

## 登录态（非常重要）

大屏运行在独立端口（如 `5190`），无法自动复用主系统页面的 `sessionStorage`（端口不同会隔离）。

如果接口返回空数据/401，请用下面方式注入一次用户身份：

- 在大屏地址后追加 `userId`（等同主系统的 token，通常就是登录后拿到的 userId）：
  - `http://localhost:5190/?userId=1`

刷新后大屏会把该值写入本域 `sessionStorage.token`，之后接口请求会自动携带：

- `Authorization: Bearer <userId>`
- `X-User-Id: <userId>`

也支持 **URL 自动登录（仅用于内网演示环境）**：

- `http://localhost:5190/?autoLogin=1&phone=17688498714&password=111111`

首次打开会调用 `/api/auth/loginByPhone` 获取 `userId` 写入 `sessionStorage.token`，然后自动把 `password` 从地址栏移除（避免泄露）。

