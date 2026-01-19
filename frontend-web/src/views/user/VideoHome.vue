<template>
  <div class="video-home-container">
    <!-- 顶部筛选栏 - 美化版 -->
    <div class="filter-section">
      <div class="filter-wrapper">
        <div class="filter-left">
          <div class="filter-tabs">
            <button 
              v-for="tab in filterTabs" 
              :key="tab.value"
              :class="['filter-tab', { active: filterType === tab.value }]"
              @click="setFilterType(tab.value)"
            >
              <i :class="tab.icon"></i>
              <span class="tab-text">{{ tab.label }}</span>
              <span v-if="tab.value === 'hot'" class="hot-badge">
                <i class="el-icon-fire"></i>
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 页面装饰元素 -->
    <div class="decoration-elements">
      <div class="floating-element el-1"></div>
      <div class="floating-element el-2"></div>
      <div class="floating-element el-3"></div>
      <div class="glow-effect"></div>
    </div>
    
    <!-- 视频网格列表 -->
    <div class="video-grid-section">
      <!-- 顶部信息栏 -->
      <div class="info-header">
        <div class="header-left">
          <h2 class="section-title">
            <i class="el-icon-video-camera"></i>
            <span class="title-text">热门视频推荐</span>
            <span class="title-badge">
              <i class="el-icon-star"></i>
              精选
            </span>
          </h2>
          <div class="header-stats">
            <div class="stat-item">
              <i class="stat-icon el-icon-video-camera"></i>
              <span class="stat-value">{{ videos.length }}</span>
              <span class="stat-label">个视频</span>
            </div>
            <div class="stat-item">
              <i class="stat-icon el-icon-view"></i>
              <span class="stat-value">{{ totalViews }}</span>
              <span class="stat-label">次播放</span>
            </div>
          </div>
        </div>
        
        <div class="header-right">
          <div class="sort-options">
            <span class="sort-label">排序:</span>
            <button 
              v-for="option in sortOptions" 
              :key="option.value"
              :class="['sort-option', { active: sortType === option.value }]"
              @click="sortType = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-content">
          <div class="bilibili-loader">
            <div class="loader-circle"></div>
            <div class="loader-circle"></div>
            <div class="loader-circle"></div>
            <div class="loader-text">加载中...</div>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-else-if="videos.length === 0" class="empty-container">
        <div class="empty-content">
          <div class="empty-illustration">
            <div class="empty-icon-wrapper">
              <i class="el-icon-video-camera"></i>
              <div class="empty-icon-glow"></div>
            </div>
          </div>
          <p class="empty-title">暂时没有视频内容</p>
          <p class="empty-subtitle">稍后再来看看或者上传你的第一个视频</p>
          <button class="refresh-button" @click="refreshVideos">
            <i class="el-icon-refresh"></i>
            刷新页面
          </button>
        </div>
      </div>
      
      <!-- 视频网格 - 田字格布局 -->
      <div v-else class="video-grid-container">
        <div class="video-grid">
          <!-- 第一个大卡片 (2x2) -->
          <div 
            v-if="videos.length > 0"
            class="video-card featured-card"
            @click="goToVideoDetail(videos[0])"
          >
            <!-- 热门标签 -->
            <div class="featured-badge">
              <div class="badge-content">
                <i class="el-icon-star"></i>
                <span>编辑推荐</span>
              </div>
              <div class="badge-glow"></div>
            </div>
            
            <!-- 视频封面 -->
            <div class="video-cover featured-cover">
              <div class="video-cover-placeholder" v-if="!videos[0].thumbnail">
                <div class="placeholder-effect"></div>
              </div>
              <img 
                v-else
                :src="videos[0].thumbnail" 
                :alt="videos[0].title"
                class="cover-image"
              >
              <div class="play-button-overlay">
                <div class="play-button">
                  <i class="el-icon-video-play"></i>
                </div>
              </div>
            </div>
            
            <!-- 视频信息 -->
            <div class="video-info featured-info">
              <div class="video-title" :title="videos[0].title">
                {{ videos[0].title || '无标题' }}
              </div>
              <div class="video-description">
                精彩视频内容，不容错过！快来一起观看吧！
              </div>
              <div class="video-meta">
                <div class="video-author">
                  <div class="author-details">
                    <span class="author-name">{{ videos[0].author?.name || '未知作者' }}</span>
                    <span class="author-followers">1.2万粉丝</span>
                  </div>
                </div>
                <div class="video-stats">
                  <div class="video-duration-stat">
                    <i class="el-icon-time"></i>
                    <span>{{ videos[0].duration || '0:00' }}</span>
                  </div>
                  <div class="video-views-stat">
                    <i class="el-icon-view"></i>
                    <span>{{ formatViews(videos[0].views || 0) }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 卡片装饰 -->
            <div class="card-glow"></div>
            <div class="card-sparkle s1"></div>
            <div class="card-sparkle s2"></div>
          </div>
          
          <!-- 其他普通卡片 (从第二个视频开始) -->
          <div 
            v-for="(video, index) in videos.slice(1)" 
            :key="video.id"
            class="video-card normal-card"
            :class="`card-${(index % 6) + 1}`"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="goToVideoDetail(video)"
          >
            <!-- 视频封面 -->
            <div class="video-cover normal-cover">
              <div class="video-cover-placeholder" v-if="!video.thumbnail">
                <div class="placeholder-effect"></div>
              </div>
              <img 
                v-else
                :src="video.thumbnail" 
                :alt="video.title"
                class="cover-image"
              >
            </div>
            
            <!-- 视频信息 -->
            <div class="video-info normal-info">
              <div class="video-title" :title="video.title">
                {{ video.title || '无标题' }}
              </div>
              <div class="video-meta">
                <div class="video-author">
                  <div class="author-avatar normal-avatar">
                  </div>
                  <span class="author-name">{{ video.author?.name || '未知作者' }}</span>
                </div>
                <div class="video-stats">
                  <div class="video-duration-stat">
                    <i class="el-icon-time"></i>
                    <span>{{ video.duration || '0:00' }}</span>
                  </div>
                  <div class="video-views-stat">
                    <i class="el-icon-view"></i>
                    <span>{{ formatViews(video.views || 0) }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 卡片装饰 -->
            <div class="card-hover-effect"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { userVideoApi } from '@/api/user'

export default {
  name: 'VideoHome',
  data() {
    return {
      filterType: 'recommend',
      sortType: 'recommend',
      videos: [],
      loading: false,
      filterTabs: [
        { value: 'recommend', label: '推荐', icon: 'el-icon-star-on' },
        { value: 'hot', label: '热门', icon: 'el-icon-fire' },
        { value: 'newest', label: '最新', icon: 'el-icon-time' }
      ],
      sortOptions: [
        { value: 'recommend', label: '综合推荐' },
        { value: 'hot', label: '最多播放' },
        { value: 'newest', label: '最新发布' }
      ]
    }
  },
  
  computed: {
    totalViews() {
      const total = this.videos.reduce((sum, video) => sum + (video.views || 0), 0)
      return this.formatViews(total)
    }
  },
  
  mounted() {
    this.loadVideos()
    // 监听全局搜索事件
    this.$bus = this.$root.$children.find(c => c.$options.name === 'App') || this.$root;
    if (this.$bus) {
      this.$bus.$on('global-search', (keyword) => {
        this.$message.info(`搜索: ${keyword}`)
        // 这里可以添加实际的搜索逻辑
      })
    }
  },
  
  beforeDestroy() {
    if (this.$bus) {
      this.$bus.$off('global-search')
    }
  },
  
  methods: {
    // 加载视频列表
    async loadVideos() {
      this.loading = true
      
      try {
        let response
        const userId = localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')) : null
        
        if (this.filterType === 'hot') {
          response = await userVideoApi.getHotVideos()
        } else {
          response = await userVideoApi.getRecommendVideos(userId, 20)
        }
        
        if (response && response.data) {
          this.videos = response.data.map(video => ({
            id: video.id,
            title: video.title || '无标题',
            thumbnail: video.coverUrl || '',
            duration: this.formatDuration(video.duration || 0),
            views: video.playCount || 0,
            likes: video.likeCount || 0,
            uploadTime: video.createTime,
            author: {
              id: video.authorId || null,
              name: video.authorId ? `用户${video.authorId}` : '未知作者'
            }
          }))
        } else {
          this.videos = []
        }
      } catch (error) {
        console.log('加载视频列表失败:', error.message)
        this.videos = []
      } finally {
        this.loading = false
      }
    },
    
    // 设置筛选类型
    setFilterType(type) {
      this.filterType = type
      this.handleFilterChange()
    },
    
    formatDuration(seconds) {
      if (!seconds) return '0:00'
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      return `${mins}:${secs.toString().padStart(2, '0')}`
    },
    
    formatViews(views) {
      if (views >= 10000) {
        return (views / 10000).toFixed(1) + '万'
      } else if (views >= 1000) {
        return (views / 1000).toFixed(1) + '千'
      }
      return views.toString()
    },
    
    goToVideoDetail(video) {
      this.$router.push({
        path: `/main/video/${video.id}`,
        query: {
          from: 'home'
        }
      })
    },
    
    handleFilterChange() {
      this.loadVideos()
    },
    
    refreshVideos() {
      this.loadVideos()
    }
  }
}
</script>

<style scoped>
/* B站风格 + 田字格大卡片布局 */
.video-home-container {
  padding: 20px 5px 30px 5px;
  background: linear-gradient(135deg, #f5f7fa 0%, #f0f2f5 30%, #e6e9f0 100%);
  min-height: calc(100vh - 60px);
  position: relative;
  overflow-x: hidden;
}

/* 装饰元素 */
.decoration-elements {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.floating-element {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  filter: blur(40px);
  animation: float 20s infinite linear;
}

.floating-element.el-1 {
  width: 300px;
  height: 300px;
  top: 10%;
  left: 5%;
  animation-delay: 0s;
}

.floating-element.el-2 {
  width: 200px;
  height: 200px;
  top: 60%;
  right: 10%;
  animation-delay: 5s;
}

.floating-element.el-3 {
  width: 150px;
  height: 150px;
  top: 30%;
  right: 20%;
  animation-delay: 10s;
}

.glow-effect {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400px;
  background: radial-gradient(ellipse at top, rgba(102, 126, 234, 0.08) 0%, transparent 70%);
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  25% { transform: translate(20px, 20px) rotate(90deg); }
  50% { transform: translate(0, 40px) rotate(180deg); }
  75% { transform: translate(-20px, 20px) rotate(270deg); }
}

/* 筛选区域 - B站风格 */
.filter-section {
  margin-bottom: 30px;
  position: relative;
  z-index: 2;
}

.filter-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  margin: 0 auto;
  width: 100%;
  max-width: 100%;
  padding: 0 10px;
}

.filter-left {
  flex: 1;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 10px 20px;
  border-radius: 20px;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  color: #61666d;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.filter-tab::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.filter-tab:hover {
  background: white;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  color: #00aeec;
}

.filter-tab:hover::before {
  opacity: 1;
}

.filter-tab.active {
  background: linear-gradient(135deg, #00aeec 0%, #0082c8 100%);
  color: white;
  box-shadow: 0 6px 20px rgba(0, 174, 236, 0.3);
  border-color: transparent;
}

.filter-tab.active::before {
  opacity: 0;
}

.filter-tab.active .tab-text {
  color: white;
}

.hot-badge {
  margin-left: 4px;
  background: #ff4d4f;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
}

/* 信息头部 */
.info-header {
  max-width: 1400px;
  margin: 0 auto 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
  padding: 20px 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  color: #18191c;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  position: relative;
}

.section-title i {
  color: #00aeec;
  font-size: 32px;
  text-shadow: 0 0 20px rgba(0, 174, 236, 0.4);
}

.title-text {
  background: linear-gradient(135deg, #18191c 0%, #00aeec 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-badge {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.header-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  color: #00aeec;
  font-size: 16px;
}

.stat-value {
  font-weight: 700;
  color: #18191c;
  font-size: 16px;
}

.stat-label {
  color: #61666d;
  font-size: 14px;
}

.sort-options {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.sort-label {
  color: #61666d;
  font-size: 14px;
}

.sort-option {
  padding: 4px 12px;
  border-radius: 12px;
  border: none;
  background: transparent;
  color: #61666d;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sort-option:hover {
  background: rgba(0, 174, 236, 0.1);
  color: #00aeec;
}

.sort-option.active {
  background: linear-gradient(135deg, #00aeec 0%, #0082c8 100%);
  color: white;
}

/* 视频网格容器 */
.video-grid-container {
  max-width: 1400px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* 田字格布局 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(2, auto);
  gap: 20px;
  grid-auto-rows: minmax(0, auto);
}

/* 特色大卡片 (占据2x2) */
.featured-card {
  grid-column: 1 / span 2;
  grid-row: 1 / span 2;
  background: white;
  border-radius: 24px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.05);
  animation: cardAppear 0.6s ease forwards;
  opacity: 0;
  transform: translateY(20px);
  display: flex;
  flex-direction: column;
}

@keyframes cardAppear {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.featured-card:hover {
  transform: translateY(-10px) scale(1.01);
  box-shadow: 0 25px 60px rgba(0, 174, 236, 0.15), 0 0 0 1px rgba(0, 174, 236, 0.1);
}

.featured-badge {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 3;
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 6px 20px rgba(255, 77, 79, 0.3);
}

.badge-content {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  z-index: 2;
}

.badge-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.3);
  filter: blur(10px);
  border-radius: 20px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 0.8; }
}

.featured-cover {
  height: 0;
  padding-bottom: 56.25%; /* 16:9 宽高比 */
  position: relative;
  overflow: hidden;
  background: #000;
}

.featured-cover .video-cover-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
}

.featured-cover .cover-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain; /* 改为contain，确保完整显示 */
  transition: transform 0.8s ease;
}

.featured-card:hover .cover-image {
  transform: scale(1.05);
}

.play-button-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.featured-card:hover .play-button-overlay {
  opacity: 1;
}

.play-button {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: scale(0.8);
  transition: transform 0.3s ease;
}

.featured-card:hover .play-button {
  transform: scale(1);
}

.play-button i {
  font-size: 36px;
  color: #00aeec;
  margin-left: 4px;
}

.featured-info {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.featured-info .video-title {
  font-size: 24px;
  font-weight: 700;
  color: #18191c;
  margin-bottom: 12px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.video-description {
  color: #61666d;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.featured-info .video-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.featured-info .video-stats {
  display: flex;
  gap: 16px;
}

.video-duration-stat,
.video-views-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #61666d;
  font-size: 14px;
  font-weight: 500;
}

.video-duration-stat i,
.video-views-stat i {
  font-size: 14px;
  color: #9499a0;
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-weight: 600;
  color: #18191c;
  font-size: 16px;
}

.author-followers {
  font-size: 12px;
  color: #9499a0;
}

.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(ellipse at center, rgba(0, 174, 236, 0.1) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.featured-card:hover .card-glow {
  opacity: 1;
}

.card-sparkle {
  position: absolute;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  pointer-events: none;
  opacity: 0;
  filter: blur(2px);
}

.featured-card:hover .card-sparkle {
  animation: sparkle 1.5s ease;
}

.card-sparkle.s1 {
  top: 30%;
  left: 10%;
}

.card-sparkle.s2 {
  top: 20%;
  right: 15%;
}

@keyframes sparkle {
  0% {
    opacity: 0;
    transform: scale(0) rotate(0deg);
  }
  50% {
    opacity: 1;
    transform: scale(1) rotate(180deg);
  }
  100% {
    opacity: 0;
    transform: scale(0) rotate(360deg);
  }
}

/* 普通卡片 */
.normal-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.03);
  opacity: 0;
  animation: cardAppear 0.6s ease forwards;
}

.normal-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(0, 174, 236, 0.12);
  border-color: rgba(0, 174, 236, 0.1);
}

.normal-cover {
  height: 0;
  padding-bottom: 56.25%; /* 16:9 宽高比 */
  position: relative;
  overflow: hidden;
  background: #000;
}

.normal-cover .video-cover-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f5f5 0%, #f0f0f0 100%);
}

