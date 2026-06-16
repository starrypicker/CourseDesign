<template>
  <div class="chat-container">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="chat-header">
          <h2><el-icon><ChatDotSquare /></el-icon> 客服聊天</h2>
          <el-tag type="success" size="small">在线</el-tag>
        </div>
      </template>
      <div class="chat-body" ref="chatBodyRef">
        <div v-if="messages.length === 0" class="chat-empty">
          <el-empty description="暂无消息，开始和客服聊天吧！" :image-size="80" />
        </div>
        <div v-for="(msg, index) in messages" :key="index" class="chat-message"
          :class="msg.senderRole === 'customer' ? 'msg-self' : 'msg-other'">
          <div class="msg-bubble">
            <div class="msg-sender">{{ msg.senderRole === 'customer' ? '我' : '客服' }}</div>
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
          </div>
        </div>
      </div>
      <div class="chat-input">
        <el-input v-model="inputText" type="textarea" :rows="2" placeholder="请输入消息..."
          @keydown.enter.prevent="handleSend" />
        <el-button type="primary" :disabled="!inputText.trim()" @click="handleSend" style="margin-top:8px">
          发送
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { ChatDotSquare } from '@element-plus/icons-vue'
import { getChatHistory, createChatSocket } from '@/api/chat'

const store = useStore()
const messages = ref([])
const inputText = ref('')
const chatBodyRef = ref(null)
let socket = null

const scrollToBottom = async () => {
  await nextTick()
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const loadHistory = async () => {
  try {
    const history = await getChatHistory('admin')
    messages.value = history || []
    await scrollToBottom()
  } catch (e) { console.error('加载聊天记录失败:', e) }
}

const handleSend = () => {
  const text = inputText.value.trim()
  if (!text || !socket || socket.readyState !== WebSocket.OPEN) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      ElMessage.warning('连接已断开，请刷新页面')
    }
    return
  }
  const userInfo = store.getters.userInfo
  const customerCode = userInfo?.customerCode || 'unknown'
  // 乐观更新：立即在本地显示消息
  const optimisticMsg = {
    senderCode: customerCode,
    senderName: userInfo?.customerName || customerCode,
    senderRole: 'customer',
    content: text,
    createTime: new Date().toISOString(),
    _pending: true
  }
  messages.value.push(optimisticMsg)
  scrollToBottom()
  socket.send(JSON.stringify({ receiverCode: 'admin', content: text }))
  inputText.value = ''
}

const connectWebSocket = () => {
  const token = store.state.token
  if (!token) return
  try {
    socket = createChatSocket(token)
    socket.onopen = () => {
      console.log('WebSocket 已连接')
      loadHistory()
    }
    socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        // 去重：如果是自己刚发出的消息（回显），替换 _pending 消息
        const pendingIdx = messages.value.findIndex(m => m._pending && m.content === msg.content)
        if (pendingIdx >= 0) {
          messages.value.splice(pendingIdx, 1, msg)
        } else {
          messages.value.push(msg)
        }
        scrollToBottom()
      } catch (e) { console.error('消息解析失败:', e) }
    }
    socket.onclose = () => { console.log('WebSocket 已断开') }
    socket.onerror = (e) => { console.error('WebSocket 错误:', e) }
  } catch (e) { console.error('创建 WebSocket 失败:', e) }
}

onMounted(() => {
  connectWebSocket()
})

onBeforeUnmount(() => {
  if (socket) { socket.close(); socket = null }
})
</script>

<style scoped>
.chat-container { padding: 10px 0; max-width: 700px; margin: 0 auto; }
.chat-card { height: calc(100vh - 140px); display: flex; flex-direction: column; }
.chat-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-header { display: flex; align-items: center; justify-content: space-between; }
.chat-header h2 { display: flex; align-items: center; gap: 8px; font-size: 18px; margin: 0; }
.chat-body { flex: 1; overflow-y: auto; padding: 16px; background: #f5f7fa; border-radius: 4px; margin-bottom: 8px; }
.chat-empty { display: flex; align-items: center; justify-content: center; height: 100%; }
.chat-message { display: flex; margin-bottom: 16px; }
.msg-self { justify-content: flex-end; }
.msg-other { justify-content: flex-start; }
.msg-bubble {
  max-width: 70%; padding: 10px 14px; border-radius: 8px;
  animation: msgIn 0.25s ease;
}
@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.msg-self .msg-bubble { background: #409EFF; color: #fff; }
.msg-other .msg-bubble { background: #fff; }
.msg-sender { font-size: 12px; margin-bottom: 4px; opacity: 0.8; }
.msg-content { font-size: 14px; line-height: 1.5; word-break: break-all; }
.msg-time { font-size: 11px; margin-top: 4px; opacity: 0.7; text-align: right; }
.chat-input { padding: 0; }
</style>
