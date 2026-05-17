import request from '@/utils/request'

/**
 * 获取所有Issue Type Screen映射
 */
export const getIssueTypeScreens = () => {
  return request({
    url: '/issue-type-screens',
    method: 'get'
  })
}

/**
 * 获取Issue Type的Screen
 */
export const getScreensForIssueType = (typeId, operationType) => {
  return request({
    url: `/issue-type-screens/issue-types/${typeId}`,
    method: 'get',
    params: { operationType }
  })
}

/**
 * 创建映射
 */
export const createMapping = (data) => {
  return request({
    url: '/issue-type-screens',
    method: 'post',
    data
  })
}

/**
 * 更新映射
 */
export const updateMapping = (id, data) => {
  return request({
    url: `/issue-type-screens/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除映射
 */
export const deleteMapping = (id) => {
  return request({
    url: `/issue-type-screens/${id}`,
    method: 'delete'
  })
}
