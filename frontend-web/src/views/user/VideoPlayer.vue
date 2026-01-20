<template>
  <div class="video-player-container">
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

        <div class="video-player-large">
          <div class="video-wrapper-large">
            <video
                ref="videoPlayer"
                :src="currentVideo.url || undefined"
                class="video-element-large"
                :autoplay="!!currentVideo.url"
                @timeupdate="handleTimeUpdate"
                @ended="handleVideoEnded"
                @play="handleVideoPlay"
                @pause="handleVideoPause"
                @loadedmetadata="handleVideoLoaded"
                v-if="currentVideo.url"
            >
              您的浏览器不支持 video 标签。
            </video>
            <div v-else class="video-placeholder" style="width: 100%; height: 500px; display: flex; align-items: center; justify-content: center; background: #000; color: #fff;">
              <span>正在加载视频...</span>
            </div>

            <div class="video-overlay-controls">
              <div class="play-pause-btn" @click="togglePlayPause">
                <i :class="isPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
              </div>
            </div>

            <div class="custom-video-controls">
              <div class="controls-left">
                <button class="control-btn" @click="togglePlayPause">
                  <i :class="isPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
                </button>

                <div class="volume-control" @mouseenter="showVolumeSlider = true" @mouseleave="showVolumeSlider = false">
                  <button class="control-btn" @click="toggleMute">
                    <i :class="isMuted || volume === 0 ? 'el-icon-turn-off-microphone' : 'el-icon-microphone'"></i>
                  </button>
                  <div v-show="showVolumeSlider" class="volume-slider-wrapper">
                    <el-slider
                        v-model="volume"
                        :min="0"
                        :max="100"
                        :step="1"
                        vertical
                        height="100px"
                        @change="handleVolumeChange"
                        class="volume-slider"
                    ></el-slider>
                  </div>
                </div>

                <span class="time-display">
                  {{ currentTime }} / {{ totalTime }}
                </span>
              </div>

              <div class="controls-right">
                <button class="control-btn" @click="toggleFullscreen">
                  <i :class="isFullscreen ? 'el-icon-copy-document' : 'el-icon-full-screen'"></i>
                </button>
              </div>
            </div>
          </div>

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

            <div class="video-description-large">
              {{ currentVideo.description }}
            </div>
          </div>

          <div class="video-actions-large">
            <div class="action-item" @click="handleLike">
              <div class="action-icon" :class="{ 'liked': currentVideo.isLiked }">
                <i :class="currentVideo.isLiked ? 'el-icon-star-on' : 'el-icon-star-off'"></i>
              </div>
              <div class="action-label">点赞</div>
              <div class="action-count">{{ currentVideo.likes }}</div>
            </div>

            <div class="action-item" @click="scrollToComments">
              <div class="action-icon">
                <i class="el-icon-chat-dot-round"></i>
              </div>
              <div class="action-label">评论</div>
              <div class="action-count">{{ currentVideo.commentCount || '抢沙发' }}</div>
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

      <div ref="commentSection">
        <video-comment
            v-if="currentVideo.id"
            :video-id="currentVideo.id"
            :video-author-id="currentVideo.author.id"
        />
      </div>

    </div>

    <div class="right-sidebar">
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
// 【修改点】引入新的评论组件
import VideoComment from '@/components/VideoComment.vue'

