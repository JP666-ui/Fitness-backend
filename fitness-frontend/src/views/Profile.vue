<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <!-- 左侧：个人信息展示 -->
      <el-col :xs="24" :md="8">
        <el-card class="profile-card">
          <template #header>
            <div class="card-header">
              <span>👤 个人信息</span>
              <el-button type="primary" size="small" @click="startEdit">
                <el-icon><Edit /></el-icon>
                修改
              </el-button>
            </div>
          </template>
          
          <!-- 展示模式 -->
          <div v-if="!isEditing" class="profile-display">
            <el-avatar :size="100" :icon="UserFilled" class="user-avatar" />
            <h3 class="nickname">{{ userInfo.nickname || userInfo.username }}</h3>
            <p class="username">@{{ userInfo.username }}</p>
            
            <el-divider />
            
            <div class="info-list">
              <div class="info-item">
                <span class="label">📧 邮箱：</span>
                <span class="value">{{ userInfo.email || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">📱 手机：</span>
                <span class="value">{{ userInfo.phone || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">⚧ 性别：</span>
                <span class="value">{{ getGenderText(userInfo.gender) }}</span>
              </div>
              <div class="info-item">
                <span class="label">🎂 年龄：</span>
                <span class="value">{{ userInfo.age || '--' }} 岁</span>
              </div>
              <div class="info-item">
                <span class="label">📏 身高：</span>
                <span class="value">{{ userInfo.height ? userInfo.height + ' cm' : '--' }}</span>
              </div>
              <div class="info-item">
                <span class="label">⚖️ 体重：</span>
                <span class="value">{{ userInfo.weight ? userInfo.weight + ' kg' : '--' }}</span>
              </div>
              <div class="info-item">
                <span class="label">🎯 目标体重：</span>
                <span class="value">{{ userInfo.targetWeight ? userInfo.targetWeight + ' kg' : '未设置' }}</span>
              </div>
            </div>
          </div>
          
          <!-- 编辑模式 -->
          <div v-else class="profile-edit">
            <el-form :model="editForm" label-width="80px">
              <el-form-item label="昵称">
                <el-input v-model="editForm.nickname" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="editForm.email" />
              </el-form-item>
              <el-form-item label="手机">
                <el-input v-model="editForm.phone" />
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="editForm.gender">
                  <el-radio :label="1">男</el-radio>
                  <el-radio :label="2">女</el-radio>
                  <el-radio :label="0">保密</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="年龄">
                <el-input-number v-model="editForm.age" :min="1" :max="150" />
              </el-form-item>
              <el-form-item label="身高">
                <el-input-number v-model="editForm.height" :min="50" :max="250">
                  <template #append>cm</template>
                </el-input-number>
              </el-form-item>
              <el-form-item label="体重">
                <el-input-number v-model="editForm.weight" :min="20" :max="300" :step="0.1" :precision="1">
                  <template #append>kg</template>
                </el-input-number>
              </el-form-item>
              <el-form-item label="目标体重">
                <el-input-number v-model="editForm.targetWeight" :min="20" :max="300" :step="0.5">
                  <template #append>kg</template>
                </el-input-number>
                <div class="form-hint">设置后自动计算每日热量缺口</div>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProfile">保存</el-button>
                <el-button @click="cancelEdit">取消</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>
        
        <!-- 健康数据卡片 -->
        <el-card class="health-card" style="margin-top: 20px;">
          <template #header>
            <span>❤️ 健康数据</span>
          </template>
          <div class="health-data">
            <div class="health-item">
              <span class="label">BMI：</span>
              <span class="value" :class="getBMIClass">{{ bmi || '--' }}</span>
            </div>
            <div class="health-item">
              <span class="label">状态：</span>
              <el-tag :type="bmiStatus.type">{{ bmiStatus.text }}</el-tag>
            </div>
            <div class="health-item">
              <span class="label">基础代谢：</span>
              <span class="value">{{ bmr || '--' }} kcal</span>
            </div>
            <div class="health-item">
              <span class="label">每日总消耗：</span>
              <span class="value highlight">{{ tdee || '--' }} kcal</span>
            </div>
            <div class="health-item">
              <span class="label">每日目标：</span>
              <span class="value">{{ userInfo.dailyCalorieGoal || 2000 }} kcal</span>
            </div>
          </div>
          <div v-if="!bmr" class="health-tip">
            补全身高、体重、年龄后，系统会自动计算你的基础代谢与每日消耗
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧：体重记录 -->
      <el-col :xs="24" :md="16">
        <el-card>
          <template #header>
            <span>📊 体重记录</span>
          </template>
          
          <div class="weight-form">
            <el-form :inline="true" :model="weightForm">
              <el-form-item label="当前体重">
                <el-input-number v-model="weightForm.weight" :min="20" :max="300">
                  <template #append>kg</template>
                </el-input-number>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="updateWeight">更新体重</el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <el-divider />
          
          <div class="weight-chart">
            <div ref="chartRef" style="width: 100%; height: 300px;"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, Edit } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, updateWeight as apiUpdateWeight } from '@/api/user'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})

// 编辑模式
const isEditing = ref(false)
const editForm = ref({
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  age: null,
  height: null,
  weight: null,
  targetWeight: null,
})

const weightForm = ref({
  weight: userInfo.value.weight || 70,
})

