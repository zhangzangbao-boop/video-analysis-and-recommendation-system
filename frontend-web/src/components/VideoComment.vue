<template>
  <div class="video-comment-module">
    <div class="comment-header">
      <div class="header-left">
        <span class="title">评论</span>
        <span class="count">{{ totalCount }}</span>
      </div>
      <div class="header-right">
        <span
            v-for="type in sortTypes"
            :key="type.value"
            :class="['sort-item', { active: currentSort === type.value }]"
            @click="handleSortChange(type.value)"
        >
          {{ type.label }}
        </span>
      </div>
    </div>

    <div class="comment-input-area">
      <div class="avatar-col">
        <el-avatar :size="40" :src="currentUser.avatarUrl || defaultAvatar"></el-avatar>
      </div>
      <div class="input-col">
        <textarea
            v-model="mainContent"
            class="custom-textarea"
            placeholder="发一条友善的评论"
            rows="1"
            @input="autoResize"
        ></textarea>
        <div class="action-bar" :class="{ visible: mainContent.length > 0 }">
          <el-button
              type="primary"
              size="small"
              :disabled="!mainContent.trim()"
              :loading="submitting"
              @click="submitComment(0)"
          >
            发布
          </el-button>
        </div>
      </div>
    </div>

    <div class="comment-list" v-loading="loading">
      <div v-for="item in commentList" :key="item.id" class="comment-item">
        <div class="item-avatar">
          <el-avatar :size="40" :src="item.avatarUrl || defaultAvatar"></el-avatar>
        </div>

        <div class="item-content-wrapper">
          <div class="user-info-row">
            <span class="username">{{ item.nickname }}</span>
            <span v-if="item.userId === videoAuthorId" class="tag-up">UP主</span>
            <span v-if="item.userId === currentUser.id" class="tag-me">我</span>
            <span class="time">{{ formatTime(item.createTime) }}</span>
          </div>

          <div class="content-row">
            {{ item.content }}
          </div>

          <div class="action-row">
            <div
                class="action-btn"
                :class="{ active: item.isLiked }"
                @click="handleLike(item)"
            >
              <i class="el-icon-thumb"></i>
              <span>{{ item.likeCount || '点赞' }}</span>
            </div>

            <div
                class="action-btn"
                :class="{ active: activeReplyId === item.id }"
                @click="toggleReplyBox(item.id)"
            >
              <i class="el-icon-chat-round"></i>
              <span>{{ item.replyCount || '回复' }}</span>
            </div>

            <div class="action-btn" @click="handleReport(item)">
              <i class="el-icon-warning-outline"></i>
            </div>

            <el-dropdown trigger="click" @command="handleCommand($event, item)">
              <div class="action-btn">
                <i class="el-icon-more"></i>
              </div>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="copy">复制</el-dropdown-item>
                <el-dropdown-item v-if="item.userId === currentUser.id" command="delete" style="color:red">删除</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>

          <div v-if="activeReplyId === item.id || (item.replies && item.replies.length > 0)" class="reply-section">

            <div v-if="activeReplyId === item.id" class="sub-reply-input fade-in">
              <el-avatar :size="24" :src="currentUser.avatarUrl || defaultAvatar"></el-avatar>
              <div class="sub-input-wrapper">
                <textarea
                    :ref="'replyInput' + item.id"
                    v-model="replyContent"
                    class="sub-textarea"
                    :placeholder="`回复 @${replyTargetUser || item.nickname}：`"
                ></textarea>
                <el-button
                    type="primary"
                    size="mini"
                    :disabled="!replyContent.trim()"
                    @click="submitComment(item.id, item)"
                >
                  发送
                </el-button>
              </div>
            </div>

            <div v-if="item.replies && item.replies.length > 0" class="sub-reply-list">
              <div v-for="reply in item.replies" :key="reply.id" class="sub-reply-item">
                <el-avatar :size="24" :src="reply.avatarUrl || defaultAvatar"></el-avatar>
                <div class="sub-content">
                  <span class="sub-user">{{ reply.nickname }}</span>
                  <span v-if="reply.replyNickname" class="reply-target"> 回复 <span class="at">@{{ reply.replyNickname }}</span></span>
                  <span class="sub-text">：{{ reply.content }}</span>
                  <div class="sub-footer">
                    <span class="sub-time">{{ formatTime(reply.createTime) }}</span>
                    <div class="sub-like" @click="handleLike(reply)">
                      <i class="el-icon-thumb"></i> {{ reply.likeCount || '' }}
                    </div>
                    <span class="sub-reply-btn" @click="prepareSubReply(item.id, reply)">回复</span>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
      </div>

      <div class="load-more" v-if="hasMore" @click="loadMore">
        加载更多评论
      </div>
      <div class="no-more" v-else>
        没有更多评论了
      </div>
    </div>
  </div>
