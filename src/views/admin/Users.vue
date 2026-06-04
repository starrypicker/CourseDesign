
<template>
  <div class="admin-users">
    <!-- 操作栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="6">
          <el-input v-model="searchKeyword" placeholder="搜索用户名/手机号..." prefix-icon="Search" clearable />
        </el-col>
        <el-col :xs="24" :sm="6" :md="4">
          <el-select v-model="selectedRole" placeholder="角色筛选" clearable>
            <el-option label="全部角色" value="" />
            <el-option label="顾客" value="customer" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="10" :md="14" style="text-align: right;">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 添加用户
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="never">
      <el-table :data="filteredUsers" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 10px;">
              <div style="width: 36px; height: 36px; border-radius: 50%; background: #ecf5ff; display: flex; align-items: center; justify-content: center;">
                <el-icon :size="18" color="#409EFF"><User /></el-icon>
              </div>
              <div>
                <div style="font-size: 14px; color: #303133;">{{ row.username }}</div>
                <div style="font-size: 12px; color: #909399;">{{ row.email }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="role" label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'" size="small">
              {{ row.role === 'admin' ? '管理员' : '顾客' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text :type="row.status === 'active' ? 'warning' : 'success'" size="small" @click="handleToggleStatus(row)">
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%;">
            <el-option label="顾客" value="customer" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User } from '@element-plus/icons-vue'

const searchKeyword = ref('')
const selectedRole = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('添加用户')
const userFormRef = ref(null)
const editingId = ref(null)

const users = ref([
  { id: 1, username: 'admin', email: 'admin@sports.com', phone: '13800000001', role: 'admin', status: 'active', createTime: '2026-01-01 00:00' },
  { id: 2, username: 'zhangsan', email: 'zhangsan@qq.com', phone: '13800000002', role: 'customer', status: 'active', createTime: '2026-03-15 10:30' },
  { id: 3, username: 'lisi', email: 'lisi@163.com', phone: '13800000003', role: 'customer', status: 'active', createTime: '2026-04-20 14:15' },
  { id: 4, username: 'wangwu', email: 'wangwu@gmail.com', phone: '13800000004', role: 'customer', status: 'active', createTime: '2026-05-10 09:00' },
  { id: 5, username: 'zhaoliu', email: 'zhaoliu@qq.com', phone: '13800000005', role: 'customer', status: 'disabled', createTime: '2026-05-18 16:45' },
  { id: 6, username: 'sunqi', email: 'sunqi@163.com', phone: '13800000006', role: 'customer', status: 'active', createTime: '2026-06-01 11:20' }
])

const filteredUsers = computed(() => {
  let result = users.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(u => u.username.toLowerCase().includes(kw) || u.phone.includes(kw))
  }
  if (selectedRole.value) {
    result = result.filter(u => u.role === selectedRole.value)
  }
  return result
})

const userForm = reactive({
  username: '',
  password: '',
  email: '',
  phone: '',
  role: 'customer'
})

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const resetForm = () => {
  userForm.username = ''
  userForm.password = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.role = 'customer'
}

const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '添加用户'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editingId.value = row.id
  dialogTitle.value = '编辑用户'
  userForm.username = row.username
  userForm.email = row.email
  userForm.phone = row.phone
  userForm.role = row.role
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!userFormRef.value) return
  try {
    await userFormRef.value.validate()
    if (editingId.value) {
      const user = users.value.find(u => u.id === editingId.value)
      if (user) {
        user.email = userForm.email
        user.phone = userForm.phone
        user.role = userForm.role
      }
      ElMessage.success('用户修改成功')
    } else {
      const newId = Math.max(...users.value.map(u => u.id)) + 1
      users.value.push({
        id: newId,
        username: userForm.username,
        email: userForm.email,
        phone: userForm.phone,
        role: userForm.role,
        status: 'active',
        createTime: new Date().toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).replace(/\//g, '-')
      })
      ElMessage.success('用户添加成功')
    }
    dialogVisible.value = false
  } catch (e) {}
}

const handleToggleStatus = (row) => {
  row.status = row.status === 'active' ? 'disabled' : 'active'
  ElMessage.success(`用户已${row.status === 'active' ? '启用' : '禁用'}`)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户"${row.username}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    users.value = users.value.filter(u => u.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.admin-users {
  padding: 0;
}

.toolbar-card {
  margin-bottom: 16px;
}
</style>
