<template>
  <view class="chat-page">
    <!-- 顶部标题 -->
    <view class="staff-chat-head">
      <view class="staff-chat-title">{{ patientName || '患者消息' }}</view>
      <view class="staff-chat-sub">对话将同步到患者端「在线咨询-医生咨询」</view>
    </view>

    <!-- 聊天内容区域 -->
    <scroll-view
      class="chat-body"
      :scroll-y="true"
      :scroll-with-animation="true"
      :scroll-into-view="scrollIntoView"
    >
      <block v-for="(item, index) in messages" :key="index">
        <view :class="'msg-row ' + (item.from === 'me' ? 'msg-right' : 'msg-left')" :id="'msg-' + item.id">
          <image
            v-if="item.from === 'me'"
            class="avatar"
            src="/static/assets/avatar-doctor.png"
            mode="aspectFill"
          ></image>

          <image
            v-else
            class="avatar"
            src="/static/assets/avatar-me.png"
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
        <view class="empty-title">暂无对话</view>
        <view class="empty-sub">患者在「在线咨询-医生咨询」发送的消息会在这里显示。</view>
      </view>
    </scroll-view>

    <!-- 底部输入栏 -->
    <view class="chat-input-bar">
      <view class="input-container">
        <input
          class="chat-input"
          placeholder="输入回复内容…"
          confirm-type="send"
          :value="inputValue"
          @input="onInput"
          @confirm="onSend"
        />

        <view
          :class="'voice-btn ' + (isRecording ? 'recording' : '')"
          @touchstart="startRecording"
          @touchend="stopRecording"
          @touchcancel="cancelRecording"
        >
          <text class="voice-icon-text">{{ isRecording ? '松开发送' : '按住说话' }}</text>
        </view>
      </view>

      <button class="send-btn" type="primary" size="mini" @tap="onSend">发送</button>
    </view>

    <!-- 录音提示蒙层 -->
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
import { listThread, appendMessage, markRead, formatHM } from '@/api/consult.js';

function safeDecode(value) {
  if (value == null) return '';
  const s = String(value);
  if (!s) return '';
  try {
    return decodeURIComponent(s);
  } catch (e) {
    return s;
  }
}

export default {
  data() {
    return {
      patientId: '',
      patientName: '',
      messages: [],
      inputValue: '',
      scrollIntoView: '',
      pollTimer: null,
      lastThreadSig: '',

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
    this.patientId = options && options.id ? String(options.id) : '';
    this.patientName = safeDecode((options && (options.name || options.patientName)) || '');

    uni.setNavigationBarTitle({
      title: this.patientName || '患者消息'
    });

    // 初始化录音
    this.recorderManager = uni.getRecorderManager();
    this.recorderManager.onStop((res) => {
      if (this.pendingVoiceSend) {
        this.pendingVoiceSend = false;
        const { tempFilePath, duration } = res;
        this.sendVoiceMessage(tempFilePath, Math.round((duration || 0) / 1000));
      }
    });

    // 初始化音频播放器
    this.innerAudioContext = uni.createInnerAudioContext();
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
    if (this.innerAudioContext) this.innerAudioContext.destroy();
    if (this.recordTimer) clearInterval(this.recordTimer);
  },

  methods: {
    onInput(e) {
      this.inputValue = e.detail.value;
    },

    onSend() {
      const text = (this.inputValue || '').trim();
      if (!text) return;
      this.sendDoctorText(text);
      this.inputValue = '';
    },

    sendDoctorText(text) {
      if (!this.patientId) {
        uni.showToast({ title: '缺少 patientId', icon: 'none' });
        return;
      }
      try {
        appendMessage(this.patientId, {
          sender: 'doctor',
          text,
          isVoice: false,
          time: formatHM(Date.now())
        });
        this.loadThread(true);
      } catch (e) {
        uni.showToast({ title: e && e.message ? e.message : '发送失败', icon: 'none' });
      }
    },

    sendVoiceMessage(voicePath, duration) {
      if (!this.patientId) {
        uni.showToast({ title: '缺少 patientId', icon: 'none' });
        return;
      }
      try {
        appendMessage(this.patientId, {
          sender: 'doctor',
          isVoice: true,
          voicePath,
          duration,
          time: formatHM(Date.now())
        });
        this.loadThread(true);
      } catch (e) {
        uni.showToast({ title: e && e.message ? e.message : '发送失败', icon: 'none' });
      }
    },

    loadThread(scrollToBottom) {
      if (!this.patientId) return;
      try {
        // 医护打开即视为已读患者消息
        markRead(this.patientId, 'doctor');

        const rows = listThread(this.patientId);
        const viewRows = rows.map((m) => ({
          id: m.id,
          from: m.sender === 'doctor' ? 'me' : 'other',
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
        console.warn('loadThread failed', e);
      }
    },

    startPolling() {
      if (!this.patientId) return;
      this.stopPolling();
      this.pollTimer = setInterval(() => {
        try {
          const rows = listThread(this.patientId);
          const last = rows[rows.length - 1];
          const sig = `${rows.length}_${last ? last.id : ''}_${last ? last.ts : ''}`;
          if (sig !== this.lastThreadSig) {
            this.loadThread(false);
          }
        } catch (e) {}
      }, 1000);
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
      this.recordingStartY =
        e && e.touches && e.touches[0] && e.touches[0].clientY ? e.touches[0].clientY : 0;

      this.recordTimer = setInterval(() => {
        this.recordingTime += 1;
        if (this.recordingTime >= 60) {
          this.stopRecording();
        }
      }, 1000);

      const options = {
        duration: 60000,
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 64000,
        format: 'mp3',
        frameSize: 50
      };
      this.recorderManager.start(options);
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

    // 播放语音
    playVoice(item) {
      if (!this.innerAudioContext) return;

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
</script>

<style>
@import '../../patient/chat/chat.css';

/* 顶部标题区（替换患者端的 tab 样式） */
.staff-chat-head {
  padding: 24rpx 24rpx 10rpx;
}

.staff-chat-title {
  font-size: 32rpx;
  font-weight: 900;
  color: rgba(31, 42, 55, 0.92);
}

.staff-chat-sub {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(31, 42, 55, 0.55);
}

/* 语音/输入样式与患者端保持一致 */
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