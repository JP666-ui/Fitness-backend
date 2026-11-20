<template>
  <div class="home">
    <!-- ============ 顶部：个人信息 ============ -->
    <div class="hero">
      <div class="hero-left">
        <h1>智能运动营养与健康评测系统</h1>
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

    <!-- ============ 四大功能块 ============ -->
    <div class="blocks">
      <!-- 1. 个人基础信息 -->
      <div class="block block-info" @click="goTo('/profile')">
        <div class="block-head">
          <span class="block-icon">👤</span>
          <div>
            <h2>个人基础信息</h2>
            <p>身体档案与健康评测</p>
          </div>
        </div>
        <div class="block-body">
          <div class="kv-grid">
            <div class="kv" v-for="item in infoItems" :key="item.label">
              <span class="kv-label">{{ item.label }}</span>
              <span class="kv-val">{{ item.value }}</span>
            </div>
          </div>
        </div>
        <div class="block-foot">
          <span>完善资料 · 查看健康评测</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 2. 健康计划 -->
      <div class="block block-plan">
        <div class="block-head">
          <span class="block-icon">🎯</span>
          <div>
            <h2>健康计划</h2>
            <p>目标设定与每日记录</p>
          </div>
        </div>
        <div class="block-body">
          <div class="plan-summary" v-if="plan.planName">
            <div class="plan-name">{{ plan.planName }}</div>
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
          <div class="plan-empty" v-else>还没有进行中的计划</div>

          <div class="sub-entries">
            <div class="sub-entry" @click.stop="goTo('/food')">
              <span class="sub-icon">🍎</span>
              <span class="sub-text">饮食记录</span>
              <span class="sub-val">{{ today.intake || 0 }} kcal</span>
            </div>
            <div class="sub-entry" @click.stop="goTo('/exercise')">
              <span class="sub-icon">🏃</span>
              <span class="sub-text">运动记录</span>
              <span class="sub-val">{{ today.burn || 0 }} kcal</span>
            </div>
            <div class="sub-entry" @click.stop="goTo('/weight')">
              <span class="sub-icon">⚖️</span>
              <span class="sub-text">体重记录</span>
              <span class="sub-val">{{ latestWeight ? latestWeight + ' kg' : '--' }}</span>
            </div>
          </div>
        </div>
        <div class="block-foot" @click.stop="goTo('/plan')">
          <span>查看/创建计划</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 3. AI 营养咨询 -->
      <div class="block block-ai" @click="goTo('/consult')">
        <div class="block-head">
          <span class="block-icon">🤖</span>
          <div>
            <h2>AI 营养咨询</h2>
            <p>智谱 GLM 智能问答</p>
          </div>
        </div>
        <div class="block-body">
          <div class="ai-preview">
            <div class="ai-bubble">减脂期晚餐吃什么比较好？</div>
            <div class="ai-bubble ai-reply">结合你的数据建议……</div>
          </div>
        </div>
        <div class="block-foot">
          <span>开始对话 · 获取个性化建议</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 4. 数据统计 -->
      <div class="block block-stat" @click="goTo('/statistics')">
        <div class="block-head">
          <span class="block-icon">📊</span>
          <div>
            <h2>数据统计</h2>
            <p>摄入 / 消耗 / 缺口趋势</p>
          </div>
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
        </div>
        <div class="block-foot">
          <span>查看每日趋势分析</span>
          <el-icon><ArrowRight /></el-icon>
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
    if (r.code === 200 && r.data) plan.value = r.data
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

/* ---------- 四大块 ---------- */
.blocks {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

@media (max-width: 900px) { .blocks { grid-template-columns: 1fr; } }

.block {
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  border-top: 3px solid transparent;
}

.block:hover { transform: translateY(-3px); box-shadow: 0 10px 28px rgba(0, 0, 0, 0.1); }

.block-info { border-top-color: #409eff; }
.block-plan { border-top-color: #67c23a; }
.block-ai { border-top-color: #a06cd5; }
.block-stat { border-top-color: #e6a23c; }

.block-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.block-icon { font-size: 28px; }
.block-head h2 { margin: 0 0 2px; font-size: 17px; color: #303133; }
.block-head p { margin: 0; font-size: 12px; color: #909399; }

.block-body { flex: 1; }

.block-foot {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #909399;
}

.block:hover .block-foot { color: #409eff; }

/* 信息卡 */
.kv-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }

.kv {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 10px;
  text-align: center;
}

.kv-label { display: block; font-size: 12px; color: #909399; margin-bottom: 4px; }
.kv-val { font-size: 15px; font-weight: 600; color: #303133; }

/* 计划卡 */
.plan-summary { margin-bottom: 12px; }
.plan-name { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 4px; }
.plan-meta { font-size: 12px; color: #909399; display: flex; gap: 14px; }
.plan-days { font-size: 12px; color: #909399; margin-top: 4px; }
.plan-empty { color: #c0c4cc; font-size: 13px; padding: 10px 0; }

.sub-entries { display: flex; flex-direction: column; gap: 8px; }

.sub-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  background: #f8f9fb;
  border-radius: 8px;
  transition: background 0.2s;
}

.sub-entry:hover { background: #eef5ff; }
.sub-icon { font-size: 16px; }
.sub-text { flex: 1; font-size: 13px; color: #606266; }
.sub-val { font-size: 13px; font-weight: 600; color: #303133; }

/* AI 卡 */
.ai-preview { display: flex; flex-direction: column; gap: 8px; }

.ai-bubble {
  background: #f0f2f5;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 13px;
  color: #606266;
  max-width: 88%;
}

.ai-reply { background: #f6f0ff; color: #7b5ea7; align-self: flex-end; }

/* 统计卡 */
.stat-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.stat-item { text-align: center; }

.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-num.good { color: #67c23a; }
.stat-num.bad { color: #f56c6c; }

.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
