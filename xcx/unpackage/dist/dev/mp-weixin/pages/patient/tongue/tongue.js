"use strict";
const common_vendor = require("../../../common/vendor.js");
const config_api = require("../../../config/api.js");
const api_common = require("../../../api/common.js");
const utils_session = require("../../../utils/session.js");
const _sfc_main = {
  data() {
    return {
      imagePath: "",
      uploading: false,
      uploaded: false,
      uploadedAt: "",
      patientId: ""
    };
  },
  onLoad() {
    const user = utils_session.getUser();
    this.patientId = user && (user.patientId || user.id) || "";
    this.restoreLastTongue();
  },
  onShow() {
    this.restoreLastTongue();
  },
  methods: {
    normalizeUrl(u) {
      const s = String(u || "").trim();
      if (!s)
        return "";
      if (/^https?:\/\//i.test(s))
        return s;
      if (/^(wxfile|file):\/\//i.test(s))
        return s;
      if (/^data:image\//i.test(s))
        return s;
      const base = String(config_api.API_BASE_URL).replace(/\/$/, "");
      if (!base)
        return s;
      const rel = s.startsWith("/") ? s : "/" + s;
      return base + rel;
    },
    restoreLastTongue() {
      const last = common_vendor.index.getStorageSync("tcm_tongue_last");
      if (!last || !(last.imagePath || last.url))
        return;
      const raw = last.url || last.imagePath;
      const src = this.normalizeUrl(raw) || raw;
      this.setData({
        imagePath: src,
        uploaded: !!last.uploadedAt,
        uploadedAt: last.uploadedAt || ""
      });
    },
    onTakePhoto() {
      common_vendor.index.chooseImage({
        count: 1,
        sizeType: ["compressed"],
        sourceType: ["camera"],
        success: (res) => {
          const path = res.tempFilePaths[0];
          this.setData({
            imagePath: path,
            uploaded: false,
            uploadedAt: ""
          });
        }
      });
    },
    onRetake() {
      this.onTakePhoto();
    },
    onPreview() {
      if (!this.imagePath)
        return;
      common_vendor.index.previewImage({
        urls: [this.imagePath]
      });
    },
    onUpload() {
      if (!this.imagePath) {
        common_vendor.index.showToast({
          title: "请先拍摄舌象",
          icon: "none"
        });
        return;
      }
      this.setData({ uploading: true });
      api_common.upload(this.imagePath, {
        bizType: "tongue",
        patientId: this.patientId,
        takenAt: (/* @__PURE__ */ new Date()).toISOString()
      }).then((data) => {
        const now = this.formatNow();
        const rawUrl = data && data.url || "";
        const url = this.normalizeUrl(rawUrl);
        const fileId = data && (data.fileId || data.id) || "";
        const showSrc = url || this.imagePath;
        this.setData({
          uploading: false,
          uploaded: true,
          uploadedAt: now,
          imagePath: showSrc
        });
        common_vendor.index.setStorageSync("tcm_tongue_last", {
          imagePath: showSrc,
          url,
          uploadedAt: now,
          fileId
        });
        common_vendor.index.showToast({
          title: "上传成功",
          icon: "success"
        });
      }).catch((err) => {
        this.setData({ uploading: false });
        common_vendor.index.showToast({
          title: err && err.message || "上传失败",
          icon: "none"
        });
      });
    },
    formatNow() {
      const d = /* @__PURE__ */ new Date();
      const y = d.getFullYear();
      const m = ("0" + (d.getMonth() + 1)).slice(-2);
      const day = ("0" + d.getDate()).slice(-2);
      const h = ("0" + d.getHours()).slice(-2);
      const mi = ("0" + d.getMinutes()).slice(-2);
      return `${y}-${m}-${day} ${h}:${mi}`;
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.imagePath
  }, $data.imagePath ? {
    b: $data.imagePath,
    c: common_vendor.o((...args) => $options.onPreview && $options.onPreview(...args))
  } : {
    d: common_vendor.o((...args) => $options.onTakePhoto && $options.onTakePhoto(...args))
  }, {
    e: $data.imagePath
  }, $data.imagePath ? {
    f: common_vendor.o((...args) => $options.onRetake && $options.onRetake(...args)),
    g: common_vendor.t($data.uploaded ? "已上传" : $data.uploading ? "上传中..." : "上传舌象"),
    h: $data.uploading,
    i: $data.uploaded || $data.uploading,
    j: common_vendor.o((...args) => $options.onUpload && $options.onUpload(...args))
  } : {}, {
    k: $data.uploaded
  }, $data.uploaded ? {
    l: common_vendor.t($data.uploadedAt)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
