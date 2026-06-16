<template>
  <div class="not-found">
    <div class="nf-content">
      <div class="nf-code">
        <span class="digit">4</span>
        <span class="digit digit-zero">0</span>
        <span class="digit">4</span>
      </div>
      <h2 class="nf-title">页面未找到</h2>
      <p class="nf-desc">抱歉，您访问的页面不存在或已被移除</p>
      <div class="nf-actions">
        <el-button type="primary" size="large" @click="$router.push('/home')">
          <el-icon><HomeFilled /></el-icon>返回首页
        </el-button>
        <el-button size="large" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>返回上一页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { HomeFilled, ArrowLeft } from '@element-plus/icons-vue'
</script>

<style scoped>
.not-found {
  display: flex; align-items: center; justify-content: center;
  min-height: 80vh; padding: 20px;
}
.nf-content { text-align: center; }

.nf-code {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  margin-bottom: 16px;
}
.digit {
  font-size: 120px; font-weight: 900; color: #409EFF;
  line-height: 1; animation: bounce 2s ease-in-out infinite;
}
.digit:nth-child(1) { animation-delay: 0s; }
.digit:nth-child(2) { animation-delay: 0.2s; }
.digit:nth-child(3) { animation-delay: 0.4s; }
.digit-zero {
  width: 100px; height: 100px; border: 6px solid #409EFF;
  border-radius: 50%; font-size: 0; margin-top: 10px;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.nf-title {
  font-size: 24px; color: #303133; margin-bottom: 8px;
}
.nf-desc {
  font-size: 15px; color: #909399; margin-bottom: 32px;
}
.nf-actions {
  display: flex; gap: 12px; justify-content: center;
}
</style>
