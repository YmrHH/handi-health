"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_session = require("../../../utils/session.js");
const api_consult = require("../../../api/consult.js");
const api_ai = require("../../../api/ai.js");
const common_assets = require("../../../common/assets.js");
function safeArray(v) {
  return Array.isArray(v) ? v : [];
}
function createLocalMsg(from, payload = {}) {
  return {
    id: payload.id || "msg_" + Date.now() + "_" + Math.floor(Math.random() * 1e5),
    from,
    text: payload.text || "",
    time: payload.time || api_consult.formatHM(Date.now()),
    isVoice: !!payload.isVoice,
    voicePath: payload.voicePath || "",
    duration: payload.duration || 0,
    isPlaying: false
  };
}
function toAiHistory(rows) {
  return safeArray(rows).filter((item) => item && !item.isVoice && item.text).slice(-12).map((item) => ({
    role: item.from === "me" ? "user" : "assistant",
    content: item.text
  }));
}
const _sfc_main = {
  data() {
    return {
      mode: "doctor",
      messages: [],
      inputValue: "",
      scrollTop: 0,
      scrollIntoView: "",
      isRecording: false,
      recordingTime: 0,
      recordTimer: null,
      recorderManager: null,
      innerAudioContext: null,
      currentPlayingMsg: null,
      patientId: "",
      patientName: "",
      pollTimer: null,
      lastThreadSig: "",
      pendingVoiceSend: false,
      recordingStartY: 0,
      aiLoading: false
    };
  },
  onLoad() {
    const u = utils_session.getUser();
    this.patientId = u && (u.id || u.userId) ? String(u.id || u.userId) : "";
    this.patientName = u && u.name ? String(u.name) : "";
    this.recorderManager = common_vendor.index.getRecorderManager();
    this.recorderManager.onStop((res) => {
      if (!this.pendingVoiceSend)
        return;
      this.pendingVoiceSend = false;
      const { tempFilePath, duration } = res || {};
      if (!tempFilePath)
        return;
      this.sendVoiceMessage(tempFilePath, Math.max(1, Math.round((duration || 1e3) / 1e3)));
    });
    this.innerAudioContext = common_vendor.index.createInnerAudioContext();
    this.innerAudioContext.onEnded(() => {
      if (this.currentPlayingMsg) {
        this.currentPlayingMsg.isPlaying = false;
        this.messages = [...this.messages];
        this.currentPlayingMsg = null;
      }
    });
    this.loadDoctorThread(true);
  },
  onShow() {
    if (this.mode === "doctor") {
      this.startPolling();
      this.loadDoctorThread(false);
    } else {
      this.loadAiThread(false);
    }
  },
  onHide() {
    this.stopPolling();
  },
  onUnload() {
    this.stopPolling();
    if (this.innerAudioContext) {
      this.innerAudioContext.destroy();
    }
    if (this.recordTimer) {
      clearInterval(this.recordTimer);
    }
  },
  methods: {
    switchMode(e) {
      const mode = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.mode : "doctor";
      this.mode = mode;
      this.inputValue = "";
      this.scrollTop = 0;
      this.scrollIntoView = "";
      if (mode === "doctor") {
        this.stopPolling();
        this.loadDoctorThread(true);
        this.startPolling();
      } else {
        this.stopPolling();
        this.loadAiThread(true);
      }
    },
    onInput(e) {
      this.inputValue = e && e.detail ? e.detail.value || "" : "";
    },
    onSend() {
      const text = (this.inputValue || "").trim();
      if (!text)
        return;
      if (this.mode === "doctor") {
        this.sendDoctorText(text);
      } else {
        this.sendAiText(text);
      }
      this.inputValue = "";
    },
    async sendAiText(text) {
      if (this.aiLoading)
        return;
      const userMsg = createLocalMsg("me", { text });
      const nextRows = [...this.messages, userMsg];
      this.messages = nextRows;
      this.persistAiThread(nextRows);
      this.scrollIntoView = "msg-" + userMsg.id;
      this.aiLoading = true;
      try {
        common_vendor.index.showLoading({ title: "AI思考中", mask: false });
      } catch (e) {
      }
      try {
        const payload = await api_ai.chatWithAI({
          patientId: this.patientId ? Number(this.patientId) : void 0,
          message: text,
          history: toAiHistory(nextRows)
        });
        const replyText = payload && (payload.reply || payload.content || payload.text) ? payload.reply || payload.content || payload.text : "暂时未获取到回复，请稍后重试。";
        const aiMsg = createLocalMsg("other", { text: replyText });
        const finalRows = [...this.messages, aiMsg];
        this.messages = finalRows;
        this.persistAiThread(finalRows);
        this.scrollIntoView = "msg-" + aiMsg.id;
      } catch (e) {
        const errText = e && e.message ? e.message : "AI 服务暂时不可用，请稍后重试";
        const aiMsg = createLocalMsg("other", { text: "抱歉，当前 AI 服务连接失败：" + errText });
        const finalRows = [...this.messages, aiMsg];
        this.messages = finalRows;
        this.persistAiThread(finalRows);
        this.scrollIntoView = "msg-" + aiMsg.id;
      } finally {
        this.aiLoading = false;
        try {
          common_vendor.index.hideLoading();
        } catch (e) {
        }
      }
    },
    sendDoctorText(text) {
      if (!this.patientId) {
        common_vendor.index.showToast({ title: "缺少 patientId，无法发送", icon: "none" });
        return;
      }
      try {
        api_consult.appendMessage(this.patientId, {
          sender: "patient",
          text,
          isVoice: false,
          time: api_consult.formatHM(Date.now())
        });
        this.loadDoctorThread(true);
      } catch (e) {
        common_vendor.index.showToast({ title: e && e.message || "发送失败", icon: "none" });
      }
    },
    sendVoiceMessage(voicePath, duration) {
      if (this.mode === "ai") {
        common_vendor.index.showToast({ title: "AI 对话当前仅支持文字输入", icon: "none" });
        return;
      }
      if (!this.patientId) {
        common_vendor.index.showToast({ title: "缺少 patientId，无法发送", icon: "none" });
        return;
      }
      try {
        api_consult.appendMessage(this.patientId, {
          sender: "patient",
          isVoice: true,
          voicePath,
          duration,
          time: api_consult.formatHM(Date.now())
        });
        this.loadDoctorThread(true);
      } catch (e) {
        common_vendor.index.showToast({ title: e && e.message || "发送失败", icon: "none" });
      }
    },
    startRecording(e) {
      if (this.mode === "ai") {
        common_vendor.index.showToast({ title: "AI 对话当前仅支持文字输入", icon: "none" });
        return;
      }
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
      this.recorderManager.start({
        duration: 6e4,
        sampleRate: 16e3,
        numberOfChannels: 1,
        encodeBitRate: 64e3,
        format: "mp3",
        frameSize: 50
      });
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
    playVoice(item) {
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
    },
    loadDoctorThread(scrollToBottom) {
      if (this.mode !== "doctor" || !this.patientId)
        return;
      try {
        api_consult.markRead(this.patientId, "patient");
        const rows = api_consult.listThread(this.patientId);
        const viewRows = rows.map((m) => ({
          id: m.id,
          from: m.sender === "patient" ? "me" : "other",
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
        console.warn("loadDoctorThread failed", e);
      }
    },
    loadAiThread(scrollToBottom) {
      const rows = safeArray(utils_session.getScopedStorageSync("ai_chat_thread", [], this.patientId));
      this.messages = rows;
      const last = rows[rows.length - 1];
      if (scrollToBottom && last) {
        this.scrollIntoView = "msg-" + last.id;
      }
    },
    persistAiThread(rows) {
      utils_session.setScopedStorageSync("ai_chat_thread", safeArray(rows).slice(-100), this.patientId);
    },
    startPolling() {
      if (this.mode !== "doctor" || !this.patientId)
        return;
      this.stopPolling();
      this.pollTimer = setInterval(() => {
        try {
          const rows = api_consult.listThread(this.patientId);
          const last = rows[rows.length - 1];
          const sig = `${rows.length}_${last ? last.id : ""}_${last ? last.ts : ""}`;
          if (sig !== this.lastThreadSig) {
            this.loadDoctorThread(false);
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
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.n("tab " + ($data.mode === "doctor" ? "tab-active" : "")),
    b: common_vendor.o((...args) => $options.switchMode && $options.switchMode(...args)),
    c: common_vendor.n("tab " + ($data.mode === "ai" ? "tab-active" : "")),
    d: common_vendor.o((...args) => $options.switchMode && $options.switchMode(...args)),
    e: common_vendor.f($data.messages, (item, index, i0) => {
      return common_vendor.e({
        a: item.from === "me"
      }, item.from === "me" ? {
        b: common_assets._imports_1
      } : {
        c: $data.mode === "doctor" ? "/static/assets/avatar-doctor.png" : "/static/assets/avatar-ai.png"
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
    f: $data.messages.length === 0
  }, $data.messages.length === 0 ? {
    g: common_vendor.t($data.mode === "doctor" ? "向医生描述您的情况" : "可以问问小助理："),
    h: common_vendor.t($data.mode === "doctor" ? "支持语音输入，长按下方麦克风按钮录音" : "AI 会结合您最近体征、问卷、随访和预警信息回答")
  } : {}, {
    i: $data.scrollTop,
    j: $data.scrollIntoView,
    k: $data.mode === "doctor" ? "请输入要咨询医生的问题…" : "请输入想问 AI 的问题…",
    l: $data.inputValue,
    m: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    n: common_vendor.o((...args) => $options.onSend && $options.onSend(...args)),
    o: common_vendor.t($data.isRecording ? "松开发送" : "按住说话"),
    p: common_vendor.n("voice-btn " + ($data.isRecording ? "recording" : "") + ($data.mode === "ai" ? " disabled" : "")),
    q: common_vendor.o((...args) => $options.startRecording && $options.startRecording(...args)),
    r: common_vendor.o((...args) => $options.stopRecording && $options.stopRecording(...args)),
    s: common_vendor.o((...args) => $options.cancelRecording && $options.cancelRecording(...args)),
    t: common_vendor.t($data.aiLoading ? "发送中" : "发送"),
    v: $data.aiLoading,
    w: common_vendor.o((...args) => $options.onSend && $options.onSend(...args)),
    x: $data.isRecording
  }, $data.isRecording ? {
    y: common_vendor.f(3, (i, k0, i0) => {
      return {
        a: i
      };
    }),
    z: common_vendor.t($data.recordingTime)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
