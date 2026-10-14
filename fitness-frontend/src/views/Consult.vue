<template>
  <div class="consult-page">
    <div class="page-header">
      <div>
        <h2>🤖 AI 营养咨询</h2>
        <p class="subtitle">和 AI 营养师自由交流，无需上传任何图片</p>
      </div>
      <el-button @click="openSettings">
        <el-icon><Setting /></el-icon>
        密钥设置
      </el-button>
    </div>

    <!-- 模型状态提示 -->
    <el-alert
      v-if="aiStatus.systemAvailable"
      type="success"
      :closable="false"
      show-icon
      class="status-alert"
    >
      <template #title>
        当前使用：<b>{{ aiStatus.model }}</b>
        <span v-if="aiStatus.usingCustomKey" class="key-source">（你的专属密钥）</span>
        <span v-else class="key-source">（系统免费模型，无需配置即可使用）</span>
      </template>
    </el-alert>
    <el-alert
      v-else
      type="warning"
      :closable="false"
      show-icon
      class="status-alert"
      title="系统免费模型未配置，请在「密钥设置」中填入你自己的 API Key 后使用"
    />

    <!-- 离线模式提示 -->
    <el-alert
      v-if="fallbackReason"
      type="error"
      :closable="false"
      show-icon
      class="status-alert"
      :title="`AI 调用失败，已切换为内置知识库回复：${fallbackReason}`"
    />

    <el-row :gutter="20">
      <el-col :xs="24" :md="17">
        <el-card class="chat-card" shadow="never">
          <template #header>
            <div class="chat-header">
              <span>💬 和 AI 营养师聊聊</span>
              <el-button size="small" @click="clearChat">
                <el-icon><Delete /></el-icon>
                清空对话
              </el-button>
            </div>
          </template>

          <div ref="messageContainer" class="chat-messages" v-loading="loading">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              :class="['message', msg.role === 'user' ? 'user-message' : 'ai-message']"
            >
              <div class="message-avatar">
                <el-avatar
                  :size="40"
                  :icon="msg.role === 'user' ? UserFilled : ChatDotRound"
                  :style="{ background: msg.role === 'user' ? '#409EFF' : '#67C23A' }"
                />
              </div>
              <div class="message-content">
                <div class="message-text" v-html="formatMessage(msg.content)"></div>
                <div class="message-time">{{ formatTime(msg.time) }}</div>
              </div>
            </div>

            <div v-if="loading" class="message ai-message">
              <div class="message-avatar">
                <el-avatar :size="40" :icon="ChatDotRound" style="background: #67C23A" />
              </div>
              <div class="message-content">
                <div class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="直接输入问题即可，例如：减脂期晚餐吃什么好？（Ctrl + Enter 发送）"
              @keyup.enter.ctrl="sendMessage"
            />
            <div class="input-actions">
              <el-button
                type="primary"
                :loading="loading"
                :disabled="!inputMessage.trim()"
                @click="sendMessage"
              >
                <el-icon><Promotion /></el-icon>
                发送
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="7">
        <el-card class="side-card">
          <template #header><span>⚡ 快捷提问</span></template>
          <div class="question-list">
            <el-button
              v-for="(q, index) in quickQuestions"
              :key="index"
              class="question-btn"
              :disabled="loading"
              @click="askQuickQuestion(q)"
            >
              {{ q }}
            </el-button>
          </div>
        </el-card>

        <el-card class="side-card">
          <template #header><span>📊 今日概览</span></template>
          <div class="overview-item">
            <span class="label">摄入热量</span>
            <span class="value" style="color: #e6a23c">{{ todayStats.intake }} kcal</span>
          </div>
          <div class="overview-item">
            <span class="label">消耗热量</span>
            <span class="value" style="color: #67c23a">{{ todayStats.burn }} kcal</span>
          </div>
          <div class="overview-item">
            <span class="label">净摄入</span>
            <span class="value" :style="{ color: todayStats.net > 0 ? '#f56c6c' : '#67c23a' }">
              {{ todayStats.net }} kcal
            </span>
          </div>
          <div class="overview-item">
            <span class="label">目标热量</span>
            <span class="value" style="color: #409eff">{{ todayStats.target }} kcal</span>
          </div>
          <el-divider />
          <el-alert :title="getSuggestion()" :type="getSuggestionType()" :closable="false" />
        </el-card>

        <el-card class="side-card">
          <template #header><span>📚 营养小知识</span></template>
          <div class="tip-item" v-for="(tip, index) in nutritionTips" :key="index">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ tip }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 密钥设置对话框 -->
    <el-dialog v-model="settingsVisible" title="AI 密钥设置" width="560px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 18px">
        <template #title>
          <div style="line-height: 1.7">
            <div><b>不填写密钥也能直接使用</b>——系统已内置免费模型（智谱 GLM-4-Flash）。</div>
            <div>若填入你自己的密钥，将优先使用你自己的额度。</div>
          </div>
        </template>
      </el-alert>

      <el-form label-width="110px">
        <el-form-item label="使用方式">
          <el-radio-group v-model="settingsForm.useCustom">
            <el-radio :value="false">使用系统免费模型</el-radio>
            <el-radio :value="true">使用我自己的密钥</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="settingsForm.useCustom">
          <el-form-item label="API Key">
            <el-input
              v-model="settingsForm.apiKey"
              type="password"
              show-password
              :placeholder="configInfo.apiKeyMask
                ? `已保存：${configInfo.apiKeyMask}（留空则不修改）`
                : '粘贴你的智谱 API Key'"
            />
            <div class="form-hint">
              免费申请地址：
              <el-link type="primary" href="https://open.bigmodel.cn" target="_blank">
                open.bigmodel.cn
              </el-link>
              （注册后到「API Keys」页面创建）
            </div>
          </el-form-item>

          <el-form-item label="接口地址">
            <el-input
              v-model="settingsForm.apiUrl"
              :placeholder="configInfo.defaultUrl"
            />
            <div class="form-hint">留空使用默认地址，接口需兼容 OpenAI 规范</div>
          </el-form-item>

          <el-form-item label="模型名称">
            <el-input
              v-model="settingsForm.model"
              :placeholder="configInfo.defaultModel"
            />
            <div class="form-hint">留空使用默认 glm-4-flash</div>
          </el-form-item>
        </template>
      </el-form>

      <div v-if="testResult" class="test-result">
        <el-alert
          :type="testResult.success ? 'success' : 'error'"
          :closable="false"
          show-icon
        >
          <template #title>
            <span v-if="testResult.success">
              ✅ 连接成功！使用{{ testResult.source === 'user' ? '你的密钥' : '系统免费模型' }}（{{ testResult.model }}）
            </span>
            <span v-else>❌ 连接失败：{{ testResult.error }}</span>
          </template>
        </el-alert>
      </div>

      <template #footer>
        <el-button @click="testConnection" :loading="testing">测试连接</el-button>
        <el-button v-if="configInfo.hasCustomKey" type="danger" plain @click="clearConfig">
          清除我的密钥
        </el-button>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSettings" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UserFilled, ChatDotRound, Delete, Promotion, InfoFilled, Setting,
} from '@element-plus/icons-vue'
import {
  chatWithAI, getNutritionAdvice,
  getAiConfig, saveAiConfig, removeAiConfig, testAiConfig,
} from '@/api/consult'
import { getTodayStatistics } from '@/api/statistics'

