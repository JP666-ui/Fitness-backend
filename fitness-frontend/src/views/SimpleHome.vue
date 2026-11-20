<template>
  <div class="home">
    <!-- ============ 顶部信息条 ============ -->
    <div class="hero">
      <div class="hero-left">
        <h1>个人健身计划与健康管理系统</h1>
        <p class="greet">
          {{ greeting }}，{{ userStore.nickname || userStore.username || '访客' }}
        </p>
      </div>
      <div class="hero-right">
        <div class="stat-chip" v-if="profile.bmi">
          <span class="chip-label">BMI</span>
          <span class="chip-val">{{ profile.bmi }}</span>
          <span class="chip-sub">{{ profile.bmiLevel }}</span>
        </div>
        <div class="stat-chip" v-if="assess.healthScore != null">
          <span class="chip-label">健康分</span>
          <span class="chip-val">{{ assess.healthScore }}</span>
          <span class="chip-sub">{{ assess.healthLevel }}</span>
        </div>
        <el-button class="logout-btn" @click="logout">
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </div>
    </div>

    <!-- ============ 三大类 ============ -->
    <div class="blocks">
      <!-- ========== 一、个人信息 ========== -->
      <div class="block block-info">
        <div class="block-head" @click="goTo('/profile')">
          <span class="block-icon">👤</span>
          <div class="head-text">
            <h2>个人信息</h2>
            <p>身体档案 · 健康评测</p>
          </div>
          <el-icon class="head-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="block-body">
          <div class="kv-grid">
            <div class="kv" v-for="item in infoItems" :key="item.label">
              <span class="kv-label">{{ item.label }}</span>
              <span class="kv-val">{{ item.value }}</span>
            </div>
          </div>
          <div class="entry-row">
            <div class="entry" @click="goTo('/health')">
              <span class="entry-icon">🏥</span>
              <span class="entry-text">健康评测</span>
              <span class="entry-val" v-if="assess.healthScore != null">{{ assess.healthScore }} 分</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 二、健身计划 ========== -->
      <div class="block block-plan">
        <div class="block-head" @click="goTo('/plan')">
          <span class="block-icon">🎯</span>
          <div class="head-text">
            <h2>健身计划</h2>
            <p>目标设定 · 每日记录</p>
          </div>
          <el-icon class="head-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="block-body">
          <div class="plan-summary" v-if="plan.planName">
            <div class="plan-top">
              <span class="plan-name">{{ plan.planName }}</span>
              <el-tag size="small" type="success" effect="plain">进行中</el-tag>
            </div>
            <div class="plan-meta">
              <span v-if="plan.targetCalorie">目标 {{ plan.targetCalorie }} kcal/天</span>
              <span v-if="plan.dailyDeficit">缺口 {{ plan.dailyDeficit }} kcal</span>
            </div>
            <el-progress
              v-if="plan.timePercent != null"
              :percentage="plan.timePercent"
              :stroke-width="6"
              :show-text="false"
              style="margin-top: 8px"
            />
            <div class="plan-days" v-if="plan.totalDays">
              已进行 {{ plan.passedDays }} / {{ plan.totalDays }} 天
            </div>
          </div>
          <div class="plan-empty" v-else>还没有进行中的计划，点击卡片创建</div>

          <div class="entry-row">
            <div class="entry" @click="goTo('/food')">
              <span class="entry-icon">🍎</span>
              <span class="entry-text">饮食记录</span>
              <span class="entry-val">{{ today.intake || 0 }} kcal</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <div class="entry" @click="goTo('/exercise')">
              <span class="entry-icon">🏃</span>
              <span class="entry-text">运动记录</span>
              <span class="entry-val">{{ today.burn || 0 }} kcal</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <div class="entry" @click="goTo('/weight')">
              <span class="entry-icon">⚖️</span>
              <span class="entry-text">体重记录</span>
              <span class="entry-val">{{ latestWeight ? latestWeight + ' kg' : '--' }}</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 三、健康管理 ========== -->
      <div class="block block-manage">
        <div class="block-head" @click="goTo('/statistics')">
          <span class="block-icon">📈</span>
          <div class="head-text">
            <h2>健康管理</h2>
            <p>AI 咨询 · 数据统计</p>
          </div>
          <el-icon class="head-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="block-body">
          <div class="stat-row">
            <div class="stat-item">
              <div class="stat-num">{{ today.intake || 0 }}</div>
              <div class="stat-label">今日摄入</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">{{ today.burn || 0 }}</div>
              <div class="stat-label">今日消耗</div>
            </div>
            <div class="stat-item">
              <div class="stat-num" :class="deficitClass">{{ today.deficit || 0 }}</div>
              <div class="stat-label">今日缺口</div>
            </div>
          </div>

          <div class="entry-row">
            <div class="entry" @click="goTo('/consult')">
              <span class="entry-icon">🤖</span>
              <span class="entry-text">AI 营养咨询</span>
              <span class="entry-val muted">智谱 GLM</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <div class="entry" @click="goTo('/statistics')">
              <span class="entry-icon">📊</span>
              <span class="entry-text">数据统计</span>
              <span class="entry-val muted">趋势分析</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { SwitchButton, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getTodayStatistics } from '@/api/statistics'
import { getCurrentPlan } from '@/api/plan'
import { getWeightStatistics } from '@/api/weight'
import { getAssess } from '@/api/health'

const router = useRouter()
const userStore = useUserStore()

