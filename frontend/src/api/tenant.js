import request from '@/utils/request'

export function getTenants() {
  return request({
    url: '/tenants',
    method: 'get'
  })
}

export function getTenant(id) {
  return request({
    url: `/tenants/${id}`,
    method: 'get'
  })
}

export function createTenant(data) {
  return request({
    url: '/tenants',
    method: 'post',
    data
  })
}

export function updateTenant(id, data) {
  return request({
    url: `/tenants/${id}`,
    method: 'put',
    data
  })
}

export function deleteTenant(id) {
  return request({
    url: `/tenants/${id}`,
    method: 'delete'
  })
}
