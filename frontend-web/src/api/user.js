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
  },

  // ========== 播放历史相关 ==========
  
  // 记录播放历史
  recordPlay(videoId, duration = 0, progress = 0, isFinish = false) {
    return request({
      url: `/api/v1/video/${videoId}/play`,
      method: 'post',
      params: {
        duration,
        progress,
        isFinish
      }
    })
  },

  // 获取播放历史
  getPlayHistory(limit = 20) {
    return request({
      url: '/api/v1/video/history',
      method: 'get',
      params: { limit }
    })
  },

  // ========== 点赞相关 ==========
  
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

  // 检查是否已点赞
  checkIsLiked(videoId) {
    return request({
      url: `/api/v1/video/${videoId}/like`,
      method: 'get'
    })
  },

  // 获取点赞列表
  getLikedVideos(page = 1, pageSize = 20) {
    return request({
      url: '/api/v1/user/likes',
      method: 'get',
      params: { page, pageSize }
    })
  },

  // ========== 评论相关 ==========
  
  // 添加评论
  addComment(videoId, content, parentId = null, replyUserId = null) {
    return request({
      url: `/api/v1/video/${videoId}/comment`,
      method: 'post',
      params: {
        content,
        parentId,
        replyUserId
      }
    })
  },

  // 获取视频评论列表
  getComments(videoId, limit = 20) {
    return request({
      url: `/api/v1/video/${videoId}/comment`,
      method: 'get',
      params: { limit }
    })
  },

  // 获取评论的回复列表
  getCommentReplies(parentId, limit = 10) {
    return request({
      url: `/api/v1/video/comment/${parentId}/replies`,
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

  // ========== 用户信息相关 ==========
  
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

  // ========== 搜索相关 ==========
  
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
  },

  // ========== 关注/粉丝相关 ==========
  
  // 获取关注列表（我关注的人）
  getFollowingList() {
    return request({
      url: '/api/v1/user/following',
      method: 'get'
    })
  },

  // 获取粉丝列表（关注我的人）
  getFansList() {
    return request({
      url: '/api/v1/user/fans',
      method: 'get'
    })
  }
}