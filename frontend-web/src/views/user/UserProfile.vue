<template>
  <div class="profile-page">
    <div class="profile-banner">
      <div class="banner-overlay"></div>
    </div>

    <div class="main-container">

      <div class="user-card-wrapper glass-effect slide-up">
        <div class="user-header-row">
          <div class="user-left">
            <div class="avatar-box">
              <el-avatar :size="90" :src="userInfo.avatarUrl || defaultAvatar" class="user-avatar">
                {{ userInfo.username ? userInfo.username.charAt(0) : 'U' }}
              </el-avatar>
              <div class="level-badge" v-if="userInfo.level">Lv.{{ userInfo.level }}</div>
            </div>
            <div class="user-texts">
              <div class="name-line">
                <h1 class="nickname">{{ userInfo.nickname || userInfo.username }}</h1>
                <i v-if="userInfo.gender === 1" class="el-icon-male" style="color: #409EFF; font-size: 18px; margin-left: 5px;"></i>
                <i v-else-if="userInfo.gender === 0" class="el-icon-female" style="color: #F56C6C; font-size: 18px; margin-left: 5px;"></i>
                <span class="id-tag" v-if="userInfo.id">UID: {{ userInfo.id }}</span>
              </div>
              <p class="bio-text" :title="userInfo.bio">
                <i class="el-icon-edit-outline"></i>
                {{ userInfo.bio || '这个人很懒，什么都没有写...' }}
              </p>
            </div>
          </div>

          <div class="user-right">
            <div class="data-group">
              <div class="data-item" @click="openFollowingList">
                <div class="num">{{ formatNumber(userInfo.followCount || 0) }}</div>
                <div class="label">关注</div>
              </div>
              <div class="divider"></div>
              <div class="data-item" @click="openFansList">
                <div class="num">{{ formatNumber(userInfo.fansCount || 0) }}</div>
                <div class="label">粉丝</div>
              </div>
              <div class="divider"></div>
              <div class="data-item">
                <div class="num">{{ formatNumber(userInfo.likeCount || 0) }}</div>
                <div class="label">获赞</div>
              </div>
            </div>
            <div class="btn-group">
              <el-button type="primary" round class="custom-btn primary" @click="openEditProfile">
                编辑资料
              </el-button>
              <el-button round class="custom-btn icon-only" icon="el-icon-setting" @click="openAccountSettings"></el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="nav-bar-wrapper">
        <div class="nav-pills">
          <div
              v-for="tab in tabs"
              :key="tab.key"
              class="nav-pill-item"
              :class="{ active: currentTab === tab.key }"
              @click="switchTab(tab.key)"
          >
            <i :class="tab.icon"></i>
            <span>{{ tab.label }}</span>
          </div>
        </div>
      </div>

      <div class="content-area fade-in">

        <div v-show="currentTab === 'works'" class="tab-panel">
          <div class="panel-header">
            <div class="ph-left">
              <h3>我的作品</h3>
              <span class="count-badge" v-if="worksTotal > 0">{{ worksTotal }}</span>
            </div>
            <el-button type="primary" icon="el-icon-upload" round size="medium" @click="openUploadDialog" class="upload-btn">
              发布新作品
            </el-button>
          </div>

          <div v-loading="loadingWorks" class="works-container">
            <div v-if="myWorksList.length === 0" class="empty-state">
              <img src="https://img.icons8.com/clouds/200/000000/video.png" alt="No Videos"/>
              <p>暂无作品，快去分享你的第一个视频吧！</p>
              <el-button type="text" @click="openUploadDialog">立即投稿</el-button>
            </div>

            <div v-else class="works-grid">
              <div v-for="item in myWorksList" :key="item.id" class="work-card" @click="handleVideoClick(item)">
                <div class="work-cover-box">
                  <img :src="item.coverUrl" class="cover-img" loading="lazy" />
                  <div class="duration-tag">{{ formatDuration(item.duration) }}</div>

                  <div class="status-badge processing" v-if="item.status === 'PENDING'">
                    <i class="el-icon-loading"></i> 审核中
                  </div>
                  <div class="status-badge rejected" v-else-if="item.status === 'REJECTED'">
                    <i class="el-icon-warning-outline"></i> 已驳回
                  </div>

                  <div class="play-overlay">
                    <i class="el-icon-caret-right"></i>
                  </div>
                </div>

                <div class="work-info-box">
                  <h3 class="work-title" :title="item.title">{{ item.title }}</h3>

                  <div class="work-meta-row">
                    <div class="wm-left">
                      <span class="stat"><i class="el-icon-video-play"></i> {{ formatNumber(item.playCount) }}</span>
                      <span class="stat"><i class="el-icon-chat-square"></i> {{ formatNumber(item.commentCount) }}</span>
                    </div>
                    <span class="wm-time">{{ formatTimeAgo(item.createTime) }}</span>
                  </div>

                  <div class="work-actions" @click.stop>
                    <el-tooltip content="编辑" placement="top">
                      <i class="el-icon-edit action-btn" @click="handleWorkCommand('edit', item)"></i>
                    </el-tooltip>
                    <el-tooltip content="删除" placement="top">
                      <i class="el-icon-delete action-btn delete" @click="handleWorkCommand('delete', item)"></i>
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-show="currentTab === 'history'" class="tab-panel">
          <div class="panel-header">
            <h3>播放历史</h3>
            <el-button type="text" icon="el-icon-delete" class="clear-btn" @click="clearHistory">清空历史</el-button>
          </div>
          <div v-loading="loadingHistory" class="history-grid-wrapper">
            <div v-if="playHistoryList.length === 0" class="empty-state">
              <i class="el-icon-time" style="font-size: 48px; color: #ddd; margin-bottom: 10px;"></i>
              <p>最近没有观看记录哦</p>
            </div>
            <div v-else class="history-grid">
              <div v-for="item in playHistoryList" :key="item.id" class="history-card">
                <div class="h-cover" @click="goToVideo(item.id)">
                  <img :src="item.coverUrl" />
                  <div class="h-progress-bar" style="width: 100%"></div>
                </div>
                <div class="h-info">
                  <h4 class="h-title" :title="item.title" @click="goToVideo(item.id)">{{ item.title }}</h4>
                  <div class="h-meta">
                    <span class="up-name">UP: {{ item.authorName || '未知' }}</span>
                    <i class="el-icon-close del-icon" @click.stop="deleteHistoryItem(item)" title="删除该条"></i>
                  </div>
                  <div class="h-time">观看于 {{ formatTime(item.createTime) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-show="currentTab === 'favorites'" class="tab-panel">
          <div class="fav-layout">
            <div class="fav-sidebar">
              <div class="sidebar-title">我的收藏夹</div>
              <div class="folder-item active">
                <i class="el-icon-star-on"></i>
                <span>默认收藏夹</span>
                <span class="num">{{ collectedVideos.length }}</span>
              </div>
              <div class="folder-item locked">
                <i class="el-icon-lock"></i>
                <span>私密收藏</span>
                <i class="el-icon-lock lock-icon"></i>
              </div>
            </div>
            <div class="fav-content" v-loading="loadingCollects">
              <div v-if="collectedVideos.length === 0" class="empty-state mini">
                <p>收藏夹是空的</p>
              </div>
              <div v-else class="video-grid-cards">
                <div v-for="item in collectedVideos" :key="item.id" class="grid-video-card" @click="goToVideo(item.id)">
                  <div class="g-cover">
                    <img :src="item.coverUrl" />
                    <div class="hover-mask">
                      <el-button type="danger" circle icon="el-icon-delete" size="small" @click.stop="cancelCollect(item)" title="取消收藏"></el-button>
                    </div>
                  </div>
                  <div class="g-info">
                    <h4 class="g-title">{{ item.title }}</h4>
                    <div class="g-author">@{{ item.authorName || '未知用户' }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-show="currentTab === 'interactions'" class="tab-panel">
          <div class="interaction-layout">
            <div class="sub-nav">
              <div class="sub-nav-item" :class="{active: interactionSubTab === 'likes'}" @click="interactionSubTab = 'likes'">
                我点赞的
              </div>
              <div class="sub-nav-item" :class="{active: interactionSubTab === 'comments'}" @click="interactionSubTab = 'comments'">
                我的评论
              </div>
            </div>
            <div class="interaction-content">
              <div v-if="interactionSubTab === 'likes'" v-loading="loadingLikes">
                <div v-if="likedVideos.length === 0" class="empty-state mini"><p>还没有点赞过视频</p></div>
                <div v-else class="video-grid-cards">
                  <div v-for="item in likedVideos" :key="item.id" class="grid-video-card" @click="goToVideo(item.id)">
                    <div class="g-cover">
                      <img :src="item.coverUrl" />
                      <div class="liked-mark"><i class="el-icon-thumb"></i></div>
                    </div>
                    <div class="g-info">
                      <h4 class="g-title">{{ item.title }}</h4>
                      <div class="g-author">UP: {{ item.authorName }}</div>
                    </div>
                  </div>
                </div>
              </div>
              <div v-if="interactionSubTab === 'comments'" v-loading="loadingComments">
                <div v-if="myComments.length === 0" class="empty-state mini"><p>还没有发表过评论</p></div>
                <div v-else class="comments-list-box">
                  <div v-for="item in myComments" :key="item.id" class="my-comment-row">
                    <div class="comment-left-icon"><i class="el-icon-chat-dot-round"></i></div>
                    <div class="comment-right-body">
                      <div class="c-target">
                        评论了视频 <span class="v-link" @click="goToVideo(item.videoId)">《{{ getVideoTitle(item.videoId) }}》</span>
                      </div>
                      <div class="c-content">"{{ item.content }}"</div>
                      <div class="c-meta">
                        <span>{{ formatTime(item.createTime) }}</span>
                        <span class="c-del" @click="deleteMyComment(item)">删除</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <el-dialog
        :title="isEditMode ? '编辑作品' : '发布新作品'"
        :visible.sync="showUploadDialog"
        width="600px"
        custom-class="custom-dialog"
    >
      <el-form ref="uploadForm" :model="uploadForm" label-position="top">
        <el-form-item label="视频标题" required>
          <el-input v-model="uploadForm.title" placeholder="取个吸引人的标题，更容易上热门哦" maxlength="50" show-word-limit></el-input>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="视频分区" required>
              <el-select v-model="uploadForm.categoryId" placeholder="请选择分区" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="视频标签">
              <el-input v-model="uploadForm.tags" placeholder="例如: 搞笑,生活 (用逗号分隔)" maxlength="50"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="视频简介">
          <el-input type="textarea" :rows="4" v-model="uploadForm.description" placeholder="简单介绍一下视频内容..."></el-input>
        </el-form-item>

        <div class="upload-area-group">
          <div class="upload-box main-upload" :class="{filled: !!uploadForm.videoFile || isEditMode}" @click="triggerFileInput" v-loading="generatingCover" element-loading-text="生成封面中...">
            <div v-if="!uploadForm.videoFile && !isEditMode" class="box-content">
              <i class="el-icon-upload-placeholder el-icon-video-camera"></i>
              <div class="text">上传视频文件</div>
              <div class="sub-text">支持 MP4 格式</div>
            </div>
            <div v-else class="box-content filled">
              <i class="el-icon-success" style="color: #67C23A; font-size: 32px"></i>
              <div class="text">{{ isEditMode ? '视频文件已存在' : uploadForm.videoFile.name }}</div>
              <div class="sub-text">{{ isEditMode ? '（编辑模式暂不支持更换视频源）' : '点击更换' }}</div>
            </div>
            <input type="file" ref="fileInput" accept="video/mp4" hidden @change="handleFileSelect" :disabled="isEditMode">
          </div>

          <div class="upload-box cover-upload" :style="coverStyle" @click="triggerCoverInput">
            <div v-if="!uploadForm.coverPreview" class="box-content">
              <i class="el-icon-picture-outline"></i>
              <div class="text">上传封面</div>
              <div class="sub-text" style="font-size: 12px; margin-top: 4px">(可选)自动截取</div>
            </div>
            <input type="file" ref="coverInput" accept="image/*" hidden @change="handleCoverSelect">
          </div>
        </div>
      </el-form>
      <div slot="footer">
        <el-button @click="showUploadDialog = false" plain>取消</el-button>
        <el-button type="primary" :loading="uploading || generatingCover" @click="submitUpload">
          {{ isEditMode ? '保存修改' : '确认发布' }}
        </el-button>
      </div>
    </el-dialog>

    <el-dialog title="编辑个人资料" :visible.sync="showEditDialog" width="480px" custom-class="custom-dialog">
      <div class="edit-profile-form">
        <div class="edit-avatar-center">
          <div class="avatar-edit-area">
            <div class="avatar-preview" @click="$refs.avatarInput.click()">
              <el-avatar :size="80" :src="editForm.avatarUrl || userInfo.avatarUrl || defaultAvatar"></el-avatar>
              <div class="overlay"><i class="el-icon-camera"></i></div>
            </div>
            <span class="tip" style="margin-top:5px; font-size:12px; color:#999">点击更换头像</span>
            <input type="file" ref="avatarInput" hidden accept="image/*" @change="handleAvatarSelect">
          </div>
        </div>

        <el-form :model="editForm" label-width="80px" size="small">
          <el-form-item label="昵称">
            <el-input v-model="editForm.nickname" maxlength="20" show-word-limit></el-input>
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="editForm.gender">
              <el-radio :label="1">男</el-radio>
              <el-radio :label="0">女</el-radio>
              <el-radio :label="2">保密</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="editForm.realName" placeholder="实名认证使用的姓名"></el-input>
          </el-form-item>
          <el-form-item label="个性签名">
            <el-input type="textarea" v-model="editForm.bio" :rows="3" maxlength="100" show-word-limit></el-input>
          </el-form-item>
        </el-form>
      </div>
      <div slot="footer">
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProfile" :loading="uploading">保存修改</el-button>
      </div>
    </el-dialog>

    <el-dialog title="账号安全设置" :visible.sync="showAccountDialog" width="500px" custom-class="modern-dialog">
      <el-tabs v-model="accountActiveTab">
        <el-tab-pane label="基本信息" name="info">
          <el-form :model="accountForm" label-width="80px" style="margin-top: 20px;">
            <el-form-item label="用户名">
              <el-input v-model="accountForm.username" disabled placeholder="用户名不可修改"></el-input>
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="accountForm.phone" placeholder="用于登录和找回密码"></el-input>
            </el-form-item>
            <el-form-item label="电子邮箱">
              <el-input v-model="accountForm.email" placeholder="用于接收通知"></el-input>
            </el-form-item>
            <el-alert title="修改手机号或邮箱后，请使用新的联系方式登录" type="info" :closable="false" show-icon style="margin-bottom: 0;"></el-alert>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form :model="pwdForm" :rules="pwdRules" ref="pwdForm" label-width="80px" style="margin-top: 20px;">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input type="password" v-model="pwdForm.oldPassword" show-password></el-input>
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input type="password" v-model="pwdForm.newPassword" show-password></el-input>
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input type="password" v-model="pwdForm.confirmPassword" show-password></el-input>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div slot="footer">
        <el-button @click="showAccountDialog = false">关闭</el-button>
        <el-button type="primary" @click="saveAccountSettings" :loading="uploading">保存修改</el-button>
      </div>
    </el-dialog>

    <!-- 关注列表对话框 -->
    <el-dialog 
      title="我的关注" 
      :visible.sync="showFollowingList" 
      width="500px" 
      custom-class="modern-dialog"
      @open="loadFollowingList"
    >
      <div v-loading="loadingFollowing" class="user-list-container">
        <div v-if="followingList.length === 0 && !loadingFollowing" class="empty-state">
          <i class="el-icon-user"></i>
          <p>还没有关注任何人</p>
        </div>
        <div v-else class="user-list">
          <div 
            v-for="user in followingList" 
            :key="user.id" 
            class="user-list-item"
            @click="goToUserProfile(user.id)"
          >
            <el-avatar :size="50" :src="user.avatarUrl || defaultAvatar" class="user-avatar">
              {{ user.nickname ? user.nickname.charAt(0) : 'U' }}
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ user.nickname || user.username || '未知用户' }}</div>
              <div class="user-bio">{{ user.bio || '这个人很懒，什么都没有写...' }}</div>
            </div>
            <div class="user-stats">
              <div class="stat-item">
                <span class="stat-value">{{ formatNumber(user.fansCount || 0) }}</span>
                <span class="stat-label">粉丝</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="showFollowingList = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 粉丝列表对话框 -->
    <el-dialog 
      title="我的粉丝" 
      :visible.sync="showFansList" 
      width="500px" 
      custom-class="modern-dialog"
      @open="loadFansList"
    >
      <div v-loading="loadingFans" class="user-list-container">
        <div v-if="fansList.length === 0 && !loadingFans" class="empty-state">
          <i class="el-icon-user"></i>
          <p>还没有粉丝</p>
        </div>
        <div v-else class="user-list">
          <div 
            v-for="user in fansList" 
            :key="user.id" 
            class="user-list-item"
            @click="goToUserProfile(user.id)"
          >
            <el-avatar :size="50" :src="user.avatarUrl || defaultAvatar" class="user-avatar">
              {{ user.nickname ? user.nickname.charAt(0) : 'U' }}
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ user.nickname || user.username || '未知用户' }}</div>
              <div class="user-bio">{{ user.bio || '这个人很懒，什么都没有写...' }}</div>
            </div>
            <div class="user-stats">
              <div class="stat-item">
                <span class="stat-value">{{ formatNumber(user.fansCount || 0) }}</span>
                <span class="stat-label">粉丝</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="showFansList = false">关闭</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { userVideoApi } from '@/api/user'
