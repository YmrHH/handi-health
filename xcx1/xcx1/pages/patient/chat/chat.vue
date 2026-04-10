<template>
  <view class="chat-page">
    <view class="chat-header">
      <view :class="'tab ' + (mode === 'doctor' ? 'tab-active' : '')" @tap="switchMode" data-mode="doctor">医生咨询</view>
      <view :class="'tab ' + (mode === 'ai' ? 'tab-active' : '')" @tap="switchMode" data-mode="ai">AI 小助理</view>
    </view>

    <scroll-view
      class="chat-body"
      :scroll-y="true"
      :scroll-with-animation="true"
      :scroll-top="scrollTop"
      :scroll-into-view="scrollIntoView"
    >
      <block v-for="(item, index) in messages" :key="index">
        <view :class="'msg-row ' + (item.from === 'me' ? 'msg-right' : 'msg-left')" :id="'msg-' + item.id">
          <image v-if="item.from === 'me'" class="avatar" src="/static/assets/avatar-me.png" mode="aspectFill"></image>
          <image
            v-else
            class="avatar"
            :src="mode === 'doctor' ? '/static/assets/avatar-doctor.png' : '/static/assets/avatar-ai.png'"
            mode="aspectFill"
          ></image>

          <view class="bubble">
            <view v-if="item.isVoice" class="voice-message" @tap="playVoice(item)">
              <view class="voice-icon">
                <view :class="'voice-wave ' + (item.isPlaying ? 'playing' : '')">
                  <view class="wave-bar"></view>
                  <view class="wave-bar"></view>
                  <view class="wave-bar"></view>
                </view>
              </view>
              <text class="voice-duration">{{ item.duration }}″</text>
            </view>

            <view v-else class="bubble-text">{{ item.text }}</view>
            <view class="bubble-time">{{ item.time }}</view>
          </view>
        </view>
      </block>

      <view v-if="messages.length === 0" class="empty-tip">
        <view class="empty-title">{{ mode === 'doctor' ? '向医生描述您的情况' : '可以问问小助理：' }}</view>
        <view class="empty-sub">例如：“最近老是咳嗽怎么办？”、“我的血压有点高需要就诊吗？”</view>
        <view class="empty-sub">
          {{ mode === 'doctor' ? '支持语音输入，长按下方麦克风按钮录音' : 'AI 会结合您最近体征、问卷、随访和预警信息回答' }}
        </view>
      </view>
    </scroll-view>

    <view class="chat-input-bar">
      <view class="input-container">
        <input
          class="chat-input"
          :placeholder="mode === 'doctor' ? '请输入要咨询医生的问题…' : '请输入想问 AI 的问题…'"
          confirm-type="send"
          :value="inputValue"
          @input="onInput"
          @confirm="onSend"
        />

        <view
          :class="'voice-btn ' + (isRecording ? 'recording' : '') + (mode === 'ai' ? ' disabled' : '')"
          @touchstart="startRecording"
          @touchend="stopRecording"
          @touchcancel="cancelRecording"
        >
          <text class="voice-icon-text">{{ isRecording ? '松开发送' : '按住说话' }}</text>
        </view>
      </view>

      <button class="send-btn" type="primary" size="mini" :disabled="aiLoading" @tap="onSend">
        {{ aiLoading ? '发送中' : '发送' }}
      </button>
    </view>

    <view v-if="isRecording" class="recording-mask">
      <view class="recording-indicator">
        <view class="recording-waves">
          <view class="wave-circle" v-for="i in 3" :key="i"></view>
        </view>
        <view class="recording-time">{{ recordingTime }}s</view>
        <view class="recording-tip">上滑取消发送</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getUser, getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
import { listThread, appendMessage, markRead, formatHM } from '@/api/consult.js';
import { chatWithAI } from '@/api/ai.js';

function safeArray(v) {
  return Array.isArray(v) ? v : [];
}

function createLocalMsg(from, payload = {}) {
  return {
    id: payload.id || ('msg_' + Date.now() + '_' + Math.floor(Math.random() * 100000)),
    from,
    text: payload.text || '',
    time: payload.time || formatHM(Date.now()),
    isVoice: !!payload.isVoice,
    voicePath: payload.voicePath || '',
    duration: payload.duration || 0,
    isPlaying: false
  };
}

function toAiHistory(rows) {
  return safeArray(rows)
    .filter((item) => item && !item.isVoice && item.text)
    .slice(-12)
    .map((item) => ({
      role: item.from === 'me' ? 'user' : 'assistant',
      content: item.text
    }));
}

