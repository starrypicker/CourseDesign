<template>
  <div class="admin-invoices">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <h2><el-icon><Document /></el-icon> 发票管理</h2>
        </div>
      </template>

      <el-row :gutter="12" style="margin-bottom:16px">
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="发票状态" clearable @change="fetchList" style="width:100%">
            <el-option label="待开票" :value="0" />
            <el-option label="已开票" :value="1" />
            <el-option label="已作废" :value="2" />
          </el-select>
        </el-col>
      </el-row>

      <el-table :data="records" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="orderId" label="关联订单" width="100" />
        <el-table-column prop="customerName" label="顾客" width="140" show-overflow-tooltip />
        <el-table-column prop="invoiceType" label="类型" width="80">
          <template #default="{ row }">{{ row.invoiceType === 'company' ? '企业' : '个人' }}</template>
        </el-table-column>
        <el-table-column prop="invoiceTitle" label="发票抬头" min-width="180" show-overflow-tooltip />
        <el-table-column prop="taxNo" label="税号" width="160" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="110">
          <template #default="{ row }">¥ {{ Number(row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="invoiceNo" label="发票号码" width="180" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" text type="primary" size="small" @click="handleIssue(row)">开具发票</el-button>
            <el-button v-if="row.status === 0 || row.status === 1" text type="danger" size="small" @click="handleCancelInvoice(row)">{{ row.status === 1 ? '作废' : '取消' }}</el-button>
            <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="暂无发票记录" />
      <div v-if="total > 0" style="display:flex;justify-content:center;margin-top:20px">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
          layout="prev, pager, next" @current-change="fetchList" />
      </div>
    </el-card>

    <!-- 开具发票弹窗 -->
    <el-dialog v-model="issueVisible" title="开具发票" width="450px">
      <el-form label-width="90px">
        <el-form-item label="发票号码">
          <el-input v-model="issueForm.invoiceNo" placeholder="留空则自动生成" />
        </el-form-item>
        <el-form-item label="发票抬头">
          <span>{{ issueForm.invoiceTitle }}</span>
        </el-form-item>
        <el-form-item label="发票金额">
          <span style="color:#f56c6c;font-weight:bold">¥ {{ Number(issueForm.amount || 0).toFixed(2) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmIssue" :loading="issuing">确认开票</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="发票详情" width="550px">
      <template v-if="detail">
        <div id="invoice-print-area">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="发票编号">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="发票号码">{{ detail.invoiceNo || '待生成' }}</el-descriptions-item>
            <el-descriptions-item label="关联订单">{{ detail.orderId }}</el-descriptions-item>
            <el-descriptions-item label="顾客代码">{{ detail.customerCode }}</el-descriptions-item>
            <el-descriptions-item label="顾客名称">{{ detail.customerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发票类型">{{ detail.invoiceType === 'company' ? '企业' : '个人' }}</el-descriptions-item>
            <el-descriptions-item label="发票抬头" :span="2">{{ detail.invoiceTitle }}</el-descriptions-item>
            <el-descriptions-item label="税号" :span="2" v-if="detail.taxNo">{{ detail.taxNo }}</el-descriptions-item>
            <el-descriptions-item label="发票金额">¥ {{ Number(detail.amount || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="detail.status === 1 ? 'success' : detail.status === 2 ? 'info' : 'warning'" size="small">{{ detail.statusName }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="开票日期" v-if="detail.invoiceDate">{{ detail.invoiceDate }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
      <template #footer>
        <el-button v-if="detail && detail.status === 1" type="success" @click="handlePrintInvoice">打印发票</el-button>
        <el-button v-if="detail && (detail.status === 0 || detail.status === 1)" type="danger" @click="handleCancelInvoice(detail); detailVisible = false">{{ detail.status === 1 ? '作废发票' : '取消申请' }}</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { getInvoiceList, getInvoiceById, issueInvoice, cancelInvoice } from '@/api/invoice'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const filterStatus = ref(null)

const issueVisible = ref(false)
const issuing = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const issueForm = reactive({ id: null, invoiceNo: '', invoiceTitle: '', amount: 0 })

const fetchList = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterStatus.value !== null && filterStatus.value !== '') params.status = filterStatus.value
    const res = await getInvoiceList(params)
    records.value = res.rows || []
    total.value = res.total || 0
  } catch (e) { console.error('获取发票列表失败:', e) }
  finally { loading.value = false }
}

onMounted(() => fetchList())

const handleIssue = (row) => {
  issueForm.id = row.id
  issueForm.invoiceNo = ''
  issueForm.invoiceTitle = row.invoiceTitle
  issueForm.amount = row.amount
  issueVisible.value = true
}

const confirmIssue = async () => {
  issuing.value = true
  try {
    await issueInvoice(issueForm.id, issueForm.invoiceNo || null)
    ElMessage.success('发票已开具')
    issueVisible.value = false
    fetchList()
  } catch (e) { /* handled by interceptor */ }
  finally { issuing.value = false }
}

const handleViewDetail = async (row) => {
  try {
    detail.value = await getInvoiceById(row.id)
    detailVisible.value = true
  } catch (e) { console.error('获取发票详情失败:', e) }
}

const handlePrintInvoice = () => {
  const printContent = document.getElementById('invoice-print-area')
  if (!printContent) return
  const win = window.open('', '_blank', 'width=700,height=600')
  if (!win) return
  win.document.write(`
    <html><head><title>发票打印</title>
    <style>body{font-family:'Microsoft YaHei',sans-serif;padding:20px} table{width:100%;border-collapse:collapse} td,th{border:1px solid #ddd;padding:8px;font-size:14px} .label{font-weight:bold;background:#f5f5f5}</style>
    </head><body>${printContent.innerHTML}</body></html>`)
  win.document.close()
  win.focus()
  win.print()
  win.close()
}

const handleCancelInvoice = async (row) => {
  try {
    const label = row.status === 1 ? '作废' : '取消'
    await ElMessageBox.confirm(`确定要${label}该发票吗？`, `${label}发票`, { confirmButtonText: `确认${label}`, cancelButtonText: '返回', type: 'warning' })
    await cancelInvoice(row.id)
    ElMessage.success(`发票已${label}`)
    fetchList()
  } catch (e) { if (e !== 'cancel') console.error('取消发票失败:', e) }
}
</script>

<style scoped>
.admin-invoices { padding: 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-header h2 { display: flex; align-items: center; gap: 8px; font-size: 18px; margin: 0; }
</style>
