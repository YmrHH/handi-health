"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_session = require("../../../utils/session.js");
const api_consult = require("../../../api/consult.js");
const common_assets = require("../../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      mode: "doctor",
      // doctor / ai
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
      // doctor 咨询：对话上下文
      patientId: "",
      patientName: "",
      pollTimer: null,
      lastThreadSig: "",
      pendingVoiceSend: false,
      recordingStartY: 0
    };
  },
  onLoad() {
    const u = utils_session.getUser();
    this.patientId = u && (u.id || u.userId) ? String(u.id || u.userId) : "";
    this.patientName = u && u.name ? String(u.name) : "";
    this.recorderManager = common_vendor.index.getRecorderManager();
    this.recorderManager.onStop((res) => {
      if (this.pendingVoiceSend) {
        this.pendingVoiceSend = false;
        console.log("录音完成", res);
        const { tempFilePath, duration } = res;
        this.sendVoiceMessage(tempFilePath, Math.round(duration / 1e3));
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
    this.loadDoctorThread(true);
  },
  onShow() {
    this.startPolling();
    if (this.mode === "doctor") {
      this.loadDoctorThread(false);
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
      const mode = e.currentTarget.dataset.mode;
      this.mode = mode;
      this.inputValue = "";
      this.scrollTop = 0;
      this.scrollIntoView = "";
      if (mode === "doctor") {
        this.loadDoctorThread(true);
        this.startPolling();
      } else {
        this.stopPolling();
        this.messages = [];
      }
    },
    onInput(e) {
      this.inputValue = e.detail.value;
    },
    onSend() {
      const text = this.inputValue.trim();
      if (!text) {
        return;
      }
      if (this.mode === "doctor") {
        this.sendDoctorText(text);
      } else {
        this.sendTextMessage(text);
      }
      this.inputValue = "";
    },
    // 发送文本消息
    sendTextMessage(text) {
      const now = this.formatTime(/* @__PURE__ */ new Date());
      const msgId = Date.now();
      const newMsg = {
        id: msgId,
        from: "me",
        text,
        time: now,
        isVoice: false
      };
      this.messages = [...this.messages, newMsg];
      this.scrollIntoView = "msg-" + msgId;
      this.simulateReply(text);
    },
    // 医生咨询：发送文字（患者 -> 医生）
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
        common_vendor.index.showToast({ title: (e == null ? void 0 : e.message) || "发送失败", icon: "none" });
      }
    },
    // 发送语音消息
    sendVoiceMessage(voicePath, duration) {
      if (this.mode === "doctor") {
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
          common_vendor.index.showToast({ title: (e == null ? void 0 : e.message) || "发送失败", icon: "none" });
        }
        return;
      }
      const now = this.formatTime(/* @__PURE__ */ new Date());
      const msgId = Date.now();
      const newMsg = {
        id: msgId,
        from: "me",
        voicePath,
        time: now,
        isVoice: true,
        duration,
        isPlaying: false
      };
      this.messages = [...this.messages, newMsg];
      this.scrollIntoView = "msg-" + msgId;
      setTimeout(() => {
        this.simulateReply("已收到您的语音消息");
      }, 800);
    },
    // 模拟自动回复
    simulateReply(userInput) {
      setTimeout(() => {
        const msgId = Date.now();
        const reply = {
          id: msgId,
          from: "other",
          text: this.mode === "doctor" ? "已收到您的情况，如症状明显加重或出现胸痛、呼吸困难，请及时就近就医。" : "根据您输入的内容，建议先继续规范用药、注意休息。如有持续不适请联系医生或前往医院就诊。",
          time: this.formatTime(/* @__PURE__ */ new Date()),
          isVoice: false
        };
        this.messages = [...this.messages, reply];
        this.scrollIntoView = "msg-" + msgId;
        if (this.mode === "ai" && Math.random() < 0.3) {
          setTimeout(() => {
            const voiceReplyId = Date.now();
            const voiceReply = {
              id: voiceReplyId,
              from: "other",
              voicePath: "/static/demo-audio.mp3",
              // 这里使用一个示例路径，实际项目中应该是动态生成的语音文件
              time: this.formatTime(/* @__PURE__ */ new Date()),
              isVoice: true,
              duration: 5,
              // 假设是5秒的语音
              isPlaying: false
            };
            this.messages = [...this.messages, voiceReply];
            this.scrollIntoView = "msg-" + voiceReplyId;
          }, 1e3);
        }
      }, 600);
    },
    // 开始录音
    startRecording(e) {
      console.log("开始录音");
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
        // 最长60秒
        sampleRate: 16e3,
        numberOfChannels: 1,
        encodeBitRate: 64e3,
        format: "mp3",
        frameSize: 50
      };
      this.recorderManager.start(options);
    },
    // 停止录音并发送
    stopRecording(e) {
      if (!this.isRecording)
        return;
      console.log("停止录音");
      clearInterval(this.recordTimer);
      this.isRecording = false;
      if (e && e.changedTouches && e.changedTouches[0]) {
        const touch = e.changedTouches[0];
        const startY = this.recordingStartY || touch.clientY;
        if (touch.clientY < startY - 100) {
          console.log("上滑取消发送");
          this.cancelRecording();
          return;
        }
      }
      this.pendingVoiceSend = true;
      this.recorderManager.stop();
    },
    // 取消录音
    cancelRecording() {
      console.log("取消录音");
      clearInterval(this.recordTimer);
      this.isRecording = false;
      this.pendingVoiceSend = false;
      this.recorderManager.stop();
      common_vendor.index.showToast({
        title: "已取消录音",
        icon: "none"
      });
    },
    // 播放语音消息
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
    formatTime(date) {
      const h = date.getHours().toString().padStart(2, "0");
      const m = date.getMinutes().toString().padStart(2, "0");
      return `${h}:${m}`;
    },
    // doctor 咨询：加载会话并映射到页面消息格式
    loadDoctorThread(scrollToBottom) {
      if (this.mode !== "doctor")
        return;
      if (!this.patientId)
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
    startPolling() {
      if (this.mode !== "doctor")
        return;
      if (!this.patientId)
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
    a: common_vendor.n("tab " + ($data.mode == "doctor" ? "tab-active" : "")),
    b: common_vendor.o((...args) => $options.switchMode && $options.switchMode(...args)),
    c: common_vendor.n("tab " + ($data.mode == "ai" ? "tab-active" : "")),
    d: common_vendor.o((...args) => $options.switchMode && $options.switchMode(...args)),
    e: common_vendor.f($data.messages, (item, index, i0) => {
      return common_vendor.e({
        a: item.from == "me"
      }, item.from == "me" ? {
        b: common_assets._imports_1
      } : {}, {
        c: item.from != "me"
      }, item.from != "me" ? {
        d: $data.mode == "doctor" ? "/static/assets/avatar-doctor.png" : "/static/assets/avatar-ai.png"
      } : {}, {
        e: item.isVoice
      }, item.isVoice ? {
        f: common_vendor.n("voice-wave " + (item.isPlaying ? "playing" : "")),
        g: common_vendor.t(item.duration),
        h: common_vendor.o(($event) => $options.playVoice(item), index)
      } : {
        i: common_vendor.t(item.text)
      }, {
        j: common_vendor.t(item.time),
        k: common_vendor.n("msg-row " + (item.from == "me" ? "msg-right" : "msg-left")),
        l: "msg-" + item.id,
        m: index
      });
    }),
    f: $data.messages.length == 0
  }, $data.messages.length == 0 ? {
    g: common_vendor.t($data.mode == "doctor" ? "向医生描述您的情况" : "可以问问小助理：")
  } : {}, {
    h: $data.scrollTop,
    i: $data.scrollIntoView,
    j: $data.mode == "doctor" ? "请输入要咨询医生的问题…" : "请输入想问 AI 的问题…",
    k: $data.inputValue,
    l: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    m: common_vendor.o((...args) => $options.onSend && $options.onSend(...args)),
    n: common_vendor.t($data.isRecording ? "松开发送" : "按住说话"),
    o: common_vendor.n("voice-btn " + ($data.isRecording ? "recording" : "")),
    p: common_vendor.o((...args) => $options.startRecording && $options.startRecording(...args)),
    q: common_vendor.o((...args) => $options.stopRecording && $options.stopRecording(...args)),
    r: common_vendor.o((...args) => $options.cancelRecording && $options.cancelRecording(...args)),
    s: common_vendor.o((...args) => $options.onSend && $options.onSend(...args)),
    t: $data.isRecording
  }, $data.isRecording ? {
    v: common_vendor.f(3, (i, k0, i0) => {
      return {
        a: i
      };
    }),
    w: common_vendor.t($data.recordingTime)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
