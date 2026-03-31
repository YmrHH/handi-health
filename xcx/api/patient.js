// api/patient.js
import { request } from '@/utils/request.js';

// 首页汇总
export function getHomeSummary(params) {
  return request({
    url: '/patient/home',
    method: 'GET',
    data: params || {}
  });
}

// 获取/更新基础信息（身高、基础体重等）
export function getBasicInfo() {
  return request({ url: '/patient/basic-info', method: 'GET' });
}

export function updateBasicInfo(data) {
  return request({ url: '/patient/basic-info', method: 'PUT', data });
}

// 日常自测
export function getDailyMeasurementToday(params) {
  return request({
    url: '/patient/daily-measurements/today',
    method: 'GET',
    data: params || {}
  });
}

export function saveDailyMeasurement(data) {
  return request({ url: '/patient/daily-measurements', method: 'POST', data });
}

// 医院化验结果
export function getLabs(params) {
  return request({ url: '/patient/labs', method: 'GET', data: params || {} });
}

// 康养任务
export function getRehabTasks(params) {
  return request({ url: '/patient/rehab-tasks', method: 'GET', data: params || {} });
}

export function updateRehabTaskStatus(taskId, status) {
  return request({
    url: `/patient/rehab-tasks/${encodeURIComponent(taskId)}/status`,
    method: 'PUT',
    data: { status }
  });
}

export function updateRehabTaskDifficulty(taskId, difficulty) {
  return request({
    url: `/patient/rehab-tasks/${encodeURIComponent(taskId)}/difficulty`,
    method: 'PUT',
    data: { difficulty }
  });
}

// 体质/证候问卷
export function submitTcmSurvey(data) {
  return request({ url: '/patient/tcm-surveys', method: 'POST', data });
}

// 患者资料
export function getProfile() {
  return request({ url: '/patient/profile', method: 'GET' });
}

export function updateProfile(data) {
  return request({ url: '/patient/profile', method: 'PUT', data });
}

// 账号关联
export function listLinkedAccounts() {
  return request({ url: '/patient/linked-accounts', method: 'GET' });
}

export function addLinkedAccount(data) {
  return request({ url: '/patient/linked-accounts', method: 'POST', data });
}

export function deleteLinkedAccount(id) {
  return request({ url: `/patient/linked-accounts/${encodeURIComponent(id)}`, method: 'DELETE' });
}

// 消息中心
export function listMessages(params) {
  return request({ url: '/patient/messages', method: 'GET', data: params || {} });
}

export function markMessageRead(messageId) {
  return request({ url: `/patient/messages/${encodeURIComponent(messageId)}/read`, method: 'PUT' });
}

export function markAllMessagesRead(params) {
  return request({ url: '/patient/messages/read-all', method: 'PUT', data: params || {} });
}

// 意见反馈
export function submitFeedback(data) {
  return request({ url: '/patient/feedback', method: 'POST', data });
}

export function listFeedback(params) {
  return request({ url: '/patient/feedback', method: 'GET', data: params || {} });
}


// 随访关联（任务/记录聚合）
export function getFollowupAssociation(params) {
  return request({ url: '/patient/followup-association', method: 'GET', data: params || {} });
}

// 随访记录
export function listFollowups(params) {
  return request({ url: '/patient/followups', method: 'GET', data: params || {} });
}

export function getFollowupDetail(id) {
  return request({ url: `/patient/followups/${encodeURIComponent(id)}`, method: 'GET' });
}

export function applyFollowup(data) {
  return request({ url: '/patient/followup-applications', method: 'POST', data });
}

export function listFollowupApplications(params) {
  return request({ url: '/patient/followup-applications', method: 'GET', data: params || {} });
}