</template>

<script>
import { commentApi } from '@/api/comment'

export default {
  name: 'VideoComment',
  props: {
    videoId: {
      type: [String, Number],
      required: true
    },
    videoAuthorId: {
      type: [String, Number],
      default: null
    }
  },
  data() {
    return {
      currentUser: {},
      defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',

      commentList: [],
      totalCount: 0,
      loading: false,
      submitting: false,
      hasMore: false,

      currentSort: 'hot',
      sortTypes: [
        { label: '按热度', value: 'hot' },
        { label: '按时间', value: 'time' }
      ],

      mainContent: '',
      replyContent: '',

      activeReplyId: null,
      replyTargetUser: null,
      replyTargetUserId: null,

      pageLimit: 20
    }
  },
  created() {
    this.initUser()
    this.fetchComments()
  },
  methods: {
    // 【修改核心】：适配您项目的 localStorage 存储格式
    initUser() {
      try {
        // 1. 检查 Token 是否存在
        const token = localStorage.getItem('userToken')
        // 2. 获取分散存储的用户信息
        const userId = localStorage.getItem('userId')
        const username = localStorage.getItem('username')
        const avatar = localStorage.getItem('userAvatar')

        if (token && userId) {
          this.currentUser = {
            id: parseInt(userId), // 确保转为数字，方便比较
            nickname: username || `用户${userId}`,
            avatarUrl: avatar
          }
        } else {
          this.currentUser = {}
        }
      } catch (e) {
        console.error('获取用户信息失败', e)
      }
    },

    async fetchComments() {
      if (!this.videoId) return
      this.loading = true
      try {
        const res = await commentApi.getComments(this.videoId, {
          limit: this.pageLimit,
          sort: this.currentSort
        })
        if (res.code === 200) {
          this.commentList = res.data.map(item => ({
            ...item,
            replies: item.replies || []
          }))
          this.totalCount = this.commentList.length
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },

    handleSortChange(type) {
      if (this.currentSort === type) return
      this.currentSort = type
      this.fetchComments()
    },

    async submitComment(parentId, parentItem = null) {
      // 校验：依赖上面 initUser 解析出的 id
      if (!this.currentUser.id) {
        this.$message.warning('请先登录后再评论')
        // 记录当前页面路径，登录后跳回（可选优化）
        // this.$router.push(`/login?redirect=${this.$route.fullPath}`)
        this.$router.push('/login')
        return
      }

      const content = parentId === 0 ? this.mainContent : this.replyContent
      const replyUserId = parentId === 0 ? null : this.replyTargetUserId

      if (!content.trim()) return

      this.submitting = true
      try {
        const res = await commentApi.addComment({
          videoId: this.videoId,
          content: content,
          parentId: parentId === 0 ? 0 : parentId,
          replyUserId: replyUserId
        })

        if (res.code === 200) {
          // 这里返回的数据如果已经是 DTO，直接使用
          // 如果后端返回的 data 里没有 nickname，我们需要手动补全当前用户的信息以便立即展示
          const newComment = {
            ...res.data,
            // 兜底：如果后端 DTO 没返回用户信息，用本地的补上，实现“即时反馈”
            nickname: res.data.nickname || this.currentUser.nickname,
            avatarUrl: res.data.avatarUrl || this.currentUser.avatarUrl
          }

          this.$message.success('发送成功')

          if (parentId === 0) {
            this.commentList.unshift(newComment)
            this.mainContent = ''
            this.totalCount++
          } else {
            if (parentItem) {
              if (!parentItem.replies) this.$set(parentItem, 'replies', [])
              parentItem.replies.push(newComment)
              parentItem.replyCount = (parentItem.replyCount || 0) + 1
            }
            this.replyContent = ''
            this.activeReplyId = null
          }
        } else {
          this.$message.error(res.message || '发送失败')
        }
      } catch (error) {
        this.$message.error('网络异常或服务未启动')
        console.error(error)
      } finally {
        this.submitting = false
      }
    },

    toggleReplyBox(commentId) {
      if (this.activeReplyId === commentId) {
        this.activeReplyId = null
      } else {
        this.activeReplyId = commentId
        this.replyContent = ''
        this.replyTargetUser = null
        this.replyTargetUserId = null
        this.$nextTick(() => {
          const ref = this.$refs['replyInput' + commentId]
          if (ref && ref[0]) ref[0].focus()
        })
      }
    },

    prepareSubReply(parentId, replyItem) {
      this.activeReplyId = parentId
      this.replyTargetUser = replyItem.nickname
      this.replyTargetUserId = replyItem.userId
      this.replyContent = ''
      this.$nextTick(() => {
        const ref = this.$refs['replyInput' + parentId]
        if (ref && ref[0]) ref[0].focus()
      })
    },

    async handleLike(item) {
      if (!this.currentUser.id) {
        this.$message.warning('请登录')
        return
      }
      const originalCount = item.likeCount
      item.likeCount++

      try {
        await commentApi.likeComment(item.id)
      } catch (e) {
        item.likeCount = originalCount
      }
    },

    // eslint-disable-next-line no-unused-vars
    handleReport(item) {
      if (!this.currentUser.id) {
        this.$message.warning('请登录')
        return
      }
      this.$prompt('请输入举报原因', '举报评论', {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
      }).then(() => {
        this.$message.success('举报已提交，我们会尽快处理')
      }).catch(() => {});
    },

    handleCommand(command, item) {
      if (command === 'copy') {
        navigator.clipboard.writeText(item.content)
        this.$message.success('已复制')
      } else if (command === 'delete') {
        this.$confirm('确认删除这条评论吗？', '提示', { type: 'warning' })
            .then(async () => {
              const res = await commentApi.deleteComment(item.id)
              if (res.code === 200) {
                this.fetchComments() // 简单粗暴重刷
                this.$message.success('删除成功')
              }
            })
      }
    },

    autoResize(event) {
      const el = event.target
      el.style.height = 'auto'
      el.style.height = el.scrollHeight + 'px'
    },

    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      const y = date.getFullYear()
      const m = (date.getMonth() + 1).toString().padStart(2, '0')
      const d = date.getDate().toString().padStart(2, '0')
      const hh = date.getHours().toString().padStart(2, '0')
      const mm = date.getMinutes().toString().padStart(2, '0')
      return `${y}-${m}-${d} ${hh}:${mm}`
    },

    loadMore() {
      this.$message.info('加载更多逻辑待完善')
    }
  }
}
</script>

<style scoped>
/* 转换为普通 CSS，解决 sass-loader 报错 */
.video-comment-module {
  background: #fff;
  border-radius: 4px;
  padding: 20px 24px;
  margin-top: 20px;
  width: 100%;
  box-sizing: border-box;
}

/* 1. 头部样式 */
.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.comment-header .header-left .title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-right: 6px;
}

