import request from '@/utils/request'

// ==================== 管理员：任务监控 ====================

/** 所有普通用户任务执行总览 */
export function getTaskOverview(days = 7) {
  return request({
    url: '/admin/task/overview',
    method: 'get',
    params: { days },
  })
}

/** 单个用户任务执行详情（含每日明细） */
export function getTaskDetail(userId, days = 7) {
  return request({
    url: `/admin/task/detail/${userId}`,
    method: 'get',
    params: { days },
  })
}

// ==================== 管理员：用户管理 ====================

/** 用户列表（分页） */
export function getUserList(params) {
  return request({
    url: '/admin/user/list',
    method: 'get',
    params,
  })
}

/** 全部用户（不分页，用于发消息选人） */
export function getAllUsers() {
  return request({
    url: '/admin/user/all',
    method: 'get',
  })
}

/** 启用/禁用用户 */
export function updateUserStatus(id, status) {
  return request({
    url: `/admin/user/${id}/status`,
    method: 'put',
    params: { status },
  })
}

/** 删除用户 */
export function deleteUser(id) {
  return request({
    url: `/admin/user/${id}`,
    method: 'delete',
  })
}

/** 用户统计 */
export function getUserStatistics() {
  return request({
    url: '/admin/user/statistics',
    method: 'get',
  })
}
