
<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 左侧：用户信息卡片 -->
      <el-col :xs="24" :sm="24" :md="8">
        <el-card shadow="never" class="user-card">
          <div class="user-avatar">
            <el-icon :size="64" color="#409EFF"><UserFilled /></el-icon>
          </div>
          <h2 class="user-name">{{ userInfo?.username || '用户' }}</h2>
          <el-tag type="primary" size="small">{{ userInfo?.role === 'admin' ? '管理员' : '普通用户' }}</el-tag>
          <p class="user-join">注册时间：{{ userInfo?.createTime || '2026-06-01' }}</p>
        </el-card>
      </el-col>

      <!-- 右侧：信息编辑 -->
      <el-col :xs="24" :sm="24" :md="16">
        <el-card shadow="never">
          <el-tabs v-model="activeTab">
            <!-- 基本信息 -->
            <el-tab-pane label="基本信息" name="info">
              <el-form :model="profileForm" label-width="100px" style="max-width: 500px; margin-top: 20px;">
                <el-form-item label="用户名">
                  <el-input v-model="profileForm.username" disabled />
                </el-form-item>
                <el-form-item label="昵称">
                  <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleSaveProfile">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 修改密码 -->
            <el-tab-pane label="修改密码" name="password">
              <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px" style="max-width: 500px; margin-top: 20px;">
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const store = useStore()
const activeTab = ref('info')
const passwordFormRef = ref(null)

const userInfo = computed(() => store.getters.userInfo)

const profileForm = reactive({
  username: userInfo.value?.username || 'user01',
  nickname: userInfo.value?.nickname || '',
  email: userInfo.value?.email || '',
  phone: userInfo.value?.phone || ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleSaveProfile = () => {
  ElMessage.success('个人信息保存成功（模拟）')
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
    ElMessage.success('密码修改成功（模拟）')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e) {
    // 验证失败
  }
}
</script>

<style scoped>
.profile-container {
  padding: 10px 0;
}

.user-card {
  text-align: center;
  padding: 20px 0;
}

.user-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.user-name {
  font-size: 20px;
  margin-bottom: 8px;
  color: #303133;
}

.user-join {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}
</style>