// 引入 request 用于直接调用更新接口
import request from '@/utils/request'

export default {
  name: 'UserProfile',
  data() {
    const validatePass = (rule, value, callback) => {
      if (value === '') callback(new Error('请输入密码'));
      else {
        if (this.pwdForm.confirmPassword !== '') this.$refs.pwdForm.validateField('confirmPassword');
        callback();
      }
    };
    const validatePass2 = (rule, value, callback) => {
      if (value === '') callback(new Error('请再次输入密码'));
      else if (value !== this.pwdForm.newPassword) callback(new Error('两次输入密码不一致!'));
      else callback();
    };

    return {
      defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      userInfo: {},

      currentTab: 'works',
      tabs: [
        { key: 'works', label: '我的作品', icon: 'el-icon-video-camera-solid' },
        { key: 'history', label: '播放历史', icon: 'el-icon-time' },
        { key: 'favorites', label: '我的收藏', icon: 'el-icon-star-on' },
        { key: 'interactions', label: '互动中心', icon: 'el-icon-chat-dot-square' }
      ],
      interactionSubTab: 'likes',

      // Data Lists
      myWorksList: [],
      worksTotal: 0,
      playHistoryList: [],
      collectedVideos: [],
      likedVideos: [],
      myComments: [],
      videoTitleCache: {},

      // Loading States
      loadingWorks: false,
      loadingHistory: false,
      loadingCollects: false,
      loadingLikes: false,
      loadingComments: false,

      // Dialogs
      showUploadDialog: false,
      showEditDialog: false,
      showAccountDialog: false,
      accountActiveTab: 'info',
      uploading: false,
      generatingCover: false,

      // 编辑模式
      isEditMode: false,
      currentEditId: null,

      // Forms
      // 包含 tags
      uploadForm: { title: '', description: '', categoryId: null, tags: '', videoFile: null, coverFile: null, coverPreview: null },

      editForm: { nickname: '', bio: '', avatarUrl: '', avatarFile: null, gender: 2, realName: '' },
      accountForm: { username: '', phone: '', email: '' },
      pwdForm: { oldPassword: '', newPassword: '', confirmPassword: '' },

      pwdRules: {
        oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
        newPassword: [{ validator: validatePass, trigger: 'blur' }],
        confirmPassword: [{ validator: validatePass2, trigger: 'blur' }]
      },

      categories: [],
      showFollowingList: false,
      showFansList: false,
      followingList: [],
      fansList: [],
      loadingFollowing: false,
      loadingFans: false
    }
  },
  computed: {
    coverStyle() {
      return this.uploadForm.coverPreview ? { backgroundImage: `url(${this.uploadForm.coverPreview})`, backgroundSize: 'cover', borderStyle: 'solid', borderColor: '#409EFF' } : {}
    }
  },
  created() {
    if (!localStorage.getItem('userToken')) {
      this.$router.push('/login')
    } else {
      this.loadUserInfo()
      this.loadCategories()
      this.switchTab('works')
    }
  },
  methods: {
    async loadUserInfo() {
      const res = await userVideoApi.getCurrentUser()
      if (res.code === 200) {
        this.userInfo = res.data
        this.editForm.nickname = res.data.nickname
        this.editForm.bio = res.data.bio
        this.editForm.avatarUrl = res.data.avatarUrl
        this.editForm.gender = res.data.gender !== undefined ? res.data.gender : 2
        this.editForm.realName = res.data.realName || ''
      }
    },
    switchTab(key) {
      this.currentTab = key
      if(key === 'works') this.loadMyWorks()
      if(key === 'history') this.loadHistory()
      if(key === 'favorites') this.loadFavorites()
      if(key === 'interactions') {
        this.loadLikes()
        this.loadComments()
      }
    },

    // --- Data Loaders ---
    async loadMyWorks() {
      this.loadingWorks = true
      try {
        const res = await userVideoApi.getMyVideos({ page: 1, pageSize: 20 })
        this.myWorksList = res.data.list || res.data || []
        this.worksTotal = res.data.total || this.myWorksList.length
      } finally { this.loadingWorks = false }
    },
    async loadHistory() {
      this.loadingHistory = true
      try {
        const res = await userVideoApi.getPlayHistory(50)
        this.playHistoryList = res.data || []
      } finally { this.loadingHistory = false }
    },
    async loadFavorites() {
      this.loadingCollects = true
      try {
        const res = await userVideoApi.getCollectedVideos(1, 20)
        this.collectedVideos = (res && res.data && (res.data.list || res.data)) || []
      } catch(e) { this.collectedVideos = [] }
      this.loadingCollects = false
    },
    async loadLikes() {
      this.loadingLikes = true
      const res = await userVideoApi.getLikedVideos(1, 50)
      this.likedVideos = (res.data && (res.data.list || res.data)) || []
      this.loadingLikes = false
    },
    async loadComments() {
      this.loadingComments = true
      const res = await userVideoApi.getMyComments(1, 50)
      this.myComments = (res.data && (res.data.list || res.data)) || []
      this.loadingComments = false
      this.loadVideoTitles()
    },

    // --- Actions ---
    handleWorkCommand(cmd, item) {
      if(cmd === 'delete') {
        this.$confirm('确定要删除这个作品吗? 删除后无法恢复。','提示',{type:'warning'}).then(async () => {
          await userVideoApi.deleteMyVideo(item.id)
          this.$message.success('已删除')
          this.loadMyWorks()
        })
      } else if (cmd === 'edit') {
        // 进入编辑模式
        this.isEditMode = true;
        this.currentEditId = item.id;

        // 回填数据
        this.uploadForm = {
          title: item.title,
          description: item.description,
          categoryId: item.categoryId,
          tags: item.tags || '',
          videoFile: null,
          coverFile: null,
          coverPreview: item.coverUrl
        };

        this.showUploadDialog = true;
      }
    },
    async cancelCollect(item) {
      await userVideoApi.uncollectVideo(item.id)
      this.$message.success('已移出收藏夹')
      this.loadFavorites()
    },
    async cancelLike(item) {
      await userVideoApi.unlikeVideo(item.id)
      this.loadLikes()
    },
    async deleteHistoryItem(item) {
      this.$confirm('删除这条观看记录?', '提示', { type: 'warning' }).then(async () => {
        try {
          await userVideoApi.deletePlayHistoryItem(item.id)
          this.playHistoryList = this.playHistoryList.filter(i => i.id !== item.id)
          this.$message.success('已删除')
        } catch (e) { console.warn(e) }
      })
    },
    async clearHistory() {
      this.$confirm('确定清空所有播放历史?', '提示', { type: 'warning' }).then(async () => {
        try {
          await userVideoApi.clearPlayHistory()
          this.playHistoryList = []
          this.$message.success('历史已清空')
        } catch (e) { console.warn('清空失败', e) }
      })
    },
    async deleteMyComment(item) {
      this.$confirm('确定删除这条评论?', '提示', {type: 'warning'}).then(async () => {
        await userVideoApi.deleteComment(item.id)
        this.$message.success('已删除')
        this.loadComments()
      })
    },

    // --- Upload / Edit Logic ---
    openUploadDialog() {
      this.isEditMode = false;
      this.currentEditId = null;
      this.uploadForm = { title: '', description: '', categoryId: null, tags: '', videoFile: null, coverFile: null, coverPreview: null }
      this.showUploadDialog = true
    },
    triggerFileInput() { this.$refs.fileInput.click() },
    triggerCoverInput() { this.$refs.coverInput.click() },

    handleFileSelect(e) {
      const file = e.target.files[0]
      if (file) {
        this.uploadForm.videoFile = file
        // 仅在发布模式且无封面时自动截取
        if (!this.isEditMode && !this.uploadForm.coverFile) {
          this.generateVideoCover(file)
        }
      }
    },

    handleCoverSelect(e) {
      const file = e.target.files[0]
      if(file) {
        this.uploadForm.coverFile = file
        this.uploadForm.coverPreview = URL.createObjectURL(file)
      }
    },

    generateVideoCover(file) {
      this.generatingCover = true
      const video = document.createElement('video')
      const url = URL.createObjectURL(file)

      video.src = url
      video.crossOrigin = 'anonymous'
      video.muted = true
      video.currentTime = 1

      video.onloadeddata = () => {
        if (video.duration < 1) video.currentTime = 0
      }

      video.onseeked = () => {
        const canvas = document.createElement('canvas')
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        const ctx = canvas.getContext('2d')
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

        canvas.toBlob((blob) => {
          if (blob) {
            const coverFile = new File([blob], 'auto_cover.jpg', { type: 'image/jpeg' })
            if (!this.uploadForm.coverFile) {
              this.uploadForm.coverFile = coverFile
              this.uploadForm.coverPreview = URL.createObjectURL(blob)
            }
          }
          URL.revokeObjectURL(url)
          this.generatingCover = false
        }, 'image/jpeg', 0.8)
      }

      video.onerror = () => {
        console.warn('自动截取封面失败')
        URL.revokeObjectURL(url)
        this.generatingCover = false
      }
    },

    async submitUpload() {
      if(!this.uploadForm.title) return this.$message.warning('请填写标题')
      // 编辑模式下可以不选新视频文件
      if(!this.isEditMode && !this.uploadForm.videoFile) return this.$message.warning('请选择视频文件')

      this.uploading = true
      try {
        if (this.isEditMode) {
          // === 编辑模式 ===
          const updateData = {
            title: this.uploadForm.title,
            description: this.uploadForm.description,
            categoryId: this.uploadForm.categoryId,
            tags: this.uploadForm.tags,
            coverUrl: this.uploadForm.coverPreview
          };

          // 如果有新封面，先上传
          if (this.uploadForm.coverFile) {
            const res = await userVideoApi.uploadFile(this.uploadForm.coverFile);
            if(res.code === 200) updateData.coverUrl = res.data;
          }

          // 更新接口
          await request({
            url: `/api/v1/video/${this.currentEditId}`,
            method: 'put',
            data: updateData
          });
          this.$message.success('修改成功，视频已重新提交审核');
        } else {
          // === 发布模式 ===
          const fd = new FormData()
          fd.append('file', this.uploadForm.videoFile)
          fd.append('title', this.uploadForm.title)
          fd.append('categoryId', this.uploadForm.categoryId || 1)
          if(this.uploadForm.tags) fd.append('tags', this.uploadForm.tags)
          if(this.uploadForm.description) fd.append('description', this.uploadForm.description)
          if(this.uploadForm.coverFile) fd.append('coverFile', this.uploadForm.coverFile)

          await userVideoApi.uploadVideo(fd)
          this.$message.success('投稿成功！视频审核中...')
        }

        this.showUploadDialog = false
        if (this.currentTab === 'works') {
          this.loadMyWorks()
        } else {
          this.switchTab('works')
        }
      } catch(e) {
        console.error(e)
        this.$message.error('操作失败，请重试')
      } finally {
        this.uploading = false
      }
    },

    // --- Edit Profile ---
    openEditProfile() {
      this.showEditDialog = true
      this.editForm.avatarFile = null
      this.editForm.nickname = this.userInfo.nickname
      this.editForm.bio = this.userInfo.bio
      this.editForm.avatarUrl = this.userInfo.avatarUrl
      this.editForm.gender = this.userInfo.gender
      this.editForm.realName = this.userInfo.realName
    },

    handleAvatarSelect(e) {
      const file = e.target.files[0]
      if(file) {
        this.editForm.avatarFile = file
        const reader = new FileReader()
        reader.onload = (ev) => { this.editForm.avatarUrl = ev.target.result }
        reader.readAsDataURL(file)
      }
    },

    async saveProfile() {
      this.uploading = true;
      try {
        if (this.editForm.avatarFile) {
          const res = await userVideoApi.uploadFile(this.editForm.avatarFile)
          if (res.code === 200) {
            this.editForm.avatarUrl = res.data
          }
        }
        await userVideoApi.updateProfile({
          nickname: this.editForm.nickname,
          bio: this.editForm.bio,
          avatarUrl: this.editForm.avatarUrl,
          gender: this.editForm.gender,
          realName: this.editForm.realName
        })
        this.$message.success('资料已更新')
        this.showEditDialog = false
        this.loadUserInfo()
      } catch (e) {
        this.$message.error('保存失败')
      } finally {
        this.uploading = false;
      }
    },

    // --- Account Settings ---
    openAccountSettings() {
      this.showAccountDialog = true
      this.accountActiveTab = 'info'
      this.accountForm.username = this.userInfo.username
      this.accountForm.phone = this.userInfo.phone
      this.accountForm.email = this.userInfo.email
      this.pwdForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
      if(this.$refs.pwdForm) this.$refs.pwdForm.resetFields()
    },

    async saveAccountSettings() {
      this.uploading = true
      try {
        if (this.accountActiveTab === 'info') {
          await userVideoApi.updateProfile({
            phone: this.accountForm.phone,
            email: this.accountForm.email
          })
          this.$message.success('账号信息已更新')
          this.loadUserInfo()
        } else {
          this.$refs.pwdForm.validate(async valid => {
            if (valid) {
              await userVideoApi.changePassword({
                oldPassword: this.pwdForm.oldPassword,
                newPassword: this.pwdForm.newPassword
              })
              this.$message.success('密码修改成功，请重新登录')
              setTimeout(() => {
                localStorage.removeItem('userToken')
                this.$router.push('/login')
              }, 1000)
            }
          })
        }
      } catch (e) {
        this.$message.error(e.message || '保存失败')
      } finally {
        this.uploading = false
      }
    },

    // --- Utils ---
    handleVideoClick(item) {
      // 【修复】处理视频点击：已审核的视频正常跳转，未审核的视频预览
      if (item.status === 'PASSED') {
        // 审核通过：正常跳转
        this.goToVideo(item.id)
      } else {
        // 未审核或已驳回：预览模式
        this.previewVideo(item)
      }
    },
    goToVideo(id) {
      // 【修复】确保ID以字符串形式传递，避免Long类型精度丢失
      this.$router.push(`/main/video/${String(id)}`)
    },
    previewVideo(item) {
      // 【新增】预览未审核视频 - 使用弹窗组件
      this.$msgbox({
        title: '视频预览',
        message: this.$createElement('div', {
          style: { padding: '20px', textAlign: 'center' }
        }, [
          this.$createElement('h3', { style: { marginBottom: '15px' } }, item.title || '无标题'),
          this.$createElement('p', { style: { color: '#666', marginBottom: '15px' } }, item.description || '暂无简介'),
          item.videoUrl ? this.$createElement('video', {
            attrs: {
              controls: true,
              src: item.videoUrl
            },
            style: {
              maxWidth: '100%',
              maxHeight: '400px',
              borderRadius: '8px',
              marginTop: '15px'
            }
          }, '您的浏览器不支持视频播放') : this.$createElement('p', { style: { color: '#999', marginTop: '15px' } }, '视频文件加载中...'),
          this.$createElement('p', {
            style: { color: '#999', fontSize: '12px', marginTop: '15px' }
          }, `状态：${item.status === 'PENDING' ? '审核中' : item.status === 'REJECTED' ? '已驳回' : '未知'}`)
        ]),
        showCancelButton: false,
        confirmButtonText: '关闭',
        customClass: 'video-preview-dialog'
      })
    },
    async loadCategories() {
      const res = await userVideoApi.getCategories()
      this.categories = res.data || []
    },
    formatDuration(s) {
      if(!s) return '00:00'
      const m = Math.floor(s/60); const sec=Math.floor(s%60)
      return `${m}:${sec<10?'0'+sec:sec}`
    },
    formatNumber(n) {
      if (!n) return 0
      return n > 10000 ? (n/10000).toFixed(1)+'w' : n
    },
    formatTime(t) { return t ? t.replace('T', ' ').substring(0,16) : '' },
    formatTimeAgo(t) {
      return t ? t.split('T')[0] : '刚刚'
    },
    
    // 打开关注列表
    openFollowingList() {
      this.showFollowingList = true
      this.loadFollowingList()
    },
    
    // 打开粉丝列表
    openFansList() {
      this.showFansList = true
      this.loadFansList()
    },
    
    // 加载关注列表
    async loadFollowingList() {
      this.loadingFollowing = true
      try {
        const res = await userVideoApi.getFollowingList()
        if (res.code === 200) {
          this.followingList = res.data || []
        } else {
          this.$message.error('加载关注列表失败')
          this.followingList = []
        }
      } catch (error) {
        console.error('加载关注列表失败:', error)
        this.$message.error('加载关注列表失败')
        this.followingList = []
      } finally {
        this.loadingFollowing = false
      }
    },
    
    // 加载粉丝列表
    async loadFansList() {
      this.loadingFans = true
      try {
        const res = await userVideoApi.getFansList()
        if (res.code === 200) {
          this.fansList = res.data || []
        } else {
          this.$message.error('加载粉丝列表失败')
          this.fansList = []
        }
      } catch (error) {
        console.error('加载粉丝列表失败:', error)
        this.$message.error('加载粉丝列表失败')
        this.fansList = []
      } finally {
        this.loadingFans = false
      }
    },
    
    // 跳转到用户主页（如果实现的话）
    goToUserProfile(userId) {
      // 如果当前用户是自己，跳转到个人中心
      const currentUserId = localStorage.getItem('userId')
      if (currentUserId && parseInt(currentUserId) === userId) {
        this.$router.push('/main/profile')
      } else {
        // 可以跳转到其他用户的个人主页（如果实现了的话）
        this.$message.info('用户主页功能开发中')
      }
    },
    async loadVideoTitles() {
      const ids = [...new Set(this.myComments.map(c => c.videoId))]
      for(let id of ids) {
        if(!this.videoTitleCache[id]) {
          try {
            const res = await userVideoApi.getVideoById(id)
            this.$set(this.videoTitleCache, id, res.data.title)
          } catch(e) { console.warn('获取视频标题失败', e) }
        }
      }
    },
    getVideoTitle(id) { return this.videoTitleCache[id] || `视频ID:${id}` }
  }
}
</script>

