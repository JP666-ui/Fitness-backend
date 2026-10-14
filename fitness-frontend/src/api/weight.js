import request from '@/utils/request'

/**
 * 记录体重（每天一条，重复则更新）
 * @param {Object} data - { weight, recordDate, remark }
 */
export function addWeight(data) {
  return request({
    url: '/weight/add',
    method: 'post',
    data,
  })
}

/**
 * 获取体重记录列表
 * @param {number} days - 统计天数
 */
export function getWeightList(days = 30) {
  return request({
    url: '/weight/list',
    method: 'get',
    params: { days },
  })
}

/**
 * 获取体重变化统计
 * @param {number} days - 统计天数
 */
export function getWeightStatistics(days = 30) {
  return request({
    url: '/weight/statistics',
    method: 'get',
    params: { days },
  })
}

/**
 * 删除体重记录
 */
export function deleteWeight(id) {
  return request({
    url: `/weight/${id}`,
    method: 'delete',
  })
}

/**
 * 查询今日是否已记录
 */
export function getTodayWeight() {
  return request({
    url: '/weight/today',
    method: 'get',
  })
}
