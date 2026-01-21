import request from '@/utils/request'

/**
 * 认证API
 */
export const authApi = {
  // 用户登录
  login(username, password) {
    return request({
      url: '/api/auth/login',
      method: 'post',
      data: {
        username,
        password
      }
    })
  }
}

/**
 * 用户管理API
 */
export const userApi = {
  // 获取用户列表
  getUserList(params) {
    return request({
      url: '/api/admin/user/list',
      method: 'get',
      params
    })
  },
  
  // 获取用户详情
  getUserById(id) {
    return request({
      url: `/api/admin/user/${id}`,
      method: 'get'
    })
  },
  
  // 更新用户状态
  updateUserStatus(id, status) {
    // 直接传递状态字符串，后端会自动转换
    return request({
      url: `/api/admin/user/${id}/status`,
      method: 'put',
      params: { statusStr: status }
    })
  },
  
  // 重置用户密码（管理员操作，重置为默认密码 123456）
  resetPassword(id) {
    return request({
      url: `/api/admin/user/reset-password/${id}`,
      method: 'post'
    })
  },
  
  // 创建用户
  createUser(data) {
    return request({
      url: '/api/admin/user',
      method: 'post',
      data
    })
  }
}

/**
 * 统计API
 */
export const statsApi = {
  // 获取统计数据
  getStats(dateRange = 'week') {
    return request({
      url: '/api/admin/stats',
      method: 'get',
      params: { dateRange }
    })
  }
}

/**
 * 日志API
 */
export const logsApi = {
  // 获取日志列表
  getLogList(params) {
    return request({
      url: '/api/admin/logs/list',
      method: 'get',
      params
    })
  }
}

/**
 * 视频管理API
 */
export const videoApi = {
  // 获取视频列表
  getVideoList(params) {
    return request({
      url: '/api/admin/video/list',
      method: 'get',
      params
    })
  },

  // 获取视频详情
  getVideoById(id) {
    return request({
      url: `/api/admin/video/${id}`,
      method: 'get'
    })
  },

  // 审核视频
  // data结构: { videoId: Long, action: 'pass'|'reject', reason: String }
  auditVideo(data) {
    return request({
      url: '/api/admin/video/audit',
      method: 'post',
      data
    })
  },

  // 批量操作视频
  batchOperateVideos(data) {
    return request({
      url: '/api/admin/video/batch',
      method: 'post',
      data
    })
  },

  // 删除/下架视频
  deleteVideo(id) {
    return request({
      url: `/api/admin/video/${id}`,
      method: 'delete'
    })
  },

  // 设置/取消热门
  setHot(id, isHot) {
    return request({
      url: `/api/admin/video/${id}/hot`,
      method: 'put',
      params: { isHot }
    })
  }
}