.comment-header .header-left .count {
  font-size: 14px;
  color: #999;
}

.comment-header .header-right .sort-item {
  font-size: 14px;
  color: #666;
  cursor: pointer;
  margin-left: 20px;
  padding-bottom: 2px;
}

.comment-header .header-right .sort-item.active {
  color: #333;
  font-weight: 600;
}

.comment-header .header-right .sort-item:hover {
  color: #1890ff;
}

/* 2. 输入框区域 */
.comment-input-area {
  display: flex;
  gap: 16px;
  margin-bottom: 30px;
}

.comment-input-area .input-col {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.comment-input-area .input-col .custom-textarea {
  width: 100%;
  min-height: 40px;
  padding: 8px 0;
  border: none;
  border-bottom: 1px solid #e5e9ef;
  background: transparent;
  color: #333;
  font-size: 14px;
  line-height: 22px;
  resize: none;
  outline: none;
  transition: all 0.3s;
}

.comment-input-area .input-col .custom-textarea:focus,
.comment-input-area .input-col .custom-textarea:hover {
  border-bottom-color: #1890ff;
}

.comment-input-area .input-col .action-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  opacity: 0.5;
  pointer-events: none;
  transition: opacity 0.3s;
}

.comment-input-area .input-col .action-bar.visible {
  opacity: 1;
  pointer-events: auto;
}