const messages = ref([
  {
    role: 'assistant',
    content:
      '你好！我是你的 AI 营养师小健 😊\n\n'
      + '直接问我任何饮食、营养、健身方面的问题就行，不需要上传任何图片。\n\n'
      + '我会结合你的身高体重、健身目标和今天的饮食记录来回答。',
    time: new Date(),
  },
])
const inputMessage = ref('')
const loading = ref(false)
const messageContainer = ref(null)
const fallbackReason = ref('')

const quickQuestions = [
  '减脂期晚餐吃什么好？',
  '我今天吃了包子和油条，热量高吗？',
  '运动后应该补充什么？',
  '哪些食物蛋白质含量高？',
  '晚上吃什么不容易胖？',
  '怎么计算我的基础代谢？',
]

const nutritionTips = [
  '每天饮水量建议 2000-2500ml',
  '蛋白质建议每公斤体重 1.2-1.6g',
  '膳食纤维每日建议 25-30g',
  '优质碳水：燕麦、糙米、红薯',
  '优质蛋白：鸡胸肉、鱼、鸡蛋',
  '健康脂肪：坚果、牛油果、橄榄油',
]

const todayStats = ref({ intake: 0, burn: 0, net: 0, target: 2000 })
const aiStatus = ref({ systemAvailable: false, model: '', usingCustomKey: false })

