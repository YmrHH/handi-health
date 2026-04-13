"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const utils_ble_ecb02Hub = require("./utils/ble/ecb02-hub.js");
const uni_modules_zpMixins_index = require("./uni_modules/zp-mixins/index.js");
if (!Math) {
  "./pages/index/index.js";
  "./pages/patient/home/home.js";
  "./pages/patient/dashboard/dashboard.js";
  "./pages/patient/chat/chat.js";
  "./pages/patient/profile/profile.js";
  "./pages/staff/alerts/alerts.js";
  "./pages/patient/monitor/monitor.js";
  "./pages/patient/rehab/rehab.js";
  "./pages/staff/home/home.js";
  "./pages/staff/tasks/tasks.js";
  "./pages/staff/patients/patients.js";
  "./pages/staff/alarm/alarm.js";
  "./pages/staff/profile/profile.js";
  "./pages/staff/followup/followup.js";
  "./pages/patient/survey/survey.js";
  "./pages/patient/tongue/tongue.js";
  "./pages/patient/profile/profileedit/profileedit.js";
  "./pages/patient/profile/accountadd/accountadd.js";
  "./pages/patient/healthFile/healthFile.js";
  "./pages/patient/devices/devices.js";
  "./pages/patient/family/family.js";
  "./pages/patient/followup/followup.js";
  "./pages/patient/followupDetail/followupDetail.js";
  "./pages/patient/message/message.js";
  "./pages/patient/privacy/privacy.js";
  "./pages/patient/feedback/feedback.js";
  "./pages/patient/help/help.js";
  "./pages/patient/bp/bp.js";
  "./pages/patient/hr/hr.js";
  "./pages/patient/spo2/spo2.js";
  "./pages/patient/weight/weight.js";
  "./pages/patient/trends/trends.js";
  "./pages/staff/patientData/patientData.js";
  "./pages/staff/patientChat/patientChat.js";
  "./pages/staff/managedPatients/managedPatients.js";
}
const _sfc_main = {
  data() {
    return {};
  },
  globalData: {
    role: null,
    // 'patient' 或 'staff'
    userInfo: null,
    setRole(role) {
      this.role = role;
      common_vendor.index.setStorageSync("role", role);
    }
  },
  onLaunch() {
    const role = common_vendor.index.getStorageSync("role");
    if (role) {
      this.globalData.role = role;
    }
    utils_ble_ecb02Hub.initEcb02BleHub();
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  app.mixin(uni_modules_zpMixins_index.zpMixins);
  return {
    app
  };
}
createApp().app.mount("#app");
exports.createApp = createApp;
//# sourceMappingURL=../.sourcemap/mp-weixin/app.js.map
