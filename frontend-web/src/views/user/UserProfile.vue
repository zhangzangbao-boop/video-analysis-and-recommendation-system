<template>
  <div class="profile-container">
    <div v-if="!isLogin" class="not-logged-card">
      <el-card class="not-logged-card-inner">
        <div class="not-logged-content">
          <div class="not-logged-icon"><i class="el-icon-user"></i></div>
          <h3 class="not-logged-title">您尚未登录</h3>
          <p class="not-logged-desc">登录后可查看个人资料、播放历史和账号设置</p>
          <div class="not-logged-actions">
            <el-button type="primary" size="large" icon="el-icon-user" @click="$router.push('/login')" class="login-btn">立即登录</el-button>
            <el-button type="text" size="large" @click="$router.push('/main/video')">返回首页</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <div v-else>
      <el-card class="profile-main-card">
        <div class="profile-header-enhanced">
          <div class="avatar-section-enhanced">
            <div class="avatar-wrapper">
              <el-avatar :size="120" :src="userInfo.avatar" class="user-avatar-enhanced">{{ userInfo.username.charAt(0) }}</el-avatar>
              <div class="avatar-badge"><i class="el-icon-star-on"></i><span>活跃用户</span></div>
            </div>
          </div>
          <div class="user-info-enhanced">
            <div class="user-title-section">
              <h1 class="username-enhanced">{{ userInfo.username }}</h1>
              <el-tag type="success" size="small">在线</el-tag>
            </div>
            <div class="user-bio-enhanced">{{ userInfo.bio || '这个人很懒，还没有写简介哦~' }}</div>
            <div class="stats-cards">
              <div class="stat-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);" @click="showFollowingList = true">
                <div class="stat-icon"><i class="el-icon-star-on"></i></div>
                <div class="stat-content">
                  <div class="stat-number">{{ userInfo.followCount || 0 }}</div>
                  <div class="stat-label">关注</div>
                </div>
              </div>
              <div class="stat-card" style="background: linear-gradient(135deg, #5ee7df 0%, #b490ca 100%);" @click="showFansList = true">
                <div class="stat-icon"><i class="el-icon-user"></i></div>
                <div class="stat-content">
                  <div class="stat-number">{{ userInfo.fansCount || 0 }}</div>
                  <div class="stat-label">粉丝</div>
                </div>
              </div>
            </div>
          </div>
          <div class="quick-actions">
            <el-button type="primary" icon="el-icon-edit" class="action-btn">编辑资料</el-button>
            <el-button icon="el-icon-setting" class="action-btn">账号设置</el-button>
          </div>
        </div>
      </el-card>

      <el-card class="tabs-card">
        <div class="custom-tabs">
          <div
              v-for="tab in tabs"
              :key="tab.name"
              class="custom-tab-item"
              :class="{ active: activeTab === tab.name }"
              @click="handleTabClick(tab.name)"
          >
            <i :class="tab.icon"></i>
            <span>{{ tab.label }}</span>
            <div class="tab-indicator" v-if="activeTab === tab.name"></div>
          </div>
        </div>

        <div class="tab-content">
          <div v-show="activeTab === 'works'" class="tab-pane enhanced">
            <div class="content-header">
              <h3><i class="el-icon-video-camera"></i> 我的作品 <span class="count-badge">({{ worksTotal }})</span></h3>
              <p>管理您上传的视频，查看审核状态</p>
            </div>

            <div v-loading="loadingWorks">
              <el-table 
                :data="myWorksList" 
                style="width: 100%" 
                v-if="myWorksList.length > 0" 
                border 
                stripe
                @row-click="handleRowClick"
                :row-style="{ cursor: 'pointer' }"
              >
                <el-table-column label="封面" width="140" align="center">
                  <template slot-scope="scope">
                    <img 
                      :src="scope.row.coverUrl" 
                      style="width: 100px; height: 56px; object-fit: cover; border-radius: 4px; cursor: pointer;"
                      @click.stop="goToVideo(scope.row.id)"
                    >
                  </template>
                </el-table-column>

                <el-table-column prop="title" label="标题" min-width="200">
                  <template slot-scope="scope">
                    <span 
                      style="font-weight: bold; cursor: pointer; color: #409EFF;"
                      @click.stop="goToVideo(scope.row.id)"
                    >
                      {{ scope.row.title }}
                    </span>
                  </template>
                </el-table-column>

                <el-table-column label="状态" width="150" align="center">
                  <template slot-scope="scope">
                    <el-tag v-if="scope.row.status === 'PASSED'" type="success" size="small">已发布</el-tag>
                    <el-tag v-else-if="scope.row.status === 'PENDING'" type="warning" size="small">审核中</el-tag>

                    <el-tooltip
                        v-else-if="scope.row.status === 'REJECTED'"
                        class="item"
                        effect="dark"
                        :content="scope.row.auditMsg || '管理员未填写驳回原因'"
                        placement="top">
                      <el-tag type="danger" size="small" style="cursor: pointer;">
                        已驳回 <i class="el-icon-question"></i>
                      </el-tag>
                    </el-tooltip>
                  </template>
                </el-table-column>

                <el-table-column prop="createTime" label="上传时间" width="180" align="center" :formatter="formatDate"></el-table-column>

                <el-table-column label="操作" width="120" align="center">
                  <template slot-scope="scope">
                    <el-button 
                      type="text" 
                      style="color: #409EFF; margin-right: 10px;" 
                      icon="el-icon-video-play" 
                      @click.stop="goToVideo(scope.row.id)"
                    >
                      播放
                    </el-button>
                    <el-button 
                      type="text" 
                      style="color: #F56C6C" 
                      icon="el-icon-delete" 
                      @click.stop="handleDeleteWork(scope.row)"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div v-else class="empty-state">
                <div class="empty-icon"><i class="el-icon-folder-opened"></i></div>
                <h4>暂无作品</h4>
                <p>快去上传你的第一个视频吧</p>
                <el-button type="primary" size="medium" @click="goToUpload">去上传</el-button>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'upload'" class="tab-pane enhanced">
            <div class="content-header">
              <h3><i class="el-icon-upload"></i> 上传视频</h3>
              <p>分享你的精彩瞬间</p>
            </div>
            <div class="upload-video-section">
              <el-form ref="uploadForm" :model="uploadForm" :rules="uploadRules" label-width="100px" class="upload-form">
                <el-form-item label="视频标题" prop="title">
                  <el-input v-model="uploadForm.title" placeholder="请输入视频标题" maxlength="50" show-word-limit></el-input>
                </el-form-item>
                <el-form-item label="视频介绍" prop="description">
                  <el-input v-model="uploadForm.description" type="textarea" :rows="4" placeholder="简单介绍一下你的视频吧" maxlength="500" show-word-limit></el-input>
                </el-form-item>
                <el-form-item label="视频分类" prop="categoryId">
                  <el-select v-model="uploadForm.categoryId" placeholder="请选择分类" style="width: 100%">
                    <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"></el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="上传视频" prop="videoFile">
                  <div class="upload-area" @click="triggerFileInput">
                    <div v-if="!uploadForm.videoFile" class="upload-placeholder">
                      <i class="el-icon-upload"></i>
                      <p>点击上传视频 (MP4)</p>
                    </div>
                    <div v-else class="upload-preview">
                      <i class="el-icon-video-play" style="font-size: 24px; margin-right: 10px; color: #409EFF;"></i>
                      <span>{{ uploadForm.videoFile.name }}</span>
                    </div>
                  </div>
                  <input type="file" ref="fileInput" accept="video/mp4,video/quicktime" @change="handleFileSelect" style="display: none;">
                </el-form-item>
                <el-form-item label="上传封面" prop="coverFile">
                  <div class="upload-area" @click="triggerCoverInput" style="min-height: 120px;">
                    <div v-if="!uploadForm.coverFile && !uploadForm.coverPreview" class="upload-placeholder">
                      <i class="el-icon-picture"></i>
                      <p>点击上传封面 (JPG/PNG)</p>
                    </div>
                    <div v-else class="cover-preview">
                      <img :src="uploadForm.coverPreview" style="max-height: 120px; object-fit: contain;">
                    </div>
                  </div>
                  <input type="file" ref="coverInput" accept="image/jpeg,image/png" @change="handleCoverSelect" style="display: none;">
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="uploading" @click="submitUpload" class="submit-btn">发布视频</el-button>
                  <el-button @click="resetUploadForm">重置</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <!-- 播放历史 -->
          <div v-show="activeTab === 'history'" class="tab-pane enhanced">
            <div class="content-header">
              <h3>播放历史</h3>
              <el-button size="small" @click="loadPlayHistory">刷新</el-button>
            </div>
            <div v-if="loadingHistory" class="loading-state">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            <div v-else-if="playHistoryList.length === 0" class="empty-state">
              <div class="empty-icon"><i class="el-icon-time"></i></div>
              <h4>暂无播放历史</h4>
              <p>您还没有观看过任何视频</p>
            </div>
            <div v-else class="video-list">
              <div 
                v-for="video in playHistoryList" 
                :key="video.id" 
                class="video-item"
                @click="goToVideo(video.id)"
              >
                <div class="video-thumbnail">
                  <img :src="video.coverUrl || video.thumbnail" :alt="video.title">
                  <div class="video-duration">{{ formatDuration(video.duration) }}</div>
                </div>
                <div class="video-info">
                  <h4 class="video-title">{{ video.title }}</h4>
                  <div class="video-meta">
                    <span><i class="el-icon-view"></i> {{ formatNumber(video.playCount || 0) }}</span>
                    <span><i class="el-icon-star-on"></i> {{ formatNumber(video.likeCount || 0) }}</span>
                    <span><i class="el-icon-time"></i> {{ formatTime(video.createTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 点赞记录 -->
          <div v-show="activeTab === 'likes'" class="tab-pane enhanced">
            <div class="content-header">
              <h3>点赞记录</h3>
              <el-button size="small" @click="loadLikedVideos">刷新</el-button>
            </div>
            <div v-if="loadingLikes" class="loading-state">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            <div v-else-if="likedVideosList.length === 0" class="empty-state">
              <div class="empty-icon"><i class="el-icon-star-on"></i></div>
              <h4>暂无点赞记录</h4>
              <p>您还没有点赞过任何视频</p>
            </div>
            <div v-else class="video-list">
              <div 
                v-for="video in likedVideosList" 
                :key="video.id" 
                class="video-item"
                @click="goToVideo(video.id)"
              >
                <div class="video-thumbnail">
                  <img :src="video.coverUrl || video.thumbnail" :alt="video.title">
                  <div class="video-duration">{{ formatDuration(video.duration) }}</div>
                </div>
                <div class="video-info">
                  <h4 class="video-title">{{ video.title }}</h4>
                  <div class="video-meta">
                    <span><i class="el-icon-view"></i> {{ formatNumber(video.playCount || 0) }}</span>
                    <span><i class="el-icon-star-on"></i> {{ formatNumber(video.likeCount || 0) }}</span>
                    <span><i class="el-icon-time"></i> {{ formatTime(video.createTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 评论记录 -->
          <div v-show="activeTab === 'comments'" class="tab-pane enhanced">
            <div class="content-header">
              <h3>评论记录</h3>
              <el-button size="small" @click="loadMyComments">刷新</el-button>
            </div>
            <div v-if="loadingComments" class="loading-state">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            <div v-else-if="myCommentsList.length === 0" class="empty-state">
              <div class="empty-icon"><i class="el-icon-chat-dot-round"></i></div>
              <h4>暂无评论记录</h4>
              <p>您还没有评论过任何视频</p>
            </div>
            <div v-else class="comment-list">
              <div 
                v-for="comment in myCommentsList" 
                :key="comment.id" 
                class="comment-item"
                @click="goToVideo(comment.videoId)"
              >
                <div class="comment-content">
                  <p class="comment-text">{{ comment.content }}</p>
                  <div class="comment-meta">
                    <span class="comment-video">视频: {{ getVideoTitle(comment.videoId) }}</span>
                    <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                    <span class="comment-likes">
                      <i class="el-icon-star-on"></i> {{ comment.likeCount || 0 }}
                    </span>
                  </div>
                </div>
                <el-button 
                  type="text" 
                  size="small" 
                  icon="el-icon-delete"
                  @click.stop="deleteComment(comment.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>

          <!-- 账号设置 -->
          <div v-show="activeTab === 'settings'" class="tab-pane enhanced">
            <div class="content-header">
              <h3>账号设置</h3>
              <el-button type="primary" size="small" @click="saveProfile" :loading="savingProfile">保存修改</el-button>
            </div>
            
            <el-tabs v-model="settingsActiveTab" type="card">
              <!-- 基本信息 -->
              <el-tab-pane label="基本信息" name="basic">
                <div class="settings-form">
                  <el-form :model="profileForm" :rules="profileRules" ref="profileForm" label-width="120px">
                    <el-form-item label="用户名">
                      <el-input v-model="profileForm.username" disabled></el-input>
                      <div class="form-tip">用户名不可修改</div>
                    </el-form-item>
                    
                    <el-form-item label="昵称" prop="nickname">
                      <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit></el-input>
                    </el-form-item>
                    
                    <el-form-item label="头像">
                      <div class="avatar-upload-section">
                        <el-avatar :size="80" :src="profileForm.avatarUrl || undefined" class="avatar-preview">
                          <i class="el-icon-user-solid"></i>
                        </el-avatar>
                        <div class="avatar-actions">
                          <el-button size="small" @click="triggerAvatarUpload">上传头像</el-button>
                          <el-button size="small" type="text" @click="removeAvatar">移除头像</el-button>
                        </div>
                        <input type="file" ref="avatarInput" accept="image/*" style="display: none" @change="handleAvatarSelect">
                      </div>
                    </el-form-item>
                    
                    <el-form-item label="真实姓名" prop="realName">
                      <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" maxlength="20"></el-input>
                    </el-form-item>
                    
                    <el-form-item label="电子邮箱" prop="email">
                      <el-input v-model="profileForm.email" placeholder="请输入电子邮箱" type="email"></el-input>
                    </el-form-item>
                    
                    <el-form-item label="性别" prop="gender">
                      <el-radio-group v-model="profileForm.gender">
                        <el-radio label="male">男</el-radio>
                        <el-radio label="female">女</el-radio>
                        <el-radio label="">保密</el-radio>
                      </el-radio-group>
                    </el-form-item>
                    
                    <el-form-item label="个人简介" prop="bio">
                      <el-input 
                        v-model="profileForm.bio" 
                        type="textarea" 
                        :rows="4" 
                        placeholder="请输入个人简介" 
                        maxlength="200" 
                        show-word-limit
                      ></el-input>
                    </el-form-item>
                    
                    <el-form-item label="手机号">
                      <el-input v-model="profileForm.phone" disabled></el-input>
                      <div class="form-tip">手机号不可修改</div>
                    </el-form-item>
                  </el-form>
                </div>
              </el-tab-pane>
              
              <!-- 修改密码 -->
              <el-tab-pane label="修改密码" name="password">
                <div class="settings-form">
                  <el-form :model="passwordForm" :rules="passwordRules" ref="passwordForm" label-width="120px">
                    <el-form-item label="当前密码" prop="oldPassword">
                      <el-input 
                        v-model="passwordForm.oldPassword" 
                        type="password" 
                        placeholder="请输入当前密码"
                        show-password
                      ></el-input>
                    </el-form-item>
                    
                    <el-form-item label="新密码" prop="newPassword">
                      <el-input 
                        v-model="passwordForm.newPassword" 
                        type="password" 
                        placeholder="请输入新密码（至少6位）"
                        show-password
                      ></el-input>
                    </el-form-item>
                    
                    <el-form-item label="确认新密码" prop="confirmPassword">
                      <el-input 
                        v-model="passwordForm.confirmPassword" 
                        type="password" 
                        placeholder="请再次输入新密码"
                        show-password
                      ></el-input>
                    </el-form-item>
                    
                    <el-form-item>
                      <el-button type="primary" @click="changePassword" :loading="changingPassword">修改密码</el-button>
                      <el-button @click="resetPasswordForm">重置</el-button>
                    </el-form-item>
                  </el-form>
                </div>
              </el-tab-pane>
              
              <!-- 账号信息 -->
              <el-tab-pane label="账号信息" name="info">
                <div class="account-info">
                  <div class="info-item">
                    <span class="info-label">用户ID：</span>
                    <span class="info-value">{{ profileForm.id || '--' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">用户等级：</span>
                    <span class="info-value">Lv.{{ profileForm.level || 1 }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">账户余额：</span>
                    <span class="info-value">¥{{ (profileForm.balance || 0).toFixed(2) }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">积分：</span>
                    <span class="info-value">{{ profileForm.points || 0 }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">粉丝数：</span>
                    <span class="info-value">{{ profileForm.fansCount || 0 }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">关注数：</span>
                    <span class="info-value">{{ profileForm.followCount || 0 }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">注册时间：</span>
                    <span class="info-value">{{ formatTime(profileForm.createTime) }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">最后登录：</span>
                    <span class="info-value">{{ formatTime(profileForm.lastLogin) }}</span>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 关注列表弹窗 -->
    <el-dialog
      title="关注列表"
      :visible.sync="showFollowingList"
      width="600px"
      :before-close="() => { showFollowingList = false }"
    >
      <div v-loading="loadingFollowing">
        <div v-if="followingList.length === 0" class="empty-list">
          <i class="el-icon-user"></i>
          <p>还没有关注任何人</p>
        </div>
        <div v-else class="user-list">
          <div 
            v-for="user in followingList" 
            :key="user.id"
            class="user-item"
          >
            <el-avatar :size="50" :src="user.avatarUrl || undefined">
              <i class="el-icon-user-solid"></i>
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ user.nickname || user.username }}</div>
              <div class="user-bio">{{ user.bio || '暂无简介' }}</div>
            </div>
            <div class="user-stats">
              <span>关注 {{ user.followCount || 0 }}</span>
              <span>粉丝 {{ user.fansCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
    
    <!-- 粉丝列表弹窗 -->
    <el-dialog
      title="粉丝列表"
      :visible.sync="showFansList"
      width="600px"
      :before-close="() => { showFansList = false }"
    >
      <div v-loading="loadingFans">
        <div v-if="fansList.length === 0" class="empty-list">
          <i class="el-icon-user"></i>
          <p>还没有粉丝</p>
        </div>
        <div v-else class="user-list">
          <div 
            v-for="user in fansList" 
            :key="user.id"
            class="user-item"
          >
            <el-avatar :size="50" :src="user.avatarUrl || undefined">
              <i class="el-icon-user-solid"></i>
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ user.nickname || user.username }}</div>
              <div class="user-bio">{{ user.bio || '暂无简介' }}</div>
            </div>
            <div class="user-stats">
              <span>关注 {{ user.followCount || 0 }}</span>
              <span>粉丝 {{ user.fansCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { userVideoApi } from '@/api/user'

export default {
  name: 'UserProfile',
  data() {
    return {
      activeTab: 'works', // 默认进入"我的作品"
      tabs: [
        { name: 'works', label: '我的作品', icon: 'el-icon-video-camera' },
        { name: 'upload', label: '上传视频', icon: 'el-icon-upload' },
        { name: 'history', label: '播放历史', icon: 'el-icon-time' },
        { name: 'likes', label: '点赞记录', icon: 'el-icon-star-on' },
        { name: 'comments', label: '评论记录', icon: 'el-icon-chat-dot-round' },
        { name: 'settings', label: '账号设置', icon: 'el-icon-setting' }
      ],
      userInfo: {
        username: localStorage.getItem('username') || '用户',
        avatar: '',
        bio: '暂无简介',
        followCount: 0,
        fansCount: 0
      },
      
      // 关注/粉丝列表
      showFollowingList: false,
      showFansList: false,
      followingList: [],
      fansList: [],
      loadingFollowing: false,
      loadingFans: false,

      // --- 我的作品数据 ---
      myWorksList: [],
      worksTotal: 0,
      loadingWorks: false,

      // --- 上传表单数据 ---
      uploadForm: {
        title: '',
        description: '',
        categoryId: null,
        videoFile: null,
        coverFile: null,
        coverPreview: null
      },
      categories: [],
      uploading: false,
      uploadRules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        videoFile: [{ required: true, message: '请选择视频', trigger: 'change' }]
      },

      // --- 播放历史数据 ---
      playHistoryList: [],
      loadingHistory: false,

      // --- 点赞记录数据 ---
      likedVideosList: [],
      loadingLikes: false,

      // --- 评论记录数据 ---
      myCommentsList: [],
      loadingComments: false,
      
      // 视频标题缓存（用于评论记录中显示视频标题）
      videoTitleCache: {},

      // --- 账号设置相关 ---
      settingsActiveTab: 'basic',
      profileForm: {
        id: null,
        username: '',
        nickname: '',
        avatarUrl: '',
        realName: '',
        email: '',
        gender: '',
        bio: '',
        phone: '',
        level: 1,
        balance: 0,
        points: 0,
        fansCount: 0,
        followCount: 0,
        createTime: null,
        lastLogin: null
      },
      profileRules: {
        nickname: [
          { max: 20, message: '昵称长度不能超过20个字符', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ],
        bio: [
          { max: 200, message: '个人简介长度不能超过200个字符', trigger: 'blur' }
        ]
      },
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入当前密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码长度至少6位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: (rule, value, callback) => {
              if (value !== this.passwordForm.newPassword) {
                callback(new Error('两次输入的密码不一致'));
              } else {
                callback();
              }
            }, trigger: 'blur' }
        ]
      },
      savingProfile: false,
      changingPassword: false
    }
  },
  computed: {
    isLogin() {
      // 简单判断是否有token
      return !!localStorage.getItem('token') || !!localStorage.getItem('userToken');
    }
  },
  mounted() {
    if (this.isLogin) {
      this.loadCategories();
      this.fetchMyWorks(); // 初始加载作品
      this.loadUserProfile(); // 加载用户信息
    }
  },
  methods: {
    handleTabClick(tabName) {
      this.activeTab = tabName;
      if (tabName === 'works') {
        this.fetchMyWorks();
      } else if (tabName === 'history') {
        this.loadPlayHistory();
      } else if (tabName === 'likes') {
        this.loadLikedVideos();
      } else if (tabName === 'comments') {
        this.loadMyComments();
      } else if (tabName === 'settings') {
        this.loadUserProfile();
      }
    },

    goToUpload() {
      this.activeTab = 'upload';
    },

  // --- 获取我的作品 ---
    async fetchMyWorks() {
      this.loadingWorks = true;
      try {
        const res = await userVideoApi.getMyVideos({ page: 1, limit: 20 });
        if (res.code === 200) {
          // 【核心修复】增加对 .list 的判断
          // 后端 PageResponse 通常使用 list 或 content 字段，而不是 records
          this.myWorksList = res.data.list || res.data.records || res.data.content || [];
          this.worksTotal = res.data.total || 0;

          // 调试日志：如果还是不显示，按 F12 看控制台打印了什么
          console.log('我的作品列表数据:', this.myWorksList);
        }
      } catch (error) {
        console.error('获取作品失败', error);
      } finally {
        this.loadingWorks = false;
      }
    },
    // --- 删除我的作品 ---
    handleDeleteWork(row) {
      this.$confirm('确定删除该作品吗？此操作不可恢复。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await userVideoApi.deleteMyVideo(row.id);
          this.$message.success('删除成功');
          this.fetchMyWorks(); // 刷新列表
        } catch (e) {
          this.$message.error('删除失败');
        }
      });
    },

    // ========== 播放历史相关 ==========
    
    // 加载播放历史
    async loadPlayHistory() {
      if (!this.isLogin) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.loadingHistory = true;
      try {
        const res = await userVideoApi.getPlayHistory(50);
        if (res && res.code === 200) {
          this.playHistoryList = res.data || [];
        } else {
          this.$message.error('加载播放历史失败');
        }
      } catch (error) {
        console.error('加载播放历史失败:', error);
        this.$message.error('加载播放历史失败，请稍后重试');
      } finally {
        this.loadingHistory = false;
      }
    },

    // ========== 点赞记录相关 ==========
    
    // 加载点赞记录
    async loadLikedVideos() {
      if (!this.isLogin) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.loadingLikes = true;
      try {
        const res = await userVideoApi.getLikedVideos(1, 50);
        if (res && res.code === 200) {
          // 处理分页数据
          if (res.data && res.data.list) {
            this.likedVideosList = res.data.list;
          } else if (Array.isArray(res.data)) {
            this.likedVideosList = res.data;
          } else {
            this.likedVideosList = [];
          }
        } else {
          this.$message.error('加载点赞记录失败');
        }
      } catch (error) {
        console.error('加载点赞记录失败:', error);
        this.$message.error('加载点赞记录失败，请稍后重试');
      } finally {
        this.loadingLikes = false;
      }
    },

    // ========== 评论记录相关 ==========
    
    // 加载评论记录
    async loadMyComments() {
      if (!this.isLogin) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.loadingComments = true;
      try {
        const res = await userVideoApi.getMyComments(1, 50);
        if (res && res.code === 200) {
          // 处理分页数据
          if (res.data && res.data.list) {
            this.myCommentsList = res.data.list;
          } else if (Array.isArray(res.data)) {
            this.myCommentsList = res.data;
          } else {
            this.myCommentsList = [];
          }
          
          // 加载视频标题缓存
          await this.loadVideoTitles();
        } else {
          this.$message.error('加载评论记录失败');
        }
      } catch (error) {
        console.error('加载评论记录失败:', error);
        this.$message.error('加载评论记录失败，请稍后重试');
      } finally {
        this.loadingComments = false;
      }
    },
    
    // 加载视频标题缓存
    async loadVideoTitles() {
      const videoIds = [...new Set(this.myCommentsList.map(c => c.videoId))];
      for (const videoId of videoIds) {
        if (!this.videoTitleCache[videoId]) {
          try {
            const res = await userVideoApi.getVideoById(videoId);
            if (res && res.data) {
              this.videoTitleCache[videoId] = res.data.title || '未知视频';
            }
          } catch (error) {
            console.error(`加载视频${videoId}标题失败:`, error);
            this.videoTitleCache[videoId] = '未知视频';
          }
        }
      }
    },
    
    // 获取视频标题
    getVideoTitle(videoId) {
      return this.videoTitleCache[videoId] || '加载中...';
    },
    
    // 删除评论
    async deleteComment(commentId) {
      this.$confirm('确定删除这条评论吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await userVideoApi.deleteComment(commentId);
          this.$message.success('删除成功');
          this.loadMyComments(); // 刷新列表
        } catch (error) {
          console.error('删除评论失败:', error);
          this.$message.error('删除失败，请稍后重试');
        }
      });
    },

    // ========== 工具方法 ==========
    
    // 跳转到视频播放页
    goToVideo(videoId) {
      if (!videoId) {
        this.$message.warning('视频ID无效');
        return;
      }
      this.$router.push({
        path: `/main/video/${videoId}`,
        query: { from: 'profile' }
      });
    },
    
    // 处理表格行点击
    handleRowClick(row) {
      // 只有已发布的视频才能跳转
      if (row.status === 'PASSED' && row.id) {
        this.goToVideo(row.id);
      } else if (row.status === 'PENDING') {
        this.$message.info('视频正在审核中，审核通过后才能播放');
      } else if (row.status === 'REJECTED') {
        this.$message.warning('视频已被驳回，无法播放');
      }
    },
    
    // 格式化时长（秒 -> mm:ss）
    formatDuration(seconds) {
      if (!seconds) return '0:00';
      const mins = Math.floor(seconds / 60);
      const secs = Math.floor(seconds % 60);
      return `${mins}:${secs.toString().padStart(2, '0')}`;
    },
    
    // 格式化数字（添加单位）
    formatNumber(num) {
      if (!num) return '0';
      if (num < 1000) return num.toString();
      if (num < 10000) return (num / 1000).toFixed(1) + 'k';
      if (num < 100000000) return (num / 10000).toFixed(1) + '万';
      return (num / 100000000).toFixed(1) + '亿';
    },
    
    // 格式化时间
    formatTime(timeStr) {
      if (!timeStr) return '--';
      const date = new Date(timeStr);
      const now = new Date();
      const diff = now - date;
      
      if (diff < 60000) return '刚刚';
      if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
      if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
      if (diff < 604800000) return Math.floor(diff / 86400000) + '天前';
      
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },

    // ========== 账号设置相关 ==========
    
    // 加载用户信息
    async loadUserProfile() {
      if (!this.isLogin) {
        this.$message.warning('请先登录');
        return;
      }
      
      try {
        const res = await userVideoApi.getCurrentUser();
        if (res && res.code === 200 && res.data) {
          // 更新用户信息显示
          this.userInfo = {
            username: res.data.username || '',
            avatar: res.data.avatarUrl || '',
            bio: res.data.bio || '暂无简介',
            followCount: res.data.followCount || 0,
            fansCount: res.data.fansCount || 0
          };
          
          // 更新表单数据
          this.profileForm = {
            id: res.data.id,
            username: res.data.username || '',
            nickname: res.data.nickname || '',
            avatarUrl: res.data.avatarUrl || '',
            realName: res.data.realName || '',
            email: res.data.email || '',
            gender: res.data.gender || '',
            bio: res.data.bio || '',
            phone: res.data.phone || '',
            level: res.data.level || 1,
            balance: res.data.balance || 0,
            points: res.data.points || 0,
            fansCount: res.data.fansCount || 0,
            followCount: res.data.followCount || 0,
            createTime: res.data.createTime,
            lastLogin: res.data.lastLogin
          };
          
          // 更新用户信息显示
          this.userInfo.username = res.data.nickname || res.data.username || '用户';
          this.userInfo.avatar = res.data.avatarUrl || '';
          this.userInfo.bio = res.data.bio || '暂无简介';
          this.userInfo.followCount = res.data.followCount || 0;
          this.userInfo.fansCount = res.data.fansCount || 0;
        }
      } catch (error) {
        console.error('加载用户信息失败:', error);
        this.$message.error('加载用户信息失败，请稍后重试');
      }
    },
    
    // 保存用户信息
    async saveProfile() {
      this.$refs.profileForm.validate(async (valid) => {
        if (!valid) {
          this.$message.warning('请检查表单填写是否正确');
          return;
        }
        
        this.savingProfile = true;
        try {
          const updateData = {
            nickname: this.profileForm.nickname,
            avatarUrl: this.profileForm.avatarUrl,
            realName: this.profileForm.realName,
            email: this.profileForm.email,
            gender: this.profileForm.gender || null,
            bio: this.profileForm.bio
          };
          
          const res = await userVideoApi.updateProfile(updateData);
          if (res && res.code === 200) {
            this.$message.success('保存成功');
            // 更新本地存储的用户名
            if (res.data && res.data.nickname) {
              localStorage.setItem('username', res.data.nickname);
            }
            // 重新加载用户信息
            await this.loadUserProfile();
          } else {
            this.$message.error(res?.msg || '保存失败');
          }
        } catch (error) {
          console.error('保存用户信息失败:', error);
          this.$message.error('保存失败，请稍后重试');
        } finally {
          this.savingProfile = false;
        }
      });
    },
    
    // 修改密码
    async changePassword() {
      this.$refs.passwordForm.validate(async (valid) => {
        if (!valid) {
          this.$message.warning('请检查表单填写是否正确');
          return;
        }
        
        this.changingPassword = true;
        try {
          const res = await userVideoApi.changePassword(
            this.passwordForm.oldPassword,
            this.passwordForm.newPassword
          );
          if (res && res.code === 200) {
            this.$message.success('密码修改成功，请重新登录');
            this.resetPasswordForm();
            // 可以选择自动退出登录
            setTimeout(() => {
              localStorage.clear();
              this.$router.push('/login');
            }, 1500);
          } else {
            this.$message.error(res?.msg || '密码修改失败');
          }
        } catch (error) {
          console.error('修改密码失败:', error);
          this.$message.error('修改密码失败，请稍后重试');
        } finally {
          this.changingPassword = false;
        }
      });
    },
    
    // 重置密码表单
    resetPasswordForm() {
      this.passwordForm = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      };
      if (this.$refs.passwordForm) {
        this.$refs.passwordForm.clearValidate();
      }
    },
    
    // 触发头像上传
    triggerAvatarUpload() {
      this.$refs.avatarInput.click();
    },
    
    // 处理头像选择
    handleAvatarSelect(event) {
      const file = event.target.files[0];
      if (!file) return;
      
      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        this.$message.warning('请选择图片文件');
        return;
      }
      
      // 验证文件大小（限制5MB）
      if (file.size > 5 * 1024 * 1024) {
        this.$message.warning('图片大小不能超过5MB');
        return;
      }
      
      // 读取文件并预览
      const reader = new FileReader();
      reader.onload = (e) => {
        // 这里可以上传到服务器，暂时使用base64
        // 实际项目中应该上传到OSS或服务器
        this.profileForm.avatarUrl = e.target.result;
        this.$message.info('头像已更新，请点击保存按钮保存修改');
      };
      reader.readAsDataURL(file);
      
      // 清空input，以便可以重复选择同一文件
      event.target.value = '';
    },
    
    // 移除头像
    removeAvatar() {
      this.profileForm.avatarUrl = '';
      this.$message.info('头像已移除，请点击保存按钮保存修改');
    },

    // --- 上传相关逻辑 ---
    triggerFileInput() { this.$refs.fileInput.click(); },
    triggerCoverInput() { this.$refs.coverInput.click(); },

    handleFileSelect(e) {
      const file = e.target.files[0];
      if (file) this.uploadForm.videoFile = file;
    },

    handleCoverSelect(e) {
      const file = e.target.files[0];
      if (file) {
        this.uploadForm.coverFile = file;
        this.uploadForm.coverPreview = URL.createObjectURL(file);
      }
    },

    async loadCategories() {
      try {
        const res = await userVideoApi.getCategories();
        this.categories = res.data || [];
      } catch (e) {
        console.error('分类加载失败');
      }
    },

    resetUploadForm() {
      this.$refs.uploadForm.resetFields();
      this.uploadForm.coverPreview = null;
      this.uploadForm.videoFile = null;
      this.uploadForm.coverFile = null;
    },

    async submitUpload() {
      this.$refs.uploadForm.validate(async valid => {
        if (!valid) return;
        this.uploading = true;
        try {
          const fd = new FormData();
          fd.append('file', this.uploadForm.videoFile);
          fd.append('title', this.uploadForm.title);
          if (this.uploadForm.description) fd.append('description', this.uploadForm.description);
          if (this.uploadForm.categoryId) fd.append('categoryId', this.uploadForm.categoryId);
          if (this.uploadForm.coverFile) fd.append('coverFile', this.uploadForm.coverFile);

          await userVideoApi.uploadVideo(fd);
          this.$message.success('发布成功，请等待审核');
          this.resetUploadForm();
          this.activeTab = 'works'; // 自动跳转到作品页
          this.fetchMyWorks();      // 刷新列表可以看到新视频（状态为 Pending）
        } catch (e) {
          this.$message.error('上传失败: ' + (e.response?.data?.msg || e.message || '未知错误'));
        } finally {
          this.uploading = false;
        }
      });
    },

    formatDate(row, column, cellValue) {
      if (!cellValue) return '';
      return cellValue.replace('T', ' ').substring(0, 16);
    },
    
    // ========== 关注/粉丝相关 ==========
    
    // 加载关注列表
    async loadFollowingList() {
      this.loadingFollowing = true;
      try {
        const res = await userVideoApi.getFollowingList();
        if (res && res.code === 200) {
          this.followingList = res.data || [];
        } else {
          this.$message.error('加载关注列表失败');
          this.followingList = [];
        }
      } catch (error) {
        console.error('加载关注列表失败:', error);
        this.$message.error('加载关注列表失败，请稍后重试');
        this.followingList = [];
      } finally {
        this.loadingFollowing = false;
      }
    },
    
    // 加载粉丝列表
    async loadFansList() {
      this.loadingFans = true;
      try {
        const res = await userVideoApi.getFansList();
        if (res && res.code === 200) {
          this.fansList = res.data || [];
        } else {
          this.$message.error('加载粉丝列表失败');
          this.fansList = [];
        }
      } catch (error) {
        console.error('加载粉丝列表失败:', error);
        this.$message.error('加载粉丝列表失败，请稍后重试');
        this.fansList = [];
      } finally {
        this.loadingFans = false;
      }
    }
  },
  watch: {
    showFollowingList(newVal) {
      if (newVal) {
        this.loadFollowingList();
      }
    },
    showFansList(newVal) {
      if (newVal) {
        this.loadFansList();
      }
    }
  }
}
</script>

<style scoped>
/* 保持原有样式，增加少量新样式 */
.profile-container { padding: 20px; max-width: 1200px; margin: 0 auto; background: #f5f7fa; min-height: 100vh; }
.profile-header-enhanced { display: flex; gap: 30px; padding: 30px; align-items: center; }
.avatar-section-enhanced { position: relative; }
.user-info-enhanced { flex: 1; }
.username-enhanced { margin: 0 0 10px 0; font-size: 28px; color: #333; }
.stats-cards { display: flex; gap: 20px; margin-top: 20px; }
.stat-card { flex: 1; padding: 15px; border-radius: 10px; color: white; display: flex; align-items: center; gap: 10px; box-shadow: 0 4px 10px rgba(0,0,0,0.1); cursor: pointer; transition: all 0.3s; }
.stat-card:hover { transform: translateY(-3px); box-shadow: 0 6px 15px rgba(0,0,0,0.15); }
.stat-number { font-size: 24px; font-weight: bold; }
.count-badge { font-size: 14px; color: #409EFF; font-weight: normal; margin-left: 8px; }
.custom-tabs { display: flex; border-bottom: 2px solid #eee; margin-bottom: 20px; padding: 0 20px; }
.custom-tab-item { padding: 15px 25px; cursor: pointer; position: relative; color: #666; display: flex; align-items: center; gap: 8px; transition: all 0.3s; }
.custom-tab-item:hover { color: #409EFF; }
.custom-tab-item.active { color: #409EFF; font-weight: bold; }
.tab-indicator { position: absolute; bottom: -2px; left: 0; width: 100%; height: 3px; background: #409EFF; }

/* 上传区域样式 */
.upload-form { max-width: 800px; margin: 0 auto; }
.upload-area { border: 2px dashed #dcdfe6; padding: 20px; text-align: center; cursor: pointer; border-radius: 8px; background: #fafafa; transition: border-color 0.3s; display: flex; align-items: center; justify-content: center; min-height: 100px; }
.upload-area:hover { border-color: #409EFF; background: #f0f7ff; }
.upload-placeholder { color: #909399; }
.upload-placeholder i { font-size: 32px; margin-bottom: 10px; }
.submit-btn { width: 200px; }

/* 空状态 */
.empty-state { text-align: center; padding: 60px 0; color: #999; }
.empty-icon { font-size: 64px; margin-bottom: 20px; color: #e0e0e0; }

/* 加载状态 */
.loading-state { text-align: center; padding: 40px 0; color: #999; }
.loading-state i { font-size: 24px; animation: rotate 1s linear infinite; }
@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 内容头部 */
.content-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 10px; border-bottom: 1px solid #eee; }
.content-header h3 { margin: 0; font-size: 18px; color: #333; }

/* 视频列表 */
.video-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.video-item { cursor: pointer; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); transition: transform 0.3s, box-shadow 0.3s; }
.video-item:hover { transform: translateY(-4px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.video-thumbnail { position: relative; width: 100%; padding-top: 56.25%; background: #000; overflow: hidden; }
.video-thumbnail img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; }
.video-duration { position: absolute; bottom: 8px; right: 8px; background: rgba(0,0,0,0.7); color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
.video-info { padding: 12px; }
.video-title { margin: 0 0 8px 0; font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.video-meta { display: flex; gap: 12px; font-size: 12px; color: #999; }
.video-meta span { display: flex; align-items: center; gap: 4px; }

/* 评论列表 */
.comment-list { display: flex; flex-direction: column; gap: 12px; }
.comment-item { display: flex; justify-content: space-between; align-items: flex-start; padding: 16px; background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); cursor: pointer; transition: background 0.3s; }
.comment-item:hover { background: #f5f7fa; }
.comment-content { flex: 1; }
.comment-text { margin: 0 0 8px 0; color: #333; line-height: 1.6; }
.comment-meta { display: flex; gap: 16px; font-size: 12px; color: #999; }
.comment-video { color: #409EFF; }
.comment-likes { display: flex; align-items: center; gap: 4px; }

/* 账号设置样式 */
.settings-form { max-width: 800px; margin: 0 auto; padding: 20px; }
.form-tip { font-size: 12px; color: #999; margin-top: 4px; }

.avatar-upload-section { display: flex; align-items: center; gap: 20px; }
.avatar-preview { border: 2px solid #eee; }
.avatar-actions { display: flex; flex-direction: column; gap: 8px; }

.account-info { padding: 20px; }
.info-item { display: flex; padding: 12px 0; border-bottom: 1px solid #eee; }
.info-item:last-child { border-bottom: none; }
.info-label { width: 120px; color: #666; font-weight: 500; }
.info-value { flex: 1; color: #333; }

/* 关注/粉丝列表样式 */
.user-list {
  max-height: 500px;
  overflow-y: auto;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-bottom: 1px solid #eee;
  transition: background 0.2s;
}

.user-item:hover {
  background: #f5f7fa;
}

.user-item:last-child {
  border-bottom: none;
}

.user-item .user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
}

.user-bio {
  font-size: 13px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-stats {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #666;
}

.empty-list {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-list i {
  font-size: 48px;
  margin-bottom: 15px;
  display: block;
  color: #ddd;
}

.empty-list p {
  margin: 0;
  font-size: 14px;
}

.count-badge {
  font-size: 14px;
  color: #409EFF;
  font-weight: normal;
  margin-left: 8px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .profile-header-enhanced { flex-direction: column; text-align: center; }
  .stats-cards { flex-direction: column; }
  .custom-tabs { overflow-x: auto; }
}
</style>