.normal-cover .cover-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain; /* 改为contain，确保完整显示 */
  transition: transform 0.6s ease;
}

.normal-card:hover .cover-image {
  transform: scale(1.05);
}

.normal-info {
  padding: 12px;
  height: 90px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.normal-info .video-title {
  font-weight: 600;
  color: #18191c;
  line-height: 1.4;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
  margin-bottom: 12px;
}

.normal-card:hover .video-title {
  color: #00aeec;
}

.normal-info .video-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.normal-info .video-stats {
  display: flex;
  gap: 12px;
  margin-left: auto;
}

.normal-info .video-duration-stat,
.normal-info .video-views-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #61666d;
  font-size: 12px;
  font-weight: 500;
}

.normal-info .video-duration-stat i,
.normal-info .video-views-stat i {
  font-size: 12px;
  color: #9499a0;
}

.normal-avatar {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.normal-info .author-name {
  font-size: 13px;
  color: #61666d;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-hover-effect {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(0, 174, 236, 0.05), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.normal-card:hover .card-hover-effect {
  opacity: 1;
}

/* 卡片颜色变体 */
.card-1:hover {
  border-color: rgba(255, 77, 79, 0.2);
  box-shadow: 0 16px 40px rgba(255, 77, 79, 0.12);
}

.card-2:hover {
  border-color: rgba(0, 174, 236, 0.2);
  box-shadow: 0 16px 40px rgba(0, 174, 236, 0.12);
}

.card-3:hover {
  border-color: rgba(82, 196, 26, 0.2);
  box-shadow: 0 16px 40px rgba(82, 196, 26, 0.12);
}

.card-4:hover {
  border-color: rgba(250, 219, 20, 0.2);
  box-shadow: 0 16px 40px rgba(250, 219, 20, 0.12);
}

.card-5:hover {
  border-color: rgba(114, 46, 209, 0.2);
  box-shadow: 0 16px 40px rgba(114, 46, 209, 0.12);
}

.card-6:hover {
  border-color: rgba(24, 144, 255, 0.2);
  box-shadow: 0 16px 40px rgba(24, 144, 255, 0.12);
}

/* 加载状态 */
.loading-container {
  max-width: 1400px;
  margin: 60px auto;
}

.loading-content {
  text-align: center;
}

.bilibili-loader {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.loader-circle {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #00aeec, #0082c8);
  animation: bounce 1.4s infinite ease-in-out both;
}

.loader-circle:nth-child(2) {
  animation-delay: 0.2s;
}

.loader-circle:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 80%, 100% { 
    transform: scale(0);
  } 
  40% { 
    transform: scale(1.0);
  }
}

.loader-text {
  color: #61666d;
  font-size: 16px;
  font-weight: 500;
}

/* 空状态 */
.empty-container {
  max-width: 1400px;
  margin: 60px auto;
}

.empty-content {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
}

.empty-illustration {
  margin-bottom: 24px;
}

.empty-icon-wrapper {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  position: relative;
}

.empty-icon-wrapper i {
  font-size: 36px;
  color: #00aeec;
}

.empty-icon-glow {
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(ellipse at center, rgba(0, 174, 236, 0.3) 0%, transparent 70%);
  border-radius: 50%;
}

.empty-title {
  color: #18191c;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.empty-subtitle {
  color: #61666d;
  font-size: 14px;
  margin-bottom: 24px;
}

.refresh-button {
  padding: 12px 32px;
  background: linear-gradient(135deg, #00aeec 0%, #0082c8 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 auto;
}

.refresh-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 174, 236, 0.3);
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .filter-wrapper,
  .info-header,
  .video-grid-container {
    max-width: 95%;
  }
  
  .video-grid {
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: auto;
  }
  
  .featured-card {
    grid-column: 1 / span 2;
    grid-row: 1;
  }
}

@media (max-width: 1024px) {
  .video-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .featured-card {
    grid-column: 1 / span 2;
    grid-row: 1;
  }
  
  .featured-cover {
    padding-bottom: 56.25%; /* 保持16:9比例 */
  }
  
  .info-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .video-home-container {
    padding: 15px;
  }
  
  .filter-wrapper {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
  
  .video-grid {
    grid-template-columns: 1fr;
  }
  
  .featured-card {
    grid-column: 1;
    grid-row: 1;
  }
  
  .featured-cover {
    padding-bottom: 56.25%; /* 保持16:9比例 */
  }
  
  .featured-info .video-title {
    font-size: 20px;
  }
  
  .normal-info .video-stats {
    gap: 8px;
  }
  
  .header-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .section-title {
    font-size: 22px;
  }
  
  .filter-tabs {
    justify-content: center;
  }
  
  .featured-cover {
    padding-bottom: 56.25%; /* 保持16:9比例 */
  }
  
  .featured-info .video-title {
    font-size: 18px;
  }
  
  .normal-cover {
    padding-bottom: 56.25%; /* 保持16:9比例 */
  }
  
  .normal-info .video-stats {
    flex-direction: column;
    gap: 4px;
    align-items: flex-end;
  }
}
</style>
