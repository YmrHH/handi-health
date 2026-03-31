"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_session = require("../../../utils/session.js");
const api_patient = require("../../../api/patient.js");
const api_common = require("../../../api/common.js");
function nowStr() {
  const d = /* @__PURE__ */ new Date();
  const p = (n) => n < 10 ? "0" + n : "" + n;
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}
function uid() {
  return "fb_" + Date.now() + "_" + Math.floor(Math.random() * 1e4);
}
const _sfc_main = {
  data() {
    return {
      typeOptions: ["功能建议", "数据问题", "蓝牙连接", "随访相关", "其它"],
      typeIndex: 0,
      form: {
        content: "",
        contact: ""
      },
      images: [],
      recent: []
    };
  },
  onShow() {
    this.loadRecent();
  },
  methods: {
    loadRecent() {
      const arr = utils_session.getScopedStorageSync("feedback_list", []);
      const list = Array.isArray(arr) ? arr : [];
      this.recent = list.slice(0, 5);
      api_patient.listFeedback().then((res) => {
        const remote = Array.isArray(res) ? res : res && res.list || [];
        if (remote && remote.length) {
          this.recent = remote.slice(0, 5);
        }
      }).catch(() => {
      });
    },
    onTypeChange(e) {
      this.typeIndex = Number(e.detail.value || 0);
    },
    chooseImages() {
      common_vendor.index.chooseImage({
        count: 3 - this.images.length,
        sizeType: ["compressed"],
        sourceType: ["album", "camera"],
        success: (res) => {
          const temp = res && res.tempFilePaths || [];
          this.images = this.images.concat(temp).slice(0, 3);
        }
      });
    },
    preview(idx) {
      common_vendor.index.previewImage({
        current: this.images[idx],
        urls: this.images
      });
    },
    removeImg(idx) {
      this.images.splice(idx, 1);
    },
    submit() {
      const content = (this.form.content || "").trim();
      if (!content)
        return common_vendor.index.showToast({ title: "请填写问题描述", icon: "none" });
      const payload = {
        id: uid(),
        time: nowStr(),
        type: this.typeOptions[this.typeIndex],
        content,
        contact: (this.form.contact || "").trim(),
        images: this.images.slice()
      };
      common_vendor.index.showModal({
        title: "确认提交",
        content: "我们将用于改进体验，必要时可能联系你核实问题。",
        confirmText: "提交",
        cancelText: "取消",
        success: (r) => {
          if (!r.confirm)
            return;
          common_vendor.index.showLoading({ title: "提交中…", mask: true });
          (async () => {
            try {
              const imageUrls = [];
              for (const p of this.images) {
                if (!p)
                  continue;
                if (/^https?:\/\//i.test(p)) {
                  imageUrls.push(p);
                  continue;
                }
                const up = await api_common.upload(p, { bizType: "feedback" });
                const url = up && up.url || "";
                if (url)
                  imageUrls.push(url);
              }
              const remotePayload = {
                type: payload.type,
                content: payload.content,
                contact: payload.contact,
                imageUrls
              };
              const ret = await api_patient.submitFeedback(remotePayload);
              const stored = {
                ...payload,
                id: ret && ret.id || payload.id,
                time: ret && ret.time || payload.time,
                images: imageUrls
              };
              const arr = utils_session.getScopedStorageSync("feedback_list", []);
              const list = Array.isArray(arr) ? arr : [];
              list.unshift(stored);
              utils_session.setScopedStorageSync("feedback_list", list);
              this.form.content = "";
              this.form.contact = "";
              this.images = [];
              this.loadRecent();
              common_vendor.index.showToast({ title: "已提交", icon: "success" });
            } catch (err) {
              {
                common_vendor.index.showToast({ title: err && err.message || "提交失败", icon: "none" });
              }
            } finally {
              common_vendor.index.hideLoading();
            }
          })();
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.typeOptions[$data.typeIndex]),
    b: $data.typeOptions,
    c: $data.typeIndex,
    d: common_vendor.o((...args) => $options.onTypeChange && $options.onTypeChange(...args)),
    e: $data.form.content,
    f: common_vendor.o(($event) => $data.form.content = $event.detail.value),
    g: common_vendor.t($data.form.content.length),
    h: $data.form.contact,
    i: common_vendor.o(($event) => $data.form.contact = $event.detail.value),
    j: common_vendor.t($data.images.length),
    k: common_vendor.f($data.images, (img, idx, i0) => {
      return {
        a: img,
        b: common_vendor.o(($event) => $options.preview(idx), idx),
        c: common_vendor.o(($event) => $options.removeImg(idx), idx),
        d: idx
      };
    }),
    l: $data.images.length < 3
  }, $data.images.length < 3 ? {
    m: common_vendor.o((...args) => $options.chooseImages && $options.chooseImages(...args))
  } : {}, {
    n: common_vendor.o((...args) => $options.submit && $options.submit(...args)),
    o: $data.recent.length === 0
  }, $data.recent.length === 0 ? {} : {}, {
    p: common_vendor.f($data.recent, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.type),
        b: common_vendor.t(item.time),
        c: common_vendor.t(item.content),
        d: item.id
      };
    })
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
