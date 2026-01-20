import request from '@/utils/request'

/**
 * 用户端视频API
 * 包含：视频基础、互动（点赞/收藏/分享）、评论、用户关系、个人信息
 */
export const userVideoApi = {

  // ==========================================
  // 1. 视频基础浏览
  // ==========================================

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

  // ==========================================
  // 2. 核心互动 (点赞/收藏/分享/举报) 【新增部分】
  // ==========================================

  // 点赞视频
  likeVideo(videoId) {
    return request({
      url: `/api/v1/video/${videoId}/like`,
      method: 'post'
    })
  },

  // 取消点赞
  unlikeVideo(videoId) {
    return request({
      url: `/api/v1/video/${videoId}/like`,
      method: 'delete'
    })
  },

  // 检查点赞状态
  checkIsLiked(videoId) {
    return request({
      url: `/api/v1/video/${videoId}/like/status`,
      method: 'get'
    })
  },

  // 收藏视频
  collectVideo(videoId, folderId = 0) {
    return request({
      url: `/api/v1/video/${videoId}/collect`,
      method: 'post',
      params: { folderId }
    })
  },

  // 取消收藏
  uncollectVideo(videoId) {
    return request({
      url: `/api/v1/video/${videoId}/collect`,
      method: 'delete'
    })
  },

  // 检查收藏状态
  checkIsCollected(videoId) {
    return request({
      url: `/api/v1/video/${videoId}/collect/status`,
      method: 'get'
    })
  },

  // 分享上报 (增加分享数)
  shareVideo(videoId) {
    return request({
      url: `/api/v1/video/${videoId}/share`,
      method: 'post'
    })
  },

  // 举报视频
  reportVideo(data) {
    return request({
      url: '/api/v1/video/report',
      method: 'post',
      data // { videoId, reason }
    })
  },

  // 记录播放历史 (埋点)
  recordPlay(videoId, duration, progress, isFinish) {
    return request({
      url: `/api/v1/video/${videoId}/play`,
      method: 'post',
      data: {
        duration,
        progress,
        isFinish
      }
    })
  },

  // ==========================================
  // 3. 用户关系 (关注/取关) 【新增部分】
  // ==========================================

  // 关注用户
  followUser(userId) {
    return request({
      url: `/api/v1/user/follow/${userId}`,
      method: 'post'
    })
  },

  // 取消关注
  unfollowUser(userId) {
    return request({
      url: `/api/v1/user/follow/${userId}`,
      method: 'delete'
    })
  },

  // 检查关注状态
  checkIsFollowed(userId) {
    return request({
      url: `/api/v1/user/follow/${userId}/status`,
      method: 'get'
    })
  },

  // ==========================================
  // 4. 评论相关 (保留原有兼容，新版建议用 comment.js)
  // ==========================================

  // 添加评论
  addComment(videoId, content, parentId = null) {
    return request({
      url: `/api/v1/video/${videoId}/comment`,
      method: 'post',
      params: {
        content,
        parentId
      }
    })
  },

  // 获取视频评论
  getComments(videoId, limit = 20) {
    return request({
      url: `/api/v1/video/${videoId}/comment`,
      method: 'get',
      params: { limit }
    })
  },

  // 删除评论
  deleteComment(commentId) {
    return request({
      url: `/api/v1/video/comment/${commentId}`,
      method: 'delete'
    })
  },

  // 获取用户的评论列表
  getMyComments(page = 1, pageSize = 20) {
    return request({
      url: '/api/v1/user/comments',
      method: 'get',
      params: { page, pageSize }
    })
  },

  // ==========================================
  // 5. 用户信息与设置
  // ==========================================

  // 获取当前用户信息
  getCurrentUser() {
    return request({
      url: '/api/v1/user/profile',
      method: 'get'
    })
  },

  // 更新用户信息
  updateProfile(data) {
    return request({
      url: '/api/v1/user/profile',
      method: 'put',
      data
    })
  },

  // 修改密码
  changePassword(oldPassword, newPassword) {
    return request({
      url: '/api/v1/user/password',
      method: 'put',
      data: {
        oldPassword,
        newPassword
      }
    })
  },

  // ==========================================
  // 6. 搜索
  // ==========================================

  // 搜索视频
  searchVideos(keyword, categoryId = null, page = 1, pageSize = 20) {
    return request({
      url: '/api/v1/video/search',
      method: 'get',
      params: {
        keyword,
        categoryId,
        page,
        pageSize
      }
    })
  }
}