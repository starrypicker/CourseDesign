
<template>
  <div class="admin-users">
    <!-- 操作栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="6">
          <el-input v-model="searchKeyword" placeholder="搜索顾客名称..." clearable @keyup.enter="handleSearch" />
        </el-col>
        <el-col :xs="24" :sm="6" :md="4">
          <el-select v-model="selectedStatus" placeholder="状态筛选" clearable @change="handleSearch">
            <el-option label="全部状态" value="" />
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="4" :md="3">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
        <el-col :xs="24" :sm="6" :md="11" style="text-align: right;">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 添加顾客
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="never">
      <el-table :data="users" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="customerCode" label="编码" width="80" />
        <el-table-column label="顾客" min-width="160">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 10px;">
              <div style="width: 36px; height: 36px; border-radius: 50%; background: #ecf5ff; display: flex; align-items: center; justify-content: center;">
                <el-icon :size="18" color="#409EFF"><User /></el-icon>
              </div>
              <div>
                <div style="font-size: 14px; color: #303133;">{{ row.customerName }}</div>
                <div style="font-size: 12px; color: #909399;">{{ row.email || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="address" label="地址" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="display:flex; justify-content:center; margin-top:20px;" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="80px">
        <el-form-item label="编码" prop="customerCode">
          <el-input v-model="userForm.customerCode" placeholder="请输入顾客编码" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="名称" prop="customerName">
          <el-input v-model="userForm.customerName" placeholder="请输入顾客名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="userForm.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="userForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="邮编">
          <el-input v-model="userForm.postalCode" placeholder="请输入邮编" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" placeholder="请输入登录密码" show-password />
        </el-form-item>
        <el-form-item v-if="editingId" label="新密码">
          <el-input v-model="userForm.password" type="password" placeholder="留空则不修改密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, User } from '@element-plus/icons-vue'
import { getCustomerList, addCustomer, updateCustomer, deleteCustomer } from '@/api/customer'

const searchKeyword = ref('')
const selectedStatus = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('添加顾客')
const userFormRef = ref(null)
const editingId = ref(null)
const submitting = ref(false)

const users = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)

// 加载顾客数据
const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) {
      params.customerName = searchKeyword.value
    }
    if (selectedStatus.value !== '') {
      params.status = selectedStatus.value
    }
    const res = await getCustomerList(params)
    users.value = res.rows || []
    total.value = res.total || 0
  } catch (e) {
    console.error('获取顾客列表失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchUsers())

const handleSearch = () => {
  pageNum.value = 1
  fetchUsers()
}

const handleReset = () => {
  searchKeyword.value = ''
  selectedStatus.value = ''
  pageNum.value = 1
  fetchUsers()
}

const userForm = reactive({
  customerCode: '',
  customerName: '',
  contactName: '',
  email: '',
  phone: '',
  address: '',
  postalCode: '',
  password: ''
})

const userRules = {
  customerCode: [{ required: true, message: '请输入顾客编码', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入顾客名称', trigger: 'blur' }]
}

const resetForm = () => {
  userForm.customerCode = ''
  userForm.customerName = ''
  userForm.contactName = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.address = ''
  userForm.postalCode = ''
  userForm.password = ''
}

const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '添加顾客'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editingId.value = row.customerCode
  dialogTitle.value = '编辑顾客'
  userForm.customerCode = row.customerCode
  userForm.customerName = row.customerName || ''
  userForm.contactName = row.contactName || ''
  userForm.email = row.email || ''
  userForm.phone = row.phone || ''
  userForm.address = row.address || ''
  userForm.postalCode = row.postalCode || ''
  userForm.password = ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!userFormRef.value) return
  try {
    await userFormRef.value.validate()
    submitting.value = true
    const data = {
      customerCode: userForm.customerCode,
      customerName: userForm.customerName,
      contactName: userForm.contactName,
      email: userForm.email,
      phone: userForm.phone,
      address: userForm.address,
      postalCode: userForm.postalCode,
      status: 1
    }
    // 只有填了密码才传
    if (userForm.password) {
      data.password = userForm.password
    }
    if (editingId.value) {
      await updateCustomer(data)
      ElMessage.success('顾客修改成功')
    } else {
      if (!userForm.password) {
        data.password = '123456'
      }
      await addCustomer(data)
      ElMessage.success('顾客添加成功')
    }
    dialogVisible.value = false
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel') console.error('保存顾客失败:', e)
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateCustomer({ customerCode: row.customerCode, status: newStatus })
    ElMessage.success(`顾客已${newStatus === 1 ? '启用' : '禁用'}`)
    fetchUsers()
  } catch (e) {
    console.error('状态切换失败:', e)
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除顾客"${row.customerName}"吗？删除后该顾客的订单将保留`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteCustomer(row.customerCode)
    ElMessage.success('删除成功')
    fetchUsers()
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
