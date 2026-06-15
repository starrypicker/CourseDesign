<template>
  <div class="admin-chat">
    <el-row :gutter="16" style="height:calc(100vh - 140px)">
      <el-col :xs="8" :sm="6" :md="5">
        <el-card shadow="never" class="contact-card">
          <template #header>
            <div class="contact-header">
              <span>顾客列表</span>
              <el-badge v-if="totalUnread > 0" :value="totalUnread" type="danger" />
            </div>
          </template>
          <div class="contact-list">
            <div v-for="contact in contacts" :key="contact" class="contact-item"
              :class="{ active: activeContact === contact }" @click="selectContact(contact)">
              <div class="contact-avatar">
                <el-icon :size="28" color="#409EFF"><UserFilled /></el-icon>
              </div>
              <span class="contact-name">{{ contact }}</span>
            </div>
            <el-empty v-if="contacts.length === 0" description="暂无顾客消息" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="16" :sm="18" :md="19">
        <el-card shadow="never" class="chat-main-card" v-if="activeContact">
          <template #header>
            <div class="chat-header">
              <span><el-icon><ChatDotSquare /></el-icon> {{ activeContact }}</span>
            </div>
          </template>
          <div class="chat-body" ref="chatBodyRef">
            <div v-for="(msg, index) in currentMessages" :key="index" class="chat-message"
              :class="msg.senderRole === 'admin' ? 'msg-self' : 'msg-other'">
              <div class="msg-bubble">
                <div class="msg-sender">{{ msg.senderRole === 'admin' ? '我' : msg.senderName || msg.senderCode }}</div>
                <div class="msg-content">{{ msg.content }}</div>
                <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input v-model="inputText" type="textarea" :rows="2" placeholder="输入回复..."
              @keydown.enter.prevent="handleSend" />
            <el-button type="primary" :disabled="!inputText.trim()" @click="handleSend" style="margin-top:8px">
              发送
            </el-button>
          </div>
        </el-card>
        <el-card shadow="never" class="chat-main-card empty-state" v-else>
          <el-empty description="请从左侧选择一个顾客开始聊天" :image-size="100" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useStore } from 'vuex'
import { ChatDotSquare, UserFilled } from '@element-plus/icons-vue'
import { getContacts, getChatHistory, markAsRead, getUnreadCount, createChatSocket } from '@/api/chat'

const store = useStore()
const contacts = ref([])
const activeContact = ref(null)
const allMessages = ref({})
const inputText = ref('')
const chatBodyRef = ref(null)
const totalUnread = ref(0)
let socket = null

const currentMessages = computed(() => {
  return activeContact.value ? (allMessages.value[activeContact.value] || []) : []
})

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

const loadContacts = async () => {
  try {
    const list = await getContacts()
    contacts.value = list || []
  } catch (e) { console.error('获取联系人列表失败:', e) }
}

const selectContact = async (contactCode) => {
  activeContact.value = contactCode
  try {
    const history = await getChatHistory(contactCode)
    allMessages.value[contactCode] = history || []
    await markAsRead(contactCode)
    await scrollToBottom()
  } catch (e) { console.error('加载聊天记录失败:', e) }
}

const handleSend = () => {
  const text = inputText.value.trim()
  if (!text || !socket || socket.readyState !== WebSocket.OPEN || !activeContact.value) return
  // 乐观更新：立即在本地显示消息
  const optimisticMsg = {
    senderCode: 'admin',
    senderName: store.getters.userInfo?.username || '管理员',
    senderRole: 'admin',
    receiverCode: activeContact.value,
    content: text,
    createTime: new Date().toISOString(),
    _pending: true
  }
  if (!allMessages.value[activeContact.value]) {
    allMessages.value[activeContact.value] = []
  }
  allMessages.value[activeContact.value].push(optimisticMsg)
  scrollToBottom()
  socket.send(JSON.stringify({ receiverCode: activeContact.value, content: text }))
  inputText.value = ''
}

