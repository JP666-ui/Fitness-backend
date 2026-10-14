<template>
  <div class="statistics-page">
    <div class="page-header">
      <h2>📊 数据统计</h2>
      <p class="subtitle">以「日」为单位，记录你的每一分坚持</p>
    </div>

    <!-- 时间范围 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-radio-group v-model="days" @change="fetchData">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="14">近 14 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
        </el-radio-group>
        <el-button @click="fetchData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
      <!-- 统计起点说明 -->
      <div v-if="rangeInfo.clampedByPlan" class="range-note">
        <el-icon><InfoFilled /></el-icon>
        统计从训练计划创建日 <b>{{ rangeInfo.planStartDate }}</b> 开始，
        共 {{ rangeInfo.days }} 天（已自动收窄）
      </div>
      <div v-else-if="rangeInfo.startDate" class="range-note">
        <el-icon><InfoFilled /></el-icon>
        统计区间：{{ rangeInfo.startDate }} ~ {{ rangeInfo.endDate }}，共 {{ rangeInfo.days }} 天
      </div>
    </el-card>

    <!-- 坚持进度条：按「已用时间 / 计划总时长」计算 -->
    <el-card class="progress-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>🏆 计划时间进度</span>
          <el-tag :type="streakTagType" size="small">{{ streakLevelText }}</el-tag>
        </div>
      </template>

      <!-- 无计划时提示 -->
      <el-alert
        v-if="!planInfo.startDate"
        type="info"
        :closable="false"
        show-icon
        title="还没有训练计划，创建计划后会显示时间进度"
      />

      <template v-else>
        <div class="progress-wrapper">
          <el-progress
            :percentage="timePercent"
            :stroke-width="26"
            :color="progressColors"
            :format="() => ''"
          />
          <div class="progress-overlay">
            <span class="progress-num">{{ timePercent }}%</span>
          </div>
        </div>

        <!-- 时间轴：开始 → 今天 → 结束 -->
        <div class="timeline">
          <div class="timeline-item">
            <div class="timeline-dot start"></div>
            <div class="timeline-label">计划开始</div>
            <div class="timeline-date">{{ planInfo.startDate }}</div>
          </div>
          <div class="timeline-bar">
            <div class="timeline-fill" :style="{ width: timePercent + '%' }"></div>
            <div class="timeline-now" :style="{ left: timePercent + '%' }">
              <span class="now-badge">今天</span>
            </div>
          </div>
          <div class="timeline-item">
            <div class="timeline-dot end"></div>
            <div class="timeline-label">预期完成</div>
            <div class="timeline-date">{{ planInfo.endDate }}</div>
          </div>
        </div>

        <!-- 时间统计 -->
        <el-row :gutter="12" class="time-stats">
          <el-col :xs="8" :sm="8">
            <div class="time-item">
              <div class="time-value">{{ planInfo.passedDays || 0 }}</div>
              <div class="time-label">已进行 (天)</div>
            </div>
          </el-col>
          <el-col :xs="8" :sm="8">
            <div class="time-item">
              <div class="time-value" :class="planInfo.remainingDays <= 7 ? 'warning' : 'primary'">
                {{ planInfo.remainingDays || 0 }}
              </div>
              <div class="time-label">剩余 (天)</div>
            </div>
          </el-col>
          <el-col :xs="8" :sm="8">
            <div class="time-item">
              <div class="time-value">{{ planInfo.totalDays || 0 }}</div>
              <div class="time-label">总周期 (天)</div>
            </div>
          </el-col>
        </el-row>

        <!-- 时间 vs 目标 对比 -->
        <div class="compare-box" v-if="summary.statDays">
          <div class="compare-title">时间进度 vs 目标进度</div>
          <div class="compare-row">
            <span class="compare-label">时间已用</span>
            <div class="compare-track">
              <div class="compare-fill time" :style="{ width: timePercent + '%' }"></div>
            </div>
            <span class="compare-value">{{ timePercent }}%</span>
          </div>
          <div class="compare-row" v-if="weightProgress !== null">
            <span class="compare-label">目标达成</span>
            <div class="compare-track">
              <div class="compare-fill goal" :style="{ width: weightProgress + '%' }"></div>
            </div>
            <span class="compare-value">{{ weightProgress }}%</span>
          </div>
          <div class="compare-hint" :class="compareHintClass">
            {{ compareHint }}
          </div>
        </div>

        <!-- 鼓励话语 -->
        <div class="encourage-box">
          <span class="encourage-emoji">{{ encourageEmoji }}</span>
          <div class="encourage-content">
            <div class="encourage-text">{{ encourageText }}</div>
            <div class="encourage-sub">{{ encourageSub }}</div>
          </div>
        </div>
      </template>
    </el-card>

    <!-- 概览卡片 -->
    <el-row :gutter="16" class="overview-row">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon><Food /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.avgIntake || 0 }}</div>
            <div class="stat-label">日均摄入 (kcal)</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon><Odometer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.avgTotalBurn || 0 }}</div>
            <div class="stat-label">日均总消耗 (kcal)</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: #409eff">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.targetCalorie || 0 }}</div>
            <div class="stat-label">每日目标 (kcal)</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" :style="{ background: deficitColor }">
            <el-icon><Histogram /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ avgDeficit }}</div>
            <div class="stat-label">
              日均{{ avgDeficitValue < 0 ? '缺口' : '盈余' }} (kcal)
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-card class="chart-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>📈 每日热量趋势</span>
          <div class="legend">
            <span class="legend-item"><i style="background:#e6a23c"></i>摄入</span>
            <span class="legend-item"><i style="background:#67c23a"></i>总消耗</span>
            <span class="legend-item"><i style="background:#409eff"></i>目标</span>
          </div>
        </div>
      </template>
      <div ref="trendChartRef" style="width: 100%; height: 360px"></div>
    </el-card>

    <!-- 热量缺口图 -->
    <el-card class="chart-card" shadow="never">
      <template #header>
        <span>🔥 每日热量缺口</span>
      </template>
      <div ref="deficitChartRef" style="width: 100%; height: 300px"></div>
      <div class="chart-note">
        柱子向上表示当天有热量缺口（消耗 &gt; 摄入），有利于减脂
      </div>
    </el-card>

    <!-- 每日明细表格 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <span>📋 每日明细</span>
      </template>
      <el-table :data="daily" stripe v-loading="loading" max-height="420">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="intake" label="摄入 (kcal)" width="130">
          <template #default="{ row }">
            <span :style="{ color: row.intake > summary.targetCalorie ? '#f56c6c' : '#303133' }">
              {{ row.intake }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="totalBurn" label="总消耗 (kcal)" width="140">
          <template #default="{ row }">
            <span style="color: #67c23a">{{ row.totalBurn || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="exerciseBurn" label="运动消耗" width="110">
          <template #default="{ row }">
            <span style="color: #909399">{{ row.exerciseBurn || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="热量缺口" width="140">
          <template #default="{ row }">
            <span v-if="!row.hasRecord" style="color: #c0c4cc">--</span>
            <el-tag v-else :type="row.deficit >= 0 ? 'success' : 'warning'" size="small">
              {{ row.deficit >= 0 ? '缺口 ' + row.deficit : '盈余 ' + Math.abs(row.deficit) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="距目标" width="130">
          <template #default="{ row }">
            <span v-if="!row.hasRecord" style="color: #c0c4cc">--</span>
            <span v-else :style="{ color: row.targetDiff >= 0 ? '#67c23a' : '#f56c6c' }">
              {{ row.targetDiff >= 0 ? '剩 ' + row.targetDiff : '超 ' + Math.abs(row.targetDiff) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="达标" width="100">
          <template #default="{ row }">
            <el-tag v-if="!row.hasRecord" type="info" size="small">未记录</el-tag>
            <el-tag v-else-if="row.onTarget" type="success" size="small">✓ 达标</el-tag>
            <el-tag v-else type="danger" size="small">超标</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Food, Odometer, TrendCharts, Histogram, Refresh, InfoFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDailyStatistics } from '@/api/statistics'
import { getWeightStatistics } from '@/api/weight'

const days = ref(7)
const loading = ref(false)
const daily = ref([])
const summary = ref({})
const rangeInfo = ref({})
const planInfo = ref({})

const trendChartRef = ref(null)
const deficitChartRef = ref(null)
let trendChart = null
let deficitChart = null

// 日均缺口（正数=缺口，负数=盈余）
const avgDeficitValue = computed(() => {
  const s = summary.value
  if (!s.daysWithRecord) return 0
  // totalDeficit = 消耗 − 摄入；正数代表有缺口
  return Math.round((s.totalDeficit || 0) / s.daysWithRecord)
})

const avgDeficit = computed(() => Math.abs(avgDeficitValue.value))

const deficitColor = computed(() => (avgDeficitValue.value >= 0 ? '#67c23a' : '#e6a23c'))
const totalDeficitClass = computed(() => ((summary.value.totalDeficit || 0) >= 0 ? 'success' : 'warning'))

/**
 * 时间进度：已用天数 / 计划总天数
 * 这是「现在的时间」与「预期最终时间」的比较
 */
const timePercent = computed(() => {
  const p = planInfo.value
  if (p.timePercent !== undefined && p.timePercent !== null) {
    return p.timePercent
  }
  return 0
})

/**
 * 目标达成进度（体重维度），用于与时间进度对比
 * 需要从体重接口获取，这里先用 0 占位，由 fetchWeightProgress 填充
 */
const weightProgress = ref(null)

/**
 * 时间与目标的对比结论
 */
const compareHint = computed(() => {
  if (weightProgress.value === null) return ''
  const time = timePercent.value
  const goal = weightProgress.value
  const diff = goal - time

  if (diff >= 5) {
    return `目标进度领先时间进度 ${diff}%，节奏很好，继续保持！`
  }
  if (diff >= -5) {
    return '目标进度与时间进度基本同步，稳稳地往前走'
  }
  return `目标进度落后时间进度 ${Math.abs(diff)}%，可以适当加大缺口或增加运动`
})

const compareHintClass = computed(() => {
  if (weightProgress.value === null) return ''
  const diff = weightProgress.value - timePercent.value
  if (diff >= 5) return 'good'
  if (diff >= -5) return 'normal'
  return 'behind'
})

const streakLevelText = computed(() => {
  const p = timePercent.value
  if (p === 0) return '即将开始'
  if (p < 25) return '起步阶段'
  if (p < 50) return '稳步推进'
  if (p < 75) return '过半啦'
  if (p < 100) return '最后冲刺'
  return '计划周期结束'
})

const streakTagType = computed(() => {
  const p = timePercent.value
  if (p < 25) return 'info'
  if (p < 50) return 'primary'
  if (p < 75) return 'warning'
  return 'success'
})

// 进度条颜色分段
const progressColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#409eff', percentage: 60 },
  { color: '#67c23a', percentage: 80 },
  { color: '#529b2e', percentage: 100 },
]

// 随进度变化的鼓励话语
const encourageText = computed(() => {
  const p = timePercent.value
  if (p === 0) return '计划即将开始，做好准备！'
  if (p < 20) return '万事开头难，你已经迈出第一步！'
  if (p < 40) return '渐入佳境，保持这个节奏！'
  if (p < 60) return '时间过半，你的坚持看得见！'
  if (p < 80) return '胜利在望，别松懈！'
  if (p < 100) return '就快到了，最后冲刺！'
  return '计划周期已结束，回顾一下你的成果吧 🎉'
})

const encourageEmoji = computed(() => {
  const p = timePercent.value
  if (p === 0) return '🌱'
  if (p < 20) return '💪'
  if (p < 40) return '🔥'
  if (p < 60) return '⭐'
  if (p < 80) return '🎯'
  if (p < 100) return '🏃'
  return '🏆'
})

const encourageSub = computed(() => {
  const p = planInfo.value
  if (!p.startDate) {
    return '创建训练计划后，这里会显示时间进度'
  }
  if (p.expired) {
    return `计划周期已于 ${p.endDate} 结束，共 ${p.totalDays} 天`
  }
  return `计划从 ${p.startDate} 到 ${p.endDate}，共 ${p.totalDays} 天，还剩 ${p.remainingDays} 天`
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDailyStatistics(days.value)
    if (res.code === 200 && res.data) {
      daily.value = (res.data.daily || []).slice().reverse()
      summary.value = res.data.summary || {}
      rangeInfo.value = {
        days: res.data.days,
        requestedDays: res.data.requestedDays,
        startDate: res.data.startDate,
        endDate: res.data.endDate,
        clampedByPlan: res.data.clampedByPlan,
        planStartDate: res.data.planStartDate,
      }
      planInfo.value = res.data.plan || {}
      await nextTick()
      renderCharts(res.data.daily || [])
    } else {
      ElMessage.warning(res.msg || '获取数据失败')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取数据失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

// 渲染图表
const renderCharts = (data) => {
  if (!data.length) return

  const dates = data.map((d) => d.date.slice(5))  // MM-DD
  const intakes = data.map((d) => d.intake)
  // 总消耗 = 基础消耗 + 运动消耗
  const burns = data.map((d) => d.totalBurn || d.burn || 0)
  const target = summary.value.targetCalorie || 2000
  const targets = data.map(() => target)
  // 缺口：总消耗 − 摄入，正数代表有缺口
  const deficits = data.map((d) => d.deficit)

  // ---- 趋势图 ----
  if (trendChartRef.value) {
    if (!trendChart) {
      trendChart = echarts.init(trendChartRef.value)
    }
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 30, bottom: 40 },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: { fontSize: 11 },
      },
      yAxis: { type: 'value', name: 'kcal' },
      series: [
        {
          name: '摄入',
          data: intakes,
          type: 'bar',
          itemStyle: { color: '#e6a23c', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 26,
        },
        {
          name: '总消耗',
          data: burns,
          type: 'bar',
          itemStyle: { color: '#67c23a', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 26,
        },
        {
          name: '目标',
          data: targets,
          type: 'line',
          smooth: true,
          symbol: 'none',
          lineStyle: { color: '#409eff', type: 'dashed', width: 2 },
        },
      ],
    }, true)
  }

  // ---- 缺口图 ----
  if (deficitChartRef.value) {
    if (!deficitChart) {
      deficitChart = echarts.init(deficitChartRef.value)
    }
    deficitChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const v = params[0].value
          const label = v >= 0 ? `热量缺口 ${v} kcal` : `热量盈余 ${Math.abs(v)} kcal`
          return `${params[0].axisValue}<br/>${label}`
        },
      },
      grid: { left: 50, right: 20, top: 30, bottom: 40 },
      xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', name: 'kcal' },
      series: [
        {
          name: '热量缺口',
          type: 'bar',
          data: deficits.map((v) => ({
            value: v,
            itemStyle: { color: v >= 0 ? '#67c23a' : '#f56c6c', borderRadius: 4 },
          })),
          barMaxWidth: 30,
        },
      ],
    }, true)
  }
}

const handleResize = () => {
  trendChart?.resize()
  deficitChart?.resize()
}

/**
 * 获取体重维度的目标达成进度，用于与时间进度对比
 */
const fetchWeightProgress = async () => {
  try {
    const res = await getWeightStatistics(90)
    if (res.code === 200 && res.data && res.data.hasData) {
      weightProgress.value = res.data.progress ?? null
    } else {
      weightProgress.value = null
    }
  } catch (error) {
    console.error('获取体重进度失败:', error)
    weightProgress.value = null
  }
}

onMounted(() => {
  fetchData()
  fetchWeightProgress()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  deficitChart?.dispose()
})
</script>

<style scoped>
.statistics-page {
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

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.range-note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #f4f8ff;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.range-note b {
  color: #409eff;
}

.range-note .el-icon {
  color: #409eff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* ---------- 进度卡片 ---------- */
.progress-card {
  margin-bottom: 16px;
}

.progress-wrapper {
  position: relative;
  margin-bottom: 18px;
}

.progress-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.progress-num {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
  text-shadow: 0 0 4px rgba(255, 255, 255, 0.9);
}

/* ---------- 时间轴 ---------- */
.timeline {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 22px 0 18px;
}

.timeline-item {
  text-align: center;
  flex-shrink: 0;
  min-width: 84px;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin: 0 auto 6px;
}

.timeline-dot.start {
  background: #409eff;
}

.timeline-dot.end {
  background: #67c23a;
}

.timeline-label {
  font-size: 12px;
  color: #909399;
}

.timeline-date {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-top: 2px;
}

.timeline-bar {
  flex: 1;
  height: 8px;
  background: #ebeef5;
  border-radius: 4px;
  position: relative;
}

.timeline-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 4px;
  transition: width 0.6s ease;
}

.timeline-now {
  position: absolute;
  top: -22px;
  transform: translateX(-50%);
  transition: left 0.6s ease;
}

.now-badge {
  display: inline-block;
  padding: 1px 8px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  border-radius: 8px;
  white-space: nowrap;
}

/* ---------- 时间统计 ---------- */
.time-stats {
  margin-bottom: 16px;
}

.time-item {
  text-align: center;
  padding: 12px 8px;
  background: #f5f7fa;
  border-radius: 8px;
}

.time-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.time-value.primary { color: #409eff; }
.time-value.warning { color: #f56c6c; }

.time-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* ---------- 时间 vs 目标 对比 ---------- */
.compare-box {
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
  margin-bottom: 16px;
}

.compare-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}

.compare-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.compare-label {
  width: 68px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.compare-track {
  flex: 1;
  height: 14px;
  background: #ebeef5;
  border-radius: 7px;
  overflow: hidden;
}

.compare-fill {
  height: 100%;
  border-radius: 7px;
  transition: width 0.6s ease;
}

.compare-fill.time {
  background: linear-gradient(90deg, #409eff, #66b1ff);
}

.compare-fill.goal {
  background: linear-gradient(90deg, #67c23a, #95d475);
}

.compare-value {
  width: 42px;
  text-align: right;
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  flex-shrink: 0;
}

.compare-hint {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.6;
}

.compare-hint.good {
  background: #f0f9eb;
  color: #529b2e;
}

.compare-hint.normal {
  background: #ecf5ff;
  color: #409eff;
}

.compare-hint.behind {
  background: #fdf6ec;
  color: #b88230;
}

.encourage-box {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f0f9eb 0%, #ecf5ff 100%);
  border-radius: 10px;
  border-left: 4px solid #67c23a;
  margin-bottom: 18px;
}

.encourage-emoji {
  font-size: 32px;
  flex-shrink: 0;
}

.encourage-content {
  flex: 1;
}

.encourage-text {
  font-size: 16px;
  font-weight: 700;
  color: #529b2e;
  margin-bottom: 4px;
}

.encourage-sub {
  font-size: 13px;
  color: #909399;
}

.streak-stats {
  margin-top: 4px;
}

.streak-item {
  text-align: center;
  padding: 12px 8px;
  background: #f5f7fa;
  border-radius: 8px;
}

.streak-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.streak-value.success { color: #67c23a; }
.streak-value.warning { color: #e6a23c; }

.streak-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* ---------- 概览 ---------- */
.overview-row {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 18px;
  margin-bottom: 12px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
  margin-right: 14px;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* ---------- 图表 ---------- */
.chart-card {
  margin-bottom: 16px;
}

.legend {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #606266;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.legend-item i {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  display: inline-block;
}

.chart-note {
  text-align: center;
  font-size: 12px;
  color: #a8abb2;
  margin-top: 8px;
}

.table-card {
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .encourage-emoji {
    font-size: 24px;
  }
  .encourage-text {
    font-size: 14px;
  }
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
