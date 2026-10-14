<template>
  <div class="plan-page">
    <div class="page-header">
      <h2>📋 我的健身计划</h2>
      <p class="subtitle">系统根据你的身高体重自动计算每日热量缺口</p>
    </div>

    <!-- 资料不完整提示 -->
    <el-alert
      v-if="calorie.profileComplete === false"
      type="warning"
      :closable="false"
      show-icon
      class="mb-16"
    >
      <template #title>
        请先补全个人资料（身高、体重、年龄）才能精确计算热量缺口
        <el-link type="primary" @click="goProfile" style="margin-left: 8px">去完善 →</el-link>
      </template>
    </el-alert>

    <!-- 热量缺口核心卡片 -->
    <el-card class="deficit-card" shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>🔥 热量缺口分析</span>
          <el-tag v-if="calorie.goalType" :type="goalTagType" size="small">
            {{ goalText }}
          </el-tag>
        </div>
      </template>

      <!-- 核心数字对比 -->
      <el-row :gutter="16" class="calorie-row">
        <el-col :xs="12" :sm="6">
          <div class="calorie-box">
            <div class="calorie-value">{{ calorie.bmr || '--' }}</div>
            <div class="calorie-label">基础代谢 BMR</div>
            <div class="calorie-hint">静息状态消耗</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="calorie-box highlight">
            <div class="calorie-value">{{ calorie.tdee || '--' }}</div>
            <div class="calorie-label">每日总消耗 TDEE</div>
            <div class="calorie-hint">{{ calorie.activityLevel || '含日常活动' }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="calorie-box danger" v-if="calorie.goalType === 'loss'">
            <div class="calorie-value">-{{ calorie.dailyDeficit }}</div>
            <div class="calorie-label">每日热量缺口</div>
            <div class="calorie-hint">消耗 &gt; 摄入</div>
          </div>
          <div class="calorie-box success" v-else-if="calorie.goalType === 'gain'">
            <div class="calorie-value">+{{ calorie.dailySurplus || 300 }}</div>
            <div class="calorie-label">每日热量盈余</div>
            <div class="calorie-hint">摄入 &gt; 消耗</div>
          </div>
          <div class="calorie-box" v-else>
            <div class="calorie-value">0</div>
            <div class="calorie-label">热量缺口</div>
            <div class="calorie-hint">维持平衡</div>
          </div>
        </el-col>        <el-col :xs="12" :sm="6">
          <div class="calorie-box primary">
            <div class="calorie-value">{{ calorie.suggestedIntake || '--' }}</div>
            <div class="calorie-label">建议每日摄入</div>
            <div class="calorie-hint">目标 kcal</div>
          </div>
        </el-col>
      </el-row>

      <!-- 缺口推导过程 -->
      <div v-if="calorie.steps && calorie.steps.length" class="derive-section">
        <div class="derive-title">📐 热量缺口是怎么算出来的</div>
        <div class="derive-steps">
          <div v-for="s in calorie.steps" :key="s.step" class="derive-step">
            <div class="step-no">{{ s.step }}</div>
            <div class="step-body">
              <div class="step-head">
                <span class="step-title">{{ s.title }}</span>
                <span class="step-result">{{ s.result }}</span>
              </div>
              <div class="step-formula">{{ s.formula }}</div>
              <div class="step-desc">{{ s.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 不同节奏方案对比 -->
      <div v-if="calorie.alternatives && calorie.alternatives.length" class="alt-section">
        <div class="derive-title">⚖️ 不同减重节奏对比</div>
        <el-table :data="calorie.alternatives" size="small" border>
          <el-table-column prop="name" label="节奏" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.recommended" type="success" size="small">推荐</el-tag>
              <span v-else>{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column label="每周减重" width="110">
            <template #default="{ row }">
              {{ row.weeklyLoss }} kg
              <el-tooltip v-if="row.bmrProtected" content="已保护，摄入不低于基础代谢">
                <el-tag type="warning" size="small" style="margin-left: 4px">保护</el-tag>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="每日缺口" width="110">
            <template #default="{ row }">
              <span style="color: #f56c6c">{{ row.dailyDeficit }} kcal</span>
            </template>
          </el-table-column>
          <el-table-column label="建议摄入" width="110">
            <template #default="{ row }">{{ row.suggestedIntake }} kcal</template>
          </el-table-column>
          <el-table-column label="预计耗时">
            <template #default="{ row }">{{ row.days }} 天</template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 缺口可视化 -->
      <div class="deficit-visual" v-if="calorie.profileComplete">
        <div class="visual-title">
          <span>每日热量收支</span>
          <span class="visual-tip">
            消耗 {{ calorie.tdee }} kcal − 摄入 {{ calorie.suggestedIntake }} kcal =
            <b>{{ Math.abs(calorie.dailyDeficit || 0) }} kcal 缺口</b>
          </span>
        </div>
        <div class="bar-container">
          <div class="bar-item">
            <div class="bar-label">摄入</div>
            <div class="bar-track">
              <div class="bar-fill intake" :style="{ width: intakePercent + '%' }">
                {{ calorie.suggestedIntake }} kcal
              </div>
            </div>
          </div>
          <div class="bar-item">
            <div class="bar-label">消耗</div>
            <div class="bar-track">
              <div class="bar-fill burn" :style="{ width: '100%' }">
                {{ calorie.tdee }} kcal
              </div>
            </div>
          </div>
          <div class="bar-gap" v-if="calorie.dailyDeficit > 0"
               :style="{ width: (100 - intakePercent) + '%' }">
            <span>缺口 {{ calorie.dailyDeficit }} kcal</span>
          </div>
        </div>
      </div>

      <!-- 预测信息 -->
      <el-descriptions :column="descColumn" border class="mt-16" v-if="calorie.profileComplete">
        <el-descriptions-item label="当前体重">
          {{ calorie.currentWeight || '--' }} kg
        </el-descriptions-item>
        <el-descriptions-item label="目标体重">
          {{ calorie.targetWeight != null ? calorie.targetWeight + ' kg' : '未设置' }}
        </el-descriptions-item>
        <el-descriptions-item label="需减重" v-if="calorie.goalType === 'loss'">
          {{ calorie.weightToLose }} kg
        </el-descriptions-item>
        <el-descriptions-item label="需增重" v-else-if="calorie.goalType === 'gain'">
          {{ calorie.weightToGain }} kg
        </el-descriptions-item>
        <el-descriptions-item label="每周变化" v-if="calorie.weeklyLoss">
          {{ calorie.weeklyLoss }} kg / 周
        </el-descriptions-item>
        <el-descriptions-item label="预计达成" v-if="calorie.estimatedDate">
          {{ calorie.estimatedDate }}（{{ calorie.estimatedDays }} 天）
        </el-descriptions-item>
        <el-descriptions-item label="预计体重" v-if="calorie.expectedWeight">
          {{ calorie.expectedWeight }} kg
        </el-descriptions-item>
      </el-descriptions>

      <!-- 安全性提示 -->
      <el-alert
        v-if="calorie.safetyText"
        :type="safetyAlertType"
        :closable="false"
        show-icon
        class="mt-16"
        :title="calorie.safetyText"
      />

      <div v-if="calorie.message" class="calorie-message">{{ calorie.message }}</div>
    </el-card>

    <!-- 当前计划 -->
    <el-card class="plan-card mt-16" shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>🎯 当前计划</span>
          <div>
            <el-button size="small" @click="showChangeDialog">
              <el-icon><Refresh /></el-icon>
              更换计划
            </el-button>
            <el-button size="small" type="primary" @click="showEditDialog" v-if="plan">
              <el-icon><Edit /></el-icon>
              调整目标
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="plan" class="plan-detail">
        <div class="plan-title-row">
          <h3>{{ plan.planName }}</h3>
          <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
        </div>

        <!-- 进度条 -->
        <div class="progress-section">
          <div class="progress-header">
            <span>计划进度</span>
            <span class="progress-percent">{{ progress.percent || 0 }}%</span>
          </div>
          <el-progress
            :percentage="progress.percent || 0"
            :stroke-width="20"
            :color="progressColors"
            :format="() => encourageText"
          />
          <div class="progress-info">
            <span>已进行 {{ progress.passedDays }} 天</span>
            <span>剩余 {{ progress.remainingDays }} 天</span>
            <span>共 {{ progress.totalDays }} 天</span>
          </div>
        </div>

        <!-- 鼓励话语 -->
        <div class="encourage-box">
          <span class="encourage-emoji">{{ encourageEmoji }}</span>
          <span class="encourage-text">{{ encourageText }}</span>
        </div>

        <el-descriptions :column="descColumn" border class="mt-16">
          <el-descriptions-item label="目标体重">
            {{ plan.targetWeight != null ? plan.targetWeight + ' kg' : '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="每日目标摄入">
            {{ plan.targetCalorie }} kcal
          </el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ plan.startDate }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ plan.endDate }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="plan.remark" class="plan-remark">💡 {{ plan.remark }}</div>
      </div>

      <el-empty v-else description="还没有计划，点击下方按钮创建一个">
        <el-button type="primary" @click="showChangeDialog">创建计划</el-button>
      </el-empty>
    </el-card>

    <!-- 今日实际 vs 目标 -->
    <el-card class="mt-16" shadow="never">
      <template #header><span>📊 今日执行情况</span></template>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="8">
          <div class="today-item">
            <div class="today-label">已摄入</div>
            <div class="today-value intake">{{ today.intake }} kcal</div>
            <el-progress
              :percentage="intakeProgress"
              :stroke-width="10"
              :color="intakeProgress > 100 ? '#f56c6c' : '#e6a23c'"
              :show-text="false"
            />
            <div class="today-sub">目标 {{ today.target }} kcal</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="today-item">
            <div class="today-label">已消耗</div>
            <div class="today-value burn">{{ today.burn }} kcal</div>
            <el-progress
              :percentage="burnProgress"
              :stroke-width="10"
              color="#67c23a"
              :show-text="false"
            />
            <div class="today-sub">通过运动消耗</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="today-item">
            <div class="today-label">剩余可摄入</div>
            <div class="today-value" :class="today.remaining >= 0 ? 'success' : 'danger'">
              {{ today.remaining }} kcal
            </div>
            <el-progress
              :percentage="remainingProgress"
              :stroke-width="10"
              :color="today.remaining >= 0 ? '#409eff' : '#f56c6c'"
              :show-text="false"
            />
            <div class="today-sub">
              {{ today.remaining >= 0 ? '还有余量，继续保持' : '已超标，注意控制' }}
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 历史计划 -->
    <el-card class="mt-16" shadow="never" v-if="history.length">
      <template #header><span>📚 历史计划</span></template>
      <el-table :data="history" stripe>
        <el-table-column prop="planName" label="计划名称" />
        <el-table-column prop="targetWeight" label="目标体重" width="110">
          <template #default="{ row }">
            {{ row.targetWeight != null ? row.targetWeight + ' kg' : '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="targetCalorie" label="目标摄入" width="110">
          <template #default="{ row }">{{ row.targetCalorie }} kcal</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始" width="120" />
        <el-table-column prop="endDate" label="结束" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagTypeOf(row.status)" size="small">
              {{ statusTextOf(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 选择计划类型 -->
    <el-dialog v-model="changeDialogVisible" title="选择计划类型" width="560px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
        title="已有进行中的计划时会直接沿用，不会重复创建"
      />
      <div class="type-list">
        <div
          v-for="t in planTypes"
          :key="t.type"
          class="type-item"
          :class="{ active: selectedType === t.type }"
          @click="selectedType = t.type"
        >
          <div class="type-icon">{{ t.icon }}</div>
          <div class="type-info">
            <div class="type-name">{{ t.name }}</div>
            <div class="type-desc">{{ t.description }}</div>
          </div>
          <el-icon v-if="selectedType === t.type" class="type-check"><Check /></el-icon>
        </div>
      </div>
      <template #footer>
        <el-button @click="changeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangeType" :loading="saving">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 调整目标 -->
    <el-dialog v-model="editDialogVisible" title="调整目标" width="520px">
      <el-form label-width="110px">
        <el-form-item label="目标体重">
          <el-input-number
            v-model="editForm.targetWeight"
            :min="30"
            :max="200"
            :step="0.5"
            @change="fetchPreview"
          />
          <span class="unit">kg</span>
        </el-form-item>
        <el-form-item label="每日热量缺口">
          <el-input-number
            v-model="editForm.dailyDeficit"
            :min="200"
            :max="1000"
            :step="50"
            @change="fetchPreview"
          />
          <span class="unit">kcal</span>
          <div class="form-hint">缺口越大减得越快，但建议不超过 1000 kcal</div>
        </el-form-item>

        <!-- 实时预览 -->
        <el-alert
          v-if="preview"
          :type="previewAlertType"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        >
          <template #title>
            <div style="line-height: 1.8">
              <div>
                需减重 <b>{{ preview.weightToLose }} kg</b>，
                共需消耗 <b>{{ preview.totalDeficit }} kcal</b>
              </div>
              <div>
                按每日缺口 <b>{{ preview.dailyDeficit }} kcal</b>，
                预计 <b>{{ preview.days }} 天</b> 达成（{{ preview.endDate }}）
                <span v-if="preview.bmrProtected" style="color: #e6a23c">
                  · 已从 {{ preview.requestedDeficit }} kcal 调整
                </span>
              </div>
              <div>
                建议每日摄入 <b>{{ preview.suggestedIntake }} kcal</b>
                <span v-if="preview.bmrProtected" style="color: #e6a23c">（已保护，不低于基础代谢）</span>
              </div>
              <div style="margin-top: 4px">{{ preview.safetyText }}</div>
            </div>
          </template>
        </el-alert>

        <el-form-item label="计划结束日期">
          <el-date-picker
            v-model="editForm.endDate"
            type="date"
            placeholder="留空则按缺口自动推算"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Edit, Check } from '@element-plus/icons-vue'
import {
  getCurrentPlan, getOrCreatePlan, getPlanList,
  getPlanTypes, updatePlan, createPlan, getDeficitPreview,
} from '@/api/plan'
import { getTodayStatistics } from '@/api/statistics'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const plan = ref(null)
const calorie = ref({})
const progress = ref({})
const history = ref([])
const planTypes = ref([])
const today = ref({ intake: 0, burn: 0, target: 2000, remaining: 0 })

const changeDialogVisible = ref(false)
const editDialogVisible = ref(false)
const selectedType = ref('loss')
const editForm = ref({ targetWeight: null, dailyDeficit: 500, endDate: '' })
const preview = ref(null)

// 预览提示类型
const previewAlertType = computed(() => {
  if (!preview.value) return 'info'
  const map = { aggressive: 'warning', healthy: 'success', gentle: 'info' }
  return map[preview.value.safety] || 'info'
})

// 响应式列数
const descColumn = computed(() => (window.innerWidth < 768 ? 1 : 2))

// 摄入占比（用于可视化条）
const intakePercent = computed(() => {
  const tdee = calorie.value.tdee || 0
  const intake = calorie.value.suggestedIntake || 0
  if (!tdee) return 0
  return Math.min(100, Math.round(intake * 100 / tdee))
})

const intakeProgress = computed(() => {
  if (!today.value.target) return 0
  return Math.min(100, Math.round(today.value.intake * 100 / today.value.target))
})

const burnProgress = computed(() => {
  // 以 500 kcal 运动消耗为满格参考
  return Math.min(100, Math.round(today.value.burn * 100 / 500))
})

const remainingProgress = computed(() => {
  if (!today.value.target) return 0
  const r = today.value.remaining
  if (r <= 0) return 100
  return Math.round(r * 100 / today.value.target)
})

// 目标类型文本
const goalText = computed(() => {
  const map = { loss: '减脂', gain: '增肌', shape: '塑形', maintain: '维持' }
  return map[calorie.value.goalType] || '分析中'
})

const goalTagType = computed(() => {
  const map = { loss: 'danger', gain: 'success', shape: 'warning', maintain: 'info' }
  return map[calorie.value.goalType] || 'info'
})

const safetyAlertType = computed(() => {
  const map = { aggressive: 'warning', healthy: 'success', gentle: 'info' }
  return map[calorie.value.safety] || 'info'
})

const statusText = computed(() => statusTextOf(plan.value?.status))
const statusTagType = computed(() => statusTagTypeOf(plan.value?.status))

const statusTextOf = (s) => ({ 0: '已取消', 1: '进行中', 2: '已完成' }[s] || '未知')
const statusTagTypeOf = (s) => ({ 0: 'info', 1: 'success', 2: 'warning' }[s] || 'info')

// 进度条颜色分段
const progressColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#409eff', percentage: 60 },
  { color: '#67c23a', percentage: 80 },
  { color: '#67c23a', percentage: 100 },
]

// 根据进度变化的鼓励话语
const encourageText = computed(() => {
  const p = progress.value.percent || 0
  if (p === 0) return '准备出发！'
  if (p < 20) return '万事开头难，你已经迈出第一步！'
  if (p < 40) return '渐入佳境，继续保持这个节奏！'
  if (p < 60) return '已经过半啦，你的坚持看得见！'
  if (p < 80) return '胜利在望，别松懈，你可以的！'
  if (p < 100) return '就快到了，最后冲刺！'
  return '太棒了，计划圆满完成！🎉'
})

const encourageEmoji = computed(() => {
  const p = progress.value.percent || 0
  if (p === 0) return '🚀'
  if (p < 20) return '💪'
  if (p < 40) return '🔥'
  if (p < 60) return '⭐'
  if (p < 80) return '🎯'
  if (p < 100) return '🏃'
  return '🏆'
})

// 加载当前计划
const fetchCurrent = async () => {
  loading.value = true
  try {
    const res = await getCurrentPlan()
    if (res.code === 200 && res.data) {
      plan.value = res.data.plan
      calorie.value = res.data.calorie || {}
      progress.value = res.data.progress || {}
      if (plan.value?.planType) {
        selectedType.value = plan.value.planType
      }
    }
  } catch (error) {
    console.error('加载计划失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchToday = async () => {
  try {
    const res = await getTodayStatistics()
    if (res.code === 200 && res.data) {
      today.value = {
        intake: res.data.todayIntake || 0,
        burn: res.data.todayBurn || 0,
        target: res.data.targetCalorie || 2000,
        remaining: res.data.remaining || 0,
      }
    }
  } catch (error) {
    console.error('获取今日统计失败:', error)
  }
}

const fetchTypes = async () => {
  try {
    const res = await getPlanTypes()
    if (res.code === 200) {
      planTypes.value = res.data || []
    }
  } catch (error) {
    console.error('获取计划类型失败:', error)
  }
}

const fetchHistory = async () => {
  try {
    const res = await getPlanList()
    if (res.code === 200) {
      history.value = (res.data || []).filter((p) => p.status !== 1)
    }
  } catch (error) {
    console.error('获取历史计划失败:', error)
  }
}

const showChangeDialog = () => {
  selectedType.value = plan.value?.planType || 'loss'
  changeDialogVisible.value = true
}

const handleChangeType = async () => {
  saving.value = true
  try {
    // 若当前已有计划且类型相同，走幂等接口（不会重复创建）
    const sameType = plan.value && plan.value.planType === selectedType.value
    const res = sameType
      ? await getOrCreatePlan(selectedType.value)
      : await createPlan(selectedType.value)

    if (res.code === 200) {
      ElMessage.success(sameType ? '已沿用当前计划' : '计划已更新')
      changeDialogVisible.value = false
      await fetchCurrent()
      await fetchHistory()
      await fetchToday()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

const showEditDialog = () => {
  if (!plan.value) return
  editForm.value = {
    targetWeight: plan.value.targetWeight,
    // 缺口：计划存的是正数缺口，增肌计划为负
    dailyDeficit: plan.value.dailyDeficit && plan.value.dailyDeficit > 0
      ? plan.value.dailyDeficit
      : (calorie.value.dailyDeficit || 500),
    endDate: plan.value.endDate,
  }
  preview.value = null
  editDialogVisible.value = true
  fetchPreview()
}

/**
 * 实时预览：按当前目标体重与缺口反推所需天数
 */
const fetchPreview = async () => {
  if (!editForm.value.targetWeight || !editForm.value.dailyDeficit) {
    preview.value = null
    return
  }
  try {
    const res = await getDeficitPreview(
      editForm.value.targetWeight,
      editForm.value.dailyDeficit,
    )
    if (res.code === 200 && res.data) {
      preview.value = res.data.weightToLose ? res.data : null
    }
  } catch (error) {
    console.error('预览失败:', error)
  }
}

const handleSaveEdit = async () => {
  if (!plan.value) return
  saving.value = true
  try {
    // 未指定结束日期时，按缺口自动推算
    let endDate = editForm.value.endDate
    if (!endDate && preview.value?.days) {
      const d = new Date()
      d.setDate(d.getDate() + preview.value.days)
      endDate = d.toISOString().slice(0, 10)
    }

    const res = await updatePlan(plan.value.id, {
      targetWeight: editForm.value.targetWeight,
      endDate,
      targetCalorie: preview.value?.suggestedIntake || plan.value.targetCalorie,
    })
    if (res.code === 200) {
      ElMessage.success('已更新，热量缺口已重新计算')
      editDialogVisible.value = false
      await fetchCurrent()
      await fetchToday()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const goProfile = () => router.push('/profile')

onMounted(() => {
  fetchCurrent()
  fetchToday()
  fetchTypes()
  fetchHistory()
})
</script>

<style scoped>
.plan-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  color: #303133;
  margin-bottom: 6px;
}

.subtitle {
  color: #909399;
  font-size: 14px;
}

.mb-16 { margin-bottom: 16px; }
.mt-16 { margin-top: 16px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* ---------- 热量缺口卡片 ---------- */
.calorie-row {
  margin-bottom: 20px;
}

.calorie-box {
  text-align: center;
  padding: 18px 10px;
  background: #f5f7fa;
  border-radius: 10px;
  border: 2px solid transparent;
}

.calorie-box.highlight {
  background: #ecf5ff;
  border-color: #409eff;
}

.calorie-box.danger {
  background: #fef0f0;
  border-color: #f56c6c;
}

.calorie-box.success {
  background: #f0f9eb;
  border-color: #67c23a;
}

.calorie-box.primary {
  background: #f0f9eb;
  border-color: #67c23a;
}

.calorie-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.calorie-box.danger .calorie-value { color: #f56c6c; }
.calorie-box.success .calorie-value { color: #67c23a; }
.calorie-box.primary .calorie-value { color: #67c23a; }
.calorie-box.highlight .calorie-value { color: #409eff; }

.calorie-label {
  font-size: 13px;
  color: #606266;
  margin-top: 6px;
}

.calorie-hint {
  font-size: 11px;
  color: #a8abb2;
  margin-top: 3px;
}

/* ---------- 推导过程 ---------- */
.derive-section {
  margin-bottom: 18px;
}

.derive-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.derive-steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
}

.derive-step {
  display: flex;
  gap: 10px;
  padding: 14px;
  background: #f5f7fa;
  border-radius: 10px;
  border-left: 3px solid #409eff;
}

.step-no {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.step-body {
  flex: 1;
  min-width: 0;
}

.step-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
}

.step-title {
  font-size: 13px;
  color: #606266;
}

.step-result {
  font-size: 15px;
  font-weight: 700;
  color: #409eff;
  white-space: nowrap;
}

.step-formula {
  font-size: 12px;
  color: #303133;
  font-family: Consolas, Monaco, monospace;
  background: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  margin-bottom: 5px;
  word-break: break-all;
}

.step-desc {
  font-size: 11px;
  color: #a8abb2;
  line-height: 1.5;
}

.alt-section {
  margin-bottom: 18px;
}

/* ---------- 可视化条 ---------- */
.deficit-visual {
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
  margin-bottom: 8px;
}

.visual-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  font-size: 14px;
  color: #606266;
  flex-wrap: wrap;
  gap: 8px;
}

.visual-tip b {
  color: #f56c6c;
}

.bar-container {
  position: relative;
}

.bar-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.bar-label {
  width: 44px;
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.bar-track {
  flex: 1;
  height: 28px;
  background: #ebeef5;
  border-radius: 14px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 12px;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border-radius: 14px;
  transition: width 0.6s ease;
  min-width: 60px;
}

.bar-fill.intake {
  background: linear-gradient(90deg, #e6a23c, #f0b355);
}

.bar-fill.burn {
  background: linear-gradient(90deg, #409eff, #66b1ff);
}

.bar-gap {
  margin-left: 44px;
  height: 22px;
  background: repeating-linear-gradient(
    45deg,
    #fef0f0,
    #fef0f0 8px,
    #fde2e2 8px,
    #fde2e2 16px
  );
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #f56c6c;
  font-weight: 600;
  transition: width 0.6s ease;
}

.calorie-message {
  margin-top: 12px;
  padding: 10px 14px;
  background: #f4f4f5;
  border-radius: 8px;
  color: #909399;
  font-size: 13px;
}

/* ---------- 计划详情 ---------- */
.plan-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.plan-title-row h3 {
  font-size: 20px;
  color: #303133;
}

.progress-section {
  margin-bottom: 18px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
}

.progress-percent {
  font-weight: 700;
  color: #409eff;
  font-size: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.encourage-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f0f9eb 0%, #ecf5ff 100%);
  border-radius: 10px;
  border-left: 4px solid #67c23a;
}

.encourage-emoji {
  font-size: 26px;
}

.encourage-text {
  font-size: 15px;
  font-weight: 600;
  color: #529b2e;
}

.plan-remark {
  margin-top: 14px;
  padding: 10px 14px;
  background: #fdf6ec;
  border-radius: 8px;
  color: #b88230;
  font-size: 13px;
  line-height: 1.6;
}

/* ---------- 今日执行 ---------- */
.today-item {
  padding: 12px 4px;
}

.today-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.today-value {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}

.today-value.intake { color: #e6a23c; }
.today-value.burn { color: #67c23a; }
.today-value.success { color: #409eff; }
.today-value.danger { color: #f56c6c; }

.today-sub {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 6px;
}

/* ---------- 类型选择 ---------- */
.type-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 2px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.type-item:hover {
  border-color: #c6e2ff;
  background: #fafcff;
}

.type-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.type-icon {
  font-size: 28px;
}

.type-info {
  flex: 1;
}

.type-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.type-desc {
  font-size: 12px;
  color: #909399;
}

.type-check {
  color: #409eff;
  font-size: 20px;
}

.unit {
  margin-left: 8px;
  color: #909399;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .calorie-box {
    margin-bottom: 12px;
  }
  .visual-title {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