const connectWebSocket = () => {
  const token = store.state.token
  if (!token) return
  try {
    socket = createChatSocket(token)
    socket.onopen = () => {
      console.log('Admin WebSocket 已连接')
      loadContacts()
    }
    socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        // 确定是来自哪个顾客的消息
        const contactKey = msg.senderRole === 'customer' ? msg.senderCode : msg.receiverCode
        if (contactKey && contactKey !== 'admin') {
          if (!allMessages.value[contactKey]) {
            allMessages.value[contactKey] = []
          }
          // 去重：替换自己发出的 _pending 消息
          const pendingIdx = allMessages.value[contactKey].findIndex(m => m._pending && m.content === msg.content)
          if (pendingIdx >= 0) {
            allMessages.value[contactKey].splice(pendingIdx, 1, msg)
          } else {
            allMessages.value[contactKey].push(msg)
          }
          // 如果是当前活跃聊天对象的消息
          if (activeContact.value === contactKey) {
            markAsRead(contactKey).catch(() => {})
            scrollToBottom()
          } else {
            // 非活跃联系人的消息，增加未读计数
            totalUnread.value++
          }
          // 更新联系人列表
          if (!contacts.value.includes(contactKey)) {
            contacts.value.push(contactKey)
          }
        }
      } catch (e) { console.error('消息解析失败:', e) }
    }
    socket.onclose = () => { console.log('Admin WebSocket 已断开') }
    socket.onerror = (e) => { console.error('Admin WebSocket 错误:', e) }
  } catch (e) { console.error('创建 WebSocket 失败:', e) }
}

const loadUnreadCount = async () => {
  try {
    const count = await getUnreadCount()
    totalUnread.value = count || 0
  } catch (e) {}
}

onMounted(() => {
  connectWebSocket()
  loadUnreadCount()
})

onBeforeUnmount(() => {
  if (socket) { socket.close(); socket = null }
})
</script>

<style scoped>
.admin-chat { padding: 0; height: 100%; }
.contact-card { height: 100%; display: flex; flex-direction: column; }
.contact-card :deep(.el-card__body) { flex: 1; overflow-y: auto; padding: 8px; }
.contact-header { display: flex; align-items: center; justify-content: space-between; font-weight: 500; }
.contact-list { display: flex; flex-direction: column; }
.contact-item { display: flex; align-items: center; gap: 10px; padding: 10px; border-radius: 6px; cursor: pointer; margin-bottom: 4px; }
.contact-item:hover { background: #f0f2f5; }
.contact-item.active { background: #ecf5ff; }
.contact-avatar { width: 36px; height: 36px; border-radius: 50%; background: #ecf5ff; display: flex; align-items: center; justify-content: center; }
.contact-name { font-size: 14px; color: #303133; }
.chat-main-card { height: 100%; display: flex; flex-direction: column; }
.chat-main-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-main-card.empty-state { display: flex; align-items: center; justify-content: center; }
.chat-main-card.empty-state :deep(.el-card__body) { display: flex; align-items: center; justify-content: center; }
.chat-header { display: flex; align-items: center; gap: 8px; font-weight: 500; }
.chat-body { flex: 1; overflow-y: auto; padding: 16px; background: #f5f7fa; border-radius: 4px; margin-bottom: 8px; }
.chat-message { display: flex; margin-bottom: 16px; }
.msg-self { justify-content: flex-end; }
.msg-other { justify-content: flex-start; }
.msg-bubble { max-width: 65%; padding: 10px 14px; border-radius: 8px; }
.msg-self .msg-bubble { background: #409EFF; color: #fff; }
.msg-other .msg-bubble { background: #fff; }
.msg-sender { font-size: 12px; margin-bottom: 4px; opacity: 0.8; }
.msg-content { font-size: 14px; line-height: 1.5; word-break: break-all; }
.msg-time { font-size: 11px; margin-top: 4px; opacity: 0.7; text-align: right; }
.chat-input { padding: 0; }
</style>
