import request from '@/utils/request'

export function getWorkItemTypes(tenantId = 1) {
  return request({
    url: '/api/v1/work-item-types',
    method: 'get',
    params: { tenantId }
  })
}

export function getWorkItemType(typeCode, tenantId = 1) {
  return request({
    url: `/work-item-types/${typeCode}`,
    method: 'get',
    params: { tenantId }
  })
}

export function createWorkItemType(data, tenantId = 1) {
  return request({
    url: '/work-item-types',
    method: 'post',
    params: { tenantId },
    data
  })
}

export function updateWorkItemType(id, data) {
  return request({
    url: `/work-item-types/${id}`,
    method: 'put',
    data
  })
}

export function deleteWorkItemType(id) {
  return request({
    url: `/work-item-types/${id}`,
    method: 'delete'
  })
}
