<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @keyup.enter="handleLogin">
    <el-form-item prop="username"><el-input v-model="form.username" :prefix-icon="User" placeholder="请输入用户名" /></el-form-item>
    <el-form-item prop="password"><el-input v-model="form.password" type="password" :prefix-icon="Lock" placeholder="请输入密码" show-password /></el-form-item>
    <el-form-item>
      <el-button type="primary" :loading="loading" style="width:100%" @click="handleLogin">{{ role === 'admin' ? '管理员登录' : '顾客登录' }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'

const props = defineProps({ role: { type: String, required: true, validator: val => ['customer','admin'].includes(val) } })
const router = useRouter()
const route = useRoute()
const store = useStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return // 表单验证未通过，element-plus会自动提示
  }
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password, role: props.role })
    store.dispatch('login', { token: res.token, userInfo: res.userInfo })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    if (props.role === 'admin') router.push(redirect || '/admin/dashboard')
    else router.push(redirect || '/home')
  } catch (error) {
    // API错误已由axios拦截器处理，此处仅对未预期的错误兜底
    if (error.message && error.message !== 'Error') {
      ElMessage.error(error.message)
    }
  } finally { loading.value = false }
}
</script>

<style scoped>
.el-form { margin-top: 20px; }
</style>