export default {
  data() {
    return {
      mode: 'doctor',
      messages: [],
      inputValue: '',
      scrollTop: 0,
      scrollIntoView: '',
      isRecording: false,
      recordingTime: 0,
      recordTimer: null,
      recorderManager: null,
      innerAudioContext: null,
      currentPlayingMsg: null,

      patientId: '',
      patientName: '',
      pollTimer: null,
      lastThreadSig: '',
      pendingVoiceSend: false,
      recordingStartY: 0,
      aiLoading: false
    };
  },
  onLoad() {
    const u = getUser();
    this.patientId = (u && (u.id || u.userId)) ? String(u.id || u.userId) : '';
    this.patientName = (u && u.name) ? String(u.name) : '';

    this.recorderManager = uni.getRecorderManager();
    this.recorderManager.onStop((res) => {
      if (!this.pendingVoiceSend) return;
      this.pendingVoiceSend = false;
      const { tempFilePath, duration } = res || {};
      if (!tempFilePath) return;
      this.sendVoiceMessage(tempFilePath, Math.max(1, Math.round((duration || 1000) / 1000)));
    });

    this.innerAudioContext = uni.createInnerAudioContext();
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
    if (this.mode === 'doctor') {
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
      const mode = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.mode : 'doctor';
      this.mode = mode;
      this.inputValue = '';
      this.scrollTop = 0;
      this.scrollIntoView = '';

      if (mode === 'doctor') {
        this.stopPolling();
        this.loadDoctorThread(true);
        this.startPolling();
      } else {
        this.stopPolling();
        this.loadAiThread(true);
      }
    },

    onInput(e) {
      this.inputValue = e && e.detail ? (e.detail.value || '') : '';
    },

    onSend() {
      const text = (this.inputValue || '').trim();
      if (!text) return;

      if (this.mode === 'doctor') {
        this.sendDoctorText(text);
      } else {
        this.sendAiText(text);
      }
      this.inputValue = '';
    },

    async sendAiText(text) {
      if (this.aiLoading) return;
      const userMsg = createLocalMsg('me', { text });
      const nextRows = [...this.messages, userMsg];
      this.messages = nextRows;
      this.persistAiThread(nextRows);
      this.scrollIntoView = 'msg-' + userMsg.id;
      this.aiLoading = true;

      try {
        uni.showLoading({ title: 'AI思考中', mask: false });
      } catch (e) {}

      try {
        const payload = await chatWithAI({
          patientId: this.patientId ? Number(this.patientId) : undefined,
          message: text,
          history: toAiHistory(nextRows)
        });
        const replyText = (payload && (payload.reply || payload.content || payload.text))
          ? (payload.reply || payload.content || payload.text)
          : '暂时未获取到回复，请稍后重试。';
        const aiMsg = createLocalMsg('other', { text: replyText });
        const finalRows = [...this.messages, aiMsg];
        this.messages = finalRows;
        this.persistAiThread(finalRows);
        this.scrollIntoView = 'msg-' + aiMsg.id;
      } catch (e) {
        const errText = e && e.message ? e.message : 'AI 服务暂时不可用，请稍后重试';
        const aiMsg = createLocalMsg('other', { text: '抱歉，当前 AI 服务连接失败：' + errText });
        const finalRows = [...this.messages, aiMsg];
        this.messages = finalRows;
        this.persistAiThread(finalRows);
        this.scrollIntoView = 'msg-' + aiMsg.id;
      } finally {
        this.aiLoading = false;
        try {
          uni.hideLoading();
        } catch (e) {}
      }
    },

    sendDoctorText(text) {
      if (!this.patientId) {
        uni.showToast({ title: '缺少 patientId，无法发送', icon: 'none' });
        return;
      }

      try {
        appendMessage(this.patientId, {
          sender: 'patient',
          text,
          isVoice: false,
          time: formatHM(Date.now())
        });
        this.loadDoctorThread(true);
      } catch (e) {
        uni.showToast({ title: (e && e.message) || '发送失败', icon: 'none' });
      }
    },

    sendVoiceMessage(voicePath, duration) {
      if (this.mode === 'ai') {
        uni.showToast({ title: 'AI 对话当前仅支持文字输入', icon: 'none' });
        return;
      }
      if (!this.patientId) {
        uni.showToast({ title: '缺少 patientId，无法发送', icon: 'none' });
        return;
      }

      try {
        appendMessage(this.patientId, {
          sender: 'patient',
          isVoice: true,
          voicePath,
          duration,
          time: formatHM(Date.now())
        });
        this.loadDoctorThread(true);
      } catch (e) {
        uni.showToast({ title: (e && e.message) || '发送失败', icon: 'none' });
      }
    },

    startRecording(e) {
      if (this.mode === 'ai') {
        uni.showToast({ title: 'AI 对话当前仅支持文字输入', icon: 'none' });
        return;
      }

      this.isRecording = true;
      this.pendingVoiceSend = false;
      this.recordingTime = 0;
      this.recordingStartY = (e && e.touches && e.touches[0] && e.touches[0].clientY) ? e.touches[0].clientY : 0;

      this.recordTimer = setInterval(() => {
        this.recordingTime += 1;
        if (this.recordingTime >= 60) {
          this.stopRecording();
        }
      }, 1000);

      this.recorderManager.start({
        duration: 60000,
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 64000,
        format: 'mp3',
        frameSize: 50
      });
    },

    stopRecording(e) {
      if (!this.isRecording) return;
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
      uni.showToast({ title: '已取消录音', icon: 'none' });
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
      if (this.mode !== 'doctor' || !this.patientId) return;
      try {
        markRead(this.patientId, 'patient');
        const rows = listThread(this.patientId);
        const viewRows = rows.map((m) => ({
          id: m.id,
          from: m.sender === 'patient' ? 'me' : 'other',
          text: m.text || '',
          time: m.time || '',
          isVoice: !!m.isVoice,
          voicePath: m.voicePath || '',
          duration: m.duration || 0,
          isPlaying: false
        }));
        this.messages = viewRows;

        const last = rows[rows.length - 1];
        this.lastThreadSig = `${rows.length}_${last ? last.id : ''}_${last ? last.ts : ''}`;
        if (scrollToBottom && last) {
          this.scrollIntoView = 'msg-' + last.id;
        }
      } catch (e) {
        console.warn('loadDoctorThread failed', e);
      }
    },

    loadAiThread(scrollToBottom) {
      const rows = safeArray(getScopedStorageSync('ai_chat_thread', [], this.patientId));
      this.messages = rows;
      const last = rows[rows.length - 1];
      if (scrollToBottom && last) {
        this.scrollIntoView = 'msg-' + last.id;
      }
    },

    persistAiThread(rows) {
      setScopedStorageSync('ai_chat_thread', safeArray(rows).slice(-100), this.patientId);
    },

    startPolling() {
      if (this.mode !== 'doctor' || !this.patientId) return;
      this.stopPolling();
      this.pollTimer = setInterval(() => {
        try {
          const rows = listThread(this.patientId);
          const last = rows[rows.length - 1];
          const sig = `${rows.length}_${last ? last.id : ''}_${last ? last.ts : ''}`;
          if (sig !== this.lastThreadSig) {
            this.loadDoctorThread(false);
          }
        } catch (e) {}
      }, 1000);
    },

    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer);
        this.pollTimer = null;
      }
    }
  }
};
</script>