export default {
  name: 'VideoPlayer',
  components: {
    VideoComment // 注册组件
  },
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
        commentCount: 0, // 新增评论数
        uploadTime: '',
        author: {
          id: null,
          name: '未知作者',
          avatar: ''
        },
        isFollowing: false
      },

      // 推荐视频列表
      recommendedVideos: [],

      // 热门排行榜（纯文字版）
      hotRanking: [],

      // UI状态
      rankingType: 'today',
      isPlaying: true,
      likeLoading: false,
      loading: false, // 视频加载状态

      // 视频控制相关
      volume: 100, // 音量 0-100
      isMuted: false, // 是否静音
      showVolumeSlider: false, // 显示音量滑块
      isFullscreen: false, // 是否全屏
      currentTime: '0:00', // 当前播放时间
      totalTime: '0:00', // 总时长

      // 用户行为数据
      userBehavior: {
        videoId: null,
        playDuration: 0,
        isCompleted: false
      },

      // 播放记录相关
      lastRecordTime: null // 上次记录播放历史的时间戳
    }
  },

  computed: {
    // 检查用户是否已登录
    isLogin() {
      return !!localStorage.getItem('userToken');
    }
  },

  mounted() {
    // 检查是否有路由参数（从VideoHome跳转过来）
    const videoId = this.$route.params.id
    if (videoId) {
      this.loadVideoById(parseInt(videoId))
    } else {
      this.loadInitialVideo()
    }
    this.initializeVideoPlayer()
  },

  watch: {
    // 监听路由变化，支持从VideoHome跳转到视频详情
    '$route'(to, from) {
      const videoId = to.params.id
      if (videoId && videoId !== from.params.id) {
        this.loadVideoById(parseInt(videoId))
      }
    }
  },

  beforeDestroy() {
    this.stopBehaviorTracking()
    // 移除全屏监听器
    document.removeEventListener('fullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('webkitfullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('mozfullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('MSFullscreenChange', this.handleFullscreenChange)
  },

  methods: {
    // 根据ID加载视频
    async loadVideoById(videoId) {
      this.loading = true
      try {
        // 加载指定视频详情
        const videoRes = await userVideoApi.getVideoById(videoId)
        if (videoRes && videoRes.data) {
          this.currentVideo = this.convertVideoData(videoRes.data)
          this.startBehaviorTracking()

          // 检查是否已点赞
          if (this.isLogin && this.currentVideo.id) {
            await this.checkLikeStatus()
          }

          // 加载推荐视频列表（作为侧边栏推荐）
          const recommendRes = await userVideoApi.getRecommendVideos(
              localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')) : null,
              10
          )
          if (recommendRes && recommendRes.data && recommendRes.data.length > 0) {
            // 过滤掉当前视频
            this.recommendedVideos = recommendRes.data
                .filter(v => v.id !== videoId)
                .map(v => this.convertVideoData(v))
          }

          // 加载热门排行榜
          await this.loadHotRanking()
        } else {
          this.$message.error('视频不存在')
          // 如果视频不存在，回退到加载推荐视频
          this.loadInitialVideo()
        }
      } catch (error) {
        console.error('加载视频失败:', error)
        this.$message.error('加载视频失败，请稍后重试')
        // 出错时回退到加载推荐视频
        this.loadInitialVideo()
      } finally {
        this.loading = false
      }
    },

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
          this.currentVideo = this.convertVideoData(recommendRes.data[0])
          this.startBehaviorTracking()

          // 检查是否已点赞
          if (this.isLogin && this.currentVideo.id) {
            await this.checkLikeStatus()
          }

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
    convertVideoData(backendVideo) {
      return {
        id: backendVideo.id,
        title: backendVideo.title || '无标题',
        url: backendVideo.videoUrl || '',
        thumbnail: backendVideo.coverUrl || '',
        duration: this.formatDuration(backendVideo.duration || 0),
        description: backendVideo.description || '',
        views: backendVideo.playCount || 0,
        likes: backendVideo.likeCount || 0,
        commentCount: backendVideo.commentCount || 0, // 获取评论数
        isLiked: false,
        uploadTime: this.formatTime(backendVideo.createTime),
        author: {
          id: backendVideo.authorId || null,
          name: backendVideo.authorId ? `用户${backendVideo.authorId}` : '未知作者',
          avatar: ''
        },
        isFollowing: false
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

    // 检查登录状态
    checkLogin() {
      if (!this.isLogin) {
        this.$message.warning('请先登录后再进行此操作');
        return false;
      }
      return true;
    },

    // 初始化视频播放器
    initializeVideoPlayer() {
      this.$nextTick(() => {
        const videoElement = this.$refs.videoPlayer
        if (videoElement) {
          // 初始化音量
          const savedVolume = localStorage.getItem('videoVolume')
          if (savedVolume !== null) {
            this.volume = parseInt(savedVolume)
          }
          videoElement.volume = this.volume / 100
          videoElement.muted = this.isMuted

          videoElement.addEventListener('loadeddata', () => {
            console.log('视频加载完成')
            // 更新总时长
            if (videoElement.duration) {
              this.totalTime = this.formatDuration(Math.floor(videoElement.duration))
            }
          })

          // 监听全屏状态变化
          document.addEventListener('fullscreenchange', this.handleFullscreenChange)
          document.addEventListener('webkitfullscreenchange', this.handleFullscreenChange)
          document.addEventListener('mozfullscreenchange', this.handleFullscreenChange)
          document.addEventListener('MSFullscreenChange', this.handleFullscreenChange)

          // 处理视频加载错误
          videoElement.addEventListener('error', () => {
            setTimeout(() => {
              try {
                const error = videoElement.error
                if (error && typeof error.code === 'number' && error.code > 0) {
                  // ... 错误处理逻辑保持不变
                  this.isPlaying = false
                }
              } catch (err) {
                // 静默忽略
              }
            }, 100)
          }, { once: true })
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

      this.stopBehaviorTracking()
      this.sendUserBehavior()

      if (!video.url || !video.videoUrl) {
        if (video.id) {
          try {
            const res = await userVideoApi.getVideoById(video.id)
            if (res && res.data) {
              video = this.convertVideoData(res.data)
            } else {
              this.$message.error('获取视频详情失败')
              return
            }
          } catch (error) {
            console.error('加载视频详情失败:', error)
            return
          }
        } else {
          this.$message.error('视频ID不存在')
          return
        }
      }

      const videoUrl = video.url || video.videoUrl
      if (!videoUrl) {
        this.$message.error('视频地址不存在')
        return
      }

      this.currentVideo = {
        id: video.id,
        title: video.title || '无标题',
        url: videoUrl,
        thumbnail: video.thumbnail || video.coverUrl || '',
        duration: video.duration || '0:00',
        description: video.description || '',
        views: video.views || video.playCount || 0,
        likes: video.likes || video.likeCount || 0,
        commentCount: video.commentCount || 0,
        isLiked: video.isLiked || false,
        uploadTime: video.uploadTime || this.formatTime(video.createTime) || '',
        author: video.author || {
          id: video.authorId || null,
          name: video.authorId ? `用户${video.authorId}` : '未知作者',
          avatar: ''
        },
        isFollowing: video.isFollowing || false
      }

      this.startBehaviorTracking()

      if (this.isLogin && this.currentVideo.id) {
        await this.checkLikeStatus()
      }

      await this.$nextTick()

      const newVideoElement = this.$refs.videoPlayer
      if (newVideoElement && videoUrl) {
        if (newVideoElement.src !== videoUrl) {
          newVideoElement.src = videoUrl
        }

        try {
          newVideoElement.load()
          const playHandler = () => {
            newVideoElement.play().then(() => {
              this.isPlaying = true
            }).catch(() => {})
            newVideoElement.removeEventListener('loadeddata', playHandler)
          }
          newVideoElement.addEventListener('loadeddata', playHandler)

          if (newVideoElement.readyState >= 2) {
            newVideoElement.play().then(() => {
              this.isPlaying = true
            }).catch(() => {})
          }
        } catch (err) {
          console.error('加载视频失败:', err)
        }
      }
    },

    // 点赞处理
    async handleLike() {
      if (!this.checkLogin()) return;

      if (!this.currentVideo.id) {
        this.$message.warning('视频信息不完整')
        return
      }

      this.likeLoading = true

      try {
        if (this.currentVideo.isLiked) {
          const res = await userVideoApi.unlikeVideo(this.currentVideo.id)
          if (res && res.code === 200) {
            this.currentVideo.isLiked = false
            this.currentVideo.likes = Math.max(0, this.currentVideo.likes - 1)
            this.$message.success('已取消点赞')
          }
        } else {
          const res = await userVideoApi.likeVideo(this.currentVideo.id)
          if (res && res.code === 200 && res.data) {
            this.currentVideo.isLiked = true
            this.currentVideo.likes += 1
            this.$message.success('点赞成功')
          } else if (res && res.code === 200 && !res.data) {
            this.$message.info('您已经点赞过了')
          }
        }
      } catch (error) {
        console.error('点赞操作失败:', error)
        this.$message.error('操作失败，请稍后重试')
      } finally {
        this.likeLoading = false
      }
    },

    // 检查是否已点赞
    async checkLikeStatus() {
      if (!this.isLogin || !this.currentVideo.id) return

      try {
        const res = await userVideoApi.checkIsLiked(this.currentVideo.id)
        if (res && res.code === 200) {
          this.currentVideo.isLiked = res.data || false
        }
      } catch (error) {
        console.error('检查点赞状态失败:', error)
      }
    },

    // 关注作者
    handleFollow() {
      if (!this.checkLogin()) return;

      this.currentVideo.isFollowing = true
      this.$message.success('关注成功')
      this.recordInteraction('follow')
    },

    // 分享视频
    handleShare() {
      if (!this.checkLogin()) return;
      this.$message.success('分享功能已调用')
      this.recordInteraction('share')
    },

    // 【修改点】滚动到评论区
    scrollToComments() {
      const commentEl = this.$refs.commentSection;
      if (commentEl) {
        commentEl.scrollIntoView({ behavior: 'smooth' });
      }
    },

    // 处理不喜欢
    handleDislike() {
      if (!this.checkLogin()) return;

      this.$confirm('将减少此类视频的推荐，确定吗？', '不感兴趣', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.sendDislikeFeedback()
        this.refreshRecommendations()
        this.$message.success('已记录您的偏好')
      }).catch(() => {})
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
      }
    },

    // 刷新当前视频
    refreshVideo() {
      this.switchVideo(this.recommendedVideos[0])
    },

    // 改变排行榜类型
    changeRankingType() {
      this.loadHotRanking()
    },

    // 获取排序样式
    getOrderClass(index) {
      if (index === 0) return 'first'
      if (index === 1) return 'second'
      if (index === 2) return 'third'
      return ''
    },

    // 视频播放事件处理（记录播放历史）
    handleTimeUpdate(event) {
      const video = event.target
      const currentTime = video.currentTime
      const duration = video.duration

      this.currentTime = this.formatDuration(Math.floor(currentTime))
      this.totalTime = this.formatDuration(Math.floor(duration))
      this.userBehavior.playDuration = Math.floor(currentTime)

      const progress = duration > 0 ? Math.floor((currentTime / duration) * 100) : 0

      const isFinish = currentTime >= duration * 0.9
      if (isFinish && !this.userBehavior.isCompleted) {
        this.userBehavior.isCompleted = true
        this.recordPlayHistory(this.userBehavior.playDuration, progress, true)
      } else {
        const now = Date.now()
        if (!this.lastRecordTime || now - this.lastRecordTime > 5000) {
          this.lastRecordTime = now
          this.recordPlayHistory(this.userBehavior.playDuration, progress, false)
        }
      }
    },

    handleVideoEnded() {
      console.log('视频播放结束')
      this.userBehavior.isCompleted = true
      this.isPlaying = false

      if (this.$refs.videoPlayer) {
        const video = this.$refs.videoPlayer
        const duration = Math.floor(video.duration || 0)
        this.recordPlayHistory(duration, 100, true)
      }

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
      this.isPlaying = true
      this.startBehaviorTracking()
    },

    handleVideoPause() {
      this.isPlaying = false
    },

    startBehaviorTracking() {
      this.userBehavior = {
        videoId: this.currentVideo.id,
        playDuration: 0,
        isCompleted: false
      }
    },

    stopBehaviorTracking() {
      this.sendUserBehavior()
    },

    async recordPlayHistory(duration, progress, isFinish) {
      if (!this.isLogin || !this.currentVideo.id) return

      try {
        await userVideoApi.recordPlay(
            this.currentVideo.id,
            duration,
            progress,
            isFinish
        )
      } catch (error) {
        console.error('记录播放历史失败:', error)
      }
    },

    sendUserBehavior() {
      if (!this.userBehavior.videoId) return

      this.recordPlayHistory(
          this.userBehavior.playDuration,
          this.userBehavior.isCompleted ? 100 : Math.floor((this.userBehavior.playDuration / (this.$refs.videoPlayer?.duration || 1)) * 100),
          this.userBehavior.isCompleted
      )
    },

    recordInteraction(type) {
      const interactionData = {
        userId: localStorage.getItem('userId') || 'anonymous',
        videoId: this.currentVideo.id,
        behaviorType: type,
        timestamp: new Date().toISOString()
      }
      console.log('发送交互行为:', interactionData)
    },

    sendDislikeFeedback() {
      const feedbackData = {
        userId: localStorage.getItem('userId') || 'anonymous',
        videoId: this.currentVideo.id,
        feedbackType: 'dislike',
        timestamp: new Date().toISOString()
      }
      console.log('发送不喜欢反馈:', feedbackData)
    },

    handleVolumeChange(value) {
      this.volume = value
      const videoElement = this.$refs.videoPlayer
      if (videoElement) {
        videoElement.volume = value / 100
        this.isMuted = value === 0
        videoElement.muted = this.isMuted
        localStorage.setItem('videoVolume', value.toString())
      }
    },

    toggleMute() {
      const videoElement = this.$refs.videoPlayer
      if (videoElement) {
        if (this.isMuted) {
          this.isMuted = false
          videoElement.muted = false
          if (this.volume === 0) {
            this.volume = 50
            videoElement.volume = 0.5
          }
        } else {
          this.isMuted = true
          videoElement.muted = true
        }
      }
    },

    toggleFullscreen() {
      const videoWrapper = this.$el.querySelector('.video-wrapper-large')
      if (!videoWrapper) return

      if (!this.isFullscreen) {
        if (videoWrapper.requestFullscreen) {
          videoWrapper.requestFullscreen()
        } else if (videoWrapper.webkitRequestFullscreen) {
          videoWrapper.webkitRequestFullscreen()
        } else if (videoWrapper.mozRequestFullScreen) {
          videoWrapper.mozRequestFullScreen()
        } else if (videoWrapper.msRequestFullscreen) {
          videoWrapper.msRequestFullscreen()
        }
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen()
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen()
        } else if (document.mozCancelFullScreen) {
          document.mozCancelFullScreen()
        } else if (document.msExitFullscreen) {
          document.msExitFullscreen()
        }
      }
    },

    handleFullscreenChange() {
      this.isFullscreen = !!(
          document.fullscreenElement ||
          document.webkitFullscreenElement ||
          document.mozFullScreenElement ||
          document.msFullscreenElement
      )
    },

    handleVideoLoaded() {
      const videoElement = this.$refs.videoPlayer
      if (videoElement && videoElement.duration) {
        this.totalTime = this.formatDuration(Math.floor(videoElement.duration))
      }
    }
  }
}
</script>

<style scoped>
/* 保持原有布局样式 */
.video-player-container {
  display: flex;
  gap: 20px;
  padding: 20px 10px;
  max-width: 1600px;
  margin: 0 auto;
  height: calc(100vh - 60px); /* 减去顶部导航高度 */
  width: 100%;
  box-sizing: border-box;
}

/* 主视频区域 - 放大 */
.main-video-section {
  flex: 1;
  min-width: 0; /* 防止flex溢出 */
  max-width: calc(100% - 400px); /* 为右侧边栏预留空间，减少留白 */
  display: flex;
  flex-direction: column;
  /* 允许内部滚动，如果内容超出高度 */
  overflow-y: auto;
  padding-right: 10px;
}

.main-video-card {
  margin-bottom: 20px;
  flex-shrink: 0; /* 防止被压缩 */
}

/* ... 之前的 video-player-large 样式完全保持不变 ... */
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

.video-player-large {
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.video-wrapper-large {
  position: relative;
  width: 100%;
  height: 600px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.video-element-large {
  width: 100%;
  height: 100%;
  object-fit: contain;
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

.video-wrapper-large:hover .custom-video-controls {
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

.custom-video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
  z-index: 10;
}

.controls-left, .controls-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.control-btn {
  background: transparent;
  border: none;
  color: white;
  font-size: 20px;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.time-display {
  color: white;
  font-size: 14px;
  font-weight: 500;
  min-width: 100px;
  text-align: center;
}

.volume-control {
  position: relative;
}

.volume-slider-wrapper {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 10px;
  background: rgba(0, 0, 0, 0.8);
  padding: 15px 10px;
  border-radius: 8px;
  z-index: 100;
}

.volume-slider {
  width: 100px;
}

/* 全屏样式 */
.video-wrapper-large:-webkit-full-screen { width: 100vw; height: 100vh; border-radius: 0; }
.video-wrapper-large:-moz-full-screen { width: 100vw; height: 100vh; border-radius: 0; }
.video-wrapper-large:-ms-fullscreen { width: 100vw; height: 100vh; border-radius: 0; }
.video-wrapper-large:fullscreen { width: 100vw; height: 100vh; border-radius: 0; }

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

.author-details { flex: 1; }
.author-name { font-weight: bold; font-size: 18px; margin-bottom: 5px; }
.video-stats { display: flex; gap: 20px; color: #666; font-size: 14px; }

.video-description-large {
  line-height: 1.6;
  color: #333;
  font-size: 15px;
  padding: 15px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  margin: 15px 0;
}

.video-actions-large {
  display: flex;
  justify-content: space-around;
  padding: 15px 0;
  border-top: 1px solid #eee;
  background: #ffffff;
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

.action-item:hover { background-color: #f5f5f5; }

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

.action-icon.liked { background: #ff4d4f; color: white; }
.action-label { font-size: 14px; color: #666; }
.action-count { font-size: 12px; color: #999; }

/* 右侧边栏 */
.right-sidebar {
  width: 360px;
  min-width: 320px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex-shrink: 0;
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

.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 350px;
  overflow-y: auto;
  padding-right: 5px;
}

.recommendation-item {
  display: flex;
  gap: 12px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.3s;
}

.recommendation-item:hover { background-color: #f5f5f5; }

.recommendation-thumbnail { position: relative; flex-shrink: 0; }
.recommendation-thumbnail img { width: 160px; height: 90px; border-radius: 4px; object-fit: cover; }
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

.recommendation-info { flex: 1; }
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
.recommendation-meta { color: #666; font-size: 12px; display: flex; gap: 4px; }

/* 热门排行榜（竖向排列，纯文字） */
.ranking-list-vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 350px;
  overflow-y: auto;
  padding-right: 5px;
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

.ranking-item-vertical:hover { background-color: #f5f7fa; }

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

.ranking-order-vertical.first { background: linear-gradient(135deg, #ffd700, #ffa500); color: white; }
.ranking-order-vertical.second { background: linear-gradient(135deg, #c0c0c0, #a0a0a0); color: white; }
.ranking-order-vertical.third { background: linear-gradient(135deg, #cd7f32, #b87333); color: white; }

.ranking-info-vertical { flex: 1; min-width: 0; }
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
.ranking-stats-vertical { display: flex; gap: 15px; font-size: 12px; color: #666; }

/* 滚动条通用样式 */
.scrollable::-webkit-scrollbar { width: 6px; }
.scrollable::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 3px; }
.scrollable::-webkit-scrollbar-thumb { background: #c1c1c1; border-radius: 3px; }
.scrollable::-webkit-scrollbar-thumb:hover { background: #a8a8a8; }

/* 响应式适配 */
@media (min-width: 1600px) {
  .video-player-container { max-width: 1800px; padding: 20px 40px; }
  .video-wrapper-large { height: 700px; }
}

@media (max-width: 1400px) {
  .video-player-container { max-width: 100%; padding: 20px 15px; }
  .main-video-section { max-width: calc(100% - 360px); }
  .right-sidebar { width: 340px; }
}

@media (max-width: 1200px) {
  .video-player-container { flex-direction: column; padding: 15px 10px; height: auto; }
  .main-video-section { max-width: 100%; overflow: visible; }
  .right-sidebar { width: 100%; flex-direction: row; flex-wrap: wrap; }
  .recommendation-card, .hot-ranking-card-vertical { flex: 1; min-width: 300px; }
  .video-wrapper-large { height: 500px; }
}

@media (max-width: 768px) {
  .video-player-container { padding: 10px 5px; gap: 15px; }
  .video-wrapper-large { height: 400px; }
  .video-title { font-size: 18px; }
  .video-actions-large { flex-wrap: wrap; padding: 10px 5px; gap: 5px; }
  .action-item { padding: 8px 10px; min-width: 70px; }
  .action-icon { width: 40px; height: 40px; }
  .action-icon i { font-size: 20px; }
  .right-sidebar { flex-direction: column; }
  .recommendation-thumbnail img { width: 120px; height: 68px; }
}
</style>