// ---------------- 密钥设置 ----------------
const settingsVisible = ref(false)
const saving = ref(false)
const testing = ref(false)
const testResult = ref(null)
const configInfo = ref({
  hasCustomKey: false,
  apiKeyMask: '',
  defaultModel: 'glm-4-flash',
  defaultUrl: 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
})
const settingsForm = ref({
  useCustom: false,
  apiKey: '',
  apiUrl: '',
  model: '',
})

const openSettings = async () => {
  testResult.value = null
  await fetchAiConfig()
  settingsForm.value = {
    useCustom: configInfo.value.hasCustomKey,
    apiKey: '',
    apiUrl: '',
    model: '',
  }
  settingsVisible.value = true
}

const fetchAiConfig = async () => {
  try {
    const res = await getAiConfig()
    if (res.code === 200 && res.data) {
      configInfo.value = { ...configInfo.value, ...res.data }
      aiStatus.value = {
        systemAvailable: res.data.systemAvailable,
        model: res.data.defaultModel,
        usingCustomKey: res.data.hasCustomKey && res.data.enabled === 1,
      }
    }
  } catch (error) {
    console.error('获取 AI 配置失败:', error)
  }
}

const saveSettings = async () => {
  if (settingsForm.value.useCustom
      && !settingsForm.value.apiKey
      && !configInfo.value.hasCustomKey) {
    ElMessage.warning('请填写 API Key，或选择「使用系统免费模型」')
    return
  }

  saving.value = true
  try {
    const res = await saveAiConfig({
      apiKey: settingsForm.value.apiKey,
      apiUrl: settingsForm.value.apiUrl,
      model: settingsForm.value.model,
      enabled: settingsForm.value.useCustom ? 1 : 0,
    })
    if (res.code === 200) {
      ElMessage.success('设置已保存')
      settingsVisible.value = false
      fallbackReason.value = ''
      await fetchAiConfig()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const testConnection = async () => {
  testing.value = true
  testResult.value = null
  try {
    const res = await testAiConfig({
      apiKey: settingsForm.value.useCustom ? settingsForm.value.apiKey : '',
      apiUrl: settingsForm.value.apiUrl,
      model: settingsForm.value.model,
    })
    if (res.code === 200 && res.data) {
      testResult.value = res.data
      if (res.data.success) {
        await fetchAiConfig()
      }
    } else {
      testResult.value = { success: false, error: res.msg || '测试失败' }
    }
  } catch (error) {
    testResult.value = { success: false, error: '请求失败，请检查网络' }
  } finally {
    testing.value = false
  }
}

const clearConfig = async () => {
  try {
    await ElMessageBox.confirm(
      '清除后将恢复使用系统免费模型，确定继续吗？',
      '提示',
      { type: 'warning' },
    )
    const res = await removeAiConfig()
    if (res.code === 200) {
      ElMessage.success('已清除，恢复使用系统免费模型')
      settingsVisible.value = false
      await fetchAiConfig()
    }
  } catch {
    // 取消
  }
}

// ---------------- 对话 ----------------
const sendMessage = async () => {
  const text = inputMessage.value.trim()
  if (!text) return

  messages.value.push({ role: 'user', content: text, time: new Date() })
  inputMessage.value = ''
  loading.value = true
  await nextTick()
  scrollToBottom()

  try {
    const history = messages.value
      .slice(0, -1)
      .slice(-10)
      .map((m) => ({ role: m.role, content: m.content }))

    const res = await chatWithAI({ message: text, history })

    if (res.code === 200 && res.data) {
      messages.value.push({
        role: 'assistant',
        content: res.data.answer || '抱歉，我没有理解你的问题，能再说一次吗？',
        time: new Date(),
      })
      if (res.data.fallback === true) {
        fallbackReason.value = res.data.fallbackReason || 'AI 服务暂时不可用'
      } else {
        fallbackReason.value = ''
        aiStatus.value.usingCustomKey = res.data.keySource === 'user'
      }
    } else {
      messages.value.push({
        role: 'assistant',
        content: res.msg || '请求失败，请稍后重试。',
        time: new Date(),
      })
    }
  } catch (error) {
    console.error('AI 对话失败:', error)
    messages.value.push({
      role: 'assistant',
      content: '网络请求失败，请检查后端服务是否已启动 🔌',
      time: new Date(),
    })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

const askQuickQuestion = (q) => {
  inputMessage.value = q
  sendMessage()
}

const clearChat = () => {
  messages.value = [
    { role: 'assistant', content: '对话已清空。有什么可以帮你的吗？', time: new Date() },
  ]
}

const scrollToBottom = () => {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}

const formatMessage = (content) => {
  if (!content) return ''
  const escapeHtml = (s) =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  return escapeHtml(content)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/^---$/gm, '<hr/>')
    .replace(/\n/g, '<br>')
}

const formatTime = (time) => {
  return new Date(time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const getSuggestion = () => {
  const remaining = todayStats.value.target - todayStats.value.intake
  if (remaining > 500) return `今日还可摄入 ${remaining} kcal，建议适当增加营养`
  if (remaining > 0) return `今日还可摄入 ${remaining} kcal，控制得当`
  return `今日已超标 ${Math.abs(remaining)} kcal，建议减少摄入`
}

const getSuggestionType = () => {
  const remaining = todayStats.value.target - todayStats.value.intake
  if (remaining > 500) return 'info'
  if (remaining > 0) return 'success'
  return 'warning'
}

const fetchTodayStats = async () => {
  try {
    const res = await getTodayStatistics()
    if (res.code === 200 && res.data) {
      todayStats.value = {
        intake: res.data.todayIntake || 0,
        burn: res.data.todayBurn || 0,
        net: res.data.netCalorie || 0,
        target: res.data.targetCalorie || 2000,
      }
    }
  } catch (error) {
    console.error('获取今日统计失败:', error)
  }
}

const fetchAdvice = async () => {
  try {
    const res = await getNutritionAdvice()
    if (res.code === 200 && res.data) {
      aiStatus.value.systemAvailable = res.data.aiEnabled
      aiStatus.value.model = res.data.model
    }
  } catch (error) {
    // 忽略
  }
}

onMounted(() => {
  fetchTodayStats()
  fetchAdvice()
  fetchAiConfig()
})
</script>

<style scoped>
.consult-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.page-header h2 {
  color: #303133;
  margin-bottom: 6px;
}

.subtitle {
  color: #909399;
  font-size: 14px;
}

.status-alert {
  margin-bottom: 14px;
}

.key-source {
  color: #67c23a;
  font-weight: normal;
}

.chat-card {
  height: calc(100vh - 250px);
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 12px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
  min-height: 200px;
}

.message {
  display: flex;
  margin-bottom: 16px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  margin: 0 10px;
  max-width: 72%;
}

.message-text {
  padding: 10px 14px;
  border-radius: 10px;
  background: #fff;
  color: #303133;
  line-height: 1.7;
  font-size: 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  word-break: break-word;
}

.user-message .message-text {
  background: #409eff;
  color: #fff;
}

.message-time {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 4px;
}

.user-message .message-time {
  color: #c0c4cc;
  text-align: right;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 10px;
}

.typing-indicator span {
  width: 7px;
  height: 7px;
  background: #67c23a;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 100% { transform: translateY(0); opacity: 0.5; }
  50% { transform: translateY(-5px); opacity: 1; }
}

.chat-input {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.side-card {
  margin-bottom: 16px;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.question-btn {
  text-align: left;
  white-space: normal;
  height: auto;
  padding: 8px 12px;
  line-height: 1.4;
  font-size: 13px;
  margin-left: 0 !important;
}

.overview-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f2f6fc;
  font-size: 14px;
}

.overview-item:last-of-type {
  border-bottom: none;
}

.overview-item .label {
  color: #606266;
}

.overview-item .value {
  font-weight: 600;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.tip-item .el-icon {
  color: #409eff;
  margin-top: 3px;
  flex-shrink: 0;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 4px;
}

.test-result {
  margin-top: 10px;
}

@media (max-width: 768px) {
  .chat-card {
    height: auto;
  }
  .chat-messages {
    max-height: 420px;
  }
  .message-content {
    max-width: 80%;
  }
}
</style>
