import request from '@/utils/request'

/**
 * 识别食物
 * @param {string} imageBase64 - 图片的base64编码
 */
export function recognizeFood(imageBase64) {
  return request({
    url: '/food/recognize',
    method: 'post',
    data: { image: imageBase64 },
  })
}

/**
 * 上传图片文件
 * @param {File} file - 图片文件
 */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