// BMI 计算
const bmi = computed(() => {
  if (!userInfo.value.height || !userInfo.value.weight) return null
  const heightInMeter = userInfo.value.height / 100
  return (userInfo.value.weight / (heightInMeter * heightInMeter)).toFixed(1)
})

const bmiStatus = computed(() => {
  if (!bmi.value) return { text: '未知', type: 'info' }
  const val = parseFloat(bmi.value)
  if (val < 18.5) return { text: '偏瘦', type: 'warning' }
  if (val < 24) return { text: '正常', type: 'success' }
  if (val < 28) return { text: '偏胖', type: 'warning' }
  return { text: '肥胖', type: 'danger' }
})

const getBMIClass = computed(() => {
  if (!bmi.value) return ''
  const val = parseFloat(bmi.value)
  if (val < 18.5) return 'underweight'
  if (val < 24) return 'normal'
  if (val < 28) return 'overweight'
  return 'obese'
})

/**
 * 基础代谢 BMR（Mifflin-St Jeor 公式）
 * 与后端 CalorieCalculatorService 保持一致
 */
const bmr = computed(() => {
  const h = userInfo.value.height
  const w = userInfo.value.weight
  const a = userInfo.value.age
  if (!h || !w || !a) return 0

  let value = 10 * w + 6.25 * h - 5 * a
  // gender: 1-男 2-女
  value += userInfo.value.gender === 2 ? -161 : 5
  return Math.round(value)
})

/** 每日总消耗 TDEE = BMR × 1.375（轻度活动） */
const tdee = computed(() => (bmr.value ? Math.round(bmr.value * 1.375) : 0))

const getGenderText = (gender) => {
  const map = { 0: '保密', 1: '男', 2: '女' }
  return map[gender] || '保密'
}

// 开始编辑
const startEdit = () => {
  editForm.value = {
    nickname: userInfo.value.nickname || '',
    email: userInfo.value.email || '',
    phone: userInfo.value.phone || '',
    gender: userInfo.value.gender || 0,
    age: userInfo.value.age || null,
    height: userInfo.value.height || null,
    weight: userInfo.value.weight || null,
    targetWeight: userInfo.value.targetWeight || null,
  }
  isEditing.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEditing.value = false
}

// 保存个人信息
const saveProfile = async () => {
  try {
    const res = await updateUserInfo(editForm.value)
    if (res.code === 200) {
      // 新返回结构：{ user, recalculated, plan }
      const payload = res.data || {}
      const updatedUser = payload.user || editForm.value
      userStore.setUserInfo({ ...userInfo.value, ...updatedUser })
      isEditing.value = false

      // 若触发了热量缺口重算，展示结果
      if (payload.recalculated && payload.plan?.calorie) {
        const c = payload.plan.calorie
        if (c.goalType === 'loss' && c.dailyDeficit) {
          ElMessageBox.alert(
            `<div style="line-height:1.9">
              需减重 <b>${c.weightToLose} kg</b>，每日热量缺口
              <b style="color:#f56c6c">${c.dailyDeficit} kcal</b><br/>
              建议每日摄入 <b>${c.suggestedIntake} kcal</b><br/>
              预计 <b>${c.estimatedDays} 天</b> 达成目标（${c.estimatedDate}）
             </div>`,
            '热量缺口已自动计算',
            { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' },
          )
        } else {
          ElMessage.success('保存成功，热量目标已更新')
        }
      } else if (payload.message) {
        ElMessage.warning(payload.message)
      } else {
        ElMessage.success('保存成功')
      }
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 更新体重
const updateWeight = async () => {
  try {
    const res = await apiUpdateWeight({ weight: weightForm.value.weight })
    if (res.code === 200) {
      ElMessage.success('体重更新成功')
      userStore.setUserInfo({ ...userInfo.value, weight: weightForm.value.weight })
    }
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

onMounted(() => {
  // 刷新用户信息（有缓存时不会重复请求）
  userStore.getUserInfoAction()
  weightForm.value.weight = userInfo.value.weight || 70
})
</script>

<style scoped>
.profile-page {
  padding: 20px;
}

.profile-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-display {
  text-align: center;
  padding: 20px 0;
}

.user-avatar {
  margin-bottom: 15px;
  background: #409EFF;
}

.nickname {
  font-size: 20px;
  margin-bottom: 5px;
}

.username {
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
}

.info-list {
  text-align: left;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #909399;
}

.info-item .value {
  color: #303133;
  font-weight: 500;
}

.profile-edit {
  padding: 10px 0;
}

.health-card {
  margin-top: 20px;
}

.health-data {
  padding: 10px 0;
}

.health-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.health-item:last-child {
  border-bottom: none;
}

.health-item .label {
  color: #909399;
}

.health-item .value {
  font-weight: bold;
}

.health-item .value.highlight {
  color: #409eff;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.5;
}

.health-tip {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fdf6ec;
  border-radius: 8px;
  font-size: 12px;
  color: #b88230;
  line-height: 1.6;
}

.value.underweight {
  color: #e6a23c;
}

.value.normal {
  color: #67c23a;
}

.value.overweight {
  color: #e6a23c;
}

.value.obese {
  color: #f56c6c;
}

.weight-form {
  margin-bottom: 20px;
}

.weight-chart {
  padding: 20px 0;
}
</style>
