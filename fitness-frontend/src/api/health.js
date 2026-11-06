import request from '@/utils/request'

/** 执行健康评测（实时计算，不落库） */
export function getAssess() {
  return request({
    url: '/health/assess',
    method: 'get',
  })
}

/** 执行评测并保存（同一天覆盖） */
export function saveAssess() {
  return request({
    url: '/health/assess/save',
    method: 'post',
  })
}

/** 评测历史 */
export function getHistory(days = 30) {
  return request({
    url: '/health/history',
    method: 'get',
    params: { days },
  })
}

/** 评测趋势（图表用） */
export function getTrend(days = 30) {
  return request({
    url: '/health/trend',
    method: 'get',
    params: { days },
  })
}
