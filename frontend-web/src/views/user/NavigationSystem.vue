<template>
  <div class="navigation-container">
    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input 
        placeholder="搜索视频、用户或话题..."
        prefix-icon="el-icon-search"
        v-model="searchKeyword"
        @keyup.enter.native="handleSearch"
        size="medium"
        class="global-search"
      >
        <el-button 
          slot="append" 
          icon="el-icon-search" 
          @click="handleSearch"
          type="primary"
        ></el-button>
      </el-input>
    </div>
    
    <!-- 快速导航 -->
    <div class="quick-nav-section">
      <el-card>
        <div slot="header">
          <span>快速导航</span>
        </div>
        <div class="quick-nav-grid">
          <!-- 只保留首页推荐，新增个人中心 -->
          <div 
            v-for="nav in quickNavItems" 
            :key="nav.id"
            class="quick-nav-item"
            @click="handleNavClick(nav)"
          >
            <div class="nav-icon" :style="{ backgroundColor: nav.color }">
              <i :class="nav.icon"></i>
            </div>
            <span class="nav-label">{{ nav.label }}</span>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 热门排行榜 -->
    <div class="hot-ranking-section">
      <el-card>
        <div slot="header" class="ranking-header">
          <span><i class="el-icon-trophy"></i> 今日热门排行榜</span>
          <el-select 
            v-model="rankingType" 
            size="mini" 
            @change="changeRankingType"
            style="width: 100px;"
          >
            <el-option label="今日" value="today"></el-option>
            <el-option label="本周" value="week"></el-option>
            <el-option label="本月" value="month"></el-option>
          </el-select>
        </div>
        <div class="ranking-list">
          <div 
            v-for="(item, index) in hotRanking" 
            :key="item.id"
            class="ranking-item"
            @click="goToVideo(item)"
          >
            <div class="ranking-order" :class="getOrderClass(index)">
              {{ index + 1 }}
            </div>
            <div class="ranking-content">
              <div class="ranking-title">{{ item.title }}</div>
              <div class="ranking-meta">
                <span class="ranking-author">{{ item.author }}</span>
                <span class="ranking-stats">
                  <span><i class="el-icon-view"></i> {{ item.views }}</span>
                  <span><i class="el-icon-star-on"></i> {{ item.likes }}</span>
                </span>
              </div>
            </div>
            <el-tag 
              v-if="item.trending" 
              size="mini" 
              :type="item.trending === 'up' ? 'success' : 'danger'"
            >
              <i :class="item.trending === 'up' ? 'el-icon-top' : 'el-icon-bottom'"></i>
              {{ item.change }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 分类标签 -->
    <div class="category-section">
      <el-card>
        <div slot="header">
          <span>视频分类</span>
        </div>
        <div class="category-tags">
          <el-tag
            v-for="category in categories"
            :key="category.id"
            :type="activeCategory === category.id ? 'primary' : 'info'"
            @click="handleCategorySelect(category)"
            class="category-tag"
            size="medium"
          >
            {{ category.name }}
            <span class="category-count">{{ category.count }}</span>
          </el-tag>
        </div>
      </el-card>
    </div>
    
    <!-- 热门话题 -->
    <div class="hot-topics-section">
      <el-card>
        <div slot="header">
          <span><i class="el-icon-fire"></i> 热门话题</span>
        </div>
        <div class="hot-topics">
          <div 
            v-for="topic in hotTopics" 
            :key="topic.id"
            class="topic-item"
            @click="handleTopicClick(topic)"
          >
            <div class="topic-tag">#{{ topic.name }}</div>
            <div class="topic-stats">
              <span><i class="el-icon-video-camera"></i> {{ topic.videoCount }}</span>
              <span><i class="el-icon-chat-dot-round"></i> {{ topic.discussionCount }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NavigationSystem',
  data() {
    return {
      searchKeyword: '',
      activeCategory: null,
      rankingType: 'today',
      
      // 快速导航项 - 修改为只保留首页推荐，新增个人中心
      quickNavItems: [
        { id: 1, label: '首页推荐', icon: 'el-icon-s-home', color: '#409EFF', route: '/main/video' },
        { id: 2, label: '个人中心', icon: 'el-icon-user', color: '#e6a23c', route: '/main/profile' }
      ],
      
      // 热门排行榜数据
      hotRanking: [
        { id: 1, title: '舞蹈挑战赛冠军诞生！', author: '舞蹈达人', views: '350.2万', likes: '4.5万', trending: 'up', change: '+2' },
        { id: 2, title: '搞笑情景剧：办公室日常', author: '喜剧工厂', views: '289.7万', likes: '3.8万', trending: 'up', change: '+3' },
        { id: 3, title: '旅行vlog', author: '旅行日记', views: '265.4万', likes: '3.2万', trending: 'new' },
        { id: 4, title: '五分钟学会做蛋糕', author: '美食家', views: '240.1万', likes: '2.9万', trending: 'down', change: '-1' },
        { id: 5, title: '新款手机深度评测', author: '科技测评', views: '198.5万', likes: '2.5万', trending: 'up', change: '+1' },
        { id: 6, title: '宠物搞笑瞬间合集', author: '萌宠日记', views: '175.3万', likes: '2.2万', trending: 'new' },
        { id: 7, title: '健身教程', author: '健身教练', views: '162.8万', likes: '2.0万', trending: 'steady' },
        { id: 8, title: '流行歌曲翻唱', author: '音乐人', views: '148.6万', likes: '1.8万', trending: 'up', change: '+2' }
      ],
      
      // 分类数据
      categories: [
        { id: 1, name: '搞笑', count: '1.2万', icon: 'el-icon-s-comment' },
        { id: 2, name: '美食', count: '8.5千', icon: 'el-icon-food' },
        { id: 3, name: '旅行', count: '6.3千', icon: 'el-icon-location-outline' },
        { id: 4, name: '科技', count: '4.7千', icon: 'el-icon-cpu' },
        { id: 5, name: '音乐', count: '9.1千', icon: 'el-icon-headset' },
        { id: 6, name: '体育', count: '5.4千', icon: 'el-icon-basketball' },
        { id: 7, name: '舞蹈', count: '7.8千', icon: 'el-icon-s-promotion' },
        { id: 8, name: '萌宠', count: '10.2万', icon: 'el-icon-sugar' },
        { id: 9, name: '教育', count: '3.9千', icon: 'el-icon-reading' },
        { id: 10, name: '时尚', count: '5.6千', icon: 'el-icon-present' },
        { id: 11, name: '游戏', count: '12.5万', icon: 'el-icon-ice-cream' },
        { id: 12, name: '影视', count: '6.8千', icon: 'el-icon-video-camera' }
      ],
      
      // 热门话题
      hotTopics: [
        { id: 1, name: 'vlog挑战', videoCount: '15.2万', discussionCount: '8.5千' },
        { id: 2, name: '萌宠日常', videoCount: '23.7万', discussionCount: '12.3万' },
        { id: 3, name: '美食探店', videoCount: '18.9万', discussionCount: '9.6千' },
        { id: 4, name: '舞蹈教学', videoCount: '12.4万', discussionCount: '7.1千' },
        { id: 5, name: '科技前沿', videoCount: '8.7万', discussionCount: '4.3千' },
        { id: 6, name: '旅行攻略', videoCount: '10.3万', discussionCount: '6.2千' }
      ],
      
      // 搜索历史
      searchHistory: []
    }
  },
  
  created() {
    this.loadHotRanking()
    this.loadSearchHistory()
  },
  
  methods: {
    // 搜索处理
    handleSearch() {
      if (!this.searchKeyword.trim()) {
        this.$message.warning('请输入搜索关键词')
        return
      }
      
      // 记录搜索行为
      this.recordSearchBehavior(this.searchKeyword)
      
      // 保存到搜索历史
      this.addToSearchHistory(this.searchKeyword)
      
      // 执行搜索（这里应该调用搜索API）
      console.log('搜索关键词:', this.searchKeyword)
      this.$message.success(`搜索: ${this.searchKeyword}`)
      
      // 实际应用中，这里应该跳转到搜索结果页面
      // this.$router.push({ path: '/main/search', query: { q: this.searchKeyword } })
    },
    
    // 快速导航点击
    handleNavClick(nav) {
      if (nav.route) {
        this.$router.push(nav.route)
      }
    },
    
    // 分类选择
    handleCategorySelect(category) {
      this.activeCategory = category.id
      
      // 记录分类浏览行为
      this.recordCategoryView(category)
      
      // 跳转到分类页面或过滤视频
      this.$message.success(`进入 ${category.name} 分类`)
      
      // 实际应用中，这里应该加载该分类的视频
      // this.loadCategoryVideos(category.id)
    },
    
    // 话题点击
    handleTopicClick(topic) {
      console.log('点击话题:', topic.name)
      
      // 记录话题点击行为
      this.recordTopicClick(topic)
      
      // 跳转到话题页面
      this.$message.info(`进入话题 #${topic.name}`)
    },
    
    // 跳转到视频
    goToVideo(video) {
      console.log('跳转到视频:', video.title)
      
      // 记录排行榜点击行为
      this.recordRankingClick(video)
      
      // 实际应用中，这里应该跳转到视频播放页
      this.$router.push({ 
        path: '/main/video', 
        query: { id: video.id }
      })
    },
    
    // 改变排行榜类型
    changeRankingType() {
      this.loadHotRanking()
    },
    
    // 加载热门排行榜
    loadHotRanking() {
      console.log(`加载${this.rankingType}热门排行榜`)
      
      // 实际应调用后端API
      // this.$api.video.getHotRanking({ type: this.rankingType }).then(res => {
      //   this.hotRanking = res.data
      // })
    },
    
    // 获取排序样式
    getOrderClass(index) {
      if (index === 0) return 'first'
      if (index === 1) return 'second'
      if (index === 2) return 'third'
      return ''
    },
    
    // 加载搜索历史
    loadSearchHistory() {
      const history = localStorage.getItem('searchHistory')
      if (history) {
        this.searchHistory = JSON.parse(history)
      }
    },
    
    // 添加到搜索历史
    addToSearchHistory(keyword) {
      // 去重
      const index = this.searchHistory.indexOf(keyword)
      if (index > -1) {
        this.searchHistory.splice(index, 1)
      }
      
      // 添加到开头
      this.searchHistory.unshift(keyword)
      
      // 限制数量
      if (this.searchHistory.length > 10) {
        this.searchHistory = this.searchHistory.slice(0, 10)
      }
      
      // 保存到localStorage
      localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory))
    },
    
    // 记录搜索行为
    recordSearchBehavior(keyword) {
      const behaviorData = {
        userId: localStorage.getItem('userId') || 'anonymous',
        behaviorType: 'search',
        keyword: keyword,
        timestamp: new Date().toISOString()
      }
      
      // 发送到后端
      console.log('记录搜索行为:', behaviorData)
      // this.$api.userBehavior.collect(behaviorData)
    },
    
    // 记录分类浏览行为
    recordCategoryView(category) {
      const behaviorData = {
        userId: localStorage.getItem('userId') || 'anonymous',
        behaviorType: 'category_view',
        categoryId: category.id,
        categoryName: category.name,
        timestamp: new Date().toISOString()
      }
      
      console.log('记录分类浏览行为:', behaviorData)
      // this.$api.userBehavior.collect(behaviorData)
    },
    
    // 记录话题点击行为
    recordTopicClick(topic) {
      const behaviorData = {
        userId: localStorage.getItem('userId') || 'anonymous',
        behaviorType: 'topic_click',
        topicId: topic.id,
        topicName: topic.name,
        timestamp: new Date().toISOString()
      }
      
      console.log('记录话题点击行为:', behaviorData)
      // this.$api.userBehavior.collect(behaviorData)
    },
    
    // 记录排行榜点击行为
    recordRankingClick(video) {
      const behaviorData = {
        userId: localStorage.getItem('userId') || 'anonymous',
        behaviorType: 'ranking_click',
        videoId: video.id,
        rankingPosition: this.hotRanking.findIndex(v => v.id === video.id) + 1,
        timestamp: new Date().toISOString()
      }
      
      console.log('记录排行榜点击行为:', behaviorData)
      // this.$api.userBehavior.collect(behaviorData)
    }
  }
}
</script>

