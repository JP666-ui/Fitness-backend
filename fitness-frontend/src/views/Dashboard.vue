<template>
  <div class="dashboard">
    <!-- 今日概览卡片 -->
    <el-row :gutter="20" class="overview-cards">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #409EFF;">
            <el-icon><Food /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ todayStats.todayIntake || 0 }}</div>
            <div class="stat-label">今日摄入 (kcal)</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #67C23A;">
            <el-icon><Odometer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ todayStats.todayBurn || 0 }}</div>
            <div class="stat-label">今日消耗 (kcal)</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #E6A23C;">
            <el-icon><PieChart /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ todayStats.netCalorie || 0 }}</div>
            <div class="stat-label">净摄入 (kcal)</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #F56C6C;">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ todayStats.remaining || 0 }}</div>
            <div class="stat-label">剩余可摄入 (kcal)</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- BMI 和目标体重 -->
    <el-row :gutter="20" class="bmi-row">
      <el-col :xs="24" :md="12">
        <el-card class="bmi-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>BMI 指数</span>
              <el-tag :type="bmiStatus.type">{{ bmiStatus.text }}</el-tag>
            </div>
          </template>
          <div class="bmi-content">
            <div class="bmi-value">{{ todayStats.bmi?.toFixed(1) || '--' }}</div>
            <div class="bmi-desc">身体质量指数</div>
            <el-progress 
              :percentage="bmiPercentage" 
              :color="bmiProgressColor"
              :stroke-width="10"
            />
            <div class="bmi-range">
              <span>偏瘦</span>
              <span>正常</span>
              <span>偏胖</span>
              <span>肥胖</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :md="12">
        <el-card class="weight-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>体重目标</span>
              <el-button type="primary" size="small" @click="updateWeight">
                更新体重
              </el-button>
            </div>
          </template>
          <div class="weight-content">
            <div class="weight-item">
              <div class="weight-label">当前体重</div>
              <div class="weight-value">{{ todayStats.weight || '--' }} kg</div>
            </div>
            <div class="weight-item">
              <div class="weight-label">目标体重</div>
              <div class="weight-value target">{{ todayStats.targetWeight || '--' }} kg</div>
            </div>
            <div class="weight-progress">
              <div class="progress-label">
                完成度: {{ weightProgress }}%
              </div>
              <el-progress :percentage="weightProgress" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 快捷操作 -->
    <el-card class="quick-actions" shadow="hover">
      <template #header>
        <span>快捷操作</span>
      </template>
      <div class="action-buttons">
        <el-button type="primary" size="large" @click="goToFood">
          <el-icon><Plus /></el-icon>
          记录饮食
        </el-button>
        <el-button type="success" size="large" @click="goToExercise">
          <el-icon><Plus /></el-icon>
          记录运动
        </el-button>
        <el-button type="warning" size="large" @click="goToAI">
          <el-icon><ChatDotRound /></el-icon>
          AI 咨询
        </el-button>
      </div>
    </el-card>
    
    <!-- 用户列表（仅管理员可见） -->
    <UserListPanel v-if="isAdmin" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Food, Odometer, PieChart, Timer, Plus, ChatDotRound } from '@element-plus/icons-vue'
import { getTodayStatistics } from '@/api/statistics'
import UserListPanel from '@/components/UserListPanel.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 判断是否为管理员
const isAdmin = computed(() => userStore.username === 'admin')
const todayStats = ref({})

// BMI 状态计算
const bmiStatus = computed(() => {
  const bmi = todayStats.value.bmi
  if (!bmi) return { text: '未知', type: 'info' }
  if (bmi < 18.5) return { text: '偏瘦', type: 'warning' }
  if (bmi < 24) return { text: '正常', type: 'success' }
  if (bmi < 28) return { text: '偏胖', type: 'warning' }
  return { text: '肥胖', type: 'danger' }
})

const bmiPercentage = computed(() => {
  const bmi = todayStats.value.bmi || 0
  return Math.min(Math.max((bmi / 40) * 100, 0), 100)
})

const bmiProgressColor = computed(() => {
  const bmi = todayStats.value.bmi || 0
  if (bmi < 18.5) return '#E6A23C'
  if (bmi < 24) return '#67C23A'
  if (bmi < 28) return '#E6A23C'
  return '#F56C6C'
})

// 体重进度
const weightProgress = computed(() => {
  const current = todayStats.value.weight || 0
  const target = todayStats.value.targetWeight || 0
  if (!current || !target) return 0
  // 简化计算，实际应该根据起始体重计算
  return Math.round((target / current) * 100)
})

const fetchTodayStats = async () => {
  try {
    const res = await getTodayStatistics()
    if (res.code === 200) {
      todayStats.value = res.data
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  }
}

const updateWeight = () => {
  router.push('/profile')
}

const goToFood = () => {
  router.push('/food')
}

const goToExercise = () => {
  router.push('/exercise')
}

const goToAI = () => {
  router.push('/ai')
}

onMounted(() => {
  fetchTodayStats()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.overview-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
  margin-right: 20px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.bmi-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bmi-content {
  text-align: center;
  padding: 20px;
}

.bmi-value {
  font-size: 48px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.bmi-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
}

.bmi-range {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.weight-content {
  padding: 20px;
}

.weight-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #EBEEF5;
}

.weight-item:last-child {
  border-bottom: none;
}

.weight-label {
  font-size: 14px;
  color: #606266;
}

.weight-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.weight-value.target {
  color: #67C23A;
}

.weight-progress {
  margin-top: 20px;
}

.progress-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
}

.quick-actions {
  margin-top: 20px;
}

.action-buttons {
  display: flex;
  gap: 20px;
  padding: 20px;
}

.action-buttons .el-button {
  flex: 1;
}
</style>