<style>
@import './chat.css';

.input-container {
  display: flex;
  flex: 1;
  position: relative;
  align-items: center;
  background: #f1f5f9;
  border-radius: 999rpx;
  padding: 6rpx 10rpx;
}

.chat-input {
  flex: 1;
  padding: 12rpx 18rpx;
  font-size: 26rpx;
  background: transparent;
}

.voice-btn {
  padding: 10rpx 20rpx;
  font-size: 22rpx;
  color: #64748b;
  border-left: 1px solid #e2e8f0;
}

.voice-btn.recording {
  background-color: #e2e8f0;
  color: #1f6feb;
}

.voice-btn.disabled {
  opacity: 0.6;
}

.voice-icon-text {
  font-size: 22rpx;
}

.voice-message {
  display: flex;
  align-items: center;
  padding: 8rpx 0;
}

.voice-icon {
  margin-right: 12rpx;
  display: flex;
  align-items: center;
}

.voice-wave {
  display: flex;
  align-items: center;
  height: 24rpx;
}

.wave-bar {
  width: 4rpx;
  height: 14rpx;
  background-color: #64748b;
  margin: 0 2rpx;
  border-radius: 2rpx;
}

.voice-wave.playing .wave-bar {
  animation: sound-wave 1s infinite ease;
}

.voice-wave.playing .wave-bar:nth-child(1) {
  animation-delay: 0s;
}

.voice-wave.playing .wave-bar:nth-child(2) {
  animation-delay: 0.2s;
  height: 16rpx;
}

.voice-wave.playing .wave-bar:nth-child(3) {
  animation-delay: 0.4s;
  height: 22rpx;
}

.voice-duration {
  font-size: 24rpx;
  color: #64748b;
}

.msg-right .voice-duration {
  color: #e2e8f0;
}

.recording-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  justify-content: center;
  align-items: center;
}

.recording-indicator {
  width: 240rpx;
  height: 240rpx;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.recording-waves {
  position: relative;
  width: 120rpx;
  height: 120rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

.wave-circle {
  position: absolute;
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(31, 111, 235, 0.2);
  animation: wave-expand 2s infinite;
}

.wave-circle:nth-child(2) {
  animation-delay: 0.5s;
}

.wave-circle:nth-child(3) {
  animation-delay: 1s;
}

.recording-time {
  margin-top: 20rpx;
  font-size: 36rpx;
  color: #1f6feb;
  font-weight: 600;
}

.recording-tip {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #64748b;
}

@keyframes sound-wave {
  0%,
  100% {
    height: 14rpx;
  }
  50% {
    height: 24rpx;
  }
}

@keyframes wave-expand {
  0% {
    transform: scale(0.5);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}
</style>