<style scoped>
.navigation-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.search-section {
  margin-bottom: 20px;
}

.global-search {
  max-width: 600px;
  margin: 0 auto;
}

/* 快速导航 - 修改为两列布局 */
.quick-nav-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.quick-nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s;
  background-color: #f8f9fa;
}

.quick-nav-item:hover {
  background-color: #e9ecef;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.nav-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.nav-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 热门排行榜 */
.ranking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.ranking-item:hover {
  background-color: #f5f7fa;
}

.ranking-order {
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
}

.ranking-order.first {
  background: linear-gradient(135deg, #ffd700, #ffa500);
  color: white;
}

.ranking-order.second {
  background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
  color: white;
}

.ranking-order.third {
  background: linear-gradient(135deg, #cd7f32, #b87333);
  color: white;
}

.ranking-content {
  flex: 1;
  min-width: 0;
}

.ranking-title {
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
}

.ranking-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
}

.ranking-author {
  flex: 1;
}

.ranking-stats {
  display: flex;
  gap: 8px;
}

/* 分类标签 */
.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-tag {
  cursor: pointer;
  padding: 8px 12px;
  transition: all 0.3s;
}

.category-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.category-count {
  margin-left: 6px;
  font-size: 12px;
  opacity: 0.8;
}

/* 热门话题 */
.hot-topics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topic-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.topic-item:hover {
  background-color: #f5f7fa;
}

.topic-tag {
  font-weight: bold;
  color: #409EFF;
  font-size: 14px;
}

.topic-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .navigation-container {
    padding: 10px;
  }
  
  .quick-nav-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .quick-nav-item {
    padding: 15px;
  }
  
  .nav-icon {
    width: 50px;
    height: 50px;
    font-size: 20px;
  }
  
  .ranking-item {
    padding: 8px;
  }
  
  .category-tag {
    padding: 6px 10px;
    font-size: 13px;
  }
}
</style>