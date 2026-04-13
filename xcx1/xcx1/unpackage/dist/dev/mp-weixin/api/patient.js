"use strict";
const utils_request = require("../utils/request.js");
function getHomeSummary(params) {
  return utils_request.request({
    url: "/patient/home",
    method: "GET",
    data: params || {}
  });
}
function getBasicInfo() {
  return utils_request.request({ url: "/patient/basic-info", method: "GET" });
}
function updateBasicInfo(data) {
  return utils_request.request({ url: "/patient/basic-info", method: "PUT", data });
}
function getDailyMeasurementToday(params) {
  return utils_request.request({
    url: "/patient/daily-measurements/today",
    method: "GET",
    data: params || {}
  });
}
function saveDailyMeasurement(data) {
  return utils_request.request({ url: "/patient/daily-measurements", method: "POST", data });
}
function getLabs(params) {
  return utils_request.request({ url: "/patient/labs", method: "GET", data: params || {} });
}
function getRehabTasks(params) {
  return utils_request.request({ url: "/patient/rehab-tasks", method: "GET", data: params || {} });
}
function updateRehabTaskStatus(taskId, status) {
  return utils_request.request({
    url: `/patient/rehab-tasks/${encodeURIComponent(taskId)}/status`,
    method: "PUT",
    data: { status }
  });
}
function updateRehabTaskDifficulty(taskId, difficulty) {
  return utils_request.request({
    url: `/patient/rehab-tasks/${encodeURIComponent(taskId)}/difficulty`,
    method: "PUT",
    data: { difficulty }
  });
}
function submitTcmSurvey(data) {
  return utils_request.request({ url: "/patient/tcm-surveys", method: "POST", data });
}
function getProfile() {
  return utils_request.request({ url: "/patient/profile", method: "GET" });
}
function updateProfile(data) {
  return utils_request.request({ url: "/patient/profile", method: "PUT", data });
}
function listMessages(params) {
  return utils_request.request({ url: "/patient/messages", method: "GET", data: params || {} });
}
function markMessageRead(messageId) {
  return utils_request.request({ url: `/patient/messages/${encodeURIComponent(messageId)}/read`, method: "PUT" });
}
function markAllMessagesRead(params) {
  return utils_request.request({ url: "/patient/messages/read-all", method: "PUT", data: params || {} });
}
function submitFeedback(data) {
  return utils_request.request({ url: "/patient/feedback", method: "POST", data });
}
function listFeedback(params) {
  return utils_request.request({ url: "/patient/feedback", method: "GET", data: params || {} });
}
function getFollowupAssociation(params) {
  return utils_request.request({ url: "/patient/followup-association", method: "GET", data: params || {} });
}
function listFollowups(params) {
  return utils_request.request({ url: "/patient/followups", method: "GET", data: params || {} });
}
function getFollowupDetail(id) {
  return utils_request.request({ url: `/patient/followups/${encodeURIComponent(id)}`, method: "GET" });
}
function applyFollowup(data) {
  return utils_request.request({ url: "/patient/followup-applications", method: "POST", data });
}
function listFollowupApplications(params) {
  return utils_request.request({ url: "/patient/followup-applications", method: "GET", data: params || {} });
}
exports.applyFollowup = applyFollowup;
exports.getBasicInfo = getBasicInfo;
exports.getDailyMeasurementToday = getDailyMeasurementToday;
exports.getFollowupAssociation = getFollowupAssociation;
exports.getFollowupDetail = getFollowupDetail;
exports.getHomeSummary = getHomeSummary;
exports.getLabs = getLabs;
exports.getProfile = getProfile;
exports.getRehabTasks = getRehabTasks;
exports.listFeedback = listFeedback;
exports.listFollowupApplications = listFollowupApplications;
exports.listFollowups = listFollowups;
exports.listMessages = listMessages;
exports.markAllMessagesRead = markAllMessagesRead;
exports.markMessageRead = markMessageRead;
exports.saveDailyMeasurement = saveDailyMeasurement;
exports.submitFeedback = submitFeedback;
exports.submitTcmSurvey = submitTcmSurvey;
exports.updateBasicInfo = updateBasicInfo;
exports.updateProfile = updateProfile;
exports.updateRehabTaskDifficulty = updateRehabTaskDifficulty;
exports.updateRehabTaskStatus = updateRehabTaskStatus;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/patient.js.map
