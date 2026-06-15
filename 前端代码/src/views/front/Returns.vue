<template>
  <div class="returns-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <h2><el-icon><RefreshLeft /></el-icon> 退换货申请</h2>
          <el-button type="primary" @click="dialogVisible = true">
            <el-icon><Plus /></el-icon>新建申请
          </el-button>
        </div>
      </template>

      <el-table :data="records" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="申请编号" width="90" />
        <el-table-column prop="orderId" label="订单号" width="90" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'primary' : 'warning'" size="small">{{ row.typeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款金额" width="110">
          <template #default="{ row }">
            <span v-if="row.refundAmount" style="color:#f56c6c">¥ {{ Number(row.refundAmount).toFixed(2) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="adminRemark" label="管理员备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="暂无退换货申请" />
      <div v-if="total > 0" style="display:flex;justify-content:center;margin-top:20px">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
          layout="prev, pager, next" @current-change="fetchList" />
      </div>
    </el-card>

    <!-- 新建申请弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建退换货申请" width="480px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="订单号" prop="orderId">
          <el-input-number v-model="form.orderId" :min="1" style="width:100%" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="申请类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">退货退款</el-radio>
            <el-radio :value="1">换货</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="申请原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请描述退换货原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="退换货详情" width="500px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="申请编号">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ detail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="申请类型">
            <el-tag :type="detail.type === 1 ? 'primary' : 'warning'" size="small">{{ detail.typeName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请原因">{{ detail.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail.status)" size="small">{{ detail.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="退款金额" v-if="detail.refundAmount">¥ {{ Number(detail.refundAmount).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="管理员备注">{{ detail.adminRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ detail.handledBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshLeft, Plus } from '@element-plus/icons-vue'
import { getReturnList, createReturn, getReturnById } from '@/api/return'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const submitting = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const formRef = ref(null)
const form = reactive({ orderId: null, type: 0, reason: '' })
const rules = {
  orderId: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  reason: [{ required: true, message: '请输入申请原因', trigger: 'blur' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getReturnList({ pageNum: pageNum.value, pageSize: pageSize.value })
    records.value = res.rows || []
    total.value = res.total || 0
  } catch (e) { console.error('获取退换货列表失败:', e) }
  finally { loading.value = false }
}

onMounted(() => fetchList())

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitting.value = true
    await createReturn({ orderId: form.orderId, type: form.type, reason: form.reason })
    ElMessage.success('退换货申请已提交')
    dialogVisible.value = false
    form.orderId = null; form.type = 0; form.reason = ''
    fetchList()
  } catch (e) { /* axios interceptor handles error message */ }
  finally { submitting.value = false }
}

const handleViewDetail = async (row) => {
  try {
    detail.value = await getReturnById(row.id)
    detailVisible.value = true
  } catch (e) { console.error('获取详情失败:', e) }
}

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}
</script>

<style scoped>
.returns-container { padding: 10px 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-header h2 { display: flex; align-items: center; gap: 8px; font-size: 18px; margin: 0; }
</style>
