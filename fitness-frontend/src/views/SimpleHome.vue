<template>
  <div class="simple-home">
    <div class="header-section">
      <h1>🏋️ 个人健身管理系统</h1>
      <p class="welcome">
        欢迎，{{ userStore.nickname || userStore.username || '访客' }}
        <el-tag v-if="userStore.isAdmin" size="small" type="danger" effect="dark" style="margin-left: 8px">
          管理员
        </el-tag>
      </p>
    </div>
    
    <div class="feature-grid">
      <div class="feature-card" @click="goTo('/food')">
        <div class="icon">🍎</div>
        <h3>饮食记录</h3>
        <p>记录每日饮食，计算热量摄入</p>
      </div>
      
      <div class="feature-card" @click="goTo('/exercise')">
        <div class="icon">🏃</div>
        <h3>运动记录</h3>
        <p>记录运动消耗，追踪健身进度</p>
      </div>
      
      <div class="feature-card" @click="goTo('/weight')">
        <div class="icon">⚖️</div>
        <h3>体重记录</h3>
        <p>每日记录体重，追踪变化趋势</p>
      </div>
      
      <div class="feature-card" @click="goTo('/ai')">
        <div class="icon">🤖</div>
        <h3>AI 营养咨询</h3>
        <p>智谱 GLM 智能营养分析</p>
      </div>
      
      <div class="feature-card" @click="goTo('/plan')">
        <div class="icon">📋</div>
        <h3>健身计划</h3>
        <p>制定个性化健身目标</p>
      </div>
      
      <div class="feature-card" @click="goTo('/statistics')">
        <div class="icon">📊</div>
        <h3>数据统计</h3>
        <p>查看健身数据分析</p>
      </div>
      
      <div class="feature-card" @click="goTo('/profile')">
        <div class="icon">👤</div>
        <h3>个人中心</h3>
        <p>管理个人信息和设置</p>
      </div>

      <div class="feature-card" @click="goTo('/messages')">
        <div class="icon" style="position: relative; display: inline-block">
          📬
          <span v-if="unread > 0" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
        </div>
        <h3>消息通知</h3>
        <p>查看管理员发来的提醒</p>
      </div>

      <div class="feature-card admin-entry" v-if="userStore.isAdmin" @click="goTo('/admin')">
        <div class="icon">🛠️</div>
        <h3>管理后台</h3>
        <p>任务监控 · 消息提醒 · 用户管理</p>
      </div>
    </div>
    
    <div class="action-bar">
      <el-button type="danger" size="large" @click="logout">
        <el-icon><SwitchButton /></el-icon>
        退出登录
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/notice'
import { SwitchButton } from '@element-plus/icons-vue'

const userStore = useUserStore()
const unread = ref(0)
let timer = null

const loadUnread = async () => {
  try {
    const res = await getUnreadCount()
    if (res.code === 200) unread.value = res.data.unread || 0
  } catch {
    // 未登录或网络异常时静默
  }
}

const goTo = (path) => {
  window.location.href = path
}

const logout = () => {
  userStore.logout()
  window.location.href = '/login'
}

onMounted(() => {
  loadUnread()
  // 60 秒轮询一次未读数
  timer = setInterval(loadUnread, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.simple-home {
  min-height: 100vh;
  padding: 40px 20px;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-image: 
    linear-gradient(135deg, rgba(102, 126, 234, 0.9) 0%, rgba(118, 75, 162, 0.9) 100%),
    url('https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=1920');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.header-section {
  margin-bottom: 40px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

h1 {
  color: #409EFF;
  margin-bottom: 10px;
  font-size: 32px;
}

.welcome {
  color: #606266;
  font-size: 18px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto 40px;
}

.feature-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.feature-card .icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.feature-card h3 {
  color: #303133;
  margin-bottom: 10px;
  font-size: 20px;
}

.feature-card p {
  color: #909399;
  font-size: 14px;
}

.action-bar {
  margin-top: 40px;
}

/* 未读消息角标 */
.badge {
  position: absolute;
  top: -6px;
  right: -14px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  line-height: 1;
  padding: 4px 7px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

/* 管理后台入口突出显示 */
.admin-entry {
  border: 2px dashed #764ba2;
}
</style>
