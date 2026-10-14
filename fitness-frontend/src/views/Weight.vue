<template>
  <div class="weight-page">
    <div class="page-header">
      <h2>⚖️ 每日体重记录</h2>
      <p class="subtitle">每天记录一次，见证自己的变化</p>
    </div>

    <!-- 今日记录 -->
    <el-card class="today-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>📝 记录今日体重</span>
          <el-tag v-if="todayRecorded" type="success" size="small">今日已记录</el-tag>
          <el-tag v-else type="warning" size="small">今日未记录</el-tag>
        </div>
      </template>

      <div class="record-form">
        <el-input-number
          v-model="form.weight"
          :min="20"
          :max="300"
          :step="0.1"
          :precision="1"
          size="large"
          style="width: 180px"
        />
        <span class="unit">kg</span>
        <el-date-picker
          v-model="form.recordDate"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          :disabled-date="disableFuture"
          style="width: 160px"
        />
        <el-input
          v-model="form.remark"
          placeholder="备注（可选）"
          style="width: 200px"
          maxlength="50"
        />
        <el-button type="primary" size="large" @click="handleSave" :loading="saving">
          <el-icon><Check /></el-icon>
          {{ todayRecorded ? '更新记录' : '记录体重' }}
        </el-button>
      </div>

      <!-- 快捷微调 -->
      <div class="quick-adjust">
        <span class="quick-label">快捷：</span>
        <el-button size="small" @click="adjust(-0.5)">-0.5</el-button>
        <el-button size="small" @click="adjust(-0.1)">-0.1</el-button>
        <el-button size="small" @click="adjust(0.1)">+0.1</el-button>
        <el-button size="small" @click="adjust(0.5)">+0.5</el-button>
        <el-button size="small" type="info" plain @click="useLastWeight">
          用上次体重
        </el-button>
      </div>
    </el-card>

    <!-- 变化概览 -->
    <el-row :gutter="16" class="overview-row" v-if="stats.hasData">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">当前体重</div>
          <div class="stat-value">{{ stats.currentWeight }} <small>kg</small></div>
          <div class="stat-sub">BMI {{ stats.bmi || '--' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">计划内变化</div>
          <div class="stat-value" :class="planChangeClass">
            {{ stats.planChange > 0 ? '+' : '' }}{{ stats.planChange }} <small>kg</small>
          </div>
          <div class="stat-sub">自 {{ stats.baselineDate || '--' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">距目标</div>
          <div class="stat-value" :class="stats.remaining <= 0 ? 'success' : ''">
            <template v-if="stats.targetWeight != null">
              {{ stats.remaining > 0 ? stats.remaining : '已达成' }}
              <small v-if="stats.remaining > 0">kg</small>
            </template>
            <template v-else>--</template>
          </div>
          <div class="stat-sub">
            目标 {{ stats.targetWeight != null ? stats.targetWeight + ' kg' : '未设置' }}
            <el-tag v-if="stats.targetSource === 'plan'" size="small" type="success" style="margin-left: 4px">
              来自计划
            </el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">记录天数</div>
          <div class="stat-value">{{ stats.recordCount }} <small>天</small></div>
          <div class="stat-sub">近 {{ stats.days }} 天</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 当前训练计划 -->
    <el-card class="plan-card" shadow="never" v-if="stats.plan">
      <template #header>
        <div class="card-header">
          <span>📋 关联的训练计划</span>
          <el-tag size="small" type="primary">{{ stats.plan.planName }}</el-tag>
        </div>
      </template>
      <div class="plan-info">
        <div class="plan-item">
          <span class="plan-label">计划开始</span>
          <span class="plan-value">{{ stats.plan.startDate || '--' }}</span>
        </div>
        <div class="plan-item">
          <span class="plan-label">计划结束</span>
          <span class="plan-value">{{ stats.plan.endDate || '--' }}</span>
        </div>
        <div class="plan-item">
          <span class="plan-label">目标体重</span>
          <span class="plan-value highlight">
            {{ stats.plan.targetWeight != null ? stats.plan.targetWeight + ' kg' : '未设置' }}
          </span>
        </div>
        <div class="plan-item">
          <span class="plan-label">每日缺口</span>
          <span class="plan-value danger">
            {{ stats.plan.dailyDeficit ? stats.plan.dailyDeficit + ' kcal' : '--' }}
          </span>
        </div>
        <div class="plan-item">
          <span class="plan-label">目标摄入</span>
          <span class="plan-value">{{ stats.plan.targetCalorie || '--' }} kcal</span>
        </div>
      </div>
    </el-card>

    <!-- 达标进度 -->
    <el-card class="progress-card" shadow="never" v-if="stats.hasData && stats.targetWeight != null">
      <template #header>
        <div class="card-header">
          <span>🎯 目标达成进度</span>
          <div>
            <el-tag v-if="stats.targetSource === 'plan'" size="small" type="success">
              依据训练计划
            </el-tag>
            <el-tag v-if="stats.reached" type="success" size="small" style="margin-left: 6px">
              已达成目标
            </el-tag>
          </div>
        </div>
      </template>
      <el-progress
        :percentage="stats.progress || 0"
        :stroke-width="24"
        :color="progressColors"
        :format="(p) => `${p}%`"
      />
      <div class="progress-detail">
        <span>起点 {{ stats.baselineWeight }} kg<small v-if="stats.baselineDate">（{{ stats.baselineDate }}）</small></span>
        <span class="arrow">→</span>
        <span class="current">当前 {{ stats.currentWeight }} kg</span>
        <span class="arrow">→</span>
        <span class="target">目标 {{ stats.targetWeight }} kg</span>
      </div>
      <div class="progress-amount" v-if="stats.totalNeed > 0">
        已减 <b class="done">{{ stats.doneAmount }} kg</b> /
        共需 <b>{{ stats.totalNeed }} kg</b>，
        还差 <b class="remain">{{ stats.remaining > 0 ? stats.remaining : 0 }} kg</b>
      </div>
      <div class="progress-encourage">
        <span class="emoji">{{ progressEmoji }}</span>
        <span class="text">{{ progressText }}</span>
      </div>
    </el-card>

    <!-- 趋势图 -->
    <el-card class="chart-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>📈 体重变化趋势</span>
          <el-radio-group v-model="days" size="small" @change="fetchAll">
            <el-radio-button :value="7">7天</el-radio-button>
            <el-radio-button :value="30">30天</el-radio-button>
            <el-radio-button :value="90">90天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div v-show="stats.hasData" ref="chartRef" style="width: 100%; height: 340px"></div>
      <el-empty v-if="!stats.hasData" description="还没有体重记录，从今天开始吧" />
    </el-card>

    <!-- 记录明细 -->
    <el-card class="table-card" shadow="never" v-if="records.length">
      <template #header>
        <span>📋 记录明细</span>
      </template>
      <el-table :data="tableData" stripe max-height="400">
        <el-table-column prop="recordDate" label="日期" width="130" />
        <el-table-column prop="weight" label="体重 (kg)" width="120">
          <template #default="{ row }">
            <span style="font-weight: 600">{{ row.weight }}</span>
          </template>
        </el-table-column>
        <el-table-column label="较前次" width="120">
          <template #default="{ row }">
            <span v-if="row.diff === null" style="color: #c0c4cc">--</span>
            <span v-else :style="{ color: row.diff > 0 ? '#f56c6c' : (row.diff < 0 ? '#67c23a' : '#909399') }">
              {{ row.diff > 0 ? '+' : '' }}{{ row.diff }} kg
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="bmi" label="BMI" width="100">
          <template #default="{ row }">{{ row.bmi || '--' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注">
          <template #default="{ row }">
            <span style="color: #909399">{{ row.remark || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  addWeight, getWeightList, getWeightStatistics,
  deleteWeight, getTodayWeight,
} from '@/api/weight'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const days = ref(30)
const saving = ref(false)
const records = ref([])
const stats = ref({ hasData: false })
const todayRecorded = ref(false)

const form = ref({
  weight: null,
  recordDate: new Date().toISOString().slice(0, 10),
  remark: '',
})

const chartRef = ref(null)
let chart = null

// 表格数据：倒序 + 计算与前次差值
const tableData = computed(() => {
  const list = records.value.slice().reverse()  // 最新在前
  return list.map((item, index) => {
    // 与「更早的一条」比较
    const prev = list[index + 1]
    const diff = prev ? Math.round((item.weight - prev.weight) * 10) / 10 : null
    return { ...item, diff }
  })
})

const planChangeClass = computed(() => {
  const c = stats.value.planChange
  if (c === undefined || c === null) return ''
  if (c < 0) return 'success'   // 减重
  if (c > 0) return 'danger'
  return ''
})

const progressColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#409eff', percentage: 60 },
  { color: '#67c23a', percentage: 80 },
  { color: '#529b2e', percentage: 100 },
]

// 进度鼓励话语
const progressEmoji = computed(() => {
  if (stats.value.reached) return '🏆'
  const p = stats.value.progress || 0
  if (p < 20) return '🌱'
  if (p < 40) return '💪'
  if (p < 60) return '🔥'
  if (p < 80) return '⭐'
  return '🏃'
})

const progressText = computed(() => {
  if (stats.value.reached) {
    return '恭喜达成目标体重！记得切换到「维持」模式保持成果 🎉'
  }
  const p = stats.value.progress || 0
  if (p < 20) return '万事开头难，坚持记录就是最好的开始！'
  if (p < 40) return '已经看到变化了，继续保持这个节奏！'
  if (p < 60) return '过半啦，你的努力正在变成数字上的成果！'
  if (p < 80) return '胜利在望，别在最后阶段松懈哦！'
  return '就快到了，最后冲刺一下！'
})

const disableFuture = (date) => date.getTime() > Date.now()

const adjust = (delta) => {
  const base = form.value.weight || stats.value.currentWeight || 70
  form.value.weight = Math.round((base + delta) * 10) / 10
}

const useLastWeight = () => {
  if (stats.value.currentWeight) {
    form.value.weight = stats.value.currentWeight
  } else {
    ElMessage.info('还没有历史记录')
  }
}

// 保存
const handleSave = async () => {
  if (!form.value.weight) {
    ElMessage.warning('请填写体重')
    return
  }
  saving.value = true
  try {
    const res = await addWeight({
      weight: form.value.weight,
      recordDate: form.value.recordDate,
      remark: form.value.remark,
    })
    if (res.code === 200) {
      ElMessage.success(res.data?.message || '记录成功')
      form.value.remark = ''
      await fetchAll()
      // 同步更新本地用户信息中的体重
      userStore.setUserInfo({ ...userStore.userInfo, weight: form.value.weight })
    } else {
      ElMessage.error(res.msg || '记录失败')
    }
  } catch (error) {
    ElMessage.error('记录失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除 ${row.recordDate} 的体重记录吗？`, '提示', {
      type: 'warning',
    })
    const res = await deleteWeight(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      await fetchAll()
    }
  } catch {
    // 取消
  }
}

const fetchToday = async () => {
  try {
    const res = await getTodayWeight()
    if (res.code === 200 && res.data) {
      todayRecorded.value = res.data.recorded
      if (res.data.recorded && res.data.record) {
        form.value.weight = res.data.record.weight
        form.value.remark = res.data.record.remark || ''
      }
    }
  } catch (error) {
    console.error('获取今日体重失败:', error)
  }
}

const fetchList = async () => {
  try {
    const res = await getWeightList(days.value)
    if (res.code === 200) {
      records.value = res.data || []
    }
  } catch (error) {
    console.error('获取体重列表失败:', error)
  }
}

const fetchStats = async () => {
  try {
    const res = await getWeightStatistics(days.value)
    if (res.code === 200 && res.data) {
      stats.value = res.data
      // 首次进入时用最新体重作为默认输入
      if (!form.value.weight && res.data.currentWeight) {
        form.value.weight = res.data.currentWeight
      }
    }
  } catch (error) {
    console.error('获取体重统计失败:', error)
  }
}

const renderChart = () => {
  if (!chartRef.value || !records.value.length) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const dates = records.value.map((r) => r.recordDate.slice(5))
  const weights = records.value.map((r) => r.weight)
  const target = stats.value.targetWeight

  const series = [
    {
      name: '体重',
      data: weights,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      itemStyle: { color: '#409eff' },
      lineStyle: { width: 3 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.35)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.03)' },
        ]),
      },
      markPoint: {
        data: [
          { type: 'min', name: '最低', itemStyle: { color: '#67c23a' } },
          { type: 'max', name: '最高', itemStyle: { color: '#f56c6c' } },
        ],
      },
    },
  ]

  // 目标线
  if (target != null) {
    series.push({
      name: '目标',
      data: records.value.map(() => target),
      type: 'line',
      symbol: 'none',
      lineStyle: { color: '#67c23a', type: 'dashed', width: 2 },
    })
  }

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = params[0]
        return `${p.axisValue}<br/>体重：<b>${p.value} kg</b>`
      },
    },
    legend: { data: target != null ? ['体重', '目标'] : ['体重'], top: 0 },
    grid: { left: 50, right: 30, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: {
      type: 'value',
      name: 'kg',
      scale: true,
      min: (value) => Math.floor(value.min - 1),
      max: (value) => Math.ceil(value.max + 1),
    },
    series,
  }, true)
}

