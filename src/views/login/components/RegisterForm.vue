<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="0"
    size="large"
    @keyup.enter="handleRegister"
  >
    <el-form-item prop="username">
      <el-input
        v-model="form.username"
        prefix-icon="User"
        placeholder="请输入用户名（顾客编码）"
      />
    </el-form-item>
    <el-form-item prop="customerName">
      <el-input
        v-model="form.customerName"
        prefix-icon="UserFilled"
        placeholder="请输入顾客名称"
      />
    </el-form-item>
    <el-form-item prop="password">
      <el-input
        v-model="form.password"
        type="password"
        prefix-icon="Lock"
        placeholder="请输入密码"
        show-password
      />
    </el-form-item>
    <el-form-item prop="confirmPassword">
      <el-input
        v-model="form.confirmPassword"
        type="password"
        prefix-icon="Lock"
        placeholder="请再次输入密码"
        show-password
      />
    </el-form-item>
    <el-form-item>
      <el-button
        type="primary"
        :loading="loading"
        style="width: 100%"
        @click="handleRegister"
      >
        注册
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  customerName: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  customerName: [
    { required: true, message: '请输入顾客名称', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    await register({
      username: form.username,
      password: form.password,
      customerName: form.customerName
    })
    ElMessage.success('注册成功，请登录')
    // 重置表单
    form.username = ''
    form.customerName = ''
    form.password = ''
    form.confirmPassword = ''
  } catch (e) {
    console.error('注册失败:', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.el-form {
  margin-top: 20px;
}
</style>
