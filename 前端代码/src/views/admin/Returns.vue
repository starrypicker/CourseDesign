<template>
  <div class="admin-returns">
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="12" :sm="4" :md="3">
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="0" />
            <el-option label="已通过" value="1" />
            <el-option label="已拒绝" value="2" />
            <el-option label="已完成" value="3" />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="4" :md="3">
          <el-select v-model="filterType" placeholder="类型筛选" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="退货退款" value="0" />
            <el-option label="换货" value="1" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="4" :md="4">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <el-table :data="records" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="orderId" label="订单号" width="90" />
        <el-table-column prop="customerName" label="申请人" width="120" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'primary' : 'warning'" size="small">{{ row.typeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
        <el-table-column label="退款金额" width="110">
          <template #default="{ row }">
            <span v-if="row.refundAmount" style="color:#f56c6c">¥ {{ Number(row.refundAmount).toFixed(2) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handledBy" label="处理人" width="100" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" text type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 0" text type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
            <el-button v-if="row.status === 1" text type="success" size="small" @click="handleComplete(row)">完成处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="暂无退换货申请" />
      <div v-if="total > 0" style="display:flex;justify-content:center;margin-top:20px">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
          layout="total, prev, pager, next" @current-change="fetchList" />
      </div>
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="审核退换货申请" width="480px" destroy-on-close>
      <el-form :model="auditForm" label-width="90px">
        <el-form-item label="备注">
          <el-input v-model="auditForm.adminRemark" type="textarea" :rows="3"
            :placeholder="auditForm.action === 'approve' ? '审核通过备注（选填）' : '请输入拒绝原因'" />
        </el-form-item>
        <el-form-item label="退款金额" v-if="auditForm.action === 'approve' && auditForm.currentType === 0">
          <el-input-number v-model="auditForm.refundAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button :type="auditForm.action === 'approve' ? 'success' : 'danger'" @click="handleAuditConfirm">
          {{ auditForm.action === 'approve' ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="退换货详情" width="500px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="申请编号">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ detail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.customerName || detail.customerCode }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="detail.type === 1 ? 'primary' : 'warning'" size="small">{{ detail.typeName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="原因">{{ detail.reason || '-' }}</el-descriptions-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReturnList, getReturnById, approveReturn, rejectReturn, completeReturn } from '@/api/return'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const filterStatus = ref('')
const filterType = ref('')

const auditVisible = ref(false)
const auditForm = reactive({ id: null, action: '', adminRemark: '', refundAmount: 0, currentType: 0 })
const detailVisible = ref(false)
const detail = ref(null)

const fetchList = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterStatus.value !== '') params.status = Number(filterStatus.value)
    if (filterType.value !== '') params.type = Number(filterType.value)
    const res = await getReturnList(params)
    records.value = res.rows || []
    total.value = res.total || 0
  } catch (e) { console.error('获取列表失败:', e) }
  finally { loading.value = false }
}

onMounted(() => fetchList())

const handleSearch = () => { pageNum.value = 1; fetchList() }
const handleReset = () => { filterStatus.value = ''; filterType.value = ''; pageNum.value = 1; fetchList() }

const handleViewDetail = async (row) => {
  try {
    detail.value = await getReturnById(row.id)
    detailVisible.value = true
  } catch (e) { console.error('获取详情失败:', e) }
}

const handleApprove = (row) => {
  auditForm.id = row.id
  auditForm.action = 'approve'
  auditForm.adminRemark = ''
  auditForm.refundAmount = row.refundAmount || 0
  auditForm.currentType = row.type
  auditVisible.value = true
}

const handleReject = (row) => {
  auditForm.id = row.id
  auditForm.action = 'reject'
  auditForm.adminRemark = ''
  auditForm.currentType = row.type
  auditVisible.value = true
}

const handleAuditConfirm = async () => {
  try {
    if (auditForm.action === 'approve') {
      await approveReturn(auditForm.id, {
        adminRemark: auditForm.adminRemark,
        refundAmount: auditForm.refundAmount
      })
      ElMessage.success('申请已通过')
    } else {
      if (!auditForm.adminRemark) { ElMessage.warning('请填写拒绝原因'); return }
      await rejectReturn(auditForm.id, { adminRemark: auditForm.adminRemark })
      ElMessage.success('申请已拒绝')
    }
    auditVisible.value = false
    fetchList()
  } catch (e) { console.error('操作失败:', e) }
}

const handleComplete = (row) => {
  ElMessageBox.confirm(`确认完成该退换货处理？${row.type === 0 ? '将退款并回滚库存。' : '将标记换货完成。'}`, '确认', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    await completeReturn(row.id)
    ElMessage.success('退换货处理完成')
    fetchList()
  }).catch(() => {})
}

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}
</script>

<style scoped>
.admin-returns { padding: 0; }
.toolbar-card { margin-bottom: 16px; }
</style>
