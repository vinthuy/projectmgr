import request from '@/utils/request'

export function getWorkflowSchemes() {
  return request({
    url: '/api/v1/workflow-schemes',
    method: 'get'
  })
}

export function getWorkflowSchemeDetail(id) {
  return request({
    url: `/api/v1/workflow-schemes/${id}`,
    method: 'get'
  })
}

export function createWorkflowScheme(data) {
  return request({
    url: '/api/v1/workflow-schemes',
    method: 'post',
    data
  })
}

export function updateWorkflowScheme(id, data) {
  return request({
    url: `/api/v1/workflow-schemes/${id}`,
    method: 'put',
    data
  })
}

export function deleteWorkflowScheme(id) {
  return request({
    url: `/api/v1/workflow-schemes/${id}`,
    method: 'delete'
  })
}

export function batchUpdateWorkflowSchemeMappings(schemeId, data) {
  return request({
    url: `/api/v1/workflow-schemes/${schemeId}/mappings/batch`,
    method: 'put',
    data
  })
}