const fetchAll = async () => {
  await Promise.all([fetchList(), fetchStats(), fetchToday()])
  await nextTick()
  renderChart()
}

const handleResize = () => chart?.resize()

onMounted(() => {
  fetchAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.weight-page {
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* ---------- 记录表单 ---------- */
.today-card {
  margin-bottom: 16px;
}

.record-form {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.unit {
  color: #909399;
  margin-left: -6px;
}

.quick-adjust {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.quick-label {
  font-size: 13px;
  color: #909399;
}

/* ---------- 概览 ---------- */
.overview-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  padding: 16px 8px;
  margin-bottom: 12px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-value small {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.stat-value.success { color: #67c23a; }
.stat-value.danger { color: #f56c6c; }

.stat-sub {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 6px;
}

/* ---------- 关联计划 ---------- */
.plan-card {
  margin-bottom: 16px;
}

.plan-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.plan-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  background: #f5f7fa;
  border-radius: 8px;
}

.plan-label {
  font-size: 12px;
  color: #909399;
}

.plan-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.plan-value.highlight { color: #67c23a; }
.plan-value.danger { color: #f56c6c; }

/* ---------- 进度 ---------- */
.progress-card {
  margin-bottom: 16px;
}

.progress-amount {
  text-align: center;
  margin-top: 10px;
  font-size: 14px;
  color: #606266;
}

.progress-amount .done { color: #67c23a; }
.progress-amount .remain { color: #f56c6c; }

.progress-detail small {
  color: #a8abb2;
  font-size: 11px;
}

.progress-detail {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  font-size: 13px;
  color: #606266;
  flex-wrap: wrap;
}

.progress-detail .arrow {
  color: #c0c4cc;
}

.progress-detail .current {
  color: #409eff;
  font-weight: 700;
}

.progress-detail .target {
  color: #67c23a;
  font-weight: 700;
}

.progress-encourage {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f0f9eb 0%, #ecf5ff 100%);
  border-radius: 10px;
  border-left: 4px solid #67c23a;
}

.progress-encourage .emoji {
  font-size: 26px;
}

.progress-encourage .text {
  font-size: 14px;
  font-weight: 600;
  color: #529b2e;
}

.chart-card,
.table-card {
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .record-form {
    flex-direction: column;
    align-items: stretch;
  }
  .record-form .el-input-number,
  .record-form .el-date-editor,
  .record-form .el-input {
    width: 100% !important;
  }
}
</style>
