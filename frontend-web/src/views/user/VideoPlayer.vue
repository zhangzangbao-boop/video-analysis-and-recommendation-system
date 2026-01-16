<template>
  <div class="video-player-container">
    <!-- 顶部视频播放区域（放大） -->
    <div class="main-video-section">
      <el-card class="main-video-card">
        <div class="video-header">
          <h2 class="video-title">{{ currentVideo.title }}</h2>
          <div class="video-header-actions">
            <el-button 
              type="danger" 
              :icon="currentVideo.isLiked ? 'el-icon-heart-on' : 'el-icon-heart-off'"
              @click="handleLike"
              :loading="likeLoading"
              size="small"
            >
              {{ currentVideo.likes }}
            </el-button>
            <el-button 
              type="text" 
              icon="el-icon-refresh"
              @click="refreshVideo"
              size="small"
            >
              换一个
            </el-button>
          </div>
        </div>
        
        <!-- 大视频播放器 -->
        <div class="video-player-large">
          <div class="video-wrapper-large">
            <video 
              ref="videoPlayer"
              :src="currentVideo.url || undefined"
              class="video-element-large"
              controls
              :autoplay="!!currentVideo.url"
              @timeupdate="handleTimeUpdate"
              @ended="handleVideoEnded"
              @play="handleVideoPlay"
              @pause="handleVideoPause"
              v-if="currentVideo.url"
            >
              您的浏览器不支持 video 标签。
            </video>
            <div v-else class="video-placeholder" style="width: 100%; height: 500px; display: flex; align-items: center; justify-content: center; background: #000; color: #fff;">
              <span>正在加载视频...</span>
            </div>
            
            <!-- 视频遮罩层控制按钮 -->
            <div class="video-overlay-controls">
              <div class="play-pause-btn" @click="togglePlayPause">
                <i :class="isPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
              </div>
            </div>
          </div>
          
          <!-- 视频作者信息 -->
          <div class="video-author-info">
            <div class="author-main">
              <el-avatar 
                :size="40" 
                :src="currentVideo.author.avatar || undefined"
                class="author-avatar"
              ></el-avatar>
              <div class="author-details">
                <div class="author-name">{{ currentVideo.author.name }}</div>
                <div class="video-stats">
                  <span class="stat-item">
                    <i class="el-icon-view"></i> {{ currentVideo.views }}次播放
                  </span>
                  <span class="stat-item">
                    <i class="el-icon-time"></i> {{ currentVideo.uploadTime }}
                  </span>
                </div>
              </div>
              <el-button 
                v-if="!currentVideo.isFollowing"
                type="primary" 
                size="small"
                @click="handleFollow"
              >
                关注
              </el-button>
            </div>
            
            <!-- 视频描述 -->
            <div class="video-description-large">
              {{ currentVideo.description }}
            </div>
          </div>
          
          <!-- 视频互动操作 -->
          <div class="video-actions-large">
            <div class="action-item" @click="handleLike">
              <div class="action-icon" :class="{ 'liked': currentVideo.isLiked }">
                <i :class="currentVideo.isLiked ? 'el-icon-star-on' : 'el-icon-star-off'"></i>
              </div>
              <div class="action-label">点赞</div>
              <div class="action-count">{{ currentVideo.likes }}</div>
            </div>
            
            <div class="action-item" @click="showCommentModal = true">
              <div class="action-icon">
                <i class="el-icon-chat-dot-round"></i>
              </div>
              <div class="action-label">评论</div>
              <div class="action-count">{{ currentVideo.comments.length }}</div>
            </div>
            
            <div class="action-item" @click="handleShare">
              <div class="action-icon">
                <i class="el-icon-share"></i>
              </div>
              <div class="action-label">分享</div>
            </div>
            
            <div class="action-item" @click="handleDislike">
              <div class="action-icon">
                <i class="el-icon-close"></i>
              </div>
              <div class="action-label">不感兴趣</div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 评论区 -->
      <div class="comments-section-large" v-if="showComments">
        <el-card>
          <div slot="header" class="comments-header">
            <span>评论 {{ currentVideo.comments.length }}</span>
          </div>
          
          <!-- 评论输入 -->
          <div class="comment-input-large">
            <el-avatar 
              :size="40" 
              :src="userAvatar || undefined"
              class="comment-user-avatar"
            ></el-avatar>
            <div class="comment-input-wrapper">
              <el-input
                v-model="newComment"
                placeholder="留下你的评论..."
                @keyup.enter.native="submitComment"
              ></el-input>
              <el-button 
                type="primary" 
                icon="el-icon-position" 
                @click="submitComment"
                class="comment-submit-btn"
              >
                发布
              </el-button>
            </div>
          </div>
          
          <!-- 评论列表 -->
          <div class="comment-list-large">
            <div 
              v-for="comment in currentVideo.comments" 
              :key="comment.id"
              class="comment-item-large"
            >
              <el-avatar 
                :size="36" 
                :src="comment.userAvatar || undefined"
                class="comment-avatar"
              ></el-avatar>
              <div class="comment-content-large">
                <div class="comment-header">
                  <span class="comment-username">{{ comment.userName }}</span>
                  <span class="comment-time">{{ comment.time }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
                <div class="comment-actions">
                  <el-button type="text" size="mini" @click="checkLogin('commentLike')">
                    <i class="el-icon-heart-off"></i> 点赞
                  </el-button>
                  <el-button type="text" size="mini" @click="checkLogin('reply')">
                    回复
                  </el-button>
                </div>
              </div>
            </div>
            
            <div v-if="currentVideo.comments.length === 0" class="no-comments">
              <el-empty description="暂无评论，快来发表第一条评论吧~"></el-empty>
            </div>
          </div>
        </el-card>
      </div>
    </div>
    
    <!-- 右侧区域：推荐视频 + 热门排行榜（竖向排列） -->
    <div class="right-sidebar">
      <!-- 推荐视频列表 -->
      <el-card class="recommendation-card">
        <div slot="header" class="section-header">
          <h3><i class="el-icon-video-camera"></i> 推荐视频</h3>
          <el-button 
            type="text" 
            size="small" 
            @click="refreshRecommendations"
            icon="el-icon-refresh"
          >
            换一换
          </el-button>
        </div>
        
        <!-- 推荐视频列表（可滚动） -->
        <div class="recommendation-list scrollable">
          <div 
            v-for="video in recommendedVideos" 
            :key="video.id"
            class="recommendation-item"
            @click="switchVideo(video)"
          >
            <div class="recommendation-thumbnail">
              <img :src="video.thumbnail" alt="thumbnail">
              <div class="video-duration-small">{{ video.duration }}</div>
            </div>
            <div class="recommendation-info">
              <div class="recommendation-title">{{ video.title }}</div>
              <div class="recommendation-meta">
                <span>{{ video.author.name }}</span>
                <span>· {{ video.views }}次播放</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 热门排行榜（竖向排列，纯文字，可滚动） -->
      <el-card class="hot-ranking-card-vertical">
        <div slot="header" class="section-header">
          <h3><i class="el-icon-trophy"></i> 热门排行榜</h3>
          <el-select 
            v-model="rankingType" 
            size="mini" 
            @change="changeRankingType"
            style="width: 80px;"
          >
            <el-option label="今日" value="today"></el-option>
            <el-option label="本周" value="week"></el-option>
            <el-option label="本月" value="month"></el-option>
          </el-select>
        </div>
        
        <!-- 排行榜列表（可滚动，纯文字） -->
        <div class="ranking-list-vertical scrollable">
          <div 
            v-for="(video, index) in hotRanking" 
            :key="video.id"
            class="ranking-item-vertical"
            @click="switchVideo(video)"
          >
            <div class="ranking-order-vertical" :class="getOrderClass(index)">
              {{ index + 1 }}
            </div>
            <div class="ranking-info-vertical">
              <div class="ranking-title-vertical">{{ video.title }}</div>
              <div class="ranking-stats-vertical">
                <span><i class="el-icon-view"></i> {{ video.views }}</span>
                <span><i class="el-icon-heart-on"></i> {{ video.likes }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { userVideoApi } from '@/api/user'

export default {
  name: 'VideoPlayer',
  data() {
    return {
      // 当前播放视频
      currentVideo: {
        id: null,
        title: '',
        url: '',
        thumbnail: '',
        duration: '0:00',
        description: '',
        views: 0,
        likes: 0,
        isLiked: false,
        uploadTime: '',
        author: {
          id: null,
          name: '未知作者',
          avatar: ''
        },
        isFollowing: false,
        comments: []
      },
      
      // 推荐视频列表
      recommendedVideos: [],
      
      // 热门排行榜（纯文字版）
      hotRanking: [],
      
      // UI状态
      showComments: true,
      rankingType: 'today',
      isPlaying: true,
      likeLoading: false,
      newComment: '',
      loading: false, // 视频加载状态
      
      // 用户信息
      userAvatar: localStorage.getItem('userAvatar') || '', // 从localStorage获取用户头像，如果没有则为空
      
      // 用户行为数据
      userBehavior: {
        videoId: null,
        playDuration: 0,
        isCompleted: false
      }
    }
  },
  
  computed: {
    // 检查用户是否已登录
    isLogin() {
      return !!localStorage.getItem('userToken');
    }
  },
  
  mounted() {
    this.loadInitialVideo()
    this.initializeVideoPlayer()
  },
  
  beforeDestroy() {
    this.stopBehaviorTracking()
  },
  
  methods: {
    // 加载初始视频数据
    async loadInitialVideo() {
      this.loading = true
      try {
        // 获取推荐视频列表
        const recommendRes = await userVideoApi.getRecommendVideos(
          localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')) : null,
          10
        )
        
        if (recommendRes && recommendRes.data && recommendRes.data.length > 0) {
          // 转换并设置推荐视频列表
          this.recommendedVideos = recommendRes.data.map(v => this.convertVideoData(v))
          
          // 设置第一个推荐视频为当前播放视频
          this.currentVideo = this.convertVideoData(recommendRes.data[0], true)
          this.startBehaviorTracking()
          
          // 加载热门排行榜
          await this.loadHotRanking()
        } else {
          this.$message.warning('暂无推荐视频')
        }
      } catch (error) {
        console.error('加载视频失败:', error)
        this.$message.error('加载视频失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    
    // 加载热门排行榜
    async loadHotRanking() {
      try {
        const hotRes = await userVideoApi.getHotVideos()
        if (hotRes && hotRes.data && hotRes.data.length > 0) {
          // 只取前5个作为排行榜
          this.hotRanking = hotRes.data.slice(0, 5).map(v => ({
            id: v.id,
            title: v.title,
            author: v.authorId ? `用户${v.authorId}` : '未知',
            views: this.formatNumber(v.playCount || 0),
            likes: this.formatNumber(v.likeCount || 0)
          }))
        }
      } catch (error) {
        console.error('加载排行榜失败:', error)
      }
    },
    
    // 将后端视频数据转换为前端需要的格式
    convertVideoData(backendVideo, includeComments = false) {
      return {
        id: backendVideo.id,
        title: backendVideo.title || '无标题',
        url: backendVideo.videoUrl || '',
        thumbnail: backendVideo.coverUrl || '',
        duration: this.formatDuration(backendVideo.duration || 0),
        description: backendVideo.description || '',
        views: backendVideo.playCount || 0,
        likes: backendVideo.likeCount || 0,
        isLiked: false, // 需要单独查询用户是否点赞
        uploadTime: this.formatTime(backendVideo.createTime),
        author: {
          id: backendVideo.authorId || null,
          name: backendVideo.authorId ? `用户${backendVideo.authorId}` : '未知作者',
          avatar: '' // 需要单独查询作者信息
        },
        isFollowing: false, // 需要单独查询是否关注
        comments: includeComments ? [] : [] // 评论需要单独加载
      }
    },
    
    // 格式化时长（秒 -> mm:ss）
    formatDuration(seconds) {
      if (!seconds) return '0:00'
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      return `${mins}:${secs.toString().padStart(2, '0')}`
    },
    
    // 格式化时间（相对时间）
    formatTime(timeStr) {
      if (!timeStr) return '未知'
      const time = new Date(timeStr)
      const now = new Date()
      const diff = now - time
      const seconds = Math.floor(diff / 1000)
      const minutes = Math.floor(seconds / 60)
      const hours = Math.floor(minutes / 60)
      const days = Math.floor(hours / 24)
      
      if (days > 0) return `${days}天前`
      if (hours > 0) return `${hours}小时前`
      if (minutes > 0) return `${minutes}分钟前`
      return '刚刚'
    },
    
    // 格式化数字（如：12580 -> 1.3万）
    formatNumber(num) {
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + '万'
      }
      return num.toString()
    },
    
    // 检查登录状态，如果未登录则提示
    // eslint-disable-next-line no-unused-vars
    checkLogin(action) {
      if (!this.isLogin) {
        this.$message.warning('请先登录后再进行此操作');
        return false;
      }
      return true;
    },
    
    // 处理头像加载错误（静默处理，不显示错误）
    handleAvatarError() {
      // 头像加载失败时，el-avatar会自动显示默认图标，这里不需要处理
      // 只是为了避免控制台报错
      return false
    },
    
    // 初始化视频播放器
    initializeVideoPlayer() {
      this.$nextTick(() => {
      const videoElement = this.$refs.videoPlayer
      if (videoElement) {
        videoElement.addEventListener('loadeddata', () => {
          console.log('视频加载完成')
        })
          
          // 处理视频加载错误（只处理真正的视频错误，不包括图片）
          videoElement.addEventListener('error', (e) => {
            // 延迟检查，确保错误对象已经设置
            setTimeout(() => {
              const error = videoElement.error
              // 只有当是真正的媒体错误时才处理（error.code 有值且不为null/undefined）
              if (error && typeof error.code === 'number' && error.code > 0) {
                let errorMsg = '视频加载失败'
                switch (error.code) {
                  case error.MEDIA_ERR_ABORTED:
                    errorMsg = '视频加载被中止'
                    break
                  case error.MEDIA_ERR_NETWORK:
                    errorMsg = '网络错误，请检查网络连接'
                    break
                  case error.MEDIA_ERR_DECODE:
                    errorMsg = '视频解码失败，请检查视频格式'
                    break
                  case error.MEDIA_ERR_SRC_NOT_SUPPORTED:
                    errorMsg = '视频格式不支持或视频地址无效'
                    break
                  default:
                    // 未知错误码，静默忽略
                    return
                }
                this.$message.error(errorMsg)
                this.isPlaying = false
              }
              // 如果不是视频错误或错误码无效，则不处理（静默忽略）
            }, 100)
          })
        }
      })
    },
    
    // 切换视频播放/暂停
    togglePlayPause() {
      const videoElement = this.$refs.videoPlayer
      if (videoElement) {
        if (videoElement.paused) {
          videoElement.play()
          this.isPlaying = true
        } else {
          videoElement.pause()
          this.isPlaying = false
        }
      }
    },
    
    // 切换视频
    async switchVideo(video) {
      // 先暂停并重置当前视频
      const videoElement = this.$refs.videoPlayer
      if (videoElement) {
        videoElement.pause()
        videoElement.currentTime = 0
      }
      this.isPlaying = false
      
      // 停止当前视频的行为跟踪
      this.stopBehaviorTracking()
      
      // 保存当前视频行为数据
      this.sendUserBehavior()
      
      // 如果传入的视频对象缺少url字段（如从排行榜切换），需要加载完整信息
      if (!video.url || !video.videoUrl) {
        if (video.id) {
          try {
            const res = await userVideoApi.getVideoById(video.id)
            if (res && res.data) {
              video = this.convertVideoData(res.data, true)
            } else {
              this.$message.error('获取视频详情失败')
              return
            }
          } catch (error) {
            console.error('加载视频详情失败:', error)
            this.$message.error('加载视频失败，请稍后重试')
            return
          }
        } else {
          this.$message.error('视频ID不存在')
          return
        }
      }
      
      // 确保视频URL存在
      const videoUrl = video.url || video.videoUrl
      if (!videoUrl) {
        this.$message.error('视频地址不存在')
        return
      }
      
      // 切换到新视频
      this.currentVideo = {
        id: video.id,
        title: video.title || '无标题',
        url: videoUrl,
        thumbnail: video.thumbnail || video.coverUrl || '',
        duration: video.duration || '0:00',
        description: video.description || '',
        views: video.views || video.playCount || 0,
        likes: video.likes || video.likeCount || 0,
        isLiked: video.isLiked || false,
        uploadTime: video.uploadTime || this.formatTime(video.createTime) || '',
        author: video.author || {
          id: video.authorId || null,
          name: video.authorId ? `用户${video.authorId}` : '未知作者',
          avatar: ''
        },
        isFollowing: video.isFollowing || false,
        comments: video.comments || []
      }
      
      // 开始跟踪新视频行为
      this.startBehaviorTracking()
      
      // 等待DOM更新后加载并播放新视频
      await this.$nextTick()
      
      // 重新获取video元素，因为DOM可能已更新（v-if条件）
      const newVideoElement = this.$refs.videoPlayer
      if (newVideoElement && videoUrl) {
        // 确保视频源已设置
        if (newVideoElement.src !== videoUrl) {
          newVideoElement.src = videoUrl
        }
        
        // 强制重新加载视频
        try {
          newVideoElement.load()
          
          // 监听视频加载完成后再播放
          const playHandler = () => {
            newVideoElement.play().then(() => {
          this.isPlaying = true
            }).catch(err => {
              console.error('播放视频失败:', err)
              // 不显示错误提示，可能是用户操作导致的
            })
            newVideoElement.removeEventListener('loadeddata', playHandler)
          }
          
          newVideoElement.addEventListener('loadeddata', playHandler)
          
          // 如果视频已经加载完成，直接播放
          if (newVideoElement.readyState >= 2) {
            newVideoElement.play().then(() => {
              this.isPlaying = true
            }).catch(err => {
              console.error('播放视频失败:', err)
            })
          }
        } catch (err) {
          console.error('加载视频失败:', err)
        }
      }
    },
    
    // 点赞处理
    handleLike() {
      // 检查登录状态
      if (!this.checkLogin('like')) return;
      
      this.likeLoading = true
      
      // 模拟API调用延迟
      setTimeout(() => {
        this.currentVideo.isLiked = !this.currentVideo.isLiked
        this.currentVideo.likes += this.currentVideo.isLiked ? 1 : -1
        
        // 记录点赞行为
        this.recordInteraction('like')
        
        this.likeLoading = false
      }, 300)
    },
    
    // 关注作者
    handleFollow() {
      // 检查登录状态
      if (!this.checkLogin('follow')) return;
      
      this.currentVideo.isFollowing = true
      this.$message.success('关注成功')
      
      // 记录关注行为
      this.recordInteraction('follow')
    },
    
    // 分享视频
    handleShare() {
      // 检查登录状态
      if (!this.checkLogin('share')) return;
      
      this.$message.success('分享功能已调用')
      
      // 记录分享行为
      this.recordInteraction('share')
    },
    
    // 提交评论
    submitComment() {
      // 检查登录状态
      if (!this.checkLogin('comment')) return;
      
      if (!this.newComment.trim()) return
      
      const newComment = {
        id: Date.now(),
        userName: localStorage.getItem('username') || '用户',
        userAvatar: localStorage.getItem('userAvatar') || '', // 使用用户头像，如果没有则为空（el-avatar会显示默认图标）
        content: this.newComment,
        time: '刚刚'
      }
      
      this.currentVideo.comments.unshift(newComment)
      this.newComment = ''
      
      // 记录评论行为
      this.recordInteraction('comment')
    },
    
    // 处理不喜欢
    handleDislike() {
      // 检查登录状态
      if (!this.checkLogin('dislike')) return;
      
      this.$confirm('将减少此类视频的推荐，确定吗？', '不感兴趣', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 发送不喜欢反馈到后端
        this.sendDislikeFeedback()
        
        // 刷新推荐列表
        this.refreshRecommendations()
        
        this.$message.success('已记录您的偏好')
      }).catch(() => {
        // 用户点击取消，什么都不做，直接关闭弹窗
      })
    },
    
    // 刷新推荐
    async refreshRecommendations() {
      this.$message.info('正在为您推荐新内容...')
      try {
        const res = await userVideoApi.getRecommendVideos(
          localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')) : null,
          10
        )
        if (res && res.data && res.data.length > 0) {
          this.recommendedVideos = res.data.map(v => this.convertVideoData(v))
        this.$message.success('推荐内容已更新')
        } else {
          this.$message.warning('暂无新的推荐内容')
        }
      } catch (error) {
        console.error('刷新推荐失败:', error)
        this.$message.error('刷新推荐失败，请稍后重试')
      }
    },
    
    // 刷新当前视频
    refreshVideo() {
      this.switchVideo(this.recommendedVideos[0])
    },
    
    // 改变排行榜类型
    changeRankingType() {
      // 目前只支持加载热门排行榜，可以后续扩展
      this.loadHotRanking()
    },
    
    // 获取排序样式
    getOrderClass(index) {
      if (index === 0) return 'first'
      if (index === 1) return 'second'
      if (index === 2) return 'third'
      return ''
    },
    
    // 视频播放事件处理
    handleTimeUpdate(event) {
      const video = event.target
      const currentTime = video.currentTime
      const duration = video.duration
      
      // 更新播放时长
      this.userBehavior.playDuration = currentTime
      
      // 检查是否完播
      if (currentTime >= duration * 0.9) { // 90%视为完播
        this.userBehavior.isCompleted = true
      }
    },
    
    handleVideoEnded() {
      console.log('视频播放结束')
      this.userBehavior.isCompleted = true
      this.isPlaying = false
      this.sendUserBehavior()
      
      // 自动播放下一个推荐视频
      if (this.recommendedVideos && this.recommendedVideos.length > 0) {
      setTimeout(() => {
          const nextVideo = this.recommendedVideos[0]
          if (nextVideo && nextVideo.id !== this.currentVideo.id) {
            this.switchVideo(nextVideo)
          }
      }, 3000)
      }
    },
    
    handleVideoPlay() {
      console.log('视频开始播放')
      this.isPlaying = true
      this.startBehaviorTracking()
    },
    
    handleVideoPause() {
      console.log('视频暂停')
      this.isPlaying = false
    },
    
    // 开始用户行为跟踪
    startBehaviorTracking() {
      this.userBehavior = {
        videoId: this.currentVideo.id,
        playDuration: 0,
        isCompleted: false
      }
    },
    
    // 停止行为跟踪并发送数据
    stopBehaviorTracking() {
      this.sendUserBehavior()
    },
    
    // 发送用户行为数据到后端
    sendUserBehavior() {
      if (!this.userBehavior.videoId) return
      
      const behaviorData = {
        userId: localStorage.getItem('userId') || 'anonymous',
        videoId: this.userBehavior.videoId,
        behaviorType: 'watch',
        duration: this.userBehavior.playDuration,
        isCompleted: this.userBehavior.isCompleted,
        timestamp: new Date().toISOString()
      }
      
      // 实际应调用后端API
      console.log('发送用户行为数据:', behaviorData)
      // this.$api.userBehavior.collect(behaviorData)
    },
    
    // 记录交互行为
    recordInteraction(type) {
      const interactionData = {
        userId: localStorage.getItem('userId') || 'anonymous',
        videoId: this.currentVideo.id,
        behaviorType: type,
        timestamp: new Date().toISOString()
      }
      
      // 发送到后端
      console.log('发送交互行为:', interactionData)
      // this.$api.userBehavior.collect(interactionData)
    },
    
    // 发送不喜欢反馈
    sendDislikeFeedback() {
      const feedbackData = {
        userId: localStorage.getItem('userId') || 'anonymous',
        videoId: this.currentVideo.id,
        feedbackType: 'dislike',
        timestamp: new Date().toISOString()
      }
      
      console.log('发送不喜欢反馈:', feedbackData)
      // this.$api.feedback.submit(feedbackData)
    }
  }
}
</script>

<style scoped>
.video-player-container {
  display: flex;
  gap: 20px;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  height: calc(100vh - 60px); /* 减去顶部导航高度 */
}

/* 主视频区域 - 放大 */
.main-video-section {
  flex: 1;
  min-width: 0; /* 防止flex溢出 */
}

.main-video-card {
  margin-bottom: 20px;
}

.video-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.video-title {
  margin: 0;
  font-size: 22px;
  color: #333;
  flex: 1;
}

.video-header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* 大视频播放器 */
.video-player-large {
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.video-wrapper-large {
  position: relative;
  width: 100%;
  height: 500px; /* 固定高度，放大的关键 */
  background: #000;
}

.video-element-large {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 保持视频比例 */
  background: #000;
}

.video-overlay-controls {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  background: rgba(0, 0, 0, 0.3);
}

.video-wrapper-large:hover .video-overlay-controls {
  opacity: 1;
}

.play-pause-btn {
  width: 80px;
  height: 80px;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s;
}

.play-pause-btn:hover {
  transform: scale(1.1);
  background: rgba(0, 0, 0, 0.8);
}

.play-pause-btn i {
  font-size: 40px;
  color: white;
}

/* 视频作者信息 */
.video-author-info {
  padding: 20px;
  background: white;
}

.author-main {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.author-details {
  flex: 1;
}

.author-name {
  font-weight: bold;
  font-size: 18px;
  margin-bottom: 5px;
}

.video-stats {
  display: flex;
  gap: 20px;
  color: #666;
  font-size: 14px;
}

.video-description-large {
  line-height: 1.6;
  color: #333;
  font-size: 15px;
  padding: 15px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  margin: 15px 0;
}

/* 视频互动操作 */
.video-actions-large {
  display: flex;
  justify-content: space-around;
  padding: 15px 0;
  border-top: 1px solid #eee;
  background: #ffffff; /* 添加背景色 */
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 10px 20px;
  border-radius: 8px;
  transition: background-color 0.3s;
}

.action-item:hover {
  background-color: #f5f5f5;
}

.action-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: all 0.3s;
}

.action-icon.liked {
  background: #ff4d4f;
  color: white;
}

.action-icon i {
  font-size: 24px;
}

.action-label {
  font-size: 14px;
  color: #666;
}

.action-count {
  font-size: 12px;
  color: #999;
}

/* 评论区 */
.comments-section-large {
  margin-top: 20px;
}

.comments-header {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.comment-input-large {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.comment-input-wrapper {
  flex: 1;
  display: flex;
  gap: 10px;
}

.comment-submit-btn {
  width: 80px;
}

.comment-list-large {
  max-height: 400px;
  overflow-y: auto;
}

.comment-item-large {
  display: flex;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-content-large {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.comment-username {
  font-weight: bold;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-text {
  line-height: 1.5;
  color: #333;
  margin-bottom: 10px;
}

.comment-actions {
  display: flex;
  gap: 15px;
}

.no-comments {
  padding: 40px 0;
  text-align: center;
  color: #999;
}

/* 右侧边栏 */
.right-sidebar {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 推荐视频列表 */
.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 350px; /* 固定高度 */
  overflow-y: auto; /* 添加滚动条 */
  padding-right: 5px; /* 为滚动条留出空间 */
}

/* 滚动条样式 */
.scrollable::-webkit-scrollbar {
  width: 6px;
}

.scrollable::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.scrollable::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.scrollable::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.recommendation-item {
  display: flex;
  gap: 12px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.3s;
}

.recommendation-item:hover {
  background-color: #f5f5f5;
}

.recommendation-thumbnail {
  position: relative;
  flex-shrink: 0;
}

.recommendation-thumbnail img {
  width: 160px;
  height: 90px;
  border-radius: 4px;
  object-fit: cover;
}

.video-duration-small {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 2px;
}

.recommendation-info {
  flex: 1;
}

.recommendation-title {
  font-weight: 500;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.4;
}

.recommendation-meta {
  color: #666;
  font-size: 12px;
  display: flex;
  gap: 4px;
}

/* 热门排行榜（竖向排列，纯文字） */
.ranking-list-vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 350px; /* 固定高度 */
  overflow-y: auto; /* 添加滚动条 */
  padding-right: 5px; /* 为滚动条留出空间 */
}

.ranking-item-vertical {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.ranking-item-vertical:hover {
  background-color: #f5f7fa;
}

.ranking-order-vertical {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
  margin-top: 2px;
}

.ranking-order-vertical.first {
  background: linear-gradient(135deg, #ffd700, #ffa500);
  color: white;
}

.ranking-order-vertical.second {
  background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
  color: white;
}

.ranking-order-vertical.third {
  background: linear-gradient(135deg, #cd7f32, #b87333);
  color: white;
}

.ranking-info-vertical {
  flex: 1;
  min-width: 0;
}

.ranking-title-vertical {
  font-weight: 500;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.4;
  color: #333;
}

.ranking-stats-vertical {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #666;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .video-player-container {
    flex-direction: column;
  }
  
  .right-sidebar {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
  }
  
  .recommendation-card,
  .hot-ranking-card-vertical {
    flex: 1;
    min-width: 300px;
  }
}

@media (max-width: 768px) {
  .video-player-container {
    padding: 10px;
  }
  
  .video-wrapper-large {
    height: 300px;
  }
  
  .video-title {
    font-size: 18px;
  }
  
  .video-actions-large {
    flex-wrap: wrap;
  }
  
  .action-item {
    padding: 10px;
    min-width: 80px;
  }
  
  .right-sidebar {
    flex-direction: column;
  }
  
  .recommendation-thumbnail img {
    width: 120px;
    height: 68px;
  }
}
</style>