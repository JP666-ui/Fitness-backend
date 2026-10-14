import request from '@/utils/request'

/**
 * 获取当前计划（自动加载，不创建）
 * 返回：{ plan, calorie, progress }
 */
export function getCurrentPlan() {
  return request({
    url: '/plan/current',
    method: 'get',
  })
}

/**
 * 获取或创建计划（幂等，已有计划时不会重复创建）
 * @param {string} planType - loss / gain / shape / maintain
 */
export function getOrCreatePlan(planType) {
  return request({
    url: '/plan/get-or-create',
    method: 'post',
    data: { planType },
  })
}

/**
 * 获取热量缺口分析（不依赖计划）
 * @param {number} targetWeight - 可选，目标体重
 */
export function getCalorieAnalysis(targetWeight) {
  return request({
    url: '/plan/calorie-analysis',
    method: 'get',
    params: targetWeight ? { targetWeight } : {},
  })
}

/**
 * 按指定缺口反推所需天数
 * @param {number} targetWeight - 目标体重
 * @param {number} dailyDeficit - 每日热量缺口
 */
export function getDeficitPreview(targetWeight, dailyDeficit) {
  return request({
    url: '/plan/deficit-preview',
    method: 'get',
    params: { targetWeight, dailyDeficit },
  })
}

export function getPlanList() {
  return request({
    url: '/plan/list',
    method: 'get',
  })
}

export function getActivePlan() {
  return request({
    url: '/plan/active',
    method: 'get',
  })
}

/**
 * 创建计划（会替换当前计划）
 * @param {string} planType - loss / gain / shape / maintain
 */
export function createPlan(planType) {
  return request({
    url: '/plan/create',
    method: 'post',
    data: { planType },
  })
}

export function updatePlan(id, data) {
  return request({
    url: `/plan/update/${id}`,
    method: 'put',
    data,
  })
}

export function completePlan(id) {
  return request({
    url: `/plan/complete/${id}`,
    method: 'put',
  })
}

export function cancelPlan(id) {
  return request({
    url: `/plan/cancel/${id}`,
    method: 'put',
  })
}

export function deletePlan(id) {
  return request({
    url: `/plan/${id}`,
    method: 'delete',
  })
}

export function getPlanTypes() {
  return request({
    url: '/plan/types',
    method: 'get',
  })
}