/* 3. 评论列表 */
.comment-list .comment-item {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.comment-list .comment-item:hover {
  background-color: #fafafa;
  padding: 16px 10px;
  margin: 0 -10px;
  border-radius: 4px;
}

.comment-list .comment-item .item-avatar {
  flex-shrink: 0;
}

.comment-list .comment-item .item-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 第一层：用户信息 */
.user-info-row {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.user-info-row .username {
  font-weight: 600;
  color: #61666d;
  margin-right: 8px;
  cursor: pointer;
}

.user-info-row .username:hover {
  color: #1890ff;
}

.user-info-row .tag-up {
  font-size: 12px;
  color: #fb7299;
  border: 1px solid #fb7299;
  border-radius: 2px;
  padding: 0 2px;
  margin-right: 8px;
  transform: scale(0.9);
}

.user-info-row .tag-me {
  font-size: 12px;
  color: #1890ff;
  background: rgba(24,144,255,0.1);
  padding: 0 4px;
  border-radius: 2px;
  margin-right: 8px;
}

.user-info-row .time {
  color: #999;
  font-size: 12px;
}

/* 第二层：内容 */
.content-row {
  font-size: 15px;
  color: #333;
  line-height: 24px;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 第三层：操作栏 */
.action-row {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 4px;
}

.action-row .action-btn {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  gap: 4px;
}

.action-row .action-btn i {
  font-size: 15px;
}

.action-row .action-btn:hover,
.action-row .action-btn.active {
  color: #1890ff;
}

/* 第四层：回复区 */
.reply-section {
  margin-top: 12px;
  background: #f9f9f9;
  border-radius: 4px;
  padding: 12px;
}

.reply-section .sub-reply-input {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.reply-section .sub-reply-input.fade-in {
  animation: fadeIn 0.3s;
}

.reply-section .sub-reply-input .sub-input-wrapper {
  flex: 1;
  display: flex;
  gap: 8px;
}

.reply-section .sub-reply-input .sub-input-wrapper .sub-textarea {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 6px 10px;
  font-size: 13px;
  outline: none;
  resize: none;
  height: 32px;
}

.reply-section .sub-reply-input .sub-input-wrapper .sub-textarea:focus {
  border-color: #1890ff;
}

/* 子回复列表 */
.reply-section .sub-reply-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-section .sub-reply-list .sub-reply-item {
  display: flex;
  gap: 10px;
}

.reply-section .sub-reply-list .sub-reply-item .sub-content {
  font-size: 13px;
  line-height: 20px;
}

.reply-section .sub-reply-list .sub-reply-item .sub-content .sub-user {
  color: #61666d;
  font-weight: 500;
  cursor: pointer;
}

.reply-section .sub-reply-list .sub-reply-item .sub-content .sub-user:hover {
  color: #1890ff;
}

.reply-section .sub-reply-list .sub-reply-item .sub-content .reply-target {
  color: #666;
  margin: 0 4px;
}

.reply-section .sub-reply-list .sub-reply-item .sub-content .reply-target .at {
  color: #1890ff;
}

.reply-section .sub-reply-list .sub-reply-item .sub-content .sub-text {
  color: #333;
}

.reply-section .sub-reply-list .sub-reply-item .sub-content .sub-footer {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 15px;
}

.reply-section .sub-reply-list .sub-reply-item .sub-content .sub-footer .sub-like,
.reply-section .sub-reply-list .sub-reply-item .sub-content .sub-footer .sub-reply-btn {
  cursor: pointer;
}

.reply-section .sub-reply-list .sub-reply-item .sub-content .sub-footer .sub-like:hover,
.reply-section .sub-reply-list .sub-reply-item .sub-content .sub-footer .sub-reply-btn:hover {
  color: #1890ff;
}

.load-more {
  text-align: center;
  padding: 20px 0;
  color: #666;
  cursor: pointer;
}

.load-more:hover {
  color: #1890ff;
}

.no-more {
  text-align: center;
  padding: 20px 0;
  color: #ccc;
  font-size: 12px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-5px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>