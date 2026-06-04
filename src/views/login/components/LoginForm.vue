<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="0"
    size="large"
    @keyup.enter="handleLogin"
  >
    <el-form-item prop="username">
      <el-input
        v-model="form.username"
        prefix-icon="User"
        placeholder="请输入用户名"
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
    <el-form-item>
      <el-button
        type="primary"
        :loading="loading"
        style="width: 100%"
        @click="handleLogin"
      >
        {{ role === 'admin' ? '管理员登录' : '顾客登录' }}
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'   // 封装的 Axios 实例

const props = defineProps({
  role: {
    type: String,
    required: true,
    validator: val => ['customer', 'admin'].includes(val)
  }
})

const router = useRouter()
const route = useRoute()
const store = useStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 登录处理
const handleLogin = async () => {
  if (!formRef.value) return
  try {
    // 表单验证
    await formRef.value.validate()
    loading.value = true

    // 调用登录接口，传递角色参数
    const res = await request.post('/auth/login', {
      username: form.username,
      password: form.password,
      role: props.role
    })

    // 假设返回数据：{ token: 'xxx', role: 'admin', userInfo: {...} }
    const { token, role, userInfo } = res

    // 通过 Vuex store 管理登录状态
    store.dispatch('login', { token, role, userInfo })

    ElMessage.success('登录成功')

    // 根据角色跳转到对应首页，或跳转到之前访问的页面
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else {
      if (role === 'admin') {
        router.push('/admin/dashboard')
      } else {
        router.push('/home')
      }
    }
  } catch (error) {
    // 错误已在拦截器中统一提示，此处可做额外处理
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 表单内边距微调 */
.el-form {
  margin-top: 20px;
}
</style>