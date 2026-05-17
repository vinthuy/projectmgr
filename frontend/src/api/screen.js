import request from '@/utils/request'

/**
 * 获取所有Screen列表
 */
export const getScreens = () => {
  return request({
    url: '/screens',
    method: 'get'
  })
}

/**
 * 获取Screen详情
 */
export const getScreenById = (id) => {
  return request({
    url: `/screens/${id}`,
    method: 'get'
  })
}

/**
 * 创建Screen
 */
export const createScreen = (data) => {
  return request({
    url: '/screens',
    method: 'post',
    data
  })
}

/**
 * 更新Screen
 */
export const updateScreen = (id, data) => {
  return request({
    url: `/screens/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除Screen
 */
export const deleteScreen = (id) => {
  return request({
    url: `/screens/${id}`,
    method: 'delete'
  })
}

/**
 * 添加Tab
 */
export const addTab = (screenId, tabName) => {
  return request({
    url: `/screens/${screenId}/tabs`,
    method: 'post',
    data: { tabName }
  })
}

/**
 * 更新Tab
 */
export const updateTab = (tabId, tabName) => {
  return request({
    url: `/screens/tabs/${tabId}`,
    method: 'put',
    data: { tabName }
  })
}

/**
 * 删除Tab
 */
export const deleteTab = (tabId) => {
  return request({
    url: `/screens/tabs/${tabId}`,
    method: 'delete'
  })
}

/**
 * 调整Tab顺序
 */
export const reorderTabs = (screenId, tabIds) => {
  return request({
    url: `/screens/${screenId}/tabs/reorder`,
    method: 'put',
    data: tabIds
  })
}

/**
 * 添加字段到Screen
 */
export const addFieldToScreen = (screenId, data) => {
  return request({
    url: `/screens/${screenId}/items`,
    method: 'post',
    data
  })
}

/**
 * 从Screen移除字段
 */
export const removeFieldFromScreen = (itemId) => {
  return request({
    url: `/screens/items/${itemId}`,
    method: 'delete'
  })
}

/**
 * 调整字段顺序
 */
export const reorderFields = (screenId, itemIds) => {
  return request({
    url: `/screens/${screenId}/items/reorder`,
    method: 'put',
    data: itemIds
  })
}

/**
 * 获取可用字段列表（用于添加到Screen）
 */
export const getAvailableFields = () => {
  return request({
    url: '/field-definitions',
    method: 'get'
  })
}
