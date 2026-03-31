"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_staff = require("../../../api/staff.js");
const api_auth = require("../../../api/auth.js");
const utils_session = require("../../../utils/session.js");
const StaffTabbar = () => "../../../components/staff-tabbar/staff-tabbar.js";
const _sfc_main = {
  components: { StaffTabbar },
  data() {
    return {
      form: {
        name: "",
        staffNo: "",
        department: "",
        role: "",
        phone: "",
        area: ""
      },
      saveHint: "未保存"
    };
  },
  computed: {
    avatarText() {
      const n = (this.form.name || "").trim();
      return n ? n[0] : "随";
    }
  },
  onShow() {
    this.load();
  },
  methods: {
    load() {
      try {
        const v = common_vendor.index.getStorageSync("staff_profile");
        if (v && typeof v === "object") {
          this.form = { ...this.form, ...v };
          this.saveHint = "已从本地加载";
        }
      } catch (e) {
      }
      api_staff.getStaffProfile().then((res) => {
        if (res && typeof res === "object") {
          this.form = { ...this.form, ...res };
          this.saveHint = "已从后端加载";
        }
      }).catch(() => {
      });
    },
    onSave() {
      common_vendor.index.showLoading({ title: "保存中…", mask: true });
      api_staff.updateStaffProfile(this.form).then(() => {
        try {
          common_vendor.index.setStorageSync("staff_profile", this.form);
        } catch (e) {
        }
        const now = /* @__PURE__ */ new Date();
        const pad = (x) => x < 10 ? "0" + x : "" + x;
        const t = pad(now.getHours()) + ":" + pad(now.getMinutes());
        this.saveHint = "已保存 · " + t;
        common_vendor.index.showToast({ title: "已保存", icon: "success" });
      }).catch((err) => {
        {
          common_vendor.index.showToast({ title: err && err.message || "保存失败", icon: "none" });
        }
      }).finally(() => common_vendor.index.hideLoading());
    },
    onReset() {
      common_vendor.index.showModal({
        title: "重置填写内容",
        content: "将清空当前页面已填写信息（不影响已保存的本地信息）。",
        success: (res) => {
          if (res.confirm) {
            this.form = { name: "", staffNo: "", department: "", role: "", phone: "", area: "" };
            this.saveHint = "已重置（未保存）";
          }
        }
      });
    },
    onClearCache() {
      common_vendor.index.showModal({
        title: "清除本地缓存",
        content: "将清除本机保存的随访人员信息与页面缓存，是否继续？",
        success: (res) => {
          if (res.confirm) {
            try {
              common_vendor.index.removeStorageSync("staff_profile");
              this.form = { name: "", staffNo: "", department: "", role: "", phone: "", area: "" };
              this.saveHint = "已清除缓存";
              common_vendor.index.showToast({ title: "已清除", icon: "success" });
            } catch (e) {
              common_vendor.index.showToast({ title: "清除失败", icon: "none" });
            }
          }
        }
      });
    },
    onContact() {
      common_vendor.index.showModal({
        title: "联系支持",
        content: "如需技术支持，请联系管理员或在工作群反馈问题（示例）。",
        showCancel: false
      });
    },
    onLogout() {
      common_vendor.index.showModal({
        title: "退出登录",
        content: "将返回角色选择页面。是否继续？",
        success: (res) => {
          if (res.confirm) {
            api_auth.logout().catch(() => {
            });
            utils_session.clearSession();
            common_vendor.index.reLaunch({ url: "/pages/index/index" });
          }
        }
      });
    }
  }
};
if (!Array) {
  const _easycom_staff_tabbar2 = common_vendor.resolveComponent("staff-tabbar");
  _easycom_staff_tabbar2();
}
const _easycom_staff_tabbar = () => "../../../components/staff-tabbar/staff-tabbar.js";
if (!Math) {
  _easycom_staff_tabbar();
}
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.t($options.avatarText),
    b: common_vendor.t($data.form.name || "随访人员"),
    c: common_vendor.t($data.form.role || "随访医生/护士"),
    d: common_vendor.t($data.form.department || "科室未设置"),
    e: $data.form.name,
    f: common_vendor.o(($event) => $data.form.name = $event.detail.value),
    g: $data.form.staffNo,
    h: common_vendor.o(($event) => $data.form.staffNo = $event.detail.value),
    i: $data.form.department,
    j: common_vendor.o(($event) => $data.form.department = $event.detail.value),
    k: $data.form.role,
    l: common_vendor.o(($event) => $data.form.role = $event.detail.value),
    m: $data.form.phone,
    n: common_vendor.o(($event) => $data.form.phone = $event.detail.value),
    o: $data.form.area,
    p: common_vendor.o(($event) => $data.form.area = $event.detail.value),
    q: common_vendor.o((...args) => $options.onReset && $options.onReset(...args)),
    r: common_vendor.o((...args) => $options.onSave && $options.onSave(...args)),
    s: common_vendor.t($data.saveHint),
    t: common_vendor.o((...args) => $options.onClearCache && $options.onClearCache(...args)),
    v: common_vendor.o((...args) => $options.onContact && $options.onContact(...args)),
    w: common_vendor.o((...args) => $options.onLogout && $options.onLogout(...args)),
    x: common_vendor.p({
      active: "me"
    })
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
