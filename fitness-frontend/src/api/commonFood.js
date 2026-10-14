import request from '@/utils/request'

/**
 * 获取常见食物列表
 * @param {Object} params - {category, keyword}
 */
export function getCommonFoods(params) {
  return request({
    url: '/common-food/list',
    method: 'get',
    params,
  })
}

/**
 * 获取食物分类
 */
export function getFoodCategories() {
  return request({
    url: '/common-food/categories',
    method: 'get',
  })
}

/**
 * 按名称查询食物热量
 * @param {string} name - 食物名称
 */
export function getFoodCalorie(name) {
  return request({
    url: '/common-food/calorie',
    method: 'get',
    params: { name },
  })
}
