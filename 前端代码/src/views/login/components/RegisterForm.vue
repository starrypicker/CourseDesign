<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @keyup.enter="handleRegister">
    <el-form-item prop="username"><el-input v-model="form.username" :prefix-icon="User" placeholder="请输入用户名（顾客编码）" /></el-form-item>
    <el-form-item prop="customerName"><el-input v-model="form.customerName" :prefix-icon="UserFilled" placeholder="请输入顾客名称" /></el-form-item>
    <el-form-item prop="contactName"><el-input v-model="form.contactName" :prefix-icon="UserFilled" placeholder="请输入联系人姓名" /></el-form-item>
    <el-form-item prop="phone"><el-input v-model="form.phone" :prefix-icon="Phone" placeholder="请输入手机号" /></el-form-item>
    <el-form-item prop="address"><el-input v-model="form.address" :prefix-icon="Location" placeholder="请输入地址" /></el-form-item>
    <el-form-item prop="postalCode"><el-input v-model="form.postalCode" :prefix-icon="Stamp" placeholder="请输入邮编" /></el-form-item>
    <el-form-item prop="password"><el-input v-model="form.password" type="password" :prefix-icon="Lock" placeholder="请输入密码" show-password /></el-form-item>
    <el-form-item prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" :prefix-icon="Lock" placeholder="请再次输入密码" show-password /></el-form-item>
    <el-form-item><el-button type="primary" :loading="loading" style="width:100%" @click="handleRegister">注册</el-button></el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { User, UserFilled, Lock, Phone, Location, Stamp } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', customerName: '', contactName: '', phone: '', address: '', postalCode: '', password: '', confirmPassword: '' })
const validateConfirm = (rule, value, callback) => { if (value !== form.password) callback(new Error('两次输入的密码不一致')); else callback() }
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入顾客名称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }],
  postalCode: [{ pattern: /^\d{6}$/, message: '请输入6位数字邮编', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    await register({
      username: form.username,
      password: form.password,
      customerName: form.customerName,
      contactName: form.contactName,
      phone: form.phone,
      address: form.address,
      postalCode: form.postalCode
    })
    ElMessage.success('注册成功，请登录')
    form.username = ''; form.customerName = ''; form.contactName = ''; form.phone = ''; form.address = ''; form.postalCode = ''; form.password = ''; form.confirmPassword = ''
  } catch (e) { console.error('注册失败:', e) }
  finally { loading.value = false }
}
</script>
<style scoped>.el-form { margin-top: 20px; }</style>
