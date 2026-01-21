<template>
  <div class="video-player-container">
    <div class="main-video-section">
      <el-card class="main-video-card" body-style="padding: 0">

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
            <div v-else class="video-placeholder">
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
        </div>

        <video-meta-info
            v-if="currentVideo.id"
            :video="currentVideo"
            :author="currentVideo.author"
        />

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
import VideoComment from '@/components/VideoComment.vue'
import VideoMetaInfo from '@/components/VideoMetaInfo.vue'

export default {
  name: 'VideoPlayer',
  components: {
    VideoComment,
    VideoMetaInfo
  },
  data() {
    return {
      currentVideo: {
        id: null,
        title: '',
        url: '',
        thumbnail: '',
        duration: '0:00',
        description: '',
        views: 0,
        likes: 0,
        collectCount: 0,
        shareCount: 0,
        danmakuCount: 0,
        createTime: '',
        uploadTime: '',
        author: {
          id: null,
          name: '未知作者',
          avatar: '',
          bio: ''
        }
      },
      recommendedVideos: [],
      hotRanking: [],
      rankingType: 'today',
      isPlaying: true,
      loading: false,
      volume: 100,
      isMuted: false,
      showVolumeSlider: false,
      isFullscreen: false,
      currentTime: '0:00',
      totalTime: '0:00',
      userBehavior: {
        videoId: null,
        playDuration: 0,
        isCompleted: false
      },
      lastRecordTime: null
    }
  },

  computed: {
    isLogin() {
      return !!localStorage.getItem('userToken');
    }
  },

  mounted() {
    const videoId = this.$route.params.id
    if (videoId) {
      // 【修复】直接使用字符串ID，避免parseInt导致Long类型精度丢失
      this.loadVideoById(videoId)
    } else {
      this.loadInitialVideo()
    }
    this.initializeVideoPlayer()
  },

  watch: {
    '$route'(to, from) {
      const videoId = to.params.id
      if (videoId && videoId !== from.params.id) {
        // 【修复】直接使用字符串ID，避免parseInt导致Long类型精度丢失
        this.loadVideoById(videoId)
      }
    }
  },

  beforeDestroy() {
    this.stopBehaviorTracking()
    document.removeEventListener('fullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('webkitfullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('mozfullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('MSFullscreenChange', this.handleFullscreenChange)
  },

  methods: {
    async loadVideoById(videoId) {
      this.loading = true
      try {
        // 【修复】videoId 可能是字符串（Long类型序列化），直接传递即可
        const videoRes = await userVideoApi.getVideoById(String(videoId))
        if (videoRes && videoRes.data) {
          const video = videoRes.data
          
          // 【新增】检查视频状态：未审核的视频显示提示但仍可预览
          if (video.status === 'PENDING' || video.status === 'REJECTED') {
            const statusText = video.status === 'PENDING' ? '审核中' : '已驳回'
            this.$message.warning(`该视频状态：${statusText}，仅限预览`)
          }
          
          this.currentVideo = this.convertVideoData(video)
          this.startBehaviorTracking()

          const recommendRes = await userVideoApi.getRecommendVideos(
              localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')) : null,
              10
          )
          if (recommendRes && recommendRes.data && recommendRes.data.length > 0) {
            this.recommendedVideos = recommendRes.data
                .filter(v => String(v.id) !== String(videoId))
                .map(v => this.convertVideoData(v))
          }
          await this.loadHotRanking()
        } else {
          this.$message.error('视频不存在')
          this.loadInitialVideo()
        }
      } catch (error) {
        console.error('加载视频失败:', error)
        this.$message.error('加载视频失败，请稍后重试')
        this.loadInitialVideo()
      } finally {
        this.loading = false
      }
    },

    async loadInitialVideo() {
      this.loading = true
      try {
        const recommendRes = await userVideoApi.getRecommendVideos(
            localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')) : null,
            10
        )
        if (recommendRes && recommendRes.data && recommendRes.data.length > 0) {
          this.recommendedVideos = recommendRes.data.map(v => this.convertVideoData(v))
          this.currentVideo = this.convertVideoData(recommendRes.data[0])
          this.startBehaviorTracking()
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

    async loadHotRanking() {
      try {
        const hotRes = await userVideoApi.getHotVideos()
        if (hotRes && hotRes.data && hotRes.data.length > 0) {
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
        collectCount: backendVideo.collectCount || 0,
        shareCount: backendVideo.shareCount || 0,
        danmakuCount: 0,

        createTime: backendVideo.createTime || '',
        uploadTime: this.formatTime(backendVideo.createTime),

        author: {
          id: backendVideo.authorId || null,
          name: backendVideo.authorId ? `用户${backendVideo.authorId}` : '未知作者',
          avatar: backendVideo.authorAvatar || '',
          bio: backendVideo.authorBio || ''
        }
      }
    },

    formatDuration(seconds) {
      if (!seconds) return '0:00'
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      return `${mins}:${secs.toString().padStart(2, '0')}`
    },

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

    formatNumber(num) {
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + '万'
      }
      return num.toString()
    },

    checkLogin() {
      if (!this.isLogin) {
        this.$message.warning('请先登录后再进行此操作');
        return false;
      }
      return true;
    },

    initializeVideoPlayer() {
      this.$nextTick(() => {
        const videoElement = this.$refs.videoPlayer
        if (videoElement) {
          const savedVolume = localStorage.getItem('videoVolume')
          if (savedVolume !== null) {
            this.volume = parseInt(savedVolume)
          }
          videoElement.volume = this.volume / 100
          videoElement.muted = this.isMuted

          videoElement.addEventListener('loadeddata', () => {
            if (videoElement.duration) {
              this.totalTime = this.formatDuration(Math.floor(videoElement.duration))
            }
          })

          document.addEventListener('fullscreenchange', this.handleFullscreenChange)
          document.addEventListener('webkitfullscreenchange', this.handleFullscreenChange)
          document.addEventListener('mozfullscreenchange', this.handleFullscreenChange)
          document.addEventListener('MSFullscreenChange', this.handleFullscreenChange)

          videoElement.addEventListener('error', () => {
            setTimeout(() => {
              try {
                const error = videoElement.error
                if (error && typeof error.code === 'number' && error.code > 0) {
                  this.isPlaying = false
                }
              } catch (err) {
                // 修复：添加注释
                console.error(err)
              }
            }, 100)
          }, { once: true })
        }
      })
    },

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

    async switchVideo(video) {
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
        ...this.convertVideoData(video),
        url: videoUrl,
        author: video.author || {
          id: video.authorId || null,
          name: video.authorId ? `用户${video.authorId}` : '未知作者',
          avatar: ''
        }
      }

      this.startBehaviorTracking()

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
            }).catch(() => {
              // 忽略自动播放失败
            })
            newVideoElement.removeEventListener('loadeddata', playHandler)
          }
          newVideoElement.addEventListener('loadeddata', playHandler)

          if (newVideoElement.readyState >= 2) {
            newVideoElement.play().then(() => {
              this.isPlaying = true
            }).catch(() => {
              // 忽略自动播放失败
            })
          }
        } catch (err) {
          console.error('加载视频失败:', err)
        }
      }
    },

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

    refreshVideo() {
      this.switchVideo(this.recommendedVideos[0])
    },

    changeRankingType() {
      this.loadHotRanking()
    },

    getOrderClass(index) {
      if (index === 0) return 'first'
      if (index === 1) return 'second'
      if (index === 2) return 'third'
      return ''
    },

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
/* 布局样式 */
.video-player-container {
  display: flex;
  gap: 20px;
  padding: 20px 10px;
  max-width: 1600px;
  margin: 0 auto;
  height: calc(100vh - 60px);
  width: 100%;
  box-sizing: border-box;
}

.main-video-section {
  flex: 1;
  min-width: 0;
  max-width: calc(100% - 400px);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding-right: 10px;
}

.main-video-card {
  margin-bottom: 20px;
  flex-shrink: 0;
}

/* 视频播放器样式 */
.video-player-large {
  background: #000;
  overflow: hidden;
  margin-bottom: 0;
}

.video-wrapper-large {
  position: relative;
  width: 100%;
  height: 600px;
  background: #000;
  overflow: hidden;
}

.video-element-large {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.video-placeholder {
  width: 100%;
  height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
  color: #fff;
}

/* 播放按钮遮罩 */
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

.video-wrapper-large:hover .video-overlay-controls,
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

/* 底部控制栏 */
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

/* 全屏适配 */
.video-wrapper-large:-webkit-full-screen { width: 100vw; height: 100vh; border-radius: 0; }
.video-wrapper-large:-moz-full-screen { width: 100vw; height: 100vh; border-radius: 0; }
.video-wrapper-large:-ms-fullscreen { width: 100vw; height: 100vh; border-radius: 0; }
.video-wrapper-large:fullscreen { width: 100vw; height: 100vh; border-radius: 0; }

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

/* 推荐列表 */
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

/* 排行榜 */
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

/* 滚动条 */
.scrollable::-webkit-scrollbar { width: 6px; }
.scrollable::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 3px; }
.scrollable::-webkit-scrollbar-thumb { background: #c1c1c1; border-radius: 3px; }
.scrollable::-webkit-scrollbar-thumb:hover { background: #a8a8a8; }

/* 响应式 */
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
  .right-sidebar { flex-direction: column; }
  .recommendation-thumbnail img { width: 120px; height: 68px; }
}
</style>