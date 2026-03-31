<template>
    <view class="chat-page">
        <!-- 顶部角色切换 -->
        <view class="chat-header">
            <view :class="'tab ' + (mode == 'doctor' ? 'tab-active' : '')" @tap="switchMode" data-mode="doctor">医生咨询</view>
            <view :class="'tab ' + (mode == 'ai' ? 'tab-active' : '')" @tap="switchMode" data-mode="ai">AI 小助理</view>
        </view>

        <!-- 聊天内容区域 -->
        <scroll-view class="chat-body" :scroll-y="true" :scroll-with-animation="true" :scroll-top="scrollTop" :scroll-into-view="scrollIntoView">
            <block v-for="(item, index) in messages" :key="index">
                <view :class="'msg-row ' + (item.from == 'me' ? 'msg-right' : 'msg-left')" :id="'msg-' + item.id">
                    <!-- 自己的头像 -->
                    <image v-if="item.from == 'me'" class="avatar" src="/static/assets/avatar-me.png" mode="aspectFill"></image>

                    <!-- 医生 / AI 头像 -->
                    <image
                        v-if="item.from != 'me'"
                        class="avatar"
                        :src="mode == 'doctor' ? '/static/assets/avatar-doctor.png' : '/static/assets/avatar-ai.png'"
                        mode="aspectFill"
                    ></image>

                    <view class="bubble">
                        <!-- 显示音频控件 (如果是语音消息) -->
                        <view v-if="item.isVoice" class="voice-message" @tap="playVoice(item)">
                            <view class="voice-icon">
                                <view :class="'voice-wave ' + (item.isPlaying ? 'playing' : '')">
                                    <view class="wave-bar"></view>
                                    <view class="wave-bar"></view>
                                    <view class="wave-bar"></view>
                                </view>
                            </view>
                            <text class="voice-duration">{{item.duration}}″</text>
                        </view>
                        
                        <!-- 文字消息 -->
                        <view v-else class="bubble-text">{{ item.text }}</view>
                        
                        <view class="bubble-time">{{ item.time }}</view>
                    </view>
                </view>
            </block>

            <view v-if="messages.length == 0" class="empty-tip">
                <view class="empty-title">
                    {{ mode == 'doctor' ? '向医生描述您的情况' : '可以问问小助理：' }}
                </view>
                <view class="empty-sub">例如："最近老是咳嗽怎么办？"、"我的血压有点高需要就诊吗？"</view>
                <view class="empty-sub">支持语音输入，长按下方麦克风按钮录音</view>
            </view>
        </scroll-view>

        <!-- 底部输入栏 -->
        <view class="chat-input-bar">
            <view class="input-container">
                <input
                    class="chat-input"
                    :placeholder="mode == 'doctor' ? '请输入要咨询医生的问题…' : '请输入想问 AI 的问题…'"
                    confirm-type="send"
                    :value="inputValue"
                    @input="onInput"
                    @confirm="onSend"
                />
                
                <!-- 语音按钮 -->
                <view 
                    :class="'voice-btn ' + (isRecording ? 'recording' : '')"
                    @touchstart="startRecording" 
                    @touchend="stopRecording"
                    @touchcancel="cancelRecording"
                >
                    <text class="voice-icon-text">{{isRecording ? '松开发送' : '按住说话'}}</text>
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
                <view class="recording-time">{{recordingTime}}s</view>
                <view class="recording-tip">上滑取消发送</view>
            </view>
        </view>
    </view>
</template>

<script>
import { getUser } from '@/utils/session.js';
import { listThread, appendMessage, markRead, formatHM } from '@/api/consult.js';

