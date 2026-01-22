<template>
  <div class="navigation-container">
    <!-- 搜索栏 - 更突出 -->
    <div class="search-section-enhanced">
      <el-card class="search-card">
        <div class="search-container">
          <div class="search-title">
            <h2><i class="el-icon-search"></i> 发现精彩内容</h2>
            <p>探索海量短视频，发现你的兴趣所在</p>
          </div>
          <div class="search-input-wrapper">
            <el-input 
              placeholder="输入关键词，发现更多精彩视频..."
              prefix-icon="el-icon-search"
              v-model="searchKeyword"
              @keyup.enter.native="handleSearch"
              size="large"
              class="search-input-enhanced"
            >
              <el-button 
                slot="append" 
                icon="el-icon-search" 
                @click="handleSearch"
                type="primary"
                class="search-btn"
              >
                搜索
              </el-button>
            </el-input>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 快速导航 - 视觉强化 -->
    <div class="quick-nav-section-enhanced">
      <div class="section-title">
        <h3><i class="el-icon-s-promotion"></i> 快速导航</h3>
        <p>快速找到你想去的地方</p>
      </div>
      <div class="quick-nav-grid-enhanced">
        <div 
          v-for="nav in quickNavItems" 
          :key="nav.id"
          class="quick-nav-card"
          :style="{ background: nav.bgColor }"
          @click="handleNavClick(nav)"
        >
          <div class="nav-icon-enhanced">
            <i :class="nav.icon"></i>
          </div>
          <div class="nav-info">
            <h4>{{ nav.label }}</h4>
            <p>{{ nav.desc }}</p>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 热门排行榜 + 分类标签 - 横向排列 -->
    <div class="row-section">
      <!-- 热门排行榜 -->
      <div class="ranking-section-enhanced">
        <el-card shadow="always" class="ranking-card-enhanced">
          <div slot="header" class="ranking-header-enhanced">
            <div class="ranking-title">
              <span><i class="el-icon-trophy"></i> 热门排行榜</span>
              <el-tag type="warning" size="small">实时更新</el-tag>
            </div>
          </div>
          <div class="ranking-list-enhanced">
            <div 
              v-for="(item, index) in hotRanking" 
              :key="item.id"
              class="ranking-item-enhanced"
              @click="goToVideo(item)"
            >
              <div class="ranking-badge" :class="getOrderClass(index)">
                <span class="ranking-number">{{ index + 1 }}</span>
              </div>
              <div class="ranking-content-enhanced">
                <div class="ranking-title-text">{{ item.title }}</div>
                <div class="ranking-meta-enhanced">
                  <span class="ranking-author">{{ item.author }}</span>
                  <span class="ranking-stats">
                    <span><i class="el-icon-view"></i> {{ item.views }}</span>
                    <span><i class="el-icon-star-on"></i> {{ item.likes }}</span>
                  </span>
                </div>
              </div>
              <div class="trend-indicator" v-if="item.trending">
                <i :class="item.trending === 'up' ? 'el-icon-top' : 'el-icon-bottom'"></i>
              </div>
            </div>
          </div>
        </el-card>
      </div>
      
      <!-- 分类标签 -->
      <div class="category-section-enhanced">
        <el-card shadow="always" class="category-card-enhanced">
          <div slot="header" class="category-header">
            <span><i class="el-icon-collection"></i> 热门分类</span>
          </div>
          <div class="category-tags-enhanced">
            <div class="category-group" v-for="(group, idx) in categoryGroups" :key="idx">
              <div class="category-label">{{ group.label }}</div>
              <div class="category-items">
                <div 
                  v-for="category in group.items"
                  :key="category.id"
                  class="category-tag-enhanced"
                  :style="{ background: category.color }"
                  @click="handleCategorySelect(category)"
                >
                  <i :class="category.icon"></i>
                  <span class="category-name">{{ category.name }}</span>
                  <span class="category-count">{{ category.count }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
    
    <!-- 热门话题 - 视觉强化 -->
    <div class="topics-section-enhanced" v-if="hotTopics.length > 0">
      <el-card shadow="always" class="topics-card">
        <div slot="header" class="topics-header">
          <span><i class="el-icon-fire"></i> 热门话题</span>
          <el-tag type="danger" size="small">火热</el-tag>
        </div>
        <div class="topics-grid">
          <div 
            v-for="topic in hotTopics" 
            :key="topic.id"
            class="topic-card"
            @click="handleTopicClick(topic)"
          >
            <div class="topic-tag-wrapper">
              <span class="topic-tag">#{{ topic.name }}</span>
              <span class="topic-heat" :style="{ 
                width: topic.heat + '%',
                background: getHeatColor(topic.heat)
              }"></span>
            </div>
            <div class="topic-stats-enhanced">
              <div class="topic-stat-item">
                <i class="el-icon-video-camera"></i>
                <span>{{ topic.videoCount }}</span>
              </div>
              <div class="topic-stat-item">
                <i class="el-icon-chat-dot-round"></i>
                <span>{{ topic.discussionCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 搜索结果区域 -->
    <div class="search-results-section" v-if="showSearchResults">
      <el-card shadow="always" class="search-results-card">
        <div slot="header" class="search-results-header">
          <div class="results-title">
            <i class="el-icon-search"></i>
            <span>搜索结果</span>
            <el-tag type="info" size="small" v-if="searchTotal > 0">共 {{ searchTotal }} 条</el-tag>
          </div>
          <el-button 
            type="text" 
            icon="el-icon-close" 
            @click="showSearchResults = false"
            class="close-results-btn"
          >
            关闭
          </el-button>
        </div>
        
        <!-- 搜索历史 -->
        <div class="search-history" v-if="searchHistory.length > 0 && !searching && searchResults.length === 0">
          <div class="history-header">
            <span>搜索历史</span>
            <el-button type="text" size="mini" @click="clearSearchHistory">清除</el-button>
          </div>
          <div class="history-tags">
            <el-tag 
              v-for="(item, index) in searchHistory" 
              :key="index"
              @click="useHistorySearch(item)"
              class="history-tag"
            >
              {{ item }}
            </el-tag>
          </div>
        </div>
        
        <!-- 搜索结果列表 -->
        <div class="search-results-list" v-if="searching || searchResults.length > 0">
          <div v-if="searching" class="loading-container">
            <i class="el-icon-loading"></i>
            <span>搜索中...</span>
          </div>
          <div v-else-if="searchResults.length === 0" class="no-results">
            <i class="el-icon-search"></i>
            <p>未找到相关视频</p>
            <p class="no-results-tip">试试其他关键词吧</p>
          </div>
          <div v-else class="results-grid">
            <div 
              v-for="video in searchResults" 
              :key="video.id"
              class="result-video-card"
              @click="goToSearchVideo(video)"
            >
              <div class="video-thumbnail">
                <img :src="video.coverUrl || ''" :alt="video.title" />
                <div class="video-duration">{{ formatDuration(video.duration) }}</div>
              </div>
              <div class="video-info">
                <h4 class="video-title">{{ video.title }}</h4>
                <div class="video-meta">
                  <span class="video-author">{{ video.authorName || (video.authorId ? `用户${video.authorId}` : '未知作者') }}</span>
                  <span class="video-stats">
                    <i class="el-icon-view"></i> {{ formatViews(video.playCount || 0) }}
                    <i class="el-icon-star-on"></i> {{ formatViews(video.likeCount || 0) }}
                  </span>
                </div>
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
  name: 'NavigationSystem',
  data() {
    return {
      searchKeyword: '',
      activeCategory: null,
      rankingType: 'today',
      searching: false,
      searchResults: [],
      showSearchResults: false,
      searchTotal: 0,
      
      // 快速导航项 - 增强版
      quickNavItems: [
        { 
          id: 1, 
          label: '首页推荐', 
          icon: 'el-icon-s-home', 
          desc: '发现热门内容',
          bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          route: '/main/video' 
        },
        { 
          id: 2, 
          label: '个人中心', 
          icon: 'el-icon-user', 
          desc: '查看个人资料',
          bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
          route: '/main/profile' 
        }
      ],
      
      // 热门排行榜数据
      hotRanking: [],
      loadingRanking: false,
      
      // 分类数据
      categories: [],
      categoryGroups: [],
      loadingCategories: false,
      
      // 热门话题 - 从标签生成
      hotTopics: [],
      
      // 搜索历史
      searchHistory: []
    }
  },
  
  mounted() {
    this.loadHotRanking()
    this.loadCategories()
    this.loadSearchHistory()
    
    // 监听路由参数，如果有搜索关键词则自动搜索
    if (this.$route.query.search) {
      this.searchKeyword = this.$route.query.search
      this.handleSearch()
    }
  },
  
  methods: {
    // 加载热门排行榜
    async loadHotRanking() {
      this.loadingRanking = true
      try {
        const response = await userVideoApi.getHotVideos()
        if (response && response.data) {
          this.hotRanking = response.data.slice(0, 10).map((video, index) => ({
            id: video.id,
            title: video.title || '无标题',
            author: video.authorName || (video.authorId ? `用户${video.authorId}` : '未知作者'),
            views: this.formatViews(video.playCount || 0),
            likes: this.formatViews(video.likeCount || 0),
            trending: index < 3 ? 'up' : (index % 2 === 0 ? 'new' : null)
          }))
        }
      } catch (error) {
        console.error('加载热门排行榜失败:', error)
        this.$message.error('加载热门排行榜失败')
      } finally {
        this.loadingRanking = false
      }
    },
    
    // 加载分类数据
    async loadCategories() {
      this.loadingCategories = true
      try {
        const response = await userVideoApi.getCategories()
        if (response && response.data) {
          this.categories = response.data
          
          // 生成分类分组（可以根据实际需求调整）
          const colors = [
            'linear-gradient(135deg, #f6d365 0%, #fda085 100%)',
            'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
            'linear-gradient(135deg, #5ee7df 0%, #b490ca 100%)',
            'linear-gradient(135deg, #c2e9fb 0%, #a1c4fd 100%)',
            'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
          ]
          
          const icons = [
            'el-icon-s-comment',
            'el-icon-food',
            'el-icon-location-outline',
            'el-icon-cpu',
            'el-icon-headset',
            'el-icon-basketball'
          ]
          
          // 将分类分组显示
          const allItems = this.categories.map((cat, index) => ({
            id: cat.id,
            name: cat.name,
            count: this.formatCount(cat.videoCount || 0),
            icon: icons[index % icons.length],
            color: colors[index % colors.length]
          }))
          
          // 分成两组
          const mid = Math.ceil(allItems.length / 2)
          this.categoryGroups = [
            {
              label: '热门分类',
              items: allItems.slice(0, mid)
            },
            {
              label: '更多分类',
              items: allItems.slice(mid)
            }
          ]
          
          // 生成热门话题（从分类名称生成）
          this.hotTopics = allItems.slice(0, 6).map((cat, index) => ({
            id: cat.id,
            name: cat.name,
            videoCount: cat.count,
            discussionCount: this.formatCount(Math.floor(Math.random() * 10000)),
            heat: 95 - index * 5
          }))
        }
      } catch (error) {
        console.error('加载分类失败:', error)
        this.$message.error('加载分类失败')
      } finally {
        this.loadingCategories = false
      }
    },
    
    // 搜索视频
    async handleSearch() {
      if (!this.searchKeyword.trim()) {
        this.$message.warning('请输入搜索关键词')
        return
      }
      
      this.searching = true
      this.showSearchResults = true
      
      try {
        // 确保传递正确的参数
        const keyword = this.searchKeyword.trim()
        const categoryId = this.activeCategory || null
        
        const response = await userVideoApi.searchVideos(keyword, categoryId, 1, 20)
        if (response && response.data) {
          this.searchResults = response.data.list || []
          this.searchTotal = response.data.total || 0
          
          // 保存搜索历史
          this.saveSearchHistory(keyword)
          
          if (this.searchResults.length === 0) {
            this.$message.info('未找到相关视频')
          } else {
            this.$message.success(`找到 ${this.searchTotal} 个相关视频`)
          }
        } else {
          this.searchResults = []
          this.searchTotal = 0
          this.$message.info('未找到相关视频')
        }
      } catch (error) {
        console.error('搜索失败:', error)
        this.$message.error('搜索失败，请稍后重试')
        this.searchResults = []
        this.searchTotal = 0
      } finally {
        this.searching = false
      }
    },
    
    // 保存搜索历史
    saveSearchHistory(keyword) {
      if (!keyword) return
      let history = JSON.parse(localStorage.getItem('searchHistory') || '[]')
      // 移除重复项
      history = history.filter(item => item !== keyword)
      // 添加到开头
      history.unshift(keyword)
      // 只保留最近10条
      history = history.slice(0, 10)
      localStorage.setItem('searchHistory', JSON.stringify(history))
      this.searchHistory = history
    },
    
    // 加载搜索历史
    loadSearchHistory() {
      this.searchHistory = JSON.parse(localStorage.getItem('searchHistory') || '[]')
    },
    
    // 使用历史搜索
    useHistorySearch(keyword) {
      this.searchKeyword = keyword
      this.handleSearch()
    },
    
    // 清除搜索历史
    clearSearchHistory() {
      localStorage.removeItem('searchHistory')
      this.searchHistory = []
    },
    
    handleNavClick(nav) {
      if (nav.route) {
        this.$router.push(nav.route)
      }
    },
    
    // 选择分类并搜索
    handleCategorySelect(category) {
      this.activeCategory = category.id
      this.searchKeyword = category.name
      // 清空之前的搜索结果
      this.showSearchResults = false
      this.searchResults = []
      this.handleSearch()
    },
    
    handleTopicClick(topic) {
      this.activeCategory = null // 话题搜索不限制分类
      this.searchKeyword = topic.name
      // 清空之前的搜索结果
      this.showSearchResults = false
      this.searchResults = []
      this.handleSearch()
    },
    
    goToVideo(video) {
      if (video && video.id) {
        this.$router.push({
          path: `/main/video/${video.id}`,
          query: { from: 'navigation' }
        })
      } else {
        console.error('视频数据无效:', video)
        this.$message.warning('视频信息无效，无法跳转')
      }
    },
    
    // 跳转到搜索结果视频
    goToSearchVideo(video) {
      if (video && video.id) {
        this.$router.push({
          path: `/main/video/${video.id}`,
          query: { from: 'search', keyword: this.searchKeyword }
        })
      } else {
        console.error('视频数据无效:', video)
        this.$message.warning('视频信息无效，无法跳转')
      }
    },
    
    getOrderClass(index) {
      if (index === 0) return 'first'
      if (index === 1) return 'second'
      if (index === 2) return 'third'
      return ''
    },
    
    getHeatColor(heat) {
      if (heat >= 90) return '#ff4d4f'
      if (heat >= 80) return '#ff7a45'
      if (heat >= 70) return '#ffa940'
      return '#ffd666'
    },
    
    formatViews(views) {
      if (views >= 10000) {
        return (views / 10000).toFixed(1) + '万'
      } else if (views >= 1000) {
        return (views / 1000).toFixed(1) + '千'
      }
      return views.toString()
    },
    
    formatCount(count) {
      if (count >= 10000) {
        return (count / 10000).toFixed(1) + '万'
      } else if (count >= 1000) {
        return (count / 1000).toFixed(1) + '千'
      }
      return count.toString()
    },
    
    formatDuration(seconds) {
      if (!seconds) return '0:00'
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      return `${mins}:${secs.toString().padStart(2, '0')}`
    }
  }
}
</script>

<style scoped>
/* 整体容器 */
.navigation-container {
  padding: 20px 10px;
  max-width: 1600px;
  margin: 0 auto;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  min-height: calc(100vh - 75px);
  width: 100%;
  box-sizing: border-box;
}

/* 搜索栏 - 增强版 */
.search-section-enhanced {
  margin-bottom: 30px;
}

.search-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  border-radius: 12px;
}

