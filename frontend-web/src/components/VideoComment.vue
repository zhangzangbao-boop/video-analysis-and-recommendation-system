<template>
  <div class="video-comment-module">
    <div class="comment-header">
      <div class="header-left">
        <span class="title">评论</span>
        <span class="count">{{ totalCount }}</span>
      </div>
      <div class="header-right">
        <span v-for="type in sortTypes" :key="type.value" :class="['sort-item', { active: currentSort === type.value }]" @click="handleSortChange(type.value)">
          {{ type.label }}
        </span>
      </div>
    </div>

    <div class="comment-input-area">
      <div class="avatar-col">
        <el-avatar :size="40" :src="currentUser.avatarUrl || defaultAvatar"></el-avatar>
      </div>
      <div class="input-col">
        <textarea v-model="mainContent" class="custom-textarea" placeholder="发一条友善的评论" rows="1" @input="autoResize"></textarea>
        <div class="action-bar" :class="{ visible: mainContent.length > 0 }">
          <el-button type="primary" size="small" :disabled="!mainContent.trim()" :loading="submitting" @click="submitComment(0)">发布</el-button>
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

          <div class="content-row">{{ item.content }}</div>

          <div class="action-row">
            <div class="action-btn" :class="{ active: item.isLiked }" @click="handleLike(item)">
              <i class="el-icon-thumb"></i>
              <span>{{ item.likeCount || '点赞' }}</span>
            </div>

            <div class="action-btn" :class="{ active: activeReplyId === item.id }" @click="toggleReplyBox(item.id)">
              <i class="el-icon-chat-round"></i>
              <span>{{ (item.replies && item.replies.length > 0) ? item.replies.length : '回复' }}</span>
            </div>

            <div class="action-btn" @click="handleReport(item)">
              <i class="el-icon-warning-outline"></i>
            </div>

            <el-dropdown trigger="click" @command="handleCommand($event, item)">
              <div class="action-btn"><i class="el-icon-more"></i></div>
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
                <textarea :ref="'replyInput' + item.id" v-model="replyContent" class="sub-textarea" :placeholder="`回复 @${replyTargetUser || item.nickname}：`"></textarea>
                <el-button type="primary" size="mini" :disabled="!replyContent.trim()" @click="submitComment(item.id, item)">发送</el-button>
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
                    <div class="sub-like" :class="{ active: reply.isLiked }" @click="handleLike(reply)">
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

      <div class="load-more" v-if="hasMore" @click="loadMore">加载更多评论</div>
      <div class="no-more" v-else>没有更多评论了</div>
    </div>
  </div>
</template>

<script>
import { commentApi } from '@/api/comment'

export default {
  name: 'VideoComment',
  props: {
    videoId: { type: [String, Number], required: true },
    videoAuthorId: { type: [String, Number], default: null }
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
      sortTypes: [{ label: '按热度', value: 'hot' }, { label: '按时间', value: 'time' }],
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
    initUser() {
      try {
        const token = localStorage.getItem('userToken')
        const userId = localStorage.getItem('userId')
        const username = localStorage.getItem('username')
        const avatar = localStorage.getItem('userAvatar')
        if (token && userId) {
          this.currentUser = { id: parseInt(userId), nickname: username, avatarUrl: avatar }
        }
      } catch (e) {
        // 修复：添加日志以解决 no-empty 报错
        console.error('初始化用户失败', e)
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
            likeCount: parseInt(item.likeCount) || 0,
            isLiked: !!item.isLiked,
            replies: (item.replies || []).map(r => ({
              ...r,
              likeCount: parseInt(r.likeCount) || 0,
              isLiked: !!r.isLiked
            }))
          }))
          this.totalCount = this.commentList.length
        }
      } finally {
        this.loading = false
      }
    },

    async handleLike(item) {
      if (!this.currentUser.id) return this.$message.warning('请先登录')

      const oldState = item.isLiked
      const count = parseInt(item.likeCount) || 0

      item.isLiked = !oldState
      item.likeCount = item.isLiked ? count + 1 : (count > 0 ? count - 1 : 0)

      try {
        if (!oldState) {
          await commentApi.likeComment(item.id)
        } else {
          await commentApi.unlikeComment(item.id)
        }
      } catch (e) {
        item.isLiked = oldState
        item.likeCount = count
        this.$message.error('操作失败')
      }
    },

    handleCommand(command, item) {
      if (command === 'copy') {
        const text = item.content || ''
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(text)
              .then(() => this.$message.success('已复制'))
              .catch(() => this.fallbackCopy(text))
        } else {
          this.fallbackCopy(text)
        }
      } else if (command === 'delete') {
        this.$confirm('确认删除?', '提示', { type: 'warning' }).then(async () => {
          await commentApi.deleteComment(item.id)
          this.fetchComments()
          this.$message.success('删除成功')
        })
      }
    },

    fallbackCopy(text) {
      try {
        const input = document.createElement('textarea')
        input.value = text
        document.body.appendChild(input)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$message.success('已复制')
      } catch (e) {
        this.$message.error('复制失败，请手动复制')
      }
    },

    handleSortChange(type) { if (this.currentSort === type) return; this.currentSort = type; this.fetchComments() },

    async submitComment(parentId, parentItem = null) {
      if (!this.currentUser.id) { this.$message.warning('请先登录'); return this.$router.push('/login') }
      const content = parentId === 0 ? this.mainContent : this.replyContent
      if (!content.trim()) return
      this.submitting = true
      try {
        const res = await commentApi.addComment({
          videoId: this.videoId, content, parentId: parentId || 0,
          replyUserId: parentId === 0 ? null : this.replyTargetUserId
        })
        if (res.code === 200) {
          const newComment = { ...res.data, isLiked: false, replies: [] }
          this.$message.success('发送成功')
          if (parentId === 0) {
            this.commentList.unshift(newComment)
            this.mainContent = ''
            this.totalCount++
          } else {
            if (parentItem) {
              if (!parentItem.replies) this.$set(parentItem, 'replies', [])
              parentItem.replies.push(newComment)
            }
            this.replyContent = ''; this.activeReplyId = null
          }
        } else { this.$message.error(res.message) }
      } catch (e) { this.$message.error('发送失败') } finally { this.submitting = false }
    },

    toggleReplyBox(commentId) {
      if (this.activeReplyId === commentId) { this.activeReplyId = null }
      else {
        this.activeReplyId = commentId; this.replyContent = '';
        this.replyTargetUser = null; this.replyTargetUserId = null;
        this.$nextTick(() => { const ref = this.$refs['replyInput' + commentId]; if (ref && ref[0]) ref[0].focus() })
      }
    },

    prepareSubReply(parentId, replyItem) {
      this.activeReplyId = parentId; this.replyTargetUser = replyItem.nickname;
      this.replyTargetUserId = replyItem.userId; this.replyContent = '';
      this.$nextTick(() => { const ref = this.$refs['replyInput' + parentId]; if (ref && ref[0]) ref[0].focus() })
    },

    // 修复：忽略未使用的参数 item
    // eslint-disable-next-line no-unused-vars
    handleReport(item) {
      if(!this.currentUser.id) return this.$message.warning('请先登录');
      this.$prompt('请输入举报原因', '举报').then(() => this.$message.success('已提交'))
    },

    autoResize(e) { e.target.style.height = 'auto'; e.target.style.height = e.target.scrollHeight + 'px' },
    formatTime(t) { if (!t) return ''; return t.replace('T', ' ').substring(0, 16) },
    loadMore() { this.$message.info('加载更多') }
  }
}
</script>