export default {
    data() {
        return {
            mode: 'doctor',    // doctor / ai
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

            // doctor 咨询：对话上下文
            patientId: '',
            patientName: '',
            pollTimer: null,
            lastThreadSig: '',
            pendingVoiceSend: false,
            recordingStartY: 0
        };
    },
    onLoad() {
        // 当前患者信息（用于医生咨询对话 key）
        const u = getUser();
        this.patientId = (u && (u.id || u.userId)) ? String(u.id || u.userId) : '';
        this.patientName = (u && u.name) ? String(u.name) : '';

        // 初始化录音管理器
        this.recorderManager = uni.getRecorderManager();
        
        // 监听录音事件
        this.recorderManager.onStop((res) => {
            if (this.pendingVoiceSend) {
                this.pendingVoiceSend = false;
                console.log('录音完成', res);
                // 录音文件路径
                const { tempFilePath, duration } = res;
                this.sendVoiceMessage(tempFilePath, Math.round(duration/1000));
            }
        });
        
        // 初始化音频播放器
        this.innerAudioContext = uni.createInnerAudioContext();
        this.innerAudioContext.onEnded(() => {
            if (this.currentPlayingMsg) {
                this.currentPlayingMsg.isPlaying = false;
                this.messages = [...this.messages]; // 触发视图更新
                this.currentPlayingMsg = null;
            }
        });

        // 初次进入：若默认 doctor，则加载对话
        this.loadDoctorThread(true);
    },
    onShow() {
        this.startPolling();
        if (this.mode === 'doctor') {
            this.loadDoctorThread(false);
        }
    },
    onHide() {
        this.stopPolling();
    },
    onUnload() {
        // 页面卸载时清理资源
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
            this.inputValue = '';
            this.scrollTop = 0;
            this.scrollIntoView = '';

            if (mode === 'doctor') {
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

            if (this.mode === 'doctor') {
                this.sendDoctorText(text);
            } else {
                this.sendTextMessage(text);
            }
            this.inputValue = '';
        },
        
        // 发送文本消息
        sendTextMessage(text) {
            const now = this.formatTime(new Date());
            const msgId = Date.now();
            const newMsg = {
                id: msgId,
                from: 'me',
                text,
                time: now,
                isVoice: false
            };
            
            this.messages = [...this.messages, newMsg];
            this.scrollIntoView = 'msg-' + msgId;
            
            // 模拟自动回复
            this.simulateReply(text);
        },

        // 医生咨询：发送文字（患者 -> 医生）
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
                uni.showToast({ title: e?.message || '发送失败', icon: 'none' });
            }
        },
        
        // 发送语音消息
        sendVoiceMessage(voicePath, duration) {
            if (this.mode === 'doctor') {
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
                    uni.showToast({ title: e?.message || '发送失败', icon: 'none' });
                }
                return;
            }

            // AI 模式：保持原有演示行为
            const now = this.formatTime(new Date());
            const msgId = Date.now();
            const newMsg = {
                id: msgId,
                from: 'me',
                voicePath,
                time: now,
                isVoice: true,
                duration,
                isPlaying: false
            };
            
            this.messages = [...this.messages, newMsg];
            this.scrollIntoView = 'msg-' + msgId;
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
                    from: 'other',
                    text: this.mode === 'doctor'
                          ? '已收到您的情况，如症状明显加重或出现胸痛、呼吸困难，请及时就近就医。'
                          : '根据您输入的内容，建议先继续规范用药、注意休息。如有持续不适请联系医生或前往医院就诊。',
                    time: this.formatTime(new Date()),
                    isVoice: false
                };
                
                this.messages = [...this.messages, reply];
                this.scrollIntoView = 'msg-' + msgId;
                
                // 如果是AI模式，有30%概率回复一条语音消息示例
                if (this.mode === 'ai' && Math.random() < 0.3) {
                    setTimeout(() => {
                        const voiceReplyId = Date.now();
                        const voiceReply = {
                            id: voiceReplyId,
                            from: 'other',
                            voicePath: '/static/demo-audio.mp3', // 这里使用一个示例路径，实际项目中应该是动态生成的语音文件
                            time: this.formatTime(new Date()),
                            isVoice: true,
                            duration: 5, // 假设是5秒的语音
                            isPlaying: false
                        };
                        
                        this.messages = [...this.messages, voiceReply];
                        this.scrollIntoView = 'msg-' + voiceReplyId;
                    }, 1000);
                }
            }, 600);
        },
        
        // 开始录音
        startRecording(e) {
            console.log('开始录音');
            this.isRecording = true;
            this.pendingVoiceSend = false;
            this.recordingTime = 0;
            this.recordingStartY = (e && e.touches && e.touches[0] && e.touches[0].clientY) ? e.touches[0].clientY : 0;
            
            // 开启计时器
            this.recordTimer = setInterval(() => {
                this.recordingTime += 1;
                // 限制最长录音时间为60秒
                if (this.recordingTime >= 60) {
                    this.stopRecording();
                }
            }, 1000);
            
            // 配置录音参数
            const options = {
                duration: 60000, // 最长60秒
                sampleRate: 16000,
                numberOfChannels: 1,
                encodeBitRate: 64000,
                format: 'mp3',
                frameSize: 50
            };
            
            // 开始录音
            this.recorderManager.start(options);
        },
        
        // 停止录音并发送
        stopRecording(e) {
            if (!this.isRecording) return;
            
            console.log('停止录音');
            clearInterval(this.recordTimer);
            this.isRecording = false;
            
            // 检查是否是上滑取消
            if (e && e.changedTouches && e.changedTouches[0]) {
                const touch = e.changedTouches[0];
                const startY = this.recordingStartY || touch.clientY;
                
                if (touch.clientY < startY - 100) { // 上滑超过100px
                    console.log('上滑取消发送');
                    this.cancelRecording();
                    return;
                }
            }
            
            // 停止录音，结果会在onStop回调中处理
            this.pendingVoiceSend = true;
            this.recorderManager.stop();
        },
        
        // 取消录音
        cancelRecording() {
            console.log('取消录音');
            clearInterval(this.recordTimer);
            this.isRecording = false;
            this.pendingVoiceSend = false;
            this.recorderManager.stop();
            
            // 显示取消提示
            uni.showToast({
                title: '已取消录音',
                icon: 'none'
            });
        },
        
        // 播放语音消息
        playVoice(item) {
            // 如果当前有其他正在播放的消息，先停止它
            if (this.currentPlayingMsg && this.currentPlayingMsg !== item) {
                this.currentPlayingMsg.isPlaying = false;
                this.innerAudioContext.stop();
            }
            
            if (item.isPlaying) {
                // 如果点击的是当前正在播放的消息，则暂停
                item.isPlaying = false;
                this.innerAudioContext.pause();
                this.currentPlayingMsg = null;
            } else {
                // 开始播放新的语音消息
                item.isPlaying = true;
                this.currentPlayingMsg = item;
                
                this.innerAudioContext.src = item.voicePath;
                this.innerAudioContext.play();
            }
            
            // 触发视图更新
            this.messages = [...this.messages];
        },

        formatTime(date) {
            const h = date.getHours().toString().padStart(2, '0');
            const m = date.getMinutes().toString().padStart(2, '0');
            return `${h}:${m}`;
        },

        // doctor 咨询：加载会话并映射到页面消息格式
        loadDoctorThread(scrollToBottom) {
            if (this.mode !== 'doctor') return;
            if (!this.patientId) return;

            try {
                // 患者打开即视为已读医生消息
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

        startPolling() {
            if (this.mode !== 'doctor') return;
            if (!this.patientId) return;
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

/* 添加语音相关样式 */
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
    0%, 100% {
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
