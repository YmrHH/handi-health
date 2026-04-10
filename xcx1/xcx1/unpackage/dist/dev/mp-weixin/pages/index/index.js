"use strict";
const common_vendor = require("../../common/vendor.js");
const api_auth = require("../../api/auth.js");
const utils_session = require("../../utils/session.js");
const _sfc_main = {
  data() {
    return {
      // 端口：patient 患者端 / staff 医护端 / '' 未选择
      role: "",
      form: {
        phone: "",
        password: ""
      }
    };
  },
  methods: {
    // 选择端口
    onSelectRole(e) {
      const role = e.currentTarget.dataset.role;
      this.role = role;
    },
    // 输入手机号 / 密码
    onInput(e) {
      const field = e.currentTarget.dataset.field;
      const value = e.detail.value;
      this.form = {
        ...this.form,
        [field]: value
      };
    },
    // 点击登录
    onLogin() {
      const { role, form } = this;
      if (!role) {
        common_vendor.index.showToast({
          title: "请先选择端口",
          icon: "none"
        });
        return;
      }
      const phone = (form.phone || "").trim();
      const password = (form.password || "").trim();
      if (!/^1\d{10}$/.test(phone)) {
        common_vendor.index.showToast({
          title: "请输入正确手机号",
          icon: "none"
        });
        return;
      }
      if (!password) {
        common_vendor.index.showToast({
          title: "请输入密码",
          icon: "none"
        });
        return;
      }
      common_vendor.index.showLoading({ title: "登录中…", mask: true });
      api_auth.loginByPhone({
        phone,
        password
      }).then((res) => {
        const token = res && res.token || "";
        const user = res && res.user || null;
        const backendRole = String(user && (user.backendRole || user.role) || "").trim().toUpperCase();
        const okPatient = role === "patient" && backendRole === "PATIENT";
        const okStaff = role === "staff" && (backendRole === "DOCTOR" || backendRole === "FOLLOW_UP");
        if (!(okPatient || okStaff)) {
          throw { message: `该手机号账号角色为 ${backendRole || "未知"}，与当前选择端口不匹配` };
        }
        utils_session.setRole(role);
        utils_session.setToken(token);
        if (user)
          utils_session.setUser(user);
        common_vendor.index.showToast({ title: "登录成功", icon: "success" });
        const url = role === "patient" ? "/pages/patient/home/home" : "/pages/staff/tasks/tasks";
        setTimeout(() => {
          common_vendor.index.reLaunch({ url });
        }, 200);
      }).catch((err) => {
        console.error("loginByPhone failed:", err);
        common_vendor.index.showToast({
          title: err && err.message || "手机号或密码错误",
          icon: "none"
        });
      }).finally(() => {
        common_vendor.index.hideLoading();
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.n("role-item " + ($data.role === "patient" ? "active" : "")),
    b: common_vendor.o((...args) => $options.onSelectRole && $options.onSelectRole(...args)),
    c: common_vendor.n("role-item " + ($data.role === "staff" ? "active" : "")),
    d: common_vendor.o((...args) => $options.onSelectRole && $options.onSelectRole(...args)),
    e: !$data.role
  }, !$data.role ? {} : $data.role === "patient" ? {} : $data.role === "staff" ? {} : {}, {
    f: $data.role === "patient",
    g: $data.role === "staff",
    h: !$data.role
  }, !$data.role ? {} : {
    i: $data.form.phone,
    j: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    k: $data.form.password,
    l: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    m: common_vendor.o((...args) => $options.onLogin && $options.onLogin(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
