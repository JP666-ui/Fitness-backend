import request from '@/utils/request'

// ==================== 所有用户：收件箱 ====================

/** 我的收件箱 */
export function getInbox(limit = 50) {
  return request({
    url: '/notice/inbox',
    method: 'get',
    params: { limit },
  })
}

/** 我的未读数（铃铛轮询） */
export function getUnreadCount() {
  return request({
    url: '/notice/unread',
    method: 'get',
  })
}

/** 标记单条已读 */
export function markRead(id) {
  return request({
    url: `/notice/${id}/read`,
    method: 'put',
  })
}

/** 全部标记已读 */
export function markAllRead() {
  return request({
    url: '/notice/read-all',
    method: 'put',
  })
}

// ==================== 管理员：发送 ====================

/** 发送消息（receiverId 为空/0 表示全体广播） */
export function sendNotice(data) {
  return request({
    url: '/admin/notice/send',
    method: 'post',
    data,
  })
}

/** 我发出的消息记录 */
export function getSentNotices(limit = 50) {
  return request({
    url: '/admin/notice/sent',
    method: 'get',
    params: { limit },
  })
}

/** 查看某用户收到的消息 */
export function getUserNotices(userId, limit = 50) {
  return request({
    url: `/admin/notice/user/${userId}`,
    method: 'get',
    params: { limit },
  })
}
