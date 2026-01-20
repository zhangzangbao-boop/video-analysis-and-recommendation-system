import request from '@/utils/request'

export const commentApi = {
    /**
     * 获取视频的一级评论列表
     * @param {Long} videoId
     * @param {Object} params { limit, sort: 'hot'|'time' }
     */
    getComments(videoId, params) {
        return request({
            url: `/api/v1/video/${videoId}/comment`,
            method: 'get',
            params
        })
    },

    /**
     * 获取某条评论的子回复
     * @param {Long} parentId
     */
    getReplies(parentId, params) {
        return request({
            url: `/api/v1/video/comment/${parentId}/replies`,
            method: 'get',
            params
        })
    },

    /**
     * 发表评论或回复
     * 注意：后端使用 @RequestParam 接收，所以这里用 params 传参
     */
    addComment(data) {
        return request({
            url: `/api/v1/video/${data.videoId}/comment`,
            method: 'post',
            params: {
                content: data.content,
                parentId: data.parentId,
                replyUserId: data.replyUserId
            }
        })
    },

    /**
     * 点赞评论
     */
    likeComment(commentId) {
        return request({
            url: `/api/v1/video/comment/${commentId}/like`,
            method: 'post'
        })
    },

    /**
     * 删除评论
     */
    deleteComment(commentId) {
        return request({
            url: `/api/v1/video/comment/${commentId}`,
            method: 'delete'
        })
    }
}