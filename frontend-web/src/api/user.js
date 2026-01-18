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
  },

  // 上传视频
  uploadVideo(formData) {
    return request({
      url: '/api/v1/video/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 300000 // 5分钟超时
    })
  },

  // 获取视频分类列表
  getCategories() {
    return request({
      url: '/api/v1/video/categories',
      method: 'get'
    })
  },

  // --- 新增接口 ---

  // 获取我的视频列表（包含审核状态）
  getMyVideos(params) {
    return request({
      url: '/api/v1/video/my', // 请确保后端 VideoController 有对应接口，或复用列表接口
      method: 'get',
      params
    })
  },

  // 删除我的视频
  deleteMyVideo(id) {
    return request({
      url: `/api/v1/video/${id}`,
      method: 'delete'
    })
  }
}