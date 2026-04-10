<script>
import { initEcb02BleHub } from '@/utils/ble/ecb02-hub.js';
export default {
    data() {
        return {};
    },
    globalData: {
        role: null,

        // 'patient' 或 'staff'
        userInfo: null,

        setRole(role) {
            this.role = role;
            uni.setStorageSync('role', role);
        }
    },
    onLaunch() {
        // 可以在这里做登录态校验、从 storage 里恢复角色
        const role = uni.getStorageSync('role');
        if (role) {
            this.globalData.role = role;
        }

        // 初始化 BLE 常驻接收（页面切换也持续）
        initEcb02BleHub();
    }
};
</script>
<style>
@import './app.css';
</style>
