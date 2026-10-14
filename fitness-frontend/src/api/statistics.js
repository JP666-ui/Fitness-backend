import request from '@/utils/request'

/**
 * 获取统计数据
 * @param {Object} params - 查询参数
 * @param {string} params.timeRange - 时间范围：week/month/year
 * @param {string} params.startDate - 开始日期
 * @param {string} params.endDate - 结束日期
 */
export function getStatistics(params) {
  return request({
    url: '/statistics',
    method: 'get',
    params,
  })
}

/**
 * 获取每日统计（最小单位为日）
 * @param {number} days - 统计天数
 * 返回：{ daily: [{date, intake, burn, deficit, onTarget}], summary: {...} }
 */
export function getDailyStatistics(days = 7) {
  return request({
    url: '/statistics/daily',
    method: 'get',
    params: { days },
  })
}

/**
 * 获取今日统计
 */
export function getTodayStatistics() {
  return request({
    url: '/statistics/today',
    method: 'get',
  })
}

/**
 * 获取本周统计
 */
export function getWeekStatistics() {
  return request({
    url: '/statistics/week',
    method: 'get',
  })
}

/**
 * 获取本月统计
 */
export function getMonthStatistics() {
  return request({
    url: '/statistics/month',
    method: 'get',
  })
}

/**
 * 获取年度统计
 */
export function getYearStatistics() {
  return request({
    url: '/statistics/year',
    method: 'get',
  })
}

/**
 * 导出统计数据
 * @param {Object} params - 导出参数
 */
export function exportStatistics(params) {
  return request({
    url: '/statistics/export',
    method: 'get',
    params,
    responseType: 'blob',
  })
}
