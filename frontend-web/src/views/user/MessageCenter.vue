<template>
  <div class="message-center">
    <el-card shadow="never">
      <div slot="header" class="message-header">
        <span style="font-size: 18px; font-weight: bold;">消息中心</span>
        <div>
          <el-button type="text" @click="markAllAsRead" v-if="unreadCount > 0">
            一键已读 ({{ unreadCount }})
          </el-button>
          <el-button-group style="margin-left: 10px;">
            <el-button 
              :type="filterType === null ? 'primary' : ''" 
              size="small"
              @click="filterType = null"
            >
              全部
            </el-button>
            <el-button 
              :type="filterType === 0 ? 'primary' : ''" 
              size="small"
              @click="filterType = 0"
            >
              未读
            </el-button>
            <el-button 
              :type="filterType === 1 ? 'primary' : ''" 
              size="small"
              @click="filterType = 1"
            >
              已读
            </el-button>
          </el-button-group>
        </div>
      </div>

      <div class="message-tabs">
        <div 
          v-for="tab in tabs" 
          :key="tab.type"
          :class="['tab-item', { active: activeTab === tab.type }]"
          @click="activeTab = tab.type; loadMessages()"
        >
          <i :class="tab.icon"></i>
          <span>{{ tab.label }}</span>
        </div>
      </div>

      <div v-loading="loading" class="message-list-container">
        <div v-if="messages.length === 0 && !loading" class="empty-state">
          <i class="el-icon-message"></i>
          <p>暂无消息</p>
        </div>
        <div v-else class="message-list">
          <div 
            v-for="msg in messages" 
            :key="msg.id" 
            class="message-item"
            :class="{ 'unread': msg.isRead === 0 }"
            @click="viewMessageDetail(msg)"
          >
            <div class="message-icon">
              <i :class="getMessageIcon(msg.type)"></i>
            </div>
            <div class="message-content">
              <div class="message-title-row">
                <span class="message-title">{{ msg.title }}</span>
                <el-tag v-if="msg.isRead === 0" type="danger" size="mini">未读</el-tag>
              </div>
              <div class="message-text">{{ msg.content }}</div>
              <div class="message-meta">
                <span class="message-time">{{ formatTime(msg.createTime) }}</span>
                <el-button type="text" size="mini" @click.stop="viewMessageDetail(msg)">
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 消息详情对话框 -->
    <el-dialog
      :title="currentMessage ? currentMessage.title : '消息详情'"
      :visible.sync="showDetailDialog"
      width="600px"
    >
      <div v-if="currentMessage" class="message-detail">
        <div class="detail-meta">
          <el-tag :type="getMessageTypeColor(currentMessage.type)" size="small">
            {{ getMessageTypeName(currentMessage.type) }}
          </el-tag>
          <span class="detail-time">{{ formatTime(currentMessage.createTime) }}</span>
        </div>
        <div class="detail-content">{{ currentMessage.content }}</div>
        <div v-if="currentMessage.relatedVideoId" class="detail-actions">
          <el-button type="primary" @click="goToVideo(currentMessage.relatedVideoId)">
            查看相关视频
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { userVideoApi } from '@/api/user'

