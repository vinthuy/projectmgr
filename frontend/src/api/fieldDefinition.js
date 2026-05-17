import request from '@/utils/request'

export function getFieldDefinitions(tenantId = 1) {
  return request({
    url: '/field-definitions',
    method: 'get',
    params: { tenantId }
  })
}

export function createFieldDefinition(data, tenantId = 1) {
  return request({
    url: `/field-definitions?tenantId=${tenantId}`,
    method: 'post',
    data
  })
}

export function updateFieldDefinition(id, data) {
  return request({
    url: `/field-definitions/${id}`,
    method: 'put',
    data
  })
}

export function deleteFieldDefinition(id) {
  return request({
    url: `/field-definitions/${id}`,
    method: 'delete'
  })
}
