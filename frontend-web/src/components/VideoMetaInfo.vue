<template>
  <div class="video-meta-info-card">
    <div class="meta-top-row">
      <div class="video-info-col">
        <h1 class="video-title" :title="video.title">{{ video.title }}</h1>
        <div class="video-stats-line">
          <span class="stat-item">{{ formatTime(video.createTime) }}</span>
          <span class="stat-item"><i class="el-icon-video-play"></i> {{ formatCount(video.views) }}</span>
          <span class="stat-item"><i class="el-icon-chat-line-square"></i> {{ formatCount(video.danmakuCount || 0) }}弹幕</span>
          <span class="stat-item"><i class="el-icon-time"></i> {{ video.duration }}</span>
        </div>
      </div>

      <div class="author-info-col">
        <div class="author-basic">
          <el-avatar :size="46" :src="author.avatar || defaultAvatar" class="author-avatar"></el-avatar>
          <div class="author-text">
            <div class="name-line">
              <span class="nickname">{{ author.name }}</span>
              <el-button
                  size="mini"
                  :type="isFollowing ? '' : 'danger'"
                  :plain="isFollowing"
                  class="follow-btn"
                  :loading="followLoading"
                  @click="handleFollow"
              >
                {{ isFollowing ? '已关注' : '+ 关注' }}
              </el-button>
            </div>
            <div class="bio-line" :title="author.bio || '这个人很懒，什么都没写'">
              {{ author.bio || '暂无简介' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-divider class="meta-divider"></el-divider>

    <div class="meta-action-row">
      <div class="action-left">

        <div class="action-item" :class="{ active: isLiked }" @click="handleLike">
          <i class="el-icon-thumb"></i>
          <span>{{ formatCount(currentLikeCount) || '点赞' }}</span>
        </div>

        <div class="action-item" :class="{ active: isCollected }" @click="openCollectionDialog">
          <i :class="isCollected ? 'el-icon-star-on' : 'el-icon-star-off'"></i>
          <span>{{ formatCount(currentCollectCount) || '收藏' }}</span>
        </div>

        <el-popover
            placement="bottom"
            width="260"
            trigger="click"
            v-model="showSharePopover">
          <div class="share-panel">
            <div class="share-title">分享到</div>
            <div class="share-icons">
              <div class="share-icon-btn wechat" @click="doShare('wechat')"><i class="el-icon-chat-dot-round"></i> 微信</div>
              <div class="share-icon-btn qq" @click="doShare('qq')"><i class="el-icon-chat-round"></i> QQ</div>
              <div class="share-icon-btn link" @click="doShare('link')"><i class="el-icon-link"></i> 复制链接</div>
            </div>
          </div>
          <div class="action-item" slot="reference">
            <i class="el-icon-share"></i>
            <span>{{ formatCount(currentShareCount) || '分享' }}</span>
          </div>
        </el-popover>

        <el-dropdown trigger="click" @command="handleMoreCommand">
          <div class="action-item">
            <i class="el-icon-more"></i>
            <span>更多</span>
          </div>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="report" icon="el-icon-warning-outline">举报</el-dropdown-item>
            <el-dropdown-item command="download" icon="el-icon-download">缓存视频</el-dropdown-item>
            <el-dropdown-item command="cast" icon="el-icon-monitor">投屏播放</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>

      </div>
    </div>

    <el-dialog title="选择收藏夹" :visible.sync="collectionVisible" width="30%" center append-to-body>
      <div class="collection-list">
        <div class="folder-item" :class="{ selected: selectedFolder === 0 }" @click="selectedFolder = 0">
          <i class="el-icon-folder"></i> 默认收藏夹
          <i class="el-icon-check check-mark" v-if="selectedFolder === 0"></i>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="collectionVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmCollection">确 定</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script>
import { userVideoApi } from '@/api/user'

export default {
  name: 'VideoMetaInfo',
  props: {
    video: {
      type: Object,
      required: true,
      default: () => ({})
    },
    author: {
      type: Object,
      required: true,
      default: () => ({})
    }
  },
  data() {
    return {
      defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',

      // 状态管理
      isLiked: false,
      currentLikeCount: 0,

      isCollected: false,
      currentCollectCount: 0,

      currentShareCount: 0,

      isFollowing: false,
      followLoading: false,

      // UI控制
      showSharePopover: false,
      collectionVisible: false,
      selectedFolder: 0,
    }
  },
  watch: {
    'video.id': {
      immediate: true,
      handler(val) {
        if (val) {
          this.initStatus()
        }
      }
    }
  },
  methods: {
    async initStatus() {
      // 【修复关键点1】强制转换为数字，防止字符串拼接错误
      this.currentLikeCount = parseInt(this.video.likes) || 0
      this.currentCollectCount = parseInt(this.video.collectCount) || 0
      this.currentShareCount = parseInt(this.video.shareCount) || 0

      const token = localStorage.getItem('userToken')
      if (token) {
        try {
          const [likeRes, collectRes, followRes] = await Promise.all([
            userVideoApi.checkIsLiked(this.video.id),
            userVideoApi.checkIsCollected(this.video.id),
            this.author.id ? userVideoApi.checkIsFollowed(this.author.id) : Promise.resolve({ data: false })
          ])

          this.isLiked = !!likeRes.data
          this.isCollected = !!collectRes.data
          this.isFollowing = !!followRes.data
        } catch (e) {
          console.warn('状态初始化部分失败', e)
        }
      }
    },

    checkLogin() {
      if (!localStorage.getItem('userToken')) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return false
      }
      return true
    },

    async handleLike() {
      if (!this.checkLogin()) return

      // 记录旧状态用于回滚
      const oldState = this.isLiked

      // 【修复关键点2】强制转换为数字进行计算
      let count = parseInt(this.currentLikeCount)

      // 乐观更新：立即改变 UI
      this.isLiked = !oldState

      if (this.isLiked) {
        // 如果变成了点赞状态，数量+1
        this.currentLikeCount = count + 1
      } else {
        // 如果取消了点赞，数量-1
        this.currentLikeCount = count > 0 ? count - 1 : 0
      }

      try {
        if (!oldState) {
          // 之前没点赞 -> 现在点赞
          const res = await userVideoApi.likeVideo(this.video.id)
          // 再次确认：如果后端返回 false (比如已经点过赞)，则不需要回滚，保持现状即可
          // 但如果后端报错，catch 会捕获
          if (res.code !== 200) throw new Error(res.message)
          
          // 记录用户行为到Kafka
          const userId = localStorage.getItem('userId')
          if (userId) {
            // 【修复】直接传递字符串，避免parseInt()精度丢失（大整数问题）
            userVideoApi.recordBehavior(userId, this.video.id, 'like').catch(err => {
              console.warn('记录用户行为失败:', err)
            })
          }
        } else {
          // 之前已点赞 -> 现在取消
          const res = await userVideoApi.unlikeVideo(this.video.id)
          if (res.code !== 200) throw new Error(res.message)
        }
      } catch (e) {
        // 发生错误，回滚状态和数字
        this.isLiked = oldState
        this.currentLikeCount = count
        this.$message.error('操作过于频繁，请稍后再试')
      }
    },

    async handleFollow() {
      if (!this.checkLogin()) return
      this.followLoading = true
      try {
        if (this.isFollowing) {
          await userVideoApi.unfollowUser(this.author.id)
          this.isFollowing = false
          this.$message.success('已取消关注')
        } else {
          await userVideoApi.followUser(this.author.id)
          this.isFollowing = true
          this.$message.success('关注成功')
        }
      } catch (e) {
        this.$message.error('操作失败')
      } finally {
        this.followLoading = false
      }
    },

    openCollectionDialog() {
      if (!this.checkLogin()) return
      this.collectionVisible = true
    },

    async confirmCollection() {
      try {
        await userVideoApi.collectVideo(this.video.id, this.selectedFolder)
        if (!this.isCollected) {
          this.isCollected = true
          // 同样做强制类型转换
          this.currentCollectCount = (parseInt(this.currentCollectCount) || 0) + 1
          
          // 记录用户行为到Kafka
          const userId = localStorage.getItem('userId')
          if (userId) {
            // 【修复】直接传递字符串，避免parseInt()精度丢失（大整数问题）
            userVideoApi.recordBehavior(userId, this.video.id, 'collect').catch(err => {
              console.warn('记录用户行为失败:', err)
            })
          }
        }
        this.$message.success('收藏成功')
        this.collectionVisible = false
      } catch (e) {
        this.$message.error('收藏失败')
      }
    },

    async doShare(type) {
      if (!this.checkLogin()) return

      if (type === 'link') {
        const url = window.location.href
        navigator.clipboard.writeText(url)
        this.$message.success('链接已复制到剪贴板')
      } else {
        this.$message.success(`已分享到 ${type}`)
      }

      this.showSharePopover = false
      try {
        await userVideoApi.shareVideo(this.video.id)
        this.currentShareCount = (parseInt(this.currentShareCount) || 0) + 1
        
        // 记录用户行为到Kafka
        const userId = localStorage.getItem('userId')
        if (userId) {
          // 【修复】直接传递字符串，避免parseInt()精度丢失（大整数问题）
          userVideoApi.recordBehavior(userId, this.video.id, 'share').catch(err => {
            console.warn('记录用户行为失败:', err)
          })
        }
      } catch (e) {
        console.warn('分享统计失败', e)
      }
    },

    handleMoreCommand(command) {
      if (!this.checkLogin()) return
      if (command === 'report') {
        this.$prompt('请输入举报理由', '举报', {
          confirmButtonText: '提交',
          cancelButtonText: '取消'
        }).then(({ value }) => {
          userVideoApi.reportVideo({ videoId: this.video.id, reason: value })
          this.$message.success('举报已提交')
        }).catch(() => {
        })
      } else {
        this.$message.info('该功能开发中...')
      }
    },

    formatCount(num) {
      if (!num) return 0
      return num > 10000 ? (num / 10000).toFixed(1) + '万' : num
    },

    formatTime(timeStr) {
      if (!timeStr) return ''
      return timeStr.split('T')[0]
    }
  }
}
</script>

<style scoped>
.video-meta-info-card {
  background: #fff;
  padding: 20px;
  border-radius: 0;
  margin-top: 0;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.meta-top-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.video-info-col {
  flex: 1;
  min-width: 0;
}

.video-title {
  font-size: 20px;
  font-weight: 500;
  color: #18191c;
  margin: 0 0 10px 0;
  line-height: 28px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-stats-line {
  font-size: 13px;
  color: #9499a0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.author-info-col {
  flex-shrink: 0;
  width: 280px;
}

.author-basic {
  display: flex;
  gap: 12px;
  align-items: center;
}

.author-text {
  flex: 1;
  min-width: 0;
}

.name-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.nickname {
  font-size: 15px;
  font-weight: 500;
  color: #fb7299;
  cursor: pointer;
}

.follow-btn {
  padding: 6px 16px;
}

.bio-line {
  font-size: 12px;
  color: #9499a0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-divider {
  margin: 16px 0;
  background-color: #e3e5e7;
}

.meta-action-row {
  display: flex;
  align-items: center;
}

.action-left {
  display: flex;
  gap: 32px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #61666d;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.action-item i {
  font-size: 22px;
  transition: color 0.2s;
}

.action-item:hover {
  color: #1890ff;
}

/* 激活状态样式 (实心/高亮) */
.action-item.active {
  color: #1890ff;
}

.action-item.active i {
  color: #1890ff;
}

.share-panel {
  text-align: center;
}
.share-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 12px;
}
.share-icons {
  display: flex;
  justify-content: space-around;
}
.share-icon-btn {
  cursor: pointer;
  font-size: 12px;
  color: #666;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.share-icon-btn i {
  font-size: 24px;
  padding: 8px;
  border-radius: 50%;
  background: #f1f2f3;
  transition: 0.2s;
}
.share-icon-btn:hover i {
  background: #e6f7ff;
  color: #1890ff;
}

.collection-list {
  max-height: 300px;
  overflow-y: auto;
}
.folder-item {
  padding: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 4px;
}
.folder-item:hover {
  background: #f5f7fa;
}
.folder-item.selected {
  color: #1890ff;
}
.check-mark {
  margin-left: auto;
}
</style>