.search-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.search-title {
  text-align: center;
  margin-bottom: 25px;
}

.search-title h2 {
  font-size: 28px;
  margin: 0 0 10px 0;
  font-weight: bold;
}

.search-title p {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

.search-input-wrapper {
  width: 100%;
  max-width: 600px;
}

.search-input-enhanced {
  border-radius: 50px;
  overflow: hidden;
}

.search-input-enhanced >>> .el-input__inner {
  border-radius: 50px 0 0 50px;
  border: none;
  height: 48px;
  font-size: 16px;
}

.search-btn {
  border-radius: 0 50px 50px 0;
  height: 48px;
  font-size: 16px;
  font-weight: bold;
}

/* 快速导航 - 增强版 */
.quick-nav-section-enhanced {
  margin-bottom: 30px;
}

.section-title {
  margin-bottom: 20px;
  padding-left: 10px;
}

.section-title h3 {
  font-size: 22px;
  color: #333;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title p {
  color: #666;
  margin: 0;
  font-size: 14px;
}

.quick-nav-grid-enhanced {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.quick-nav-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 25px;
  border-radius: 16px;
  cursor: pointer;
  color: white;
  transition: all 0.3s ease;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.quick-nav-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
}

.nav-icon-enhanced {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.nav-info h4 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: bold;
}

.nav-info p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

/* 横向排列区域 */
.row-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 30px;
}

