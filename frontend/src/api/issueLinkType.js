import request from '@/utils/request'

// 获取所有关系类型
export const getIssueLinkTypes = (tenantId = 1) => {
  return request({
    url: '/link-types',
    method: 'get',
    params: { tenantId }
  })
}

// 根据ID获取关系类型
export const getIssueLinkTypeById = (id) => {
  return request({
    url: `/link-types/${id}`,
    method: 'get'
  })
}

// 创建关系类型
export const createIssueLinkType = (data, tenantId = 1) => {
  return request({
    url: '/link-types',
    method: 'post',
    params: { tenantId },
    data
  })
}

// 更新关系类型
export const updateIssueLinkType = (id, data) => {
  return request({
    url: `/link-types/${id}`,
    method: 'put',
    data
  })
}

// 删除关系类型
export const deleteIssueLinkType = (id) => {
  return request({
    url: `/link-types/${id}`,
    method: 'delete'
  })
}
