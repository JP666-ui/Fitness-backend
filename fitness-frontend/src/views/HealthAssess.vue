<template>
  <div class="assess-page">
    <div class="page-header">
      <div class="head-left">
        <h1>🏥 健康评测</h1>
        <p>基于身体数据算法估算体成分，无需体脂秤</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="loadAssess" :loading="loading">重新评测</el-button>
        <el-button type="primary" :icon="Document" @click="saveResult" :loading="saving">
          保存本次评测
        </el-button>
        <el-button @click="$router.push('/simple')">返回</el-button>
      </div>
    </div>

    <el-alert
      v-if="needProfile"
      type="warning"
      :closable="false"
      show-icon
      title="请先完善个人信息"
      style="margin-bottom: 16px"
    >
      <template #default>
        身高、体重、年龄缺失，无法进行评测。
        <el-button link type="primary" @click="$router.push('/profile')">前往完善 →</el-button>
      </template>
    </el-alert>

    <div v-loading="loading">
      <!-- ============ 综合评分 ============ -->
      <el-card v-if="data.evaluation" class="score-card" shadow="never">
        <div class="score-row">
          <div class="score-main">
            <el-progress
              type="dashboard"
              :percentage="data.evaluation.healthScore"
              :width="150"
              :color="scoreColor"
            >
              <template #default="{ percentage }">
                <div class="score-num">{{ percentage }}</div>
                <div class="score-label">综合健康分</div>
              </template>
            </el-progress>
            <el-tag :type="levelTagType(data.evaluation.healthLevel)" size="large" effect="dark" class="level-tag">
              {{ data.evaluation.healthLevel }}
            </el-tag>
          </div>

          <div class="score-side">
            <div class="side-item">
              <span class="side-label">营养均衡分</span>
              <template v-if="data.evaluation?.hasNutritionData">
                <span class="side-val">{{ data.evaluation.nutritionScore }}</span>
                <el-tag size="small" effect="plain">{{ data.evaluation.nutritionLevel }}</el-tag>
              </template>
              <template v-else>
                <span class="side-val na">暂无数据</span>
                <el-tooltip content="近 30 天没有饮食记录，无法评估营养均衡度" placement="top">
                  <el-icon class="na-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </template>
            </div>
            <div class="side-item">
              <span class="side-label">代谢年龄</span>
              <span class="side-val">{{ data.metabolic?.metabolicAge }} 岁</span>
              <el-tag size="small" :type="data.metabolic?.ageDiff <= 0 ? 'success' : 'warning'" effect="plain">
                {{ data.metabolic?.ageDiff > 0 ? '偏大 ' + data.metabolic.ageDiff + ' 岁' :
                   data.metabolic?.ageDiff < 0 ? '偏小 ' + Math.abs(data.metabolic.ageDiff) + ' 岁' : '相符' }}
              </el-tag>
            </div>
            <div class="side-item">
              <span class="side-label">基础代谢</span>
              <span class="side-val">{{ data.metabolic?.bmr }} kcal</span>
            </div>
            <div class="side-item">
              <span class="side-label">内脏脂肪</span>
              <span class="side-val">{{ data.metabolic?.visceralFat }} 级</span>
              <el-tag size="small" :type="vfTagType(data.metabolic?.visceralLevel)" effect="plain">
                {{ data.metabolic?.visceralLevel }}
              </el-tag>
            </div>
          </div>

          <div class="radar-wrap">
            <div ref="radarRef" class="radar-chart"></div>
          </div>
        </div>
      </el-card>

      <!-- ============ 体成分 ============ -->
      <el-card v-if="data.body" shadow="never" class="section-card">
        <template #header><b>体成分分析</b></template>
        <el-row :gutter="16">
          <el-col :span="6" v-for="item in bodyItems" :key="item.key">
            <div class="metric-box">
              <div class="metric-label">{{ item.label }}</div>
              <div class="metric-value">
                {{ item.value }}<span class="metric-unit">{{ item.unit }}</span>
              </div>
              <div class="metric-bar">
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
                </div>
              </div>
              <el-tag v-if="item.level" size="small" :type="item.levelType" effect="plain">
                {{ item.level }}
              </el-tag>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- ============ 评估建议 ============ -->
      <el-card v-if="adviceList.length" shadow="never" class="section-card">
        <template #header><b>评估建议</b></template>
        <div class="advice-list">
          <div v-for="(a, i) in adviceList" :key="i" class="advice-item" :class="a.level">
            <el-icon class="advice-icon">
              <WarningFilled v-if="a.level === 'danger'" />
              <InfoFilled v-else-if="a.level === 'warning'" />
              <SuccessFilled v-else-if="a.level === 'success'" />
              <Comment v-else />
            </el-icon>
            <div>
              <div class="advice-title">{{ a.title }}</div>
              <div class="advice-content">{{ a.content }}</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- ============ 评测历史 ============ -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="card-head-row">
            <b>评测历史与趋势</b>
            <div>
              <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
                <el-radio-button :value="7">近 7 天</el-radio-button>
                <el-radio-button :value="30">近 30 天</el-radio-button>
                <el-radio-button :value="90">近 90 天</el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </template>
        <el-empty v-if="!trend.points?.length" description="还没有保存过评测记录，点击「保存本次评测」开始记录" />
        <template v-else>
          <div class="trend-summary" v-if="trend.change">
            <span>BMI 变化 <b :class="deltaClass(trend.change.bmiDelta)">{{ fmtDelta(trend.change.bmiDelta) }}</b></span>
            <span>体脂率变化 <b :class="deltaClass(-trend.change.bodyFatDelta)">{{ fmtDelta(trend.change.bodyFatDelta) }}%</b></span>
            <span>健康分变化 <b :class="deltaClass(trend.change.scoreDelta)">{{ fmtDelta(trend.change.scoreDelta) }}</b></span>
          </div>
          <div ref="trendRef" class="trend-chart"></div>
        </template>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Refresh, Document, WarningFilled, InfoFilled, SuccessFilled, Comment, QuestionFilled,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getAssess, saveAssess, getTrend } from '@/api/health'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const needProfile = ref(false)
