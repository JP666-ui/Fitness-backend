<template>
  <div class="messages-page">
    <div class="page-header">
      <h1>📬 我的消息</h1>
      <div class="header-actions">
        <el-button size="small" :icon="Check" :disabled="!unreadCount" @click="handleReadAll">
          全部已读（{{ unreadCount }} 封未读）
        </el-button>
        <el-button size="small" :icon="Refresh" @click="loadInbox">刷新</el-button>
        <el-button size="small" @click="goBack">
          <el-icon><Back /></el-icon>
          返回
        </el-button>
      </div>
    </div>

    <div class="notice-banner" v-if="unreadCount > 0">
      您有 <b>{{ unreadCount }}</b> 条未读消息，请及时处理
    </div>

    <el-empty v-if="!loading && !list.length" description="暂无消息" />

    <div v-loading="loading" class="msg-list">
      <div
        v-for="m in list"
        :key="m.id"
        class="msg-item"
        :class="{ unread: m.isRead === 0 }"
        @click="markRead(m)"
      >
        <div class="msg-left">
          <el-tag size="small" :type="tagType(m.type)" effect="dark">
            {{ typeName(m.type) }}
          </el-tag>
        </div>
        <div class="msg-main">
          <div class="msg-top">
            <span class="dot" v-if="m.isRead === 0"></span>
            <b class="msg-title">{{ m.title }}</b>
            <el-tag v-if="!m.receiverId" size="small" effect="plain" type="success">全体</el-tag>
            <span class="sender">来自：{{ m.senderName }}</span>
            <span class="time">{{ formatTime(m.createTime) }}</span>
          </div>
          <div class="msg-content">{{ m.content }}</div>
        </div>
        <div class="msg-right">
          <el-icon v-if="m.isRead === 1" color="#67c23a"><CircleCheckFilled /></el-icon>
          <el-icon v-else color="#e6a23c"><WarningFilled /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Refresh, Back, CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import { getInbox, markRead as apiMarkRead, markAllRead } from '@/api/notice'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const unreadCount = ref(0)
let timer = null

const loadInbox = async () => {
  loading.value = true
  try {
    const res = await getInbox(100)
    if (res.code === 200) {
      list.value = res.data
      unreadCount.value = res.data.filter((m) => m.isRead === 0).length
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const markRead = async (m) => {
  if (m.isRead === 1) return
  try {
    const res = await apiMarkRead(m.id)
    if (res.code === 200) {
      m.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  } catch (e) {
    console.error(e)
  }
}

const handleReadAll = async () => {
  try {
    const res = await markAllRead()
    if (res.code === 200) {
      ElMessage.success(`已标记 ${res.data.marked} 条消息为已读`)
      loadInbox()
    }
  } catch (e) {
    console.error(e)
  }
}

const typeName = (t) => ({ 1: '通知', 2: '提醒', 3: '警告' }[t] || '通知')
const tagType = (t) => ({ 1: 'primary', 2: 'warning', 3: 'danger' }[t] || 'primary')
const formatTime = (t) => (t ? String(t).replace('T', ' ').substring(0, 16) : '-')
const goBack = () => router.back()

onMounted(() => {
  loadInbox()
  // 60 秒自动刷新一次
  timer = setInterval(loadInbox, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.messages-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 24px;
}

.page-header {
  background: #fff;
  border-radius: 14px;
  padding: 18px 24px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.notice-banner {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  color: #e6a23c;
  border-radius: 10px;
  padding: 10px 16px;
  margin-bottom: 14px;
  font-size: 14px;
}

.msg-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msg-item {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
  cursor: pointer;
  transition: box-shadow 0.2s;
  border-left: 4px solid transparent;
}

.msg-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.msg-item.unread {
  border-left-color: #e6a23c;
  background: #fffdf7;
}

.msg-main {
  flex: 1;
  min-width: 0;
}

.msg-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.msg-title {
  font-size: 15px;
  color: #303133;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  display: inline-block;
}

.sender {
  color: #909399;
  font-size: 12px;
}

.time {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.msg-content {
  margin-top: 8px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-all;
}

.msg-right {
  padding-top: 2px;
}
</style>
