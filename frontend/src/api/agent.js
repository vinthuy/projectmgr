import request from '@/utils/request'

/**
 * 发送消息获取AI回复
 */
export const sendMessage = (data) => {
  return request({
    url: '/agent/chat',
    method: 'post',
    data
  })
}

/**
 * 获取会话列表
 */
export const getSessions = () => {
  return request({
    url: '/agent/sessions',
    method: 'get'
  })
}

/**
 * 删除会话
 */
export const deleteSession = (sessionId) => {
  return request({
    url: `/agent/sessions/${sessionId}`,
    method: 'delete'
  })
}

/**
 * 获取消息历史
 */
export const getMessageHistory = (sessionId) => {
  return request({
    url: `/agent/sessions/${sessionId}/messages`,
    method: 'get'
  })
}
