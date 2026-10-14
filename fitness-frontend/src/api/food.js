import request from '@/utils/request'

export function getFoodList() {
  return request({
    url: '/foodRecord/list',
    method: 'get',
  })
}

export function addFood(data) {
  return request({
    url: '/foodRecord/add',
    method: 'post',
    data,
  })
}

export function deleteFood(id) {
  return request({
    url: `/foodRecord/${id}`,
    method: 'delete',
  })
}

export function recognizeFood(data) {
  return request({
    url: '/foodRecord/recognize',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
