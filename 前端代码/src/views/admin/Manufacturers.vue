<template>
  <div class="admin-manufacturers">
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="6"><el-input v-model="searchKeyword" placeholder="搜索厂家名称..." clearable @keyup.enter="fetchData" /></el-col>
        <el-col :xs="24" :sm="4" :md="3"><el-button type="primary" @click="fetchData">搜索</el-button><el-button @click="handleReset">重置</el-button></el-col>
        <el-col :xs="24" :sm="12" :md="15" style="text-align:right;"><el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加厂家</el-button></el-col>
      </el-row>
    </el-card>
    <el-card shadow="never">
      <el-table :data="paginatedList" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="manufacturerCode" label="编码" width="100" />
        <el-table-column prop="manufacturerName" label="厂家名称" min-width="160" />
        <el-table-column prop="contactPerson" label="联系人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:center;margin-top:20px" v-if="filteredList.length > pageSize">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="filteredList.length"
          layout="total, prev, pager, next" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="编码" prop="manufacturerCode"><el-input v-model="form.manufacturerCode" placeholder="请输入厂家编码" :disabled="!!editingCode" /></el-form-item>
        <el-form-item label="名称" prop="manufacturerName"><el-input v-model="form.manufacturerName" placeholder="请输入厂家名称" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactPerson" placeholder="请输入联系人" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" placeholder="请输入联系电话" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" placeholder="请输入地址" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getManufacturerList, addManufacturer, updateManufacturer, deleteManufacturer } from '@/api/manufacturer'

const searchKeyword = ref('')
const list = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('添加厂家')
const formRef = ref(null)
const editingCode = ref(null)

const form = reactive({ manufacturerCode: '', manufacturerName: '', contactPerson: '', phone: '', address: '' })
const rules = {
  manufacturerCode: [{ required: true, message: '请输入厂家编码', trigger: 'blur' }],
  manufacturerName: [{ required: true, message: '请输入厂家名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try { list.value = await getManufacturerList() || [] } catch (e) { console.error('获取厂家列表失败:', e) } finally { loading.value = false }
}

onMounted(() => fetchData())

const filteredList = computed(() => {
  if (!searchKeyword.value) return list.value
  return list.value.filter(m => m.manufacturerName && m.manufacturerName.includes(searchKeyword.value))
})

const paginatedList = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleReset = () => { searchKeyword.value = ''; pageNum.value = 1; fetchData() }

const resetForm = () => { form.manufacturerCode = ''; form.manufacturerName = ''; form.contactPerson = ''; form.phone = ''; form.address = '' }

const handleAdd = () => { editingCode.value = null; dialogTitle.value = '添加厂家'; resetForm(); dialogVisible.value = true }

const handleEdit = (row) => {
  editingCode.value = row.manufacturerCode; dialogTitle.value = '编辑厂家'
  form.manufacturerCode = row.manufacturerCode; form.manufacturerName = row.manufacturerName || ''
  form.contactPerson = row.contactPerson || ''; form.phone = row.phone || ''; form.address = row.address || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    const data = { manufacturerCode: form.manufacturerCode, manufacturerName: form.manufacturerName, contactPerson: form.contactPerson, phone: form.phone, address: form.address }
    if (editingCode.value) { await updateManufacturer(data); ElMessage.success('厂家修改成功') }
    else { await addManufacturer(data); ElMessage.success('厂家添加成功') }
    dialogVisible.value = false; fetchData()
  } catch (e) {}
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除厂家"${row.manufacturerName}"吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => { await deleteManufacturer(row.manufacturerCode); ElMessage.success('删除成功'); fetchData() }).catch(() => {})
}
</script>

<style scoped>
.admin-manufacturers { padding: 0; }
.toolbar-card { margin-bottom: 16px; }
</style>