<style scoped>
/* ================= 全局样式变量 ================= */
:root {
  --primary-color: #409EFF;
  --bg-gradient: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  --card-bg: rgba(255, 255, 255, 0.85);
  --glass-border: 1px solid rgba(255, 255, 255, 0.6);
  --shadow-sm: 0 4px 12px rgba(0,0,0,0.05);
  --shadow-hover: 0 8px 24px rgba(0,0,0,0.12);
}

.profile-page {
  min-height: 100vh;
  background: #f6f8fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  padding-bottom: 50px;
}

/* ================= 1. Banner (美化版) ================= */
.profile-banner {
  height: 320px;
  background: linear-gradient(120deg, #a1c4fd 0%, #c2e9fb 100%);
  position: relative;
  overflow: hidden;
}

.profile-banner::after {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background-image: url('https://picsum.photos/1920/400?blur=4');
  opacity: 0.3;
  mix-blend-mode: overlay;
}

.banner-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0) 60%, rgba(246, 248, 250, 1) 100%);
  z-index: 1;
}

/* ================= 2. 主容器 ================= */
.main-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  position: relative;
  z-index: 10;
  margin-top: -80px; /* 上浮与 Banner 重叠 */
}

/* 个人信息卡片 (Glassmorphism) */
.user-card-wrapper {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  padding: 30px 40px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
}

