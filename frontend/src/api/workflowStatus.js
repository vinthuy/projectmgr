import request from '@/utils/request'

export function getWorkflowStatuses(tenantId = 1) {
  return request({
    url: '/workflow-statuses',
    method: 'get',
    params: { tenantId }
  })
}

export function getWorkflowStatusById(id) {
  return request({
    url: `/workflow-statuses/${id}`,
    method: 'get'
  })
}

export function getWorkflowStatusByCode(statusCode, tenantId = 1) {
  return request({
    url: `/workflow-statuses/code/${statusCode}`,
    method: 'get',
    params: { tenantId }
  })
}

export function getWorkflowStatusesByCategory(category, tenantId = 1) {
  return request({
    url: `/workflow-statuses/by-category/${category}`,
    method: 'get',
    params: { tenantId }
  })
}

export function createWorkflowStatus(data, tenantId = 1) {
  return request({
    url: '/workflow-statuses',
    method: 'post',
    params: { tenantId },
    data
  })
}

export function updateWorkflowStatus(id, data) {
  return request({
    url: `/workflow-statuses/${id}`,
    method: 'put',
    data
  })
}

export function deleteWorkflowStatus(id) {
  return request({
    url: `/workflow-statuses/${id}`,
    method: 'delete'
  })
}
