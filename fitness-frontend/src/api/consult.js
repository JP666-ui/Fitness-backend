import request from '@/utils/request'

/**
 * 与 AI 营养师自由对话（不需要上传任何图片）
 * @param {Object} data
 * @param {string} data.message - 用户消息
 * @param {Array}  data.history - 历史消息 [{role, content}]
 */
export function chatWithAI(data) {
  return request({
    url: '/consult/chat',
    method: 'post',
    data,
  })
}

/**
 * 获取营养建议（含 BMR、目标热量、AI 可用状态）
 */
export function getNutritionAdvice(params) {
  return request({
    url: '/consult/advice',
    method: 'get',
    params,
  })
}

/**
 * 获取推荐食谱
 * @param {string} type - loss / gain / maintain
 */
export function getRecommendedRecipes(params) {
  return request({
    url: '/consult/recipes',
    method: 'get',
    params,
  })
}

// ===================================================================
// AI 配置（客户可填自己的密钥；不填则用系统免费模型）
// ===================================================================

/**
 * 获取当前用户的 AI 配置
 */
export function getAiConfig() {
  return request({
    url: '/ai-config',
    method: 'get',
  })
}

/**
 * 保存 AI 配置
 * @param {Object} data - {apiKey, apiUrl, model, enabled}
 */
export function saveAiConfig(data) {
  return request({
    url: '/ai-config',
    method: 'post',
    data,
  })
}

/**
 * 删除 AI 配置（恢复使用系统免费模型）
 */
export function removeAiConfig() {
  return request({
    url: '/ai-config',
    method: 'delete',
  })
}

/**
 * 测试 AI 连接
 */
export function testAiConfig(data) {
  return request({
    url: '/ai-config/test',
    method: 'post',
    data,
  })
}

/**
 * 查询系统免费模型状态
 */
export function getAiStatus() {
  return request({
    url: '/ai-config/status',
    method: 'get',
  })
}