const data = reactive({})
const trendDays = ref(30)
const trend = ref({})

const radarRef = ref(null)
const trendRef = ref(null)
let radarChart = null
let trendChart = null

const adviceList = computed(() => {
  const a = data.advice
  if (!a) return []
  return Array.isArray(a) ? a : []
})

const scoreColor = computed(() => {
  const s = data.evaluation?.healthScore ?? 0
  if (s >= 85) return '#67c23a'
  if (s >= 70) return '#409eff'
  if (s >= 55) return '#e6a23c'
  return '#f56c6c'
})

const bodyItems = computed(() => {
  const b = data.body
  if (!b) return []
  return [
    {
      key: 'bmi', label: 'BMI', value: b.bmi, unit: '',
      percent: Math.min(100, (b.bmi / 35) * 100),
      color: b.bmi <= 23.9 && b.bmi >= 18.5 ? '#67c23a' : '#e6a23c',
      level: b.bmiLevel, levelType: levelTagType(b.bmiLevel),
    },
    {
      key: 'bf', label: '体脂率', value: b.bodyFatRate, unit: '%',
      percent: Math.min(100, (b.bodyFatRate / 45) * 100),
      color: b.bodyFatLevel === '标准' ? '#67c23a' : '#e6a23c',
      level: b.bodyFatLevel, levelType: levelTagType(b.bodyFatLevel),
    },
    {
      key: 'muscle', label: '肌肉量', value: b.muscleMass, unit: 'kg',
      percent: Math.min(100, (b.muscleRatio / 50) * 100),
      color: '#409eff',
      level: b.muscleRatio + '%', levelType: 'info',
    },
    {
      key: 'fat', label: '脂肪量', value: b.fatMass, unit: 'kg',
      percent: Math.min(100, (b.bodyFatRate / 45) * 100),
      color: '#f56c6c',
    },
    {
      key: 'lean', label: '去脂体重', value: b.leanMass, unit: 'kg',
      percent: 70, color: '#909399',
    },
    {
      key: 'water', label: '体水分率', value: b.waterRate, unit: '%',
      percent: Math.min(100, b.waterRate),
      color: '#409eff',
    },
  ]
})

const levelTagType = (l) => {
  if (['正常', '标准', '优秀', '良好'].includes(l)) return 'success'
  if (['偏瘦', '偏低', '一般', '超重', '偏高'].includes(l)) return 'warning'
  if (['肥胖', '过高', '较差', '偏弱'].includes(l)) return 'danger'
  return 'info'
}
const vfTagType = (l) => (l === '标准' ? 'success' : l === '偏高' ? 'warning' : 'danger')
const deltaClass = (d) => (d > 0 ? 'up' : d < 0 ? 'down' : 'flat')
const fmtDelta = (d) => (d > 0 ? '+' + d : d < 0 ? String(d) : '0')

