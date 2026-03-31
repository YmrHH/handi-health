// utils/util.js
// 兼容旧代码的工具方法（当前项目主要使用 ES 模块 + uni-app 方式）。

// 健康数据模拟（如需可扩展/迁移到后端）
const healthData = {
  bloodPressure: '120/80',
  heartRate: '75',
  bloodSugar: '5.5'
};

// 时间格式化
const formatTime = (date) => {
  if (!(date instanceof Date)) date = new Date(date);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  const second = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
};

module.exports = {
  healthData,
  formatTime
};
