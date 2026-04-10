import { request } from '@/utils/request.js';

// 小程序 AI 对话
// data 示例：
// {
//   patientId: 1,
//   message: '我这两天有点高血压',
//   history: [
//     { role: 'user', content: '最近睡得不好' },
//     { role: 'assistant', content: '...' }
//   ]
// }
export function chatWithAI(data) {
  return request({
    url: '/api/ai/chat',
    method: 'POST',
    data: data || {}
  });
}
