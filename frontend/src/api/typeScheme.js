import request from '@/utils/request'

export function getTypeSchemes() {
  return request({
    url: '/api/v1/type-schemes',
    method: 'get'
  })
}

export function getTypeSchemeDetail(id) {
  return request({
    url: `/api/v1/type-schemes/${id}`,
    method: 'get'
  })
}

export function createTypeScheme(data) {
  return request({
    url: '/api/v1/type-schemes',
    method: 'post',
    data
  })
}

export function updateTypeScheme(id, data) {
  return request({
    url: `/api/v1/type-schemes/${id}`,
    method: 'put',
    data
  })
}

export function deleteTypeScheme(id) {
  return request({
    url: `/api/v1/type-schemes/${id}`,
    method: 'delete'
  })
}

export function batchUpdateTypeSchemeMappings(schemeId, data) {
  return request({
    url: `/api/v1/type-schemes/${schemeId}/mappings/batch`,
    method: 'put',
    data
  })
}