<style scoped>
.video-comment-module { background: #fff; padding: 20px 24px; margin-top: 20px; }
.comment-header { display: flex; justify-content: space-between; margin-bottom: 24px; }
.comment-header .title { font-size: 20px; font-weight: 600; margin-right: 6px; }
.comment-header .count { color: #999; font-size: 14px; }
.sort-item { font-size: 14px; color: #666; margin-left: 20px; cursor: pointer; }
.sort-item.active { color: #333; font-weight: 600; }
.comment-input-area { display: flex; gap: 16px; margin-bottom: 30px; }
.input-col { flex: 1; }
.custom-textarea { width: 100%; min-height: 40px; border: none; border-bottom: 1px solid #e5e9ef; resize: none; outline: none; padding: 8px 0; }
.custom-textarea:focus { border-bottom-color: #1890ff; }
.action-bar { display: flex; justify-content: flex-end; margin-top: 8px; opacity: 0; transition: opacity 0.2s; }
.action-bar.visible { opacity: 1; }
.comment-item { display: flex; gap: 16px; padding: 16px 0; border-bottom: 1px solid #f0f0f0; }
.item-content-wrapper { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.user-info-row { display: flex; align-items: center; font-size: 13px; }
.username { font-weight: 600; color: #61666d; margin-right: 8px; }
.time { color: #999; font-size: 12px; margin-left: auto; }
.tag-up { border: 1px solid #fb7299; color: #fb7299; font-size: 12px; border-radius: 2px; padding: 0 2px; margin-right: 5px; }
.tag-me { color: #1890ff; background: #e6f7ff; font-size: 12px; padding: 0 2px; margin-right: 5px; }
.content-row { font-size: 15px; line-height: 24px; white-space: pre-wrap; }
.action-row { display: flex; gap: 24px; margin-top: 4px; align-items: center; }
.action-btn { display: flex; align-items: center; gap: 4px; font-size: 13px; color: #999; cursor: pointer; }
.action-btn:hover, .action-btn.active { color: #1890ff; }
.reply-section { background: #f9f9f9; padding: 12px; margin-top: 12px; border-radius: 4px; }
.sub-reply-list { display: flex; flex-direction: column; gap: 10px; }
.sub-reply-item { display: flex; gap: 10px; }
.sub-content { font-size: 13px; line-height: 20px; flex: 1; }
.sub-user { color: #61666d; font-weight: 500; cursor: pointer; }
.reply-target { color: #666; margin: 0 4px; }
.sub-footer { margin-top: 4px; color: #999; font-size: 12px; display: flex; gap: 15px; align-items: center; }
.sub-like { cursor: pointer; display: flex; align-items: center; gap: 2px; }
.sub-like:hover, .sub-like.active { color: #1890ff; }
.sub-reply-btn { cursor: pointer; } .sub-reply-btn:hover { color: #1890ff; }
.load-more, .no-more { text-align: center; padding: 20px; color: #999; cursor: pointer; font-size: 12px; }
.sub-input-wrapper { flex: 1; display: flex; gap: 8px; }
.sub-textarea { flex: 1; border: 1px solid #dcdfe6; border-radius: 4px; padding: 5px; resize: none; height: 32px; font-size: 13px; }
</style>