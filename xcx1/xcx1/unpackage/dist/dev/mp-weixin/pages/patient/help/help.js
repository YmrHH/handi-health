"use strict";
const common_vendor = require("../../../common/vendor.js");
const _sfc_main = {
  data() {
    return {
      faqs: [
        {
          q: "设备连接不上怎么办？",
          a: "请确认手机蓝牙已开启并允许小程序使用蓝牙权限；在「设备管理」页点击“开启蓝牙→搜索设备→连接”。若仍失败，关闭蓝牙后重试或重启设备。",
          open: true
        },
        {
          q: "为什么有些指标需要手动填写？",
          a: "部分指标（如血糖、体温、睡眠）可能因设备型号或使用场景无法自动获取，可在「设备管理」页进行手动补录，系统会同步到身体数据展示与趋势分析。",
          open: false
        },
        {
          q: "随访申请提交后多久会处理？",
          a: "提交后状态为“待处理”。医生端审核并安排随访后会通过「消息中心」通知你。若情况紧急，可在申请中选择更高紧急程度并补充说明。",
          open: false
        },
        {
          q: "医生建议从哪里来的？",
          a: "医生建议由医生 Web 端编辑并审核确认后下发到小程序端，展示在「我的」页面顶部与「消息中心」中。",
          open: false
        },
        {
          q: "如何清除本地缓存？会影响医生端数据吗？",
          a: "可在「隐私与授权」页清除本地缓存，仅清除本机展示用数据（头像姓名、已读状态等），不会影响医生端已存档的随访与记录。",
          open: false
        }
      ]
    };
  },
  methods: {
    toggle(i) {
      this.faqs = this.faqs.map((x, idx) => idx === i ? { ...x, open: !x.open } : x);
    },
    openGuide(type) {
      let title = "使用指引";
      let content = "";
      if (type === "device") {
        title = "设备连接";
        content = "路径：我的 → 设备绑定\n步骤：开启蓝牙 → 搜索设备 → 连接 → 自动同步数据到身体数据展示。";
      } else if (type === "data") {
        title = "数据补录";
        content = "路径：我的 → 设备绑定 → 手动补录\n适用：无法自动获取的指标（血糖/体温/睡眠等）。保存后会同步到身体数据展示与趋势页。";
      } else {
        title = "随访申请";
        content = "路径：我的 → 随访关联 → 查看详情\n填写当前情况与随访诉求，提交后等待医生端审核安排，进度会在消息中心提醒。";
      }
      common_vendor.index.showModal({ title, content, showCancel: false, confirmText: "知道了" });
    },
    call() {
      common_vendor.index.makePhoneCall({
        phoneNumber: "4000000000",
        fail: () => common_vendor.index.showToast({ title: "无法拨打电话", icon: "none" })
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.o(($event) => $options.openGuide("device")),
    b: common_vendor.o(($event) => $options.openGuide("data")),
    c: common_vendor.o(($event) => $options.openGuide("followup")),
    d: common_vendor.f($data.faqs, (q, idx, i0) => {
      return common_vendor.e({
        a: common_vendor.t(q.q),
        b: common_vendor.t(q.open ? "˅" : "›"),
        c: q.open
      }, q.open ? {
        d: common_vendor.t(q.a)
      } : {}, {
        e: idx,
        f: common_vendor.o(($event) => $options.toggle(idx), idx)
      });
    }),
    e: common_vendor.o((...args) => $options.call && $options.call(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/patient/help/help.js.map
