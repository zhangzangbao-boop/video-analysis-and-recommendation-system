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
  getRecommendVideos(userId, limit = 10, excludeVideoIds = null) {
    const params = { userId: userId || undefined, limit }
    if (excludeVideoIds) {
      params.excludeVideoIds = excludeVideoIds
    }
    return request({
      url: '/api/v1/video/recommend',
      method: 'get',
      params
    })
  },

  // 获取所有视频的总播放量
  getTotalPlayCount() {
    return request({
      url: '/api/v1/video/stats/total-play-count',
      method: 'get'
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
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000
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
  // 2. 核心互动
  // ==========================================

  likeVideo(videoId) {
    return request({ url: `/api/v1/video/${videoId}/like`, method: 'post' })
  },

  unlikeVideo(videoId) {
    return request({ url: `/api/v1/video/${videoId}/like`, method: 'delete' })
  },

  checkIsLiked(videoId) {
    return request({ url: `/api/v1/video/${videoId}/like/status`, method: 'get' })
  },

  collectVideo(videoId, folderId = 0) {
    return request({ url: `/api/v1/video/${videoId}/collect`, method: 'post', params: { folderId } })
  },

  uncollectVideo(videoId) {
    return request({ url: `/api/v1/video/${videoId}/collect`, method: 'delete' })
  },

  checkIsCollected(videoId) {
    return request({ url: `/api/v1/video/${videoId}/collect/status`, method: 'get' })
  },

  shareVideo(videoId) {
    return request({ url: `/api/v1/video/${videoId}/share`, method: 'post' })
  },

  reportVideo(data) {
    return request({ url: '/api/v1/video/report', method: 'post', data })
  },

  recordPlay(videoId, duration, progress, isFinish) {
    return request({
      url: `/api/v1/video/${videoId}/play`,
      method: 'post',
      data: { duration, progress, isFinish }
    })
  },

  // ==========================================
  // 3. 用户关系
  // ==========================================

  followUser(userId) {
    return request({ url: `/api/v1/user/follow/${userId}`, method: 'post' })
  },

  unfollowUser(userId) {
    return request({ url: `/api/v1/user/follow/${userId}`, method: 'delete' })
  },

  checkIsFollowed(userId) {
    return request({ url: `/api/v1/user/follow/${userId}/status`, method: 'get' })
  },

  getFollowingList() {
    return request({ url: '/api/v1/user/following', method: 'get' })
  },

  getFansList() {
    return request({ url: '/api/v1/user/fans', method: 'get' })
  },

  // ==========================================
  // 4. 个人中心数据
  // ==========================================

  // 获取播放历史
  getPlayHistory(pageSize = 20) {
    return request({
      url: '/api/v1/user/history',
      method: 'get',
      params: { page: 1, pageSize }
    })
  },

  // 【新增】删除单条播放历史
  deletePlayHistoryItem(videoId) {
    return request({
      url: `/api/v1/user/history/${videoId}`,
      method: 'delete'
    })
  },

  // 【新增】清空播放历史
  clearPlayHistory() {
    return request({
      url: '/api/v1/user/history',
      method: 'delete'
    })
  },

  // 【新增】通用上传文件 (用于头像)
  uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request({
      url: '/api/v1/user/upload/file', // 对应后端新增的上传接口
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 获取点赞的视频
  getLikedVideos(page = 1, pageSize = 20) {
    return request({
      url: '/api/v1/user/likes',
      method: 'get',
      params: { page, pageSize }
    })
  },

  // 【新增】获取收藏的视频 (需要后端支持 /api/v1/user/collects)
  // 如果后端暂无此接口，暂时复用 likes 接口模拟，或者您需要随后添加后端接口
  getCollectedVideos(page = 1, pageSize = 20) {
    return request({
      url: '/api/v1/user/collects', // 假设的接口路径
      method: 'get',
      params: { page, pageSize }
    })
  },

  // 获取我的作品
  getMyVideos(params) {
    const { page = 1, pageSize = 20 } = params || {}
    return request({
      url: '/api/v1/user/works',
      method: 'get',
      params: { page, pageSize }
    })
  },

  // 删除我的作品
  deleteMyVideo(videoId) {
    return request({
      url: `/api/v1/video/${videoId}`,
      method: 'delete'
    })
  },

  // ==========================================
  // 5. 评论相关
  // ==========================================

  addComment(data) {
    return request({
      url: `/api/v1/video/${data.videoId}/comment`,
      method: 'post',
      params: { content: data.content, parentId: data.parentId, replyUserId: data.replyUserId }
    })
  },

  getComments(videoId, limit = 20) {
    return request({ url: `/api/v1/video/${videoId}/comment`, method: 'get', params: { limit } })
  },

  deleteComment(commentId) {
    return request({ url: `/api/v1/video/comment/${commentId}`, method: 'delete' })
  },

  getMyComments(page = 1, pageSize = 20) {
    return request({ url: '/api/v1/user/comments', method: 'get', params: { page, pageSize } })
  },

  // ==========================================
  // 6. 用户信息与设置
  // ==========================================

  getCurrentUser() {
    return request({ url: '/api/v1/user/profile', method: 'get' })
  },

  updateProfile(data) {
    return request({ url: '/api/v1/user/profile', method: 'put', data })
  },

  changePassword(oldPassword, newPassword) {
    return request({
      url: '/api/v1/user/password',
      method: 'put',
      data: { oldPassword, newPassword }
    })
  },

  // ==========================================
  // 7. 搜索
  // ==========================================

  searchVideos(keyword, categoryId = null, page = 1, pageSize = 20) {
    return request({
      url: '/api/v1/video/search',
      method: 'get',
      params: { keyword, categoryId, page, pageSize }
    })
  },

  // ==========================================
  // 8. 消息通知
  // ==========================================

  // 获取消息列表
  getMessageList(params = {}) {
    return request({
      url: '/api/v1/user/message/list',
      method: 'get',
      params
    })
  },

  // 获取未读消息数
  getUnreadMessageCount() {
    return request({
      url: '/api/v1/user/message/unread-count',
      method: 'get'
    })
  },

  // 标记消息为已读
  markMessageAsRead(messageId) {
    return request({
      url: `/api/v1/user/message/${messageId}/read`,
      method: 'put'
    })
  },

  // 一键标记所有消息为已读
  markAllMessagesAsRead() {
    return request({
      url: '/api/v1/user/message/read-all',
      method: 'put'
    })
  }
}