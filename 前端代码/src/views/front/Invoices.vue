<template>
  <div class="invoices-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <h2><el-icon><Document /></el-icon> 我的发票</h2>
          <el-button type="primary" @click="dialogVisible = true">
            <el-icon><Plus /></el-icon>申请开票
          </el-button>
        </div>
      </template>

      <el-table :data="records" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="orderId" label="关联订单" width="100" />
        <el-table-column prop="invoiceTitle" label="发票抬头" min-width="180" show-overflow-tooltip />
        <el-table-column prop="amount" label="发票金额" width="120">
          <template #default="{ row }">¥ {{ Number(row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="invoiceNo" label="发票号码" width="180">
          <template #default="{ row }">{{ row.invoiceNo || '待生成' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="invoiceStatusTag(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" text type="warning" size="small" @click="handleCancelInvoice(row)">取消</el-button>
            <el-button v-if="row.status === 1" text type="success" size="small" @click="handlePrintInvoice(row)">打印</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="暂无发票记录" />
      <div v-if="total > 0" style="display:flex;justify-content:center;margin-top:20px">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
          layout="prev, pager, next" @current-change="fetchList" />
      </div>
    </el-card>

    <!-- 申请开票弹窗 -->
    <el-dialog v-model="dialogVisible" title="申请开具发票" width="480px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="关联订单号" prop="orderId">
          <el-input-number v-model="form.orderId" :min="1" style="width:100%" placeholder="请输入已付款的订单号" />
        </el-form-item>
        <el-form-item label="发票类型" prop="invoiceType">
          <el-radio-group v-model="form.invoiceType">
            <el-radio value="personal">个人</el-radio>
            <el-radio value="company">企业</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发票抬头" prop="invoiceTitle">
          <el-input v-model="form.invoiceTitle" placeholder="个人姓名或企业全称" />
        </el-form-item>
        <el-form-item label="税号" prop="taxNo" v-if="form.invoiceType === 'company'">
          <el-input v-model="form.taxNo" placeholder="企业统一社会信用代码" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="其他说明（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="发票详情" width="500px">
      <template v-if="detail">
        <div id="customer-invoice-print-area">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="发票编号">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="发票号码">{{ detail.invoiceNo || '待生成' }}</el-descriptions-item>
            <el-descriptions-item label="关联订单">{{ detail.orderId }}</el-descriptions-item>
            <el-descriptions-item label="发票类型">{{ detail.invoiceType === 'company' ? '企业' : '个人' }}</el-descriptions-item>
            <el-descriptions-item label="发票抬头" :span="2">{{ detail.invoiceTitle }}</el-descriptions-item>
            <el-descriptions-item label="税号" :span="2" v-if="detail.taxNo">{{ detail.taxNo }}</el-descriptions-item>
            <el-descriptions-item label="发票金额">¥ {{ Number(detail.amount || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="invoiceStatusTag(detail.status)" size="small">{{ detail.statusName }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="开票日期" v-if="detail.invoiceDate">{{ detail.invoiceDate }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
      <template #footer>
        <el-button v-if="detail && detail.status === 1" type="success" @click="handlePrintInvoice(detail)">打印发票</el-button>
        <el-button v-if="detail && detail.status === 0" type="warning" @click="handleCancelInvoice(detail); detailVisible = false">取消申请</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Plus } from '@element-plus/icons-vue'
import { getInvoiceList, requestInvoice, getInvoiceById, cancelInvoice } from '@/api/invoice'

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
const form = reactive({ orderId: null, invoiceType: 'personal', invoiceTitle: '', taxNo: '', remark: '' })
const rules = {
  orderId: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  invoiceType: [{ required: true, message: '请选择发票类型', trigger: 'change' }],
  invoiceTitle: [{ required: true, message: '请输入发票抬头', trigger: 'blur' }]
}

const invoiceStatusTag = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'warning'
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getInvoiceList({ pageNum: pageNum.value, pageSize: pageSize.value })
    records.value = res.rows || []
    total.value = res.total || 0
  } catch (e) { console.error('获取发票列表失败:', e) }
  finally { loading.value = false }
}

onMounted(() => fetchList())

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitting.value = true
    const data = {
      orderId: form.orderId,
      invoiceType: form.invoiceType,
      invoiceTitle: form.invoiceTitle,
      taxNo: form.invoiceType === 'company' ? form.taxNo : null,
      remark: form.remark
    }
    await requestInvoice(data)
    ElMessage.success('发票申请已提交')
    dialogVisible.value = false
    Object.assign(form, { orderId: null, invoiceType: 'personal', invoiceTitle: '', taxNo: '', remark: '' })
    fetchList()
  } catch (e) { /* handled by interceptor */ }
  finally { submitting.value = false }
}

const handleViewDetail = async (row) => {
  try {
    detail.value = await getInvoiceById(row.id)
    detailVisible.value = true
  } catch (e) { console.error('获取发票详情失败:', e) }
}

const handleCancelInvoice = async (row) => {
  try {
    const label = row.status === 1 ? '作废' : '取消'
    await ElMessageBox.confirm(`确定要${label}该发票申请吗？${row.status === 1 ? '作废后该订单可重新申请退换货。' : ''}`, `${label}发票`, { confirmButtonText: `确认${label}`, cancelButtonText: '返回', type: 'warning' })
    await cancelInvoice(row.id)
    ElMessage.success(`发票已${label}`)
    fetchList()
  } catch (e) { if (e !== 'cancel') console.error('取消发票失败:', e) }
}

const handlePrintInvoice = (row) => {
  // 先从详情弹窗获取DOM，如果没打开详情则直接用行数据构造打印内容
  const printArea = document.getElementById('customer-invoice-print-area')
  if (printArea) {
    const win = window.open('', '_blank', 'width=700,height=600')
    if (!win) return
    win.document.write(`<html><head><title>发票打印</title><style>body{font-family:'Microsoft YaHei',sans-serif;padding:20px}table{width:100%;border-collapse:collapse}td,th{border:1px solid #ddd;padding:8px;font-size:14px}.label{font-weight:bold;background:#f5f5f5}</style></head><body>${printArea.innerHTML}</body></html>`)
    win.document.close()
    win.focus()
    win.print()
    win.close()
    return
  }
  // 兜底：用行数据构造（安全转义防止XSS）
  const esc = (str) => { if (!str) return ''; const d = document.createElement('div'); d.appendChild(document.createTextNode(str)); return d.innerHTML }
  const inv = row || detail.value
  if (!inv) return
  const html = `<div style="font-family:'Microsoft YaHei',sans-serif;padding:20px;max-width:500px;margin:0 auto;border:2px solid #333;padding:30px;border-radius:8px;"><h2 style="text-align:center;">🧾 电子发票</h2><p>发票号码：${esc(inv.invoiceNo)}</p><p>发票抬头：${esc(inv.invoiceTitle)}</p><p>发票金额：¥ ${Number(inv.amount || 0).toFixed(2)}</p><p>开票日期：${esc(inv.invoiceDate)}</p></div>`
  const win = window.open('', '_blank', 'width=700,height=600')
  if (!win) return
  win.document.write(`<html><head><title>发票打印</title></head><body>${html}</body></html>`)
  win.document.close()
  win.focus()
  win.print()
  win.close()
}
</script>

<style scoped>
.invoices-container { padding: 10px 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-header h2 { display: flex; align-items: center; gap: 8px; font-size: 18px; margin: 0; }
:deep(.el-table__row) { transition: background-color 0.2s; }
:deep(.el-table__row:hover) { background-color: #f5f7fa; }
</style>