export default {
  name: 'MessageCenter',
  data() {
    return {
      messages: [],
      loading: false,
      unreadCount: 0,
      activeTab: 'ALL',
      filterType: null, // null-全部, 0-未读, 1-已读
      tabs: [
        { type: 'ALL', label: '全部消息', icon: 'el-icon-message' },
        { type: 'LIKE', label: '点赞', icon: 'el-icon-thumb' },
        { type: 'COMMENT', label: '评论', icon: 'el-icon-chat-dot-round' },
        { type: 'COLLECT', label: '收藏', icon: 'el-icon-star-on' },
        { type: 'FOLLOW', label: '关注', icon: 'el-icon-user' },
        { type: 'SYSTEM', label: '系统通知', icon: 'el-icon-bell' }
      ],
      showDetailDialog: false,
      currentMessage: null
    }
  },
  created() {
    this.loadUnreadCount()
    this.loadMessages()
  },
  methods: {
    async loadMessages() {
      this.loading = true
      try {
        const params = {
          limit: 50
        }
        if (this.activeTab !== 'ALL') {
          params.type = this.activeTab
        }
        if (this.filterType !== null) {
          params.isRead = this.filterType
        }
        const res = await userVideoApi.getMessageList(params)
        if (res.code === 200) {
          this.messages = res.data || []
        }
      } catch (error) {
        console.error('加载消息列表失败:', error)
        this.$message.error('加载消息列表失败')
      } finally {
        this.loading = false
      }
    },
    async loadUnreadCount() {
      try {
        const res = await userVideoApi.getUnreadMessageCount()
        if (res.code === 200) {
          this.unreadCount = res.data || 0
        }
      } catch (error) {
        console.error('加载未读消息数失败:', error)
      }
    },
    async markAllAsRead() {
      try {
        const res = await userVideoApi.markAllMessagesAsRead()
        if (res.code === 200) {
          this.$message.success('已全部标记为已读')
          this.unreadCount = 0
          this.messages.forEach(msg => msg.isRead = 1)
        }
      } catch (error) {
        console.error('标记已读失败:', error)
        this.$message.error('操作失败')
      }
    },
    async viewMessageDetail(msg) {
      // 如果未读，标记为已读
      if (msg.isRead === 0) {
        try {
          await userVideoApi.markMessageAsRead(msg.id)
          msg.isRead = 1
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        } catch (error) {
          console.error('标记已读失败:', error)
        }
      }
      
      this.currentMessage = msg
      this.showDetailDialog = true
    },
    goToVideo(videoId) {
      this.$router.push(`/main/video/${videoId}`)
      this.showDetailDialog = false
    },
    getMessageIcon(type) {
      const iconMap = {
        'LIKE': 'el-icon-thumb',
        'COMMENT': 'el-icon-chat-dot-round',
        'COLLECT': 'el-icon-star-on',
        'FOLLOW': 'el-icon-user',
        'SYSTEM': 'el-icon-bell'
      }
      return iconMap[type] || 'el-icon-message'
    },
    getMessageTypeName(type) {
      const typeMap = {
        'LIKE': '点赞',
        'COMMENT': '评论',
        'COLLECT': '收藏',
        'FOLLOW': '关注',
        'SYSTEM': '系统通知'
      }
      return typeMap[type] || '消息'
    },
    getMessageTypeColor(type) {
      const colorMap = {
        'LIKE': 'success',
        'COMMENT': 'primary',
        'COLLECT': 'warning',
        'FOLLOW': 'info',
        'SYSTEM': 'danger'
      }
      return colorMap[type] || ''
    },
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const diff = now - date
      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)
      
      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      if (hours < 24) return `${hours}小时前`
      if (days < 7) return `${days}天前`
      return time.substring(0, 19).replace('T', ' ')
    }
  }
}
</script>

<style scoped>
.message-center {
  padding: 20px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}

.tab-item {
  padding: 12px 20px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-item:hover {
  color: #409EFF;
}

.tab-item.active {
  color: #409EFF;
  border-bottom-color: #409EFF;
  font-weight: 600;
}

.message-list-container {
  min-height: 400px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
}

.empty-state i {
  font-size: 64px;
  color: #ddd;
  margin-bottom: 20px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  gap: 15px;
  padding: 15px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.message-item:hover {
  background: #f8f9fa;
  border-color: #409EFF;
}

.message-item.unread {
  background: #f0f7ff;
  border-left: 4px solid #409EFF;
}

.message-icon {
  flex-shrink: 0;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #409EFF;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.message-text {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
  line-height: 1.6;
}

.message-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.message-detail {
  padding: 10px 0;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-time {
  color: #999;
  font-size: 14px;
}

.detail-content {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  margin-bottom: 20px;
}

.detail-actions {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}
</style>
