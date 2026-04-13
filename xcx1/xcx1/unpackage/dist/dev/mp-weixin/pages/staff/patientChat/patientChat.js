"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_consult = require("../../../api/consult.js");
const common_assets = require("../../../common/assets.js");
function safeDecode(value) {
  if (value == null)
    return "";
  const s = String(value);
  if (!s)
    return "";
  try {
    return decodeURIComponent(s);
  } catch (e) {
    return s;
  }
}
const _sfc_main = {
  data() {
    return {
      patientId: "",
      patientName: "",
      messages: [],
      inputValue: "",
      scrollIntoView: "",
      pollTimer: null,
      lastThreadSig: "",
      // 语音
      isRecording: false,
      recordingTime: 0,
      recordTimer: null,
      recorderManager: null,
      pendingVoiceSend: false,
      recordingStartY: 0,
      innerAudioContext: null,
      currentPlayingMsg: null
    };
  },
  onLoad(options) {
    this.patientId = options && options.id ? String(options.id) : "";
    this.patientName = safeDecode(options && (options.name || options.patientName) || "");
    common_vendor.index.setNavigationBarTitle({
      title: this.patientName || "患者消息"
    });
    this.recorderManager = common_vendor.index.getRecorderManager();
    this.recorderManager.onStop((res) => {
      if (this.pendingVoiceSend) {
        this.pendingVoiceSend = false;
        const { tempFilePath, duration } = res;
        this.sendVoiceMessage(tempFilePath, Math.round((duration || 0) / 1e3));
      }
    });
    this.innerAudioContext = common_vendor.index.createInnerAudioContext();
    this.innerAudioContext.onEnded(() => {
      if (this.currentPlayingMsg) {
        this.currentPlayingMsg.isPlaying = false;
        this.messages = [...this.messages];
        this.currentPlayingMsg = null;
      }
    });
    this.loadThread(true);
  },
  onShow() {
    this.startPolling();
    this.loadThread(false);
  },
  onHide() {
    this.stopPolling();
  },
  onUnload() {
    this.stopPolling();
    if (this.innerAudioContext)
      this.innerAudioContext.destroy();
    if (this.recordTimer)
      clearInterval(this.recordTimer);
  },
  methods: {
    onInput(e) {
      this.inputValue = e.detail.value;
    },
    onSend() {
      const text = (this.inputValue || "").trim();
      if (!text)
        return;
      this.sendDoctorText(text);
      this.inputValue = "";
    },
    sendDoctorText(text) {
      if (!this.patientId) {
        common_vendor.index.showToast({ title: "缺少 patientId", icon: "none" });
        return;
      }
      try {
        api_consult.appendMessage(this.patientId, {
          sender: "doctor",
          text,
          isVoice: false,
          time: api_consult.formatHM(Date.now())
        });
        this.loadThread(true);
      } catch (e) {
        common_vendor.index.showToast({ title: e && e.message ? e.message : "发送失败", icon: "none" });
      }
    },
    sendVoiceMessage(voicePath, duration) {
      if (!this.patientId) {
        common_vendor.index.showToast({ title: "缺少 patientId", icon: "none" });
        return;
      }
      try {
        api_consult.appendMessage(this.patientId, {
          sender: "doctor",
          isVoice: true,
          voicePath,
          duration,
          time: api_consult.formatHM(Date.now())
        });
        this.loadThread(true);
      } catch (e) {
        common_vendor.index.showToast({ title: e && e.message ? e.message : "发送失败", icon: "none" });
      }
    },
    loadThread(scrollToBottom) {
      if (!this.patientId)
        return;
      try {
        api_consult.markRead(this.patientId, "doctor");
        const rows = api_consult.listThread(this.patientId);
        const viewRows = rows.map((m) => ({
          id: m.id,
          from: m.sender === "doctor" ? "me" : "other",
          text: m.text || "",
          time: m.time || "",
          isVoice: !!m.isVoice,
          voicePath: m.voicePath || "",
          duration: m.duration || 0,
          isPlaying: false
        }));
        this.messages = viewRows;
        const last = rows[rows.length - 1];
        this.lastThreadSig = `${rows.length}_${last ? last.id : ""}_${last ? last.ts : ""}`;
        if (scrollToBottom && last) {
          this.scrollIntoView = "msg-" + last.id;
        }
      } catch (e) {
        common_vendor.index.__f__("warn", "at pages/staff/patientChat/patientChat.vue:251", "loadThread failed", e);
      }
    },
    startPolling() {
      if (!this.patientId)
        return;
      this.stopPolling();
      this.pollTimer = setInterval(() => {
        try {
          const rows = api_consult.listThread(this.patientId);
          const last = rows[rows.length - 1];
          const sig = `${rows.length}_${last ? last.id : ""}_${last ? last.ts : ""}`;
          if (sig !== this.lastThreadSig) {
            this.loadThread(false);
          }
        } catch (e) {
        }
      }, 1e3);
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer);
        this.pollTimer = null;
      }
    },
    // 录音
    startRecording(e) {
      this.isRecording = true;
      this.pendingVoiceSend = false;
      this.recordingTime = 0;
      this.recordingStartY = e && e.touches && e.touches[0] && e.touches[0].clientY ? e.touches[0].clientY : 0;
      this.recordTimer = setInterval(() => {
        this.recordingTime += 1;
        if (this.recordingTime >= 60) {
          this.stopRecording();
        }
      }, 1e3);
      const options = {
        duration: 6e4,
        sampleRate: 16e3,
        numberOfChannels: 1,
        encodeBitRate: 64e3,
        format: "mp3",
        frameSize: 50
      };
      this.recorderManager.start(options);
    },
    stopRecording(e) {
      if (!this.isRecording)
        return;
      clearInterval(this.recordTimer);
      this.isRecording = false;
      if (e && e.changedTouches && e.changedTouches[0]) {
        const touch = e.changedTouches[0];
        const startY = this.recordingStartY || touch.clientY;
        if (touch.clientY < startY - 100) {
          this.cancelRecording();
          return;
        }
      }
      this.pendingVoiceSend = true;
      this.recorderManager.stop();
    },
    cancelRecording() {
      clearInterval(this.recordTimer);
      this.isRecording = false;
      this.pendingVoiceSend = false;
      this.recorderManager.stop();
      common_vendor.index.showToast({ title: "已取消录音", icon: "none" });
    },
    // 播放语音
    playVoice(item) {
      if (!this.innerAudioContext)
        return;
      if (this.currentPlayingMsg && this.currentPlayingMsg !== item) {
        this.currentPlayingMsg.isPlaying = false;
        this.innerAudioContext.stop();
      }
      if (item.isPlaying) {
        item.isPlaying = false;
        this.innerAudioContext.pause();
        this.currentPlayingMsg = null;
      } else {
        item.isPlaying = true;
        this.currentPlayingMsg = item;
        this.innerAudioContext.src = item.voicePath;
        this.innerAudioContext.play();
      }
      this.messages = [...this.messages];
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.patientName || "患者消息"),
    b: common_vendor.f($data.messages, (item, index, i0) => {
      return common_vendor.e({
        a: item.from === "me"
      }, item.from === "me" ? {
        b: common_assets._imports_0
      } : {
        c: common_assets._imports_1
      }, {
        d: item.isVoice
      }, item.isVoice ? {
        e: common_vendor.n("voice-wave " + (item.isPlaying ? "playing" : "")),
        f: common_vendor.t(item.duration),
        g: common_vendor.o(($event) => $options.playVoice(item), index)
      } : {
        h: common_vendor.t(item.text)
      }, {
        i: common_vendor.t(item.time),
        j: common_vendor.n("msg-row " + (item.from === "me" ? "msg-right" : "msg-left")),
        k: "msg-" + item.id,
        l: index
      });
    }),
    c: $data.messages.length === 0
  }, $data.messages.length === 0 ? {} : {}, {
    d: $data.scrollIntoView,
    e: $data.inputValue,
    f: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    g: common_vendor.o((...args) => $options.onSend && $options.onSend(...args)),
    h: common_vendor.t($data.isRecording ? "松开发送" : "按住说话"),
    i: common_vendor.n("voice-btn " + ($data.isRecording ? "recording" : "")),
    j: common_vendor.o((...args) => $options.startRecording && $options.startRecording(...args)),
    k: common_vendor.o((...args) => $options.stopRecording && $options.stopRecording(...args)),
    l: common_vendor.o((...args) => $options.cancelRecording && $options.cancelRecording(...args)),
    m: common_vendor.o((...args) => $options.onSend && $options.onSend(...args)),
    n: $data.isRecording
  }, $data.isRecording ? {
    o: common_vendor.f(3, (i, k0, i0) => {
      return {
        a: i
      };
    }),
    p: common_vendor.t($data.recordingTime)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/staff/patientChat/patientChat.js.map
