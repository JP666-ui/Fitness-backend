<template>
  <div class="admin-center">
    <div class="page-header">
      <h1>🛠️ 管理后台</h1>
      <p class="welcome">管理员：{{ userStore.nickname || userStore.username }}</p>
      <div class="header-actions">
        <el-button @click="goMessages">
          <el-icon><Bell /></el-icon>
          我的消息
        </el-button>
        <el-button @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          前台首页
        </el-button>
        <el-button type="danger" plain @click="logout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs" @tab-change="onTabChange">
      <!-- ============ 任务执行监控 ============ -->
      <el-tab-pane label="任务执行监控" name="tasks">
        <div class="toolbar">
          <span class="label">统计周期：</span>
          <el-radio-group v-model="taskDays" size="small" @change="loadOverview">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="14">近 14 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
          <el-button size="small" :icon="Refresh" @click="loadOverview" style="margin-left: 12px">
            刷新
          </el-button>
          <div class="summary" v-if="overview.total !== undefined">
            共 <b>{{ overview.total }}</b> 位普通用户 ·
            活跃 <b class="ok">{{ overview.activeUsers }}</b> ·
            未打卡 <b class="warn">{{ overview.idleUsers }}</b>
          </div>
        </div>

        <el-table :data="overview.list" v-loading="taskLoading" stripe border>
          <el-table-column prop="username" label="用户" min-width="110">
            <template #default="{ row }">
              <div class="user-cell">
                <span class="name">{{ row.nickname || row.username }}</span>
                <span class="sub">@{{ row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="健身计划" min-width="150">
            <template #default="{ row }">
              <el-tag v-if="row.planName" size="small" :type="planTagType(row)">
                {{ row.planName }}
              </el-tag>
              <el-tag v-else size="small" type="info">未创建</el-tag>
              <div v-if="row.dailyDeficit" class="sub">
                缺口 {{ row.dailyDeficit }} kcal/天
              </div>
            </template>
          </el-table-column>
          <el-table-column label="今日打卡" min-width="170" align="center">
            <template #default="{ row }">
              <el-tooltip content="饮食" placement="top">
                <el-icon :class="row.foodToday ? 'done' : 'undone'"><Food /></el-icon>
              </el-tooltip>
              <el-tooltip content="运动" placement="top">
                <el-icon :class="row.exerciseToday ? 'done' : 'undone'"><Basketball /></el-icon>
              </el-tooltip>
              <el-tooltip content="体重" placement="top">
                <el-icon :class="row.weightToday ? 'done' : 'undone'"><Odometer /></el-icon>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="打卡天数" min-width="110" align="center">
            <template #default="{ row }">
              {{ row.recordDays }} / {{ row.totalDays }}
            </template>
          </el-table-column>
          <el-table-column label="执行率" min-width="160">
            <template #default="{ row }">
              <el-progress
                :percentage="row.score"
                :status="scoreStatus(row.score)"
                :stroke-width="14"
                text-inside
              />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="评价" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="statusTagType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="openDetail(row)">
                详情
              </el-button>
              <el-button size="small" link type="warning" @click="openSend(row.userId, row.nickname || row.username)">
                发消息
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ============ 消息提醒 ============ -->
      <el-tab-pane label="消息提醒" name="notice">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="never" class="notice-card">
              <template #header><b>发送消息</b></template>
              <el-form :model="noticeForm" label-width="80px">
                <el-form-item label="接收人">
                  <el-select
                    v-model="noticeForm.receiverId"
                    placeholder="全体用户（广播）"
                    clearable
                    filterable
                    style="width: 100%"
                  >
                    <el-option
                      v-for="u in (overview.list || [])"
                      :key="u.userId"
                      :label="`${u.nickname || u.username} (@${u.username})`"
                      :value="u.userId"
                    />
                  </el-select>
                  <div class="tip">不选择则发送给全体普通用户</div>
                </el-form-item>
                <el-form-item label="类型">
                  <el-radio-group v-model="noticeForm.type">
                    <el-radio-button :value="1">通知</el-radio-button>
                    <el-radio-button :value="2">提醒</el-radio-button>
                    <el-radio-button :value="3">警告</el-radio-button>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="标题">
                  <el-input v-model="noticeForm.title" maxlength="100" show-word-limit
                            placeholder="例如：今日饮食记录提醒" />
                </el-form-item>
                <el-form-item label="内容">
                  <el-input v-model="noticeForm.content" type="textarea" :rows="4"
                            maxlength="500" show-word-limit
                            placeholder="消息正文内容" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="sending" @click="handleSend">
                    发送
                  </el-button>
                  <el-button @click="fillTemplate">填充示例模板</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="notice-card">
              <template #header>
                <div class="card-head-row">
                  <b>发送记录</b>
                  <el-button size="small" :icon="Refresh" @click="loadSent">刷新</el-button>
                </div>
              </template>
              <el-empty v-if="!sentList.length" description="暂无发送记录" />
              <div v-else class="sent-list">
                <div v-for="m in sentList" :key="m.id" class="sent-item">
                  <div class="sent-top">
                    <el-tag size="small" :type="noticeTagType(m.type)">{{ noticeTypeName(m.type) }}</el-tag>
                    <b>{{ m.title }}</b>
                    <el-tag size="small" :type="m.receiverId ? 'warning' : 'success'" effect="plain">
                      {{ m.receiverId ? ('定向 #' + m.receiverId) : '全体广播' }}
                    </el-tag>
                    <span class="time">{{ formatTime(m.createTime) }}</span>
                  </div>
                  <div class="sent-content">{{ m.content }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ============ 用户管理 ============ -->
      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar">
          <el-input
            v-model="userKeyword"
            placeholder="搜索用户名/昵称"
            clearable
            style="width: 220px"
            @keyup.enter="loadUsers"
            @clear="loadUsers"
          />
          <el-button size="small" :icon="Search" @click="loadUsers" style="margin-left: 8px">搜索</el-button>
        </div>
        <el-table :data="userPage.records" v-loading="userLoading" stripe border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="用户名" min-width="110" />
          <el-table-column prop="nickname" label="昵称" min-width="110" />
          <el-table-column label="角色" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.role === 1 ? 'danger' : 'info'">
                {{ row.role === 1 ? '管理员' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '正常' : '已禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="weight" label="体重" width="90">
            <template #default="{ row }">{{ row.weight ? row.weight + ' kg' : '-' }}</template>
          </el-table-column>
          <el-table-column prop="targetWeight" label="目标体重" width="100">
            <template #default="{ row }">{{ row.targetWeight ? row.targetWeight + ' kg' : '-' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" min-width="150">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <template v-if="row.role !== 1">
                <el-button size="small" link :type="row.status === 1 ? 'warning' : 'success'"
                           @click="toggleUser(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button size="small" link type="primary"
                           @click="openSend(row.userId, row.nickname || row.username)">
                  发消息
                </el-button>
                <el-button size="small" link type="danger" @click="removeUser(row)">删除</el-button>
              </template>
              <span v-else class="sub">管理员账号不可操作</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="userPageNum"
          :page-size="10"
          :total="userPage.total || 0"
          layout="total, prev, pager, next"
          style="margin-top: 12px; justify-content: flex-end"
          @current-change="loadUsers"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- ============ 用户详情抽屉 ============ -->
    <el-drawer v-model="detailVisible" :title="detailTitle" size="45%">
      <div v-if="detail.userId" class="detail-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="用户">{{ detail.nickname }} (@{{ detail.username }})</el-descriptions-item>
          <el-descriptions-item label="评价">
            <el-tag size="small" :type="statusTagType(detail.status)">{{ detail.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前体重">{{ detail.weight || '-' }} kg</el-descriptions-item>
          <el-descriptions-item label="目标体重">{{ detail.targetWeight || '-' }} kg</el-descriptions-item>
          <el-descriptions-item label="计划">{{ detail.planName || '未创建' }}</el-descriptions-item>
          <el-descriptions-item label="每日缺口">{{ detail.dailyDeficit || '-' }} kcal</el-descriptions-item>
          <el-descriptions-item label="打卡天数">{{ detail.recordDays }} / {{ detail.totalDays }}</el-descriptions-item>
          <el-descriptions-item label="执行率">{{ detail.score }}%</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 18px 0 8px">每日记录明细</h4>
        <el-table :data="detail.daily" size="small" border max-height="320">
          <el-table-column prop="date" label="日期" width="110" />
          <el-table-column prop="intake" label="摄入(kcal)" width="100" align="center" />
          <el-table-column prop="burn" label="运动消耗(kcal)" align="center" />
          <el-table-column label="打卡" width="80" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.hasRecord" color="#67c23a"><CircleCheckFilled /></el-icon>
              <el-icon v-else color="#c0c4cc"><CircleCloseFilled /></el-icon>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top: 16px">
          <el-button type="warning" @click="openSend(detail.userId, detail.nickname || detail.username)">
            给 TA 发消息
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, HomeFilled, SwitchButton, Refresh, Search,
  Food, Basketball, Odometer, CircleCheckFilled, CircleCloseFilled,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getTaskOverview, getTaskDetail,
  getUserList, updateUserStatus, deleteUser,
} from '@/api/admin'
import { sendNotice, getSentNotices } from '@/api/notice'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('tasks')

// ---------- 任务监控 ----------
const taskDays = ref(7)
const taskLoading = ref(false)
const overview = ref({})

const loadOverview = async () => {
  taskLoading.value = true
  try {
    const res = await getTaskOverview(taskDays.value)
    if (res.code === 200) overview.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    taskLoading.value = false
  }
}

const planTagType = (row) => {
  if (row.planStatus === 1) return 'success'
  if (row.planStatus === 2) return 'info'
  return 'warning'
}
const scoreStatus = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return ''
  if (score >= 30) return 'warning'
  return 'exception'
}
const statusTagType = (s) => {
  return { 优秀: 'success', 良好: '', 一般: 'warning', 懈怠: 'danger' }[s] ?? 'info'
}

// ---------- 详情抽屉 ----------
const detailVisible = ref(false)
const detail = ref({})
const detailTitle = computed(() =>
  detail.value.username ? `任务详情 - ${detail.value.nickname || detail.value.username}` : '任务详情')

const openDetail = async (row) => {
  try {
    const res = await getTaskDetail(row.userId, taskDays.value)
    if (res.code === 200) {
      detail.value = res.data
      detailVisible.value = true
    }
  } catch (e) {
    console.error(e)
  }
}

// ---------- 消息 ----------
const noticeForm = reactive({ receiverId: null, type: 1, title: '', content: '' })
const sending = ref(false)
const sentList = ref([])

const openSend = (userId, name) => {
  noticeForm.receiverId = userId
  noticeForm.title = ''
  noticeForm.content = ''
  noticeForm._name = name
  activeTab.value = 'notice'
}

const fillTemplate = () => {
  const name = noticeForm._name
  if (noticeForm.receiverId && name) {
    noticeForm.title = '打卡提醒'
    noticeForm.content = `${name}，检测到您今日尚未完成饮食/运动记录打卡，坚持记录是达成目标的第一步，记得补上哦！`
  } else {
    noticeForm.title = '周总结提醒'
    noticeForm.content = '各位同学，本周即将结束，请及时查看自己的热量缺口与体重趋势，保持记录习惯，下周继续加油！'
  }
}

const handleSend = async () => {
  if (!noticeForm.title.trim() || !noticeForm.content.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  sending.value = true
  try {
    const res = await sendNotice({
      receiverId: noticeForm.receiverId || null,
      type: noticeForm.type,
      title: noticeForm.title.trim(),
      content: noticeForm.content.trim(),
    })
    if (res.code === 200) {
      ElMessage.success(noticeForm.receiverId ? '已发送给用户' : '已广播给全体用户')
      noticeForm.title = ''
      noticeForm.content = ''
      loadSent()
    }
  } catch (e) {
    console.error(e)
  } finally {
    sending.value = false
  }
}

const loadSent = async () => {
  try {
    const res = await getSentNotices(30)
    if (res.code === 200) sentList.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const noticeTypeName = (t) => ({ 1: '通知', 2: '提醒', 3: '警告' }[t] || '通知')
const noticeTagType = (t) => ({ 1: 'primary', 2: 'warning', 3: 'danger' }[t] || 'primary')

// ---------- 用户管理 ----------
const userKeyword = ref('')
const userPageNum = ref(1)
const userLoading = ref(false)
const userPage = ref({ records: [], total: 0 })

const loadUsers = async () => {
  userLoading.value = true
  try {
    const res = await getUserList({ page: userPageNum.value, size: 10, keyword: userKeyword.value || undefined })
    if (res.code === 200) userPage.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    userLoading.value = false
  }
}

const toggleUser = async (row) => {
  const to = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确定${to === 1 ? '启用' : '禁用'}用户「${row.username}」吗？`, '提示', { type: 'warning' })
    const res = await updateUserStatus(row.id, to)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      loadUsers()
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const removeUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${row.username}」吗？该操作不可恢复！`, '危险操作', { type: 'error' })
    const res = await deleteUser(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      loadUsers()
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// ---------- 通用 ----------
const onTabChange = (name) => {
  if (name === 'notice' && !sentList.value.length) loadSent()
  if (name === 'users' && !userPage.value.records.length) loadUsers()
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').substring(0, 16) : '-')

const goMessages = () => router.push('/messages')
const goHome = () => router.push('/simple')
const logout = () => {
  userStore.logout()
  window.location.href = '/login'
}

onMounted(() => {
  // 非管理员直接踢回（防止手动输 URL）
  if (userStore.userInfo && userStore.role !== 1) {
    ElMessage.error('无管理员权限')
    router.replace('/simple')
    return
  }
  loadOverview()
})
</script>

<style scoped>
.admin-center {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 24px;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 14px;
  padding: 22px 28px;
  margin-bottom: 18px;
  position: relative;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 26px;
}

.welcome {
  margin: 0;
  opacity: 0.85;
}

.header-actions {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
}

.admin-tabs {
  background: #fff;
  border-radius: 14px;
  padding: 8px 20px 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  margin: 6px 0 14px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar .label {
  color: #606266;
  font-size: 14px;
}

.summary {
  margin-left: auto;
  color: #606266;
  font-size: 14px;
}

.summary .ok { color: #67c23a; }
.summary .warn { color: #e6a23c; }

.user-cell .name { display: block; font-weight: 600; }
.user-cell .sub, .sub { color: #909399; font-size: 12px; }

.done { color: #67c23a; margin: 0 4px; font-size: 18px; }
.undone { color: #c0c4cc; margin: 0 4px; font-size: 18px; }

.notice-card { border-radius: 10px; }
.tip { color: #909399; font-size: 12px; line-height: 1.6; }

.card-head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sent-list { max-height: 480px; overflow-y: auto; }

.sent-item {
  border-bottom: 1px dashed #ebeef5;
  padding: 10px 0;
}

.sent-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.sent-top .time {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.sent-content {
  color: #606266;
  font-size: 13px;
  margin-top: 6px;
  line-height: 1.6;
}

.detail-body { padding: 0 4px; }
</style>
