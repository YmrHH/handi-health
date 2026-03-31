<template>
  <view class="app-page staff-page">
    <view class="app-card app-card--glass staff-card">
      <view class="staff-header">
        <view>
          <view class="staff-title">在管患者</view>
          <view class="staff-sub">查看患者健康指标，对标患者端。</view>
        </view>
      </view>

      <view class="search-bar">
        <input class="search-input" v-model="keyword" placeholder="搜索患者姓名/ID" confirm-type="search" @confirm="onSearch" />
        <view class="search-btn" @tap="onSearch">搜索</view>
      </view>
    </view>

    <view class="app-card staff-card">
      <view class="section-title">患者列表</view>

      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="filteredPatients.length === 0" class="empty">暂无在管患者</view>

      <view v-else>
        <view class="patient-card" v-for="p in filteredPatients" :key="p.id">
          <view class="row">
            <view class="name">
              {{ p.name || '未知患者' }}
              <text class="sub" v-if="p.age"> · {{ p.age }}岁</text>
            </view>
            <view class="risk" :class="riskClass(p)">{{ riskText(p) }}</view>
          </view>

          <view class="meta">
            <text class="meta-item">ID：{{ p.id }}</text>
            <text class="meta-item">主要疾病：{{ p.disease || '—' }}</text>
          </view>

          <view class="btns">
            <view class="btn ghost" @tap="onOpenData" :data-id="p.id" :data-name="p.name">查看数据</view>
            <view class="btn primary" @tap="onOpenChat" :data-id="p.id" :data-name="p.name">
              患者消息
              <text v-if="unreadDoctor(p.id) > 0" class="badge">{{ unreadDoctor(p.id) > 99 ? '99+' : unreadDoctor(p.id) }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <staff-tabbar active="managed"></staff-tabbar>
  </view>
</template>

<script>
import { listPatients } from '@/api/staff.js';
import { getUnreadCount } from '@/api/consult.js';

export default {
  data() {
    return {
      keyword: '',
      patients: [],
      loading: false
    };
  },
  computed: {
    filteredPatients() {
      const kw = (this.keyword || '').trim();
      if (!kw) return this.patients;
      return this.patients.filter((p) => {
        const s = `${p.name || ''}${p.id || ''}`.toLowerCase();
        return s.includes(kw.toLowerCase());
      });
    }
  },
  onLoad() {
    this.fetchPatients();
  },
  methods: {
    unreadDoctor(patientId) {
      try {
        return getUnreadCount(patientId, 'doctor');
      } catch (e) {
        return 0;
      }
    },
    riskClass(p) {
      const r = (p && p.riskLevel) || '';
      if (r === '高' || r === 'HIGH') return 'high';
      if (r === '低' || r === 'LOW') return 'low';
      return 'mid';
    },
    riskText(p) {
      const r = (p && p.riskLevel) || '';
      if (r === '高' || r === 'HIGH') return '高风险';
      if (r === '低' || r === 'LOW') return '低风险';
      return '中风险';
    },
    onSearch() {
      // 当前为前端本地过滤：keyword 变化后 filteredPatients 会自动更新
    },
    fetchPatients() {
      this.loading = true;
      listPatients({ page: 1, pageSize: 200 })
        .then((rows) => {
          this.patients = Array.isArray(rows) ? rows : [];
        })
        .catch((e) => {
          this.patients = [];
          uni.showToast({ title: e?.message || '加载患者失败', icon: 'none' });
        })
        .finally(() => {
          this.loading = false;
        });
    },
    onOpenData(e) {
      const id = e.currentTarget.dataset.id;
      const name = e.currentTarget.dataset.name;
      uni.navigateTo({
        url: `/pages/staff/patientData/patientData?id=${encodeURIComponent(id)}&name=${encodeURIComponent(name || '')}`
      });
    }
    ,
    onOpenChat(e) {
      const id = e.currentTarget.dataset.id;
      const name = e.currentTarget.dataset.name;
      uni.navigateTo({
        url: `/pages/staff/patientChat/patientChat?id=${encodeURIComponent(id)}&name=${encodeURIComponent(name || '')}`
      });
    }
  }
};
</script>

<style>
.staff-page{ padding-bottom: 160rpx; }

.search-bar{
  display:flex;
  flex-direction:row;
  align-items:center;
  margin-top: 18rpx;
  gap: 12rpx;
}

.search-input{
  flex:1;
  height: 78rpx;
  padding: 0 18rpx;
  background: rgba(255,255,255,0.96);
  border-radius: 18rpx;
  border: 1rpx solid rgba(91,108,255,0.12);
}

.search-btn{
  height: 78rpx;
  padding: 0 20rpx;
  border-radius: 18rpx;
  background: rgba(91,108,255,0.10);
  color: rgba(91,108,255,0.95);
  display:flex;
  align-items:center;
  justify-content:center;
  font-weight: 750;
}

.section-title{
  font-size: 28rpx;
  font-weight: 850;
  color: rgba(31,42,55,0.90);
  margin-bottom: 14rpx;
}

.empty{
  padding: 40rpx 0;
  color: rgba(31,42,55,0.55);
  text-align:center;
}

.patient-card{
  padding: 18rpx;
  border-radius: 18rpx;
  border: 1rpx solid rgba(91,108,255,0.10);
  background: rgba(255,255,255,0.96);
  margin-bottom: 16rpx;
}

.row{
  display:flex;
  flex-direction:row;
  align-items:center;
  justify-content:space-between;
}

.name{
  font-size: 30rpx;
  font-weight: 900;
  color: rgba(31,42,55,0.92);
}

.sub{
  font-size: 24rpx;
  color: rgba(31,42,55,0.55);
}

.risk{
  font-size: 22rpx;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  font-weight: 850;
}
.risk.high{ background: rgba(255, 88, 88, 0.12); color: rgba(255, 88, 88, 0.95); }
.risk.mid{ background: rgba(255, 193, 7, 0.16); color: rgba(245, 158, 11, 0.95); }
.risk.low{ background: rgba(34, 197, 94, 0.12); color: rgba(34, 197, 94, 0.95); }

.meta{
  margin-top: 10rpx;
  display:flex;
  flex-direction:column;
  gap: 6rpx;
  color: rgba(31,42,55,0.55);
  font-size: 24rpx;
}

.btns{
  margin-top: 14rpx;
  display:flex;
  gap: 12rpx;
}

.btn{
  flex: 1;
  height: 76rpx;
  border-radius: 18rpx;
  display:flex;
  align-items:center;
  justify-content:center;
  font-weight: 850;
}

.btn.ghost{
  background: rgba(91,108,255,0.10);
  color: rgba(91,108,255,0.95);
}

.btn.primary{
  background: rgba(91,108,255,0.95);
  color: #fff;
}

.badge{
  margin-left: 10rpx;
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: rgba(255,255,255,0.22);
  color: #fff;
  font-size: 20rpx;
  font-weight: 850;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 36rpx;
}

.btn.primary.disabled{
  opacity: 0.6;
}

.mini-profile{
  padding: 14rpx 0 6rpx;
}
.mini-name{
  font-size: 30rpx;
  font-weight: 900;
  color: rgba(31,42,55,0.92);
}
.mini-sub{
  margin-top: 6rpx;
  font-size: 24rpx;
  color: rgba(31,42,55,0.55);
}

.metrics{
  display:flex;
  flex-wrap:wrap;
  gap: 12rpx;
  margin-top: 10rpx;
}

.metric{
  width: calc(33.333% - 8rpx);
  padding: 14rpx 10rpx;
  border-radius: 18rpx;
  border: 1rpx solid rgba(91,108,255,0.10);
  background: rgba(255,255,255,0.96);
}
.metric .k{
  font-size: 22rpx;
  color: rgba(31,42,55,0.55);
}
.metric .v{
  margin-top: 6rpx;
  font-size: 26rpx;
  font-weight: 900;
  color: rgba(31,42,55,0.92);
}

.advice-text{
  width: 100%;
  min-height: 160rpx;
  padding: 16rpx;
  border-radius: 18rpx;
  border: 1rpx solid rgba(91,108,255,0.12);
  background: rgba(255,255,255,0.96);
  box-sizing: border-box;
}

.btn-row{
  margin-top: 14rpx;
  display:flex;
  gap: 12rpx;
}
</style>