/* 热门排行榜 - 增强版 */
.ranking-card-enhanced {
  border-radius: 12px;
  height: 100%;
}

.ranking-header-enhanced {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ranking-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ranking-title span {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.ranking-list-enhanced {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item-enhanced {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  background: #f8f9fa;
}

.ranking-item-enhanced:hover {
  background: #e9ecef;
  transform: translateX(5px);
}

.ranking-badge {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
  color: white;
  flex-shrink: 0;
}

.ranking-badge.first {
  background: linear-gradient(135deg, #ffd700 0%, #ffa500 100%);
}

.ranking-badge.second {
  background: linear-gradient(135deg, #c0c0c0 0%, #a0a0a0 100%);
}

.ranking-badge.third {
  background: linear-gradient(135deg, #cd7f32 0%, #b87333 100%);
}

.ranking-badge:not(.first):not(.second):not(.third) {
  background: linear-gradient(135deg, #6c757d 0%, #495057 100%);
}

.ranking-content-enhanced {
  flex: 1;
  min-width: 0;
}

.ranking-title-text {
  font-weight: 500;
  margin-bottom: 6px;
  font-size: 15px;
  line-height: 1.4;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ranking-meta-enhanced {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
}

.ranking-author {
  flex: 1;
}

.trend-indicator {
  color: #52c41a;
  font-size: 18px;
}

.trend-indicator .el-icon-bottom {
  color: #ff4d4f;
}

/* 分类标签 - 增强版 */
.category-card-enhanced {
  border-radius: 12px;
  height: 100%;
}

.category-header {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.category-tags-enhanced {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.category-group {
  margin-bottom: 15px;
}

.category-label {
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
  font-size: 16px;
  padding-left: 5px;
}

.category-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.category-tag-enhanced {
  padding: 15px 12px;
  border-radius: 10px;
  cursor: pointer;
  color: white;
  text-align: center;
  transition: all 0.3s;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.category-tag-enhanced:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.15);
}

.category-tag-enhanced i {
  font-size: 20px;
  margin-bottom: 8px;
  display: block;
}

.category-name {
  display: block;
  font-weight: 500;
  margin-bottom: 4px;
  font-size: 14px;
}

.category-count {
  font-size: 11px;
  opacity: 0.9;
}

/* 热门话题 - 增强版 */
.topics-section-enhanced {
  margin-bottom: 30px;
}

.topics-card {
  border-radius: 12px;
}

.topics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.topic-card {
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  background: white;
}

.topic-card:hover {
  border-color: #409EFF;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.1);
  transform: translateY(-3px);
}

.topic-tag-wrapper {
  position: relative;
  margin-bottom: 15px;
}

.topic-tag {
  font-weight: bold;
  color: #409EFF;
  font-size: 16px;
  display: block;
  margin-bottom: 8px;
}

.topic-heat {
  position: absolute;
  bottom: -4px;
  left: 0;
  height: 3px;
  border-radius: 3px;
  transition: width 0.3s;
}

.topic-stats-enhanced {
  display: flex;
  justify-content: space-between;
}

.topic-stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #666;
}

/* 搜索结果区域 */
.search-results-section {
  margin-top: 30px;
}

.search-results-card {
  border-radius: 12px;
}

.search-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.results-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.close-results-btn {
  color: #999;
}

.close-results-btn:hover {
  color: #333;
}

/* 搜索历史 */
.search-history {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.history-tag:hover {
  background: #409EFF;
  color: white;
  border-color: #409EFF;
}

/* 搜索结果列表 */
.search-results-list {
  min-height: 200px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #999;
}

.loading-container i {
  font-size: 32px;
  margin-bottom: 10px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.no-results {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.no-results i {
  font-size: 48px;
  margin-bottom: 15px;
  display: block;
  color: #ddd;
}

.no-results p {
  margin: 10px 0;
  font-size: 16px;
}

.no-results-tip {
  font-size: 14px;
  color: #bbb;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.result-video-card {
  cursor: pointer;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.result-video-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 */
  background: #f5f5f5;
  overflow: hidden;
}

.video-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.video-info {
  padding: 12px;
}

.video-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.video-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.video-author {
  flex: 1;
}

.video-stats {
  display: flex;
  gap: 12px;
}

.video-stats i {
  margin-right: 4px;
}

/* 响应式设计 */
@media (min-width: 1600px) {
  .navigation-container {
    max-width: 1800px;
    padding: 20px 40px;
  }
}

@media (max-width: 1400px) {
  .navigation-container {
    max-width: 100%;
    padding: 20px 15px;
  }
}

@media (max-width: 1200px) {
  .navigation-container {
    padding: 20px 10px;
  }
  
  .row-section {
    grid-template-columns: 1fr;
  }
  
  .results-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .navigation-container {
    padding: 15px 10px;
  }
  
  .quick-nav-grid-enhanced {
    grid-template-columns: 1fr;
  }
  
  .search-title h2 {
    font-size: 22px;
  }
  
  .topics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .category-items {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .results-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 15px;
  }
}

@media (max-width: 480px) {
  .topics-grid {
    grid-template-columns: 1fr;
  }
  
  .category-items {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .search-input-wrapper {
    max-width: 100%;
  }
  
  .results-grid {
    grid-template-columns: 1fr;
  }
}
</style>