const today = ref({})
const plan = ref({})
const profile = reactive({})
const assess = reactive({})
const latestWeight = ref(null)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const infoItems = computed(() => [
  { label: '性别', value: profile.gender || '--' },
  { label: '年龄', value: profile.age ? profile.age + ' 岁' : '--' },
  { label: '身高', value: profile.height ? profile.height + ' cm' : '--' },
  { label: '体重', value: profile.weight ? profile.weight + ' kg' : '--' },
  { label: '目标体重', value: profile.targetWeight ? profile.targetWeight + ' kg' : '--' },
  { label: '健康评分', value: assess.healthScore != null ? assess.healthScore + ' 分' : '待评测' },
])

const deficitClass = computed(() => {
  const d = today.value.deficit
  if (d == null) return ''
  return d > 0 ? 'good' : d < 0 ? 'bad' : ''
})

const goTo = (path) => router.push(path)

const logout = () => {
  userStore.logout()
  window.location.href = '/login'
}

const bmiLevel = (bmi) => {
  if (!bmi) return null
  if (bmi < 18.5) return '偏瘦'
  if (bmi <= 23.9) return '正常'
  if (bmi <= 27.9) return '超重'
  return '肥胖'
}

const loadAll = async () => {
  const info = await userStore.getUserInfoAction()
  if (info) {
    profile.gender = info.gender === 1 ? '男' : info.gender === 2 ? '女' : null
    profile.age = info.age
    profile.height = info.height
    profile.weight = info.weight
    profile.targetWeight = info.targetWeight
    profile.bmi = info.bmi
    profile.bmiLevel = bmiLevel(info.bmi)
  }

  try {
    const r = await getTodayStatistics()
    if (r.code === 200) {
      today.value = {
        intake: r.data.todayIntake,
        burn: r.data.todayBurn,
        deficit: r.data.todayDeficit,
      }
    }
  } catch { /* 静默 */ }

  try {
    const r = await getCurrentPlan()
    if (r.code === 200 && r.data) {
      // /plan/current 返回 { plan, progress, calorie }
      const p = r.data.plan || r.data
      const prog = r.data.progress || {}
      plan.value = {
        planName: p.planName,
        targetCalorie: p.targetCalorie,
        dailyDeficit: p.dailyDeficit,
        totalDays: prog.totalDays,
        passedDays: prog.passedDays,
        timePercent: prog.percent,
      }
    }
  } catch { /* 静默 */ }

  try {
    const r = await getWeightStatistics(30)
    if (r.code === 200 && r.data) {
      latestWeight.value = r.data.currentWeight ?? r.data.latestWeight ?? null
    }
  } catch { /* 静默 */ }

  try {
    const r = await getAssess()
    if (r.code === 200 && r.data?.evaluation) {
      assess.healthScore = r.data.evaluation.healthScore
      assess.healthLevel = r.data.evaluation.healthLevel
    }
  } catch { /* 静默 */ }
}

onMounted(loadAll)
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ---------- 顶部 ---------- */
.hero {
  background: linear-gradient(135deg, #1f6fc0 0%, #2f9de0 100%);
  border-radius: 16px;
  padding: 24px 28px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}

.hero h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
}

.greet { margin: 0; opacity: 0.9; font-size: 14px; }

.hero-right { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.stat-chip {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  padding: 8px 14px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.chip-label { font-size: 12px; opacity: 0.85; }
.chip-val { font-size: 20px; font-weight: 700; }
.chip-sub { font-size: 12px; opacity: 0.85; }

.logout-btn {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.35);
  color: #fff;
}
.logout-btn:hover { background: rgba(255, 255, 255, 0.28); color: #fff; }

/* ---------- 三大类 ---------- */
.blocks {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  align-items: start;
}

@media (max-width: 1200px) {
  .blocks { grid-template-columns: 1fr; }
}

.block {
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  border-top: 3px solid transparent;
}

.block:hover { transform: translateY(-3px); box-shadow: 0 10px 28px rgba(0, 0, 0, 0.1); }

.block-info { border-top-color: #409eff; }
.block-plan { border-top-color: #67c23a; }
.block-manage { border-top-color: #a06cd5; }

.block-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  cursor: pointer;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.block-icon { font-size: 28px; }

.head-text { flex: 1; }

.head-text h2 {
  margin: 0 0 2px;
  font-size: 17px;
  color: #303133;
}

.head-text p { margin: 0; font-size: 12px; color: #909399; }

.head-arrow { color: #c0c4cc; transition: transform 0.2s; }
.block-head:hover .head-arrow { color: #409eff; transform: translateX(3px); }

.block-body { flex: 1; }

/* 信息网格 */
.kv-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}

.kv {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 10px;
  text-align: center;
}

.kv-label { display: block; font-size: 12px; color: #909399; margin-bottom: 4px; }
.kv-val { font-size: 15px; font-weight: 600; color: #303133; }

/* 子入口 */
.entry-row { display: flex; flex-direction: column; gap: 8px; }

.entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f8f9fb;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.entry:hover { background: #eef5ff; }
.entry-icon { font-size: 16px; }
.entry-text { flex: 1; font-size: 13px; color: #606266; }
.entry-val { font-size: 13px; font-weight: 600; color: #303133; }
.entry-val.muted { color: #909399; font-weight: 400; }
.entry .el-icon { color: #c0c4cc; }

/* 计划摘要 */
.plan-summary { margin-bottom: 14px; }

.plan-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.plan-name { font-size: 15px; font-weight: 600; color: #303133; }

.plan-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 14px;
}

.plan-days { font-size: 12px; color: #909399; margin-top: 4px; }
.plan-empty { color: #c0c4cc; font-size: 13px; padding: 12px 0 16px; }

/* 统计三列 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 14px;
  padding: 4px 0;
}

.stat-item { text-align: center; }

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-num.good { color: #67c23a; }
.stat-num.bad { color: #f56c6c; }

.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
