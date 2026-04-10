// api/common.js
import { request, uploadFile } from '@/utils/request.js';

// 通用文件上传（图片/语音等）
// formData 示例：{ bizType: 'tongue', patientId: 'p1' }
export function upload(filePath, formData) {
  return uploadFile({
    url: '/files/upload',
    filePath,
    name: 'file',
    formData: formData || {}
  });
}

// 获取服务器时间（用于联调/时间一致性，可选）
export function serverTime() {
  return request({
    url: '/common/server-time',
    method: 'GET'
  });
}
