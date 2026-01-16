import request from '@/utils/request'

/**
 * 用户端视频API
 */
export const userVideoApi = {
  // 获取热门视频列表
  getHotVideos() {
    return request({
      url: '/api/v1/video/hot',
      method: 'get'
    })
  },
  
  // 获取推荐视频列表
  getRecommendVideos(userId, limit = 10) {
    return request({
      url: '/api/v1/video/recommend',
      method: 'get',
      params: {
        userId: userId || undefined,
        limit
      }
    })
  },
  
  // 获取视频详情
  getVideoById(id) {
    return request({
      url: `/api/v1/video/${id}`,
      method: 'get'
    })
  }
}
