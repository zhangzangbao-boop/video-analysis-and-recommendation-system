import request from '@/utils/request'

export const commentApi = {
    // 获取评论列表
    getComments(videoId, params) {
        return request({
            url: `/api/v1/video/${videoId}/comment`,
            method: 'get',
            params
        })
    },

    // 获取子回复
    getReplies(parentId, params) {
        return request({
            url: `/api/v1/video/comment/${parentId}/replies`,
            method: 'get',
            params
        })
    },

    // 发表评论
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

    // 点赞评论
    likeComment(commentId) {
        return request({
            url: `/api/v1/video/comment/${commentId}/like`,
            method: 'post'
        })
    },

    // 【新增】取消点赞评论 (之前缺失这个导致报错)
    unlikeComment(commentId) {
        return request({
            url: `/api/v1/video/comment/${commentId}/like`,
            method: 'delete'
        })
    },

    // 删除评论
    deleteComment(commentId) {
        return request({
            url: `/api/v1/video/comment/${commentId}`,
            method: 'delete'
        })
    }
}