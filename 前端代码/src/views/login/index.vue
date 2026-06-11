<template>
  <div class="login-container">
    <div class="login-box">
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
.login-container { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.login-box { width: 450px; padding: 40px; background: #fff; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
.login-title { text-align: center; margin-bottom: 30px; font-size: 24px; color: var(--text-color); }
.login-tabs { margin-top: 10px; }
</style>
