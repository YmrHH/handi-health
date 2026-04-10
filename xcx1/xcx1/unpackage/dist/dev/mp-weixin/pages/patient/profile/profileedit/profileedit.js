"use strict";
const common_vendor = require("../../../../common/vendor.js");
const utils_session = require("../../../../utils/session.js");
const api_patient = require("../../../../api/patient.js");
const api_auth = require("../../../../api/auth.js");
const api_common = require("../../../../api/common.js");
const _sfc_main = {
  data() {
    return {
      defaultAvatar: "/static/assets/avatar.png",
      formData: {
        avatarUrl: "",
        name: "",
        gender: "男",
        birthday: "",
        phone: "",
        heightCm: "",
        weightKg: ""
      },
      genderOptions: ["男", "女"],
      genderIndex: 0,
      pwdData: {
        oldPassword: "",
        newPassword: "",
        confirmPassword: ""
      }
    };
  },
  onLoad() {
    this.loadUserData();
  },
  methods: {
    loadUserData() {
      try {
        const userInfo = common_vendor.index.getStorageSync("userInfo", null);
        if (userInfo && typeof userInfo === "object") {
          this.formData = { ...this.formData, ...userInfo };
          if (userInfo.gender) {
            const idx = this.genderOptions.indexOf(userInfo.gender);
            this.genderIndex = idx >= 0 ? idx : 0;
          }
        }
      } catch (e) {
      }
      api_patient.getProfile().then((p) => {
        if (p && typeof p === "object") {
          this.formData = { ...this.formData, ...p };
          if (p.gender) {
            const idx = this.genderOptions.indexOf(p.gender);
            this.genderIndex = idx >= 0 ? idx : 0;
          }
          try {
            common_vendor.index.setStorageSync("userInfo", { ...common_vendor.index.getStorageSync("userInfo"), ...p });
          } catch (e) {
          }
        }
      }).catch(() => {
      });
    },
    chooseAvatar() {
      common_vendor.index.chooseImage({
        count: 1,
        sizeType: ["compressed"],
        success: (res) => {
          const temp = res.tempFilePaths && res.tempFilePaths[0] || "";
          if (temp)
            this.formData.avatarUrl = temp;
        }
      });
    },
    onGenderChange(e) {
      const idx = Number(e.detail.value || 0);
      this.genderIndex = idx;
      this.formData.gender = this.genderOptions[idx] || "男";
    },
    onBirthChange(e) {
      this.formData.birthday = e.detail.value;
    },
    // 修改密码
    async onChangePassword() {
      const oldPassword = (this.pwdData.oldPassword || "").trim();
      const newPassword = (this.pwdData.newPassword || "").trim();
      const confirmPassword = (this.pwdData.confirmPassword || "").trim();
      if (!oldPassword || !newPassword || !confirmPassword) {
        return common_vendor.index.showToast({ title: "请填写完整的密码信息", icon: "none" });
      }
      if (newPassword.length < 6) {
        return common_vendor.index.showToast({ title: "新密码不少于6位", icon: "none" });
      }
      if (newPassword !== confirmPassword) {
        return common_vendor.index.showToast({ title: "两次新密码不一致", icon: "none" });
      }
      const u = utils_session.getUser();
      if (!u || !u.id) {
        return common_vendor.index.showToast({ title: "未获取到登录用户信息", icon: "none" });
      }
      common_vendor.index.showLoading({ title: "修改中…", mask: true });
      try {
        await api_auth.changePassword({
          userId: u.id,
          oldPassword,
          newPassword,
          confirmPassword
        });
        utils_session.setUser({ ...u, password: newPassword });
        this.pwdData = { oldPassword: "", newPassword: "", confirmPassword: "" };
        common_vendor.index.showToast({ title: "密码已修改", icon: "success" });
      } catch (err) {
        common_vendor.index.showToast({ title: err && err.message || "修改失败", icon: "none" });
      } finally {
        common_vendor.index.hideLoading();
      }
    },
    async saveUserData() {
      const payload = {
        avatarUrl: this.formData.avatarUrl,
        name: (this.formData.name || "").trim(),
        gender: this.genderOptions[this.genderIndex] || "男",
        birthday: this.formData.birthday,
        phone: this.formData.phone,
        heightCm: this.formData.heightCm,
        weightKg: this.formData.weightKg
      };
      if (!payload.name) {
        return common_vendor.index.showToast({ title: "请填写姓名", icon: "none" });
      }
      common_vendor.index.showLoading({ title: "保存中…", mask: true });
      try {
        if (payload.avatarUrl && !/^https?:\/\//i.test(payload.avatarUrl)) {
          const up = await api_common.upload(payload.avatarUrl, { bizType: "avatar" });
          payload.avatarUrl = up && up.url || payload.avatarUrl;
        }
        await api_patient.updateProfile(payload);
        const prev = utils_session.getScopedStorageSync("userInfo", {});
        utils_session.setScopedStorageSync("userInfo", { ...prev && typeof prev === "object" ? prev : {}, ...payload });
        const u = utils_session.getUser();
        if (u && typeof u === "object") {
          utils_session.setUser({ ...u, name: payload.name });
        }
        try {
          common_vendor.index.$emit("userInfoUpdated", payload);
        } catch (e) {
        }
        common_vendor.index.showToast({ title: "已保存", icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack(), 300);
      } catch (err) {
        {
          common_vendor.index.showToast({ title: err && err.message || "保存失败", icon: "none" });
        }
      } finally {
        common_vendor.index.hideLoading();
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: $data.formData.avatarUrl || $data.defaultAvatar,
    b: common_vendor.t($data.formData.name || "未设置姓名"),
    c: common_vendor.o((...args) => $options.chooseAvatar && $options.chooseAvatar(...args)),
    d: $data.formData.name,
    e: common_vendor.o(($event) => $data.formData.name = $event.detail.value),
    f: common_vendor.t($data.genderOptions[$data.genderIndex] || "请选择"),
    g: $data.genderOptions,
    h: $data.genderIndex,
    i: common_vendor.o((...args) => $options.onGenderChange && $options.onGenderChange(...args)),
    j: common_vendor.t($data.formData.birthday || "请选择"),
    k: $data.formData.birthday,
    l: common_vendor.o((...args) => $options.onBirthChange && $options.onBirthChange(...args)),
    m: $data.formData.phone,
    n: common_vendor.o(($event) => $data.formData.phone = $event.detail.value),
    o: $data.formData.heightCm,
    p: common_vendor.o(($event) => $data.formData.heightCm = $event.detail.value),
    q: $data.formData.weightKg,
    r: common_vendor.o(($event) => $data.formData.weightKg = $event.detail.value),
    s: $data.pwdData.oldPassword,
    t: common_vendor.o(($event) => $data.pwdData.oldPassword = $event.detail.value),
    v: $data.pwdData.newPassword,
    w: common_vendor.o(($event) => $data.pwdData.newPassword = $event.detail.value),
    x: $data.pwdData.confirmPassword,
    y: common_vendor.o(($event) => $data.pwdData.confirmPassword = $event.detail.value),
    z: common_vendor.o((...args) => $options.onChangePassword && $options.onChangePassword(...args)),
    A: common_vendor.o((...args) => $options.saveUserData && $options.saveUserData(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
