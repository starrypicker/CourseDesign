<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
    </div>
    <div class="login-box">
      <div class="login-logo">
        <el-icon :size="40" color="#409EFF"><Trophy /></el-icon>
      </div>
      <h2 class="login-title">体育用品销售信息系统</h2>
      <el-tabs v-model="activeTab" class="login-tabs" stretch>
        <el-tab-pane label="顾客登录" name="customer"><LoginForm :role="'customer'" /></el-tab-pane>
        <el-tab-pane label="管理员登录" name="admin"><LoginForm :role="'admin'" /></el-tab-pane>
        <el-tab-pane label="顾客注册" name="register"><RegisterForm /></el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import LoginForm from './components/LoginForm.vue'
import RegisterForm from './components/RegisterForm.vue'
const activeTab = ref('customer')
</script>

<style scoped>
.login-container {
  display: flex; justify-content: center; align-items: center;
  min-height: 100vh; position: relative; overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* 背景动画 */
.login-bg { position: absolute; inset: 0; overflow: hidden; }
.bg-shape {
  position: absolute; border-radius: 50%; opacity: 0.1;
  animation: float 20s ease-in-out infinite;
}
.shape-1 {
  width: 500px; height: 500px; background: #fff;
  top: -15%; left: -10%; animation-delay: 0s;
}
.shape-2 {
  width: 400px; height: 400px; background: #fff;
  bottom: -10%; right: -5%; animation-delay: -7s;
}
.shape-3 {
  width: 300px; height: 300px; background: #fff;
  top: 50%; left: 50%; animation-delay: -14s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

.login-box {
  position: relative; z-index: 1;
  width: 450px; padding: 40px; background: #fff;
  border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  animation: boxIn 0.5s ease;
}

@keyframes boxIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-logo {
  width: 72px; height: 72px; border-radius: 50%;
  background: linear-gradient(135deg, #ecf5ff, #d9ecff);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
}
.login-title {
  text-align: center; margin-bottom: 24px;
  font-size: 22px; color: var(--text-color); font-weight: 600;
}
.login-tabs { margin-top: 10px; }

@media (max-width: 500px) {
  .login-box { width: 95%; padding: 24px; }
}
</style>