.user-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-left {
  display: flex;
  gap: 25px;
  align-items: center;
}

.avatar-box {
  position: relative;
}
.user-avatar {
  border: 4px solid #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  background: #fff;
  font-size: 36px;
  font-weight: bold;
  color: #409EFF;
}
.level-badge {
  position: absolute;
  bottom: 0; right: -5px;
  background: linear-gradient(45deg, #f6d365 0%, #fda085 100%);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  border: 2px solid #fff;
  font-weight: bold;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.nickname {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #333;
}
.id-tag {
  font-size: 12px;
  background: rgba(0,0,0,0.05);
  color: #888;
  padding: 2px 6px;
  border-radius: 4px;
}

.bio-text {
  margin: 0;
  color: #666;
  font-size: 14px;
  max-width: 500px;
  line-height: 1.5;
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 15px;
}

.data-group {
  display: flex;
  align-items: center;
  gap: 20px;
}
.data-item {
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s;
}
.data-item:hover { transform: translateY(-2px); }
.data-item .num { font-size: 22px; font-weight: 700; color: #333; }
.data-item .label { font-size: 12px; color: #999; margin-top: 2px; }
.divider { width: 1px; height: 20px; background: #ddd; }

.btn-group {
  display: flex;
  gap: 10px;
}
.custom-btn {
  font-weight: 600;
  border: none;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.2);
  transition: all 0.3s;
}
.custom-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 15px rgba(64, 158, 255, 0.3); }
.custom-btn.icon-only {
  width: 40px; height: 40px; padding: 0;
  background: #f0f2f5; color: #606266; box-shadow: none;
}
.custom-btn.icon-only:hover { background: #e6e8eb; }

.edit-avatar-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}
.avatar-edit-area {
  position: relative;
  cursor: pointer;
}
.avatar-preview {
  position: relative;
  width: 80px; height: 80px;
  border-radius: 50%;
  overflow: hidden;
}
.avatar-preview .overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.3s;
}
.avatar-preview:hover .overlay { opacity: 1; }
.avatar-preview .overlay i { color: #fff; font-size: 24px; }

/* ================= 3. 导航栏 (胶囊式) ================= */
.nav-bar-wrapper {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.nav-pills {
  background: #fff;
  padding: 6px;
  border-radius: 50px;
  display: inline-flex;
  gap: 5px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
}

.nav-pill-item {
  padding: 10px 24px;
  border-radius: 30px;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.nav-pill-item:hover { background: #f5f7fa; color: #333; }
.nav-pill-item.active {
  background: #409EFF;
  color: #fff;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
}

/* ================= 4. 内容区 ================= */
.content-area { margin-top: 25px; min-height: 400px; }

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 10px;
}
.panel-header h3 { margin: 0; font-size: 20px; font-weight: 600; color: #333; }
.count-badge {
  background: #f0f2f5; color: #999; padding: 2px 8px;
  border-radius: 10px; font-size: 12px; margin-left: 8px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #999;
}
.empty-state img { width: 120px; margin-bottom: 15px; opacity: 0.8; }
.empty-state.mini { padding: 30px 0; }
.empty-state i {
  font-size: 48px;
  color: #ddd;
  margin-bottom: 15px;
}

/* =================== 用户列表样式 =================== */
.user-list-container {
  max-height: 500px;
  overflow-y: auto;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-list-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #f0f0f0;
}

.user-list-item:hover {
  background-color: #f8f9fa;
  border-color: #409EFF;
  transform: translateX(4px);
}

.user-list-item .user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  align-items: center;
  gap: 15px;
  flex-shrink: 0;
}

.user-stats .stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.user-stats .stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.user-stats .stat-label {
  font-size: 12px;
  color: #999;
}

/* =================== 我的作品 Grid 样式 (新) =================== */
.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); /* 网格自适应 */
  gap: 20px;
}

.work-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #eee;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.work-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.08);
  border-color: #ecf5ff;
}

.work-cover-box {
  width: 100%;
  padding-top: 56.25%; /* 16:9 */
  position: relative;
  background: #000;
}

.cover-img {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.5s;
}

.work-card:hover .cover-img { transform: scale(1.05); }

.duration-tag {
  position: absolute; bottom: 6px; right: 6px;
  background: rgba(0,0,0,0.7); color: #fff; font-size: 12px;
  padding: 2px 6px; border-radius: 4px;
}

.status-badge {
  position: absolute; top: 6px; left: 6px;
  font-size: 12px; padding: 3px 8px; border-radius: 4px;
  color: #fff; font-weight: 500; display: flex; align-items: center; gap: 4px;
}
.status-badge.processing { background: rgba(230, 162, 60, 0.9); }
.status-badge.rejected { background: rgba(245, 108, 108, 0.9); }

.play-overlay {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.2);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.3s;
}
.work-card:hover .play-overlay { opacity: 1; }
.play-overlay i { font-size: 40px; color: #fff; }

.work-info-box {
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex: 1;
}

.work-title {
  margin: 0 0 10px 0;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  height: 42px; /* 限制两行高度 */
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.work-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}
.wm-left { display: flex; gap: 10px; }
.stat { display: flex; align-items: center; gap: 4px; }
.work-actions {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  opacity: 0; /* 默认隐藏操作栏 */
  transition: opacity 0.2s;
}
.work-card:hover .work-actions { opacity: 1; } /* 悬浮显示 */

.action-btn { font-size: 16px; color: #999; cursor: pointer; transition: color 0.2s; }
.action-btn:hover { color: #409EFF; }
.action-btn.delete:hover { color: #F56C6C; }

/* =================== 其他 Grid 样式 =================== */

/* 历史记录网格 */
.history-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 20px;
}
.history-card {
  background: #fff; border-radius: 10px; overflow: hidden;
  transition: all 0.3s; cursor: pointer; border: 1px solid #eee;
}
.history-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,0.08); }
.h-cover { height: 124px; position: relative; background: #000; }
.h-cover img { width: 100%; height: 100%; object-fit: cover; }
.h-progress-bar { height: 2px; background: #F56C6C; position: absolute; bottom: 0; left: 0; }
.h-info { padding: 10px; }
.h-title {
  margin: 0 0 6px 0; font-size: 14px; color: #333; height: 40px;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.h-meta { display: flex; justify-content: space-between; font-size: 12px; color: #999; }
.del-icon { cursor: pointer; } .del-icon:hover { color: #F56C6C; }
.h-time { font-size: 11px; color: #bbb; margin-top: 6px; }

/* 收藏夹布局 */
.fav-layout { display: flex; gap: 20px; }
.fav-sidebar {
  width: 240px; background: #fff; border-radius: 12px; padding: 20px 0;
  box-shadow: var(--shadow-sm); height: fit-content;
}
.sidebar-title { padding: 0 20px 15px; font-weight: 600; color: #666; font-size: 14px; border-bottom: 1px solid #f0f0f0; margin-bottom: 10px; }
.folder-item {
  padding: 12px 20px; display: flex; align-items: center; gap: 10px; cursor: pointer;
  color: #333; font-size: 14px; border-left: 3px solid transparent;
}
.folder-item:hover { background: #f9f9f9; }
.folder-item.active { background: #e6f7ff; color: #409EFF; border-left-color: #409EFF; }
.folder-item.locked { color: #ccc; cursor: not-allowed; }
.lock-icon { margin-left: auto; font-size: 12px; }

.fav-content { flex: 1; }
.video-grid-cards {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 15px;
}
.grid-video-card {
  background: #fff; border-radius: 8px; overflow: hidden; cursor: pointer;
  transition: all 0.3s; border: 1px solid #eee;
}
.grid-video-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
.g-cover { height: 112px; position: relative; background: #000; }
.g-cover img { width: 100%; height: 100%; object-fit: cover; }
.hover-mask {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.2s;
}
.grid-video-card:hover .hover-mask { opacity: 1; }
.g-info { padding: 10px; }
.g-title {
  margin: 0 0 6px 0; font-size: 13px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.g-author { font-size: 12px; color: #999; }
.liked-mark {
  position: absolute; top: 5px; right: 5px; color: #fff; background: #F56C6C;
  padding: 2px 6px; border-radius: 4px; font-size: 12px;
}

/* 互动记录 */
.interaction-layout { background: #fff; padding: 20px; border-radius: 12px; min-height: 400px; }
.sub-nav { display: flex; gap: 30px; border-bottom: 1px solid #eee; margin-bottom: 20px; }
.sub-nav-item {
  padding-bottom: 10px; cursor: pointer; color: #666; font-weight: 500; border-bottom: 2px solid transparent;
}
.sub-nav-item.active { color: #409EFF; border-bottom-color: #409EFF; }

.comments-list-box { display: flex; flex-direction: column; gap: 20px; }
.my-comment-row { display: flex; gap: 15px; padding-bottom: 15px; border-bottom: 1px solid #f5f5f5; }
.comment-left-icon i { font-size: 20px; color: #ccc; }
.comment-right-body { flex: 1; }
.c-target { font-size: 13px; color: #666; margin-bottom: 6px; }
.v-link { color: #409EFF; cursor: pointer; }
.c-content { font-size: 14px; color: #333; line-height: 1.5; background: #f9f9f9; padding: 10px; border-radius: 6px; }
.c-meta { font-size: 12px; color: #bbb; margin-top: 6px; display: flex; justify-content: space-between; }
.c-del { cursor: pointer; } .c-del:hover { color: #F56C6C; }

/* 弹窗上传框 */
.upload-area-group { display: flex; gap: 15px; margin-top: 10px; }
.upload-box {
  border: 2px dashed #d9d9d9; border-radius: 8px; height: 140px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.3s; background: #fafafa;
}
.upload-box:hover { border-color: #409EFF; background: #ecf5ff; }
.upload-box.main-upload { flex: 2; }
.upload-box.cover-upload { flex: 1; background-size: cover; background-position: center; }
.box-content { text-align: center; color: #909399; }
.box-content i { font-size: 32px; margin-bottom: 8px; }
.box-content.filled .text { color: #67C23A; font-weight: 600; margin-top: 5px; }

/* 动画类 */
.slide-up { animation: slideUp 0.6s cubic-bezier(0.2, 0.8, 0.2, 1); }
.fade-in { animation: fadeIn 0.8s ease; }
@keyframes slideUp { from { opacity: 0; transform: translateY(40px); } to { opacity: 1; transform: translateY(0); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

/* 响应式 */
@media (max-width: 768px) {
  .user-header-row { flex-direction: column; align-items: flex-start; gap: 20px; }
  .user-right { width: 100%; align-items: flex-start; flex-direction: row; justify-content: space-between; }
  .fav-layout { flex-direction: column; }
  .fav-sidebar { width: 100%; }
}
</style>