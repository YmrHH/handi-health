<template>
  <view class="app-page hp">
    <view class="app-card app-card--glass hp-head">
      <text class="hp-title">帮助与客服</text>
      <text class="hp-sub">常见问题、使用引导与联系方式。用于出院随访监测与预警服务。</text>
    </view>

    <view class="hp-section">
      <text class="app-section-title">快速指引</text>
    </view>
    <view class="app-card hp-card">
      <view class="hp-quick">
        <view class="hp-qitem" @tap="openGuide('device')">
          <text class="hp-qtitle">设备连接</text>
          <text class="hp-qdesc">蓝牙绑定与同步</text>
        </view>
        <view class="hp-qitem" @tap="openGuide('data')">
          <text class="hp-qtitle">数据补录</text>
          <text class="hp-qdesc">无法自动获取时</text>
        </view>
        <view class="hp-qitem" @tap="openGuide('followup')">
          <text class="hp-qtitle">随访申请</text>
          <text class="hp-qdesc">主动申请与进度</text>
        </view>
      </view>
    </view>

    <view class="hp-section">
      <text class="app-section-title">常见问题</text>
    </view>
    <view class="app-card app-card--glass hp-card">
      <view v-for="(q, idx) in faqs" :key="idx" class="hp-faq" @tap="toggle(idx)">
        <view class="hp-faq-head">
          <text class="hp-faq-q">{{ q.q }}</text>
          <text class="hp-faq-arrow">{{ q.open ? '˅' : '›' }}</text>
        </view>
        <view v-if="q.open" class="hp-faq-body">
          <text class="hp-faq-a">{{ q.a }}</text>
        </view>
      </view>
    </view>

    <view class="hp-section">
      <text class="app-section-title">联系客服</text>
    </view>
    <view class="app-card hp-card">
      <view class="hp-contact-row">
        <text class="hp-contact-k">服务时间</text>
        <text class="hp-contact-v">工作日 09:00-18:00</text>
      </view>
      <view class="hp-divider"></view>

      <view class="hp-contact-row">
        <text class="hp-contact-k">电话</text>
        <text class="hp-contact-v hp-link" @tap="call">点击拨打</text>
      </view>

      <!-- 微信小程序在线客服入口 -->
      <!-- #ifdef MP-WEIXIN -->
      <view class="hp-divider"></view>
      <button class="hp-kf-btn" open-type="contact">在线客服</button>
      <!-- #endif -->

      <view class="hp-warn">
        <text class="hp-warn-title">紧急情况提示</text>
        <text class="hp-warn-text">若出现胸痛、呼吸困难、意识不清等紧急情况，请立即拨打 120 或就近就医。</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      faqs: [
        {
          q: '设备连接不上怎么办？',
          a: '请确认手机蓝牙已开启并允许小程序使用蓝牙权限；在「设备管理」页点击“开启蓝牙→搜索设备→连接”。若仍失败，关闭蓝牙后重试或重启设备。',
          open: true
        },
        {
          q: '为什么有些指标需要手动填写？',
          a: '部分指标（如血糖、体温、睡眠）可能因设备型号或使用场景无法自动获取，可在「设备管理」页进行手动补录，系统会同步到身体数据展示与趋势分析。',
          open: false
        },
        {
          q: '随访申请提交后多久会处理？',
          a: '提交后状态为“待处理”。医生端审核并安排随访后会通过「消息中心」通知你。若情况紧急，可在申请中选择更高紧急程度并补充说明。',
          open: false
        },
        {
          q: '医生建议从哪里来的？',
          a: '医生建议由医生 Web 端编辑并审核确认后下发到小程序端，展示在「我的」页面顶部与「消息中心」中。',
          open: false
        },
        {
          q: '如何清除本地缓存？会影响医生端数据吗？',
          a: '可在「隐私与授权」页清除本地缓存，仅清除本机展示用数据（头像姓名、已读状态等），不会影响医生端已存档的随访与记录。',
          open: false
        }
      ]
    };
  },
  methods: {
    toggle(i) {
      this.faqs = this.faqs.map((x, idx) => (idx === i ? { ...x, open: !x.open } : x));
    },
    openGuide(type) {
      let title = '使用指引';
      let content = '';
      if (type === 'device') {
        title = '设备连接';
        content = '路径：我的 → 设备绑定\n步骤：开启蓝牙 → 搜索设备 → 连接 → 自动同步数据到身体数据展示。';
      } else if (type === 'data') {
        title = '数据补录';
        content = '路径：我的 → 设备绑定 → 手动补录\n适用：无法自动获取的指标（血糖/体温/睡眠等）。保存后会同步到身体数据展示与趋势页。';
      } else {
        title = '随访申请';
        content = '路径：我的 → 随访关联 → 查看详情\n填写当前情况与随访诉求，提交后等待医生端审核安排，进度会在消息中心提醒。';
      }
      uni.showModal({ title, content, showCancel: false, confirmText: '知道了' });
    },
    call() {
      // 可替换为你们真实客服电话
      uni.makePhoneCall({
        phoneNumber: '4000000000',
        fail: () => uni.showToast({ title: '无法拨打电话', icon: 'none' })
      });
    }
  }
};
</script>

<style src="./help.css"></style>
