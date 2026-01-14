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
    <div class="topics-section-enhanced">
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
      hotRanking: [
        { id: 1, title: '舞蹈挑战赛冠军诞生！', author: '舞蹈达人', views: '350.2万', likes: '4.5万', trending: 'up' },
        { id: 2, title: '搞笑情景剧：办公室日常', author: '喜剧工厂', views: '289.7万', likes: '3.8万', trending: 'up' },
        { id: 3, title: '旅行vlog', author: '旅行日记', views: '265.4万', likes: '3.2万', trending: 'new' },
        { id: 4, title: '五分钟学会做蛋糕', author: '美食家', views: '240.1万', likes: '2.9万', trending: 'down' },
        { id: 5, title: '新款手机深度评测', author: '科技测评', views: '198.5万', likes: '2.5万', trending: 'up' },
        { id: 6, title: '宠物搞笑瞬间合集', author: '萌宠日记', views: '175.3万', likes: '2.2万', trending: 'new' }
      ],
      
      // 分类数据 - 分组显示
      categoryGroups: [
        {
          label: '生活娱乐',
          items: [
            { id: 1, name: '搞笑', count: '1.2万', icon: 'el-icon-s-comment', color: 'linear-gradient(135deg, #f6d365 0%, #fda085 100%)' },
            { id: 2, name: '美食', count: '8.5千', icon: 'el-icon-food', color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
            { id: 3, name: '旅行', count: '6.3千', icon: 'el-icon-location-outline', color: 'linear-gradient(135deg, #5ee7df 0%, #b490ca 100%)' }
          ]
        },
        {
          label: '知识科技',
          items: [
            { id: 4, name: '科技', count: '4.7千', icon: 'el-icon-cpu', color: 'linear-gradient(135deg, #c2e9fb 0%, #a1c4fd 100%)' },
            { id: 5, name: '音乐', count: '9.1千', icon: 'el-icon-headset', color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
            { id: 6, name: '体育', count: '5.4千', icon: 'el-icon-basketball', color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' }
          ]
        }
      ],
      
      // 热门话题 - 添加热度值
      hotTopics: [
        { id: 1, name: 'vlog挑战', videoCount: '15.2万', discussionCount: '8.5千', heat: 95 },
        { id: 2, name: '萌宠日常', videoCount: '23.7万', discussionCount: '12.3万', heat: 90 },
        { id: 3, name: '美食探店', videoCount: '18.9万', discussionCount: '9.6千', heat: 85 },
        { id: 4, name: '舞蹈教学', videoCount: '12.4万', discussionCount: '7.1千', heat: 80 },
        { id: 5, name: '科技前沿', videoCount: '8.7万', discussionCount: '4.3千', heat: 75 },
        { id: 6, name: '旅行攻略', videoCount: '10.3万', discussionCount: '6.2千', heat: 70 }
      ]
    }
  },
  
  methods: {
    handleSearch() {
      if (this.searchKeyword.trim()) {
        console.log('搜索关键词:', this.searchKeyword)
        this.$message.success(`搜索: ${this.searchKeyword}`)
      }
    },
    
    handleNavClick(nav) {
      if (nav.route) {
        this.$router.push(nav.route)
      }
    },
    
    handleCategorySelect(category) {
      this.activeCategory = category.id
      this.$message.success(`进入 ${category.name} 分类`)
    },
    
    handleTopicClick(topic) {
      this.$message.info(`进入话题 #${topic.name}`)
    },
    
    goToVideo(video) {
      console.log('跳转到视频:', video.title)
      this.$router.push({ 
        path: '/main/video'
      })
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
    }
  }
}
</script>

<style scoped>
/* 整体容器 */
.navigation-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  min-height: 100vh;
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

/* 响应式设计 */
@media (max-width: 1200px) {
  .row-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .navigation-container {
    padding: 15px;
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
}
</style>