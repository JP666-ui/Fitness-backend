import request from '@/utils/request'

export function getExerciseList() {
  return request({
    url: '/exercise/list',
    method: 'get',
  })
}

export function addExercise(data) {
  return request({
    url: '/exercise/add',
    method: 'post',
    data,
  })
}

export function updateExercise(id, data) {
  return request({
    url: `/exercise/update/${id}`,
    method: 'put',
    data,
  })
}

export function deleteExercise(id) {
  return request({
    url: `/exercise/${id}`,
    method: 'delete',
  })
}

export function getTodayCalorie() {
  return request({
    url: '/exercise/today',
    method: 'get',
  })
}

export function getExerciseStatistics(params) {
  return request({
    url: '/exercise/statistics',
    method: 'get',
    params,
  })
}

export function calculateCalorie(data) {
  return request({
    url: '/exercise/calculateCalorie',
    method: 'post',
    data,
  })
}
