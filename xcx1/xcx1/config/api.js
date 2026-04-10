// config/api.js
//
// 统一后端接口配置（SSM 后端可直接按此约定实现 Controller）。
//
// ✅ 你需要做的事：
// 1) 把 API_BASE_URL 改成你的后端域名（需在微信小程序后台配置 request 合法域名）。
// 2) 如后端有统一前缀（如 /ssm 或 /followup），在 API_BASE_URL 或 API_PREFIX 中体现。

// 后端根地址（不包含 /api）
// 说明：当前后端 application.properties 默认端口为 8081
// 如需真机联调，请替换成局域网 IP，例如：http://192.168.1.100:8081
export const API_BASE_URL = 'http://localhost:8081/';

// 接口统一前缀
// 说明：当前后端同时存在 /api/** 与非 /api 前缀的接口（如 /auth/**、/followUpTask/** 等），
// 因此这里不再强制拼接统一前缀，具体前缀由各个 api/*.js 中的 url 决定。
export const API_PREFIX = '';

// 请求超时（毫秒）
export const API_TIMEOUT = 15000;

// 是否启用本地 Demo 数据兜底（联调阶段建议 true；生产务必 false）
export const USE_MOCK_FALLBACK = false;
