import request from '@/utils/request'

/**
 * 获取屏幕方案列表
 */
export function getScreenSchemes() {
  return request({
    url: '/api/v1/screen-schemes',
    method: 'get'
  })
}

/**
 * 获取屏幕方案详情
 */
export function getScreenSchemeDetail(id) {
  return request({
    url: `/api/v1/screen-schemes/${id}`,
    method: 'get'
  })
}

/**
 * 创建屏幕方案
 */
export function createScreenScheme(data) {
  return request({
    url: '/api/v1/screen-schemes',
    method: 'post',
    data
  })
}

/**
 * 更新屏幕方案
 */
export function updateScreenScheme(id, data) {
  return request({
    url: `/api/v1/screen-schemes/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除屏幕方案
 */
export function deleteScreenScheme(id) {
  return request({
    url: `/api/v1/screen-schemes/${id}`,
    method: 'delete'
  })
}

/**
 * 添加类型映射
 */
export function addScreenSchemeMapping(schemeId, data) {
  return request({
    url: `/api/v1/screen-schemes/${schemeId}/mappings`,
    method: 'post',
    data
  })
}

/**
 * 批量更新类型映射
 */
export function batchUpdateScreenSchemeMappings(schemeId, data) {
  return request({
    url: `/api/v1/screen-schemes/${schemeId}/mappings/batch`,
    method: 'put',
    data
  })
}

/**
 * 删除类型映射
 */
export function deleteScreenSchemeMapping(mappingId) {
  return request({
    url: `/api/v1/screen-schemes/mappings/${mappingId}`,
    method: 'delete'
  })
}