const loadAssess = async () => {
  loading.value = true
  needProfile.value = false
  try {
    const res = await getAssess()
    if (res.code === 200) {
      Object.keys(data).forEach((k) => delete data[k])
      Object.assign(data, res.data)
      await nextTick()
      renderRadar()
    } else if (res.code === 400) {
      needProfile.value = true
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const saveResult = async () => {
  saving.value = true
  try {
    const res = await saveAssess()
    if (res.code === 200) {
      ElMessage.success('评测结果已保存')
      loadTrend()
    } else {
      ElMessage.warning(res.msg || '保存失败')
    }
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

const loadTrend = async () => {
  try {
    const res = await getTrend(trendDays.value)
    if (res.code === 200) {
      trend.value = res.data
      await nextTick()
      renderTrend()
    }
  } catch (e) {
    console.error(e)
  }
}

const renderRadar = () => {
  if (!radarRef.value || !data.evaluation?.dimensions) return
  if (!radarChart) radarChart = echarts.init(radarRef.value)

  const dims = data.evaluation.dimensions
  // 代谢活力用 BMR 与代谢年龄差值折算
  const ageDiff = data.metabolic?.ageDiff ?? 0
  const metaScore = Math.max(20, Math.min(100, 80 - ageDiff * 3))
  dims['代谢活力'] = Math.round(metaScore)

  const keys = Object.keys(dims)
  const values = keys.map((k) => dims[k])

  radarChart.setOption({
    radar: {
      indicator: keys.map((k) => ({ name: k, max: 100 })),
      radius: '62%',
      splitArea: { areaStyle: { color: ['#f8f9fb', '#fff'] } },
      axisName: { color: '#606266', fontSize: 12 },
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '评测维度',
        areaStyle: { color: 'rgba(64,158,255,0.25)' },
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' },
      }],
    }],
  })
  radarChart.resize()
}

const renderTrend = () => {
  if (!trendRef.value || !trend.value.points?.length) return
  if (!trendChart) trendChart = echarts.init(trendRef.value)

  const pts = trend.value.points
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['BMI', '体脂率', '健康分'], top: 0 },
    grid: { left: 50, right: 30, top: 40, bottom: 40 },
    xAxis: {
      type: 'category',
      data: pts.map((p) => p.date?.substring(5)),
      axisLabel: { fontSize: 11 },
    },
    yAxis: [
      { type: 'value', name: 'BMI / 体脂率', position: 'left' },
      { type: 'value', name: '健康分', position: 'right', min: 0, max: 100 },
    ],
    series: [
      {
        name: 'BMI', type: 'line', smooth: true, yAxisIndex: 0,
        data: pts.map((p) => p.bmi),
        itemStyle: { color: '#409eff' },
      },
      {
        name: '体脂率', type: 'line', smooth: true, yAxisIndex: 0,
        data: pts.map((p) => p.bodyFatRate),
        itemStyle: { color: '#e6a23c' },
      },
      {
        name: '健康分', type: 'line', smooth: true, yAxisIndex: 1,
        data: pts.map((p) => p.healthScore),
        itemStyle: { color: '#67c23a' },
      },
    ],
  })
  trendChart.resize()
}

const onResize = () => {
  radarChart?.resize()
  trendChart?.resize()
}

onMounted(async () => {
  await loadAssess()
  await loadTrend()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  radarChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.assess-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 24px;
}

.page-header {
  background: linear-gradient(135deg, #1f6fc0 0%, #409eff 100%);
  color: #fff;
  border-radius: 14px;
  padding: 22px 28px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.page-header h1 { margin: 0 0 6px; font-size: 24px; }
.page-header p { margin: 0; opacity: 0.85; font-size: 13px; }
.head-actions { display: flex; gap: 8px; }

.score-card, .section-card { border-radius: 14px; margin-bottom: 16px; }

.score-row {
  display: flex;
  align-items: center;
  gap: 30px;
  flex-wrap: wrap;
}

.score-main {
  text-align: center;
  min-width: 180px;
}

.score-num { font-size: 30px; font-weight: 700; color: #303133; line-height: 1.2; }
.score-label { font-size: 12px; color: #909399; }
.level-tag { margin-top: 8px; }

.score-side {
  flex: 1;
  min-width: 220px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 24px;
}

.side-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f8f9fb;
  border-radius: 10px;
}

.side-label { color: #909399; font-size: 13px; }
.side-val { font-weight: 600; color: #303133; font-size: 15px; }
.side-val.na { color: #c0c4cc; font-size: 14px; }
.na-icon { color: #c0c4cc; cursor: help; }

.radar-wrap { width: 320px; height: 260px; }
.radar-chart { width: 100%; height: 100%; }

.metric-box {
  background: #f8f9fb;
  border-radius: 10px;
  padding: 14px;
  text-align: center;
}

.metric-label { color: #909399; font-size: 13px; margin-bottom: 6px; }
.metric-value { font-size: 22px; font-weight: 700; color: #303133; }
.metric-unit { font-size: 12px; color: #909399; margin-left: 2px; }

.metric-bar { margin: 10px 0 8px; }
.bar-track { height: 6px; background: #ebeef5; border-radius: 3px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 3px; transition: width 0.5s; }

.advice-list { display: flex; flex-direction: column; gap: 10px; }

.advice-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  background: #f8f9fb;
  border-left: 4px solid #dcdfe6;
}

.advice-item.danger { background: #fef0f0; border-left-color: #f56c6c; }
.advice-item.warning { background: #fdf6ec; border-left-color: #e6a23c; }
.advice-item.success { background: #f0f9eb; border-left-color: #67c23a; }
.advice-item.info { background: #f4f8fe; border-left-color: #409eff; }

.advice-icon { font-size: 18px; margin-top: 2px; }
.advice-title { font-weight: 600; color: #303133; margin-bottom: 4px; }
.advice-content { color: #606266; font-size: 13px; line-height: 1.6; }

.card-head-row { display: flex; justify-content: space-between; align-items: center; }

.trend-summary {
  display: flex;
  gap: 24px;
  margin-bottom: 10px;
  color: #606266;
  font-size: 13px;
}

.trend-summary b { font-size: 15px; margin-left: 4px; }
.trend-summary .up { color: #f56c6c; }
.trend-summary .down { color: #67c23a; }
.trend-summary .flat { color: #909399; }

.trend-chart { width: 100%; height: 300px; }
</style>
