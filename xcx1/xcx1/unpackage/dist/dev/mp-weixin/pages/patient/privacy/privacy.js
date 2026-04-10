"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_session = require("../../../utils/session.js");
const _sfc_main = {
  data() {
    return {
      auth: {
        bluetooth: "未检测",
        notify: "未检测",
        album: "未检测"
      }
    };
  },
  onShow() {
    this.refreshAuth();
  },
  methods: {
    refreshAuth() {
      this.checkBluetooth();
      this.checkSettings();
    },
    checkBluetooth() {
      common_vendor.index.openBluetoothAdapter({
        success: () => {
          this.auth.bluetooth = "可用/已开启";
          common_vendor.index.closeBluetoothAdapter({});
        },
        fail: () => {
          this.auth.bluetooth = "未开启/未授权";
        }
      });
    },
    checkSettings() {
      common_vendor.index.getSetting({
        success: (res) => {
          const authSetting = res && res.authSetting || {};
          const camera = authSetting["scope.camera"];
          const album = authSetting["scope.writePhotosAlbum"];
          this.auth.album = camera === true || album === true ? "已授权" : "未授权";
          this.auth.notify = "建议开启（在消息中心）";
        },
        fail: () => {
          this.auth.album = "未检测";
          this.auth.notify = "未检测";
        }
      });
    },
    openSettings() {
      common_vendor.index.openSetting({
        success: () => {
        },
        fail: () => {
          common_vendor.index.showToast({ title: "无法打开设置", icon: "none" });
        }
      });
    },
    clearLocal() {
      common_vendor.index.showModal({
        title: "清除本地缓存",
        content: "将清除当前账号在本机上的本地展示数据与状态（不影响后端存档）。是否继续？",
        confirmText: "清除",
        cancelText: "取消",
        success: (res) => {
          if (!res.confirm)
            return;
          try {
            utils_session.clearCurrentUserBusinessCache();
          } catch (e) {
            console.error("清除当前账号本地缓存失败", e);
          }
          common_vendor.index.showToast({ title: "已清除", icon: "success" });
          this.refreshAuth();
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.t($data.auth.bluetooth),
    b: common_vendor.t($data.auth.notify),
    c: common_vendor.t($data.auth.album),
    d: common_vendor.o((...args) => $options.openSettings && $options.openSettings(...args)),
    e: common_vendor.o((...args) => $options.refreshAuth && $options.refreshAuth(...args)),
    f: common_vendor.o((...args) => $options.clearLocal && $options.clearLocal(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
