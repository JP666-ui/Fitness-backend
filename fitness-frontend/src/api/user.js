import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data,
  })
}

export function register(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data,
  })
}

export function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'get',
  })
}

export function updateUserInfo(data) {
  return request({
    url: '/user/update',
    method: 'put',
    data,
  })
}

export function updateWeight(data) {
  return request({
    url: '/user/weight',
    method: 'put',
    data,
  })
}

export function getHealthData() {
  return request({
    url: '/user/health',
    method: 'get',
  })
}
