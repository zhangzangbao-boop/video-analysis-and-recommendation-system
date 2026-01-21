<template>
  <div class="profile-page">
    <div class="profile-banner">
      <div class="banner-mask"></div>
    </div>

    <div class="main-container">
      <div class="user-info-card glass-effect">
        <div class="info-left">
          <div class="avatar-wrapper">
            <el-avatar :size="96" :src="userInfo.avatarUrl || defaultAvatar" class="main-avatar">
              {{ userInfo.username ? userInfo.username.charAt(0) : 'U' }}
            </el-avatar>
            <div class="vip-badge" v-if="userInfo.level > 2">PRO</div>
          </div>
          <div class="text-content">
            <div class="name-row">
              <h1 class="nickname">{{ userInfo.nickname || userInfo.username }}</h1>
              <span class="level-tag">Lv.{{ userInfo.level || 1 }}</span>
              <i v-if="userInfo.gender === 1" class="el-icon-male" style="color: #409EFF; font-size: 16px; margin-left: 5px;"></i>
              <i v-else-if="userInfo.gender === 0" class="el-icon-female" style="color: #F56C6C; font-size: 16px; margin-left: 5px;"></i>
            </div>
            <p class="bio" :title="userInfo.bio">{{ userInfo.bio || '这个人很神秘，什么都没写...' }}</p>
          </div>
        </div>

        <div class="info-right">
          <div class="stat-box" @click="showFollowingList = true">
            <div class="stat-num">{{ userInfo.followCount || 0 }}</div>
            <div class="stat-label">关注</div>
          </div>
          <div class="stat-box" @click="showFansList = true">
            <div class="stat-num">{{ userInfo.fansCount || 0 }}</div>
            <div class="stat-label">粉丝</div>
          </div>
          <div class="action-group">
            <el-button type="primary" round class="edit-btn" @click="openEditProfile">编辑资料</el-button>
            <el-button icon="el-icon-setting" circle class="setting-btn" @click="openAccountSettings"></el-button>
          </div>
        </div>
      </div>

      <div class="nav-bar-sticky">
        <div class="nav-list">
          <div
              v-for="tab in tabs"
              :key="tab.key"
              class="nav-item"
              :class="{ active: currentTab === tab.key }"
              @click="switchTab(tab.key)"
          >
            <i :class="tab.icon"></i> {{ tab.label }}
            <span class="active-line"></span>
          </div>
        </div>
      </div>

      <div class="content-wrapper fade-enter">

        <div v-show="currentTab === 'works'" class="tab-view">
          <div class="view-header">
            <div class="header-left">
              <h2>我的作品</h2>
              <span class="sub-text">共 {{ worksTotal }} 个视频</span>
            </div>
            <el-button type="primary" icon="el-icon-upload2" round @click="openUploadDialog">投稿视频</el-button>
          </div>

          <div v-loading="loadingWorks" class="works-list-container">
            <div v-if="myWorksList.length === 0" class="empty-placeholder">
              <i class="el-icon-folder-opened" style="font-size: 60px; color: #e0e0e0; margin-bottom: 20px;"></i>
              <p>空空如也，去投个稿吧~</p>
            </div>

            <div v-else class="work-list">
              <div v-for="item in myWorksList" :key="item.id" class="work-item">
                <div class="work-cover" @click="goToVideo(item.id)">
                  <img :src="item.coverUrl" loading="lazy" />
                  <span class="duration">{{ formatDuration(item.duration) }}</span>

                  <div class="status-tag success" v-if="item.status === 'PASSED'">已发布</div>
                  <div class="status-tag warning" v-else-if="item.status === 'PENDING'">审核中</div>
                  <div class="status-tag error" v-else-if="item.status === 'REJECTED'">已驳回</div>
                </div>

                <div class="work-detail">
                  <div class="work-top">
                    <h3 class="work-title" @click="goToVideo(item.id)" :title="item.title">{{ item.title }}</h3>
                    <div class="work-actions" @click.stop>
                      <el-dropdown trigger="hover" placement="bottom-end" @command="(cmd) => handleWorkCommand(cmd, item)">
                        <i class="el-icon-more action-icon"></i>
                        <el-dropdown-menu slot="dropdown">
                          <el-dropdown-item command="edit" icon="el-icon-edit">编辑</el-dropdown-item>
                          <el-dropdown-item command="delete" icon="el-icon-delete" style="color: #F56C6C">删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </el-dropdown>
                    </div>
                  </div>

                  <div class="work-desc">{{ item.description || '暂无简介' }}</div>

                  <div class="work-footer">
                    <span class="time">{{ formatTime(item.createTime) }}</span>
                    <div class="stats">
                      <span><i class="el-icon-video-play"></i> {{ formatNumber(item.playCount) }}</span>
                      <span><i class="el-icon-chat-round"></i> {{ formatNumber(item.commentCount) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-show="currentTab === 'history'" class="tab-view">
          <div class="view-header">
            <h2>历史记录</h2>
            <el-button type="text" icon="el-icon-delete" @click="clearHistory" class="text-danger">清空历史</el-button>
          </div>

          <div v-loading="loadingHistory" class="history-timeline">
            <div v-if="playHistoryList.length === 0" class="empty-placeholder">
              <p>暂无观看记录</p>
            </div>

            <div v-else class="timeline-list">
              <div v-for="item in playHistoryList" :key="item.id" class="timeline-item">
                <div class="time-node"></div>
                <div class="item-card">
                  <div class="cover" @click="goToVideo(item.id)">
                    <img :src="item.coverUrl" />
                  </div>
                  <div class="detail">
                    <h3 @click="goToVideo(item.id)">{{ item.title }}</h3>
                    <p class="view-time">观看于 {{ formatTime(item.createTime) }}</p>
                    <div class="author-row">
                      UP: {{ item.authorName || '未知' }}
                    </div>
                  </div>
                  <div class="actions">
                    <el-button icon="el-icon-delete" circle size="mini" @click="deleteHistoryItem(item)"></el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-show="currentTab === 'favorites'" class="tab-view">
          <div class="fav-container">
            <div class="fav-menu">
              <div class="menu-title">我的收藏</div>
              <div class="menu-item active">
                <i class="el-icon-folder-opened"></i> 默认收藏夹
                <span class="count">{{ collectedVideos.length }}</span>
              </div>
              <div class="menu-item disabled">
                <i class="el-icon-lock"></i> 私密收藏
                <i class="el-icon-lock lock-icon"></i>
              </div>
            </div>

            <div class="fav-body">
              <div v-loading="loadingCollects" class="video-grid-modern compact">
                <div v-if="collectedVideos.length === 0" class="empty-placeholder">
                  <p>还没收藏过视频哦</p>
                </div>
                <div v-else v-for="item in collectedVideos" :key="item.id" class="video-card" @click="goToVideo(item.id)">
                  <div class="card-cover">
                    <img :src="item.coverUrl" />
                    <div class="hover-overlay red">
                      <el-button type="danger" round size="mini" @click.stop="cancelCollect(item)">取消收藏</el-button>
                    </div>
                  </div>
                  <div class="card-info">
                    <h3 class="title">{{ item.title }}</h3>
                    <div class="data-row">
                      <span>UP: {{ item.authorName || '未知' }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-show="currentTab === 'interactions'" class="tab-view">
          <div class="fav-container">

            <div class="fav-menu">
              <div class="menu-title">互动中心</div>

              <div
                  class="menu-item"
                  :class="{ active: interactionSubTab === 'likes' }"
                  @click="interactionSubTab = 'likes'"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <i class="el-icon-thumb"></i> 点赞视频
                </div>
                <span class="count" v-if="likedVideos.length">{{ likedVideos.length }}</span>
              </div>

              <div
                  class="menu-item"
                  :class="{ active: interactionSubTab === 'comments' }"
                  @click="interactionSubTab = 'comments'"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <i class="el-icon-chat-line-square"></i> 我的评论
                </div>
                <span class="count" v-if="myComments.length">{{ myComments.length }}</span>
              </div>
            </div>

            <div class="fav-body">

              <div class="fav-content-header">
                <div class="fav-title">
                  {{ interactionSubTab === 'likes' ? '我点赞的视频' : '我发布的评论' }}
                </div>
              </div>

              <div v-if="interactionSubTab === 'likes'" v-loading="loadingLikes">
                <div v-if="likedVideos.length === 0" class="empty-placeholder">
                  <p>暂无点赞</p>
                </div>
                <div v-else class="video-grid-modern">
                  <div v-for="item in likedVideos" :key="item.id" class="video-card" @click="goToVideo(item.id)">
                    <div class="card-cover">
                      <img :src="item.coverUrl" />
                      <div class="like-badge"><i class="el-icon-thumb"></i> 已赞</div>
                      <div class="hover-overlay red">
                        <el-button type="danger" round size="mini" @click.stop="cancelLike(item)">取消点赞</el-button>
                      </div>
                    </div>
                    <div class="card-info">
                      <h3 class="title">{{ item.title }}</h3>
                      <div class="data-row">
                        <span>UP: {{ item.authorName || '未知' }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="interactionSubTab === 'comments'" v-loading="loadingComments">
                <div v-if="myComments.length === 0" class="empty-placeholder">
                  <p>暂无评论</p>
                </div>
                <div v-else class="comment-timeline">
                  <div v-for="item in myComments" :key="item.id" class="comment-card-item">
                    <div class="comment-main">
                      <div class="comment-quote">
                        评论了视频: <strong class="target-link" @click="goToVideo(item.videoId)">{{ getVideoTitle(item.videoId) }}</strong>
                      </div>
                      <div class="comment-content">
                        {{ item.content }}
                      </div>
                      <div class="comment-footer">
                        {{ formatTime(item.createTime) }}
                        <span class="delete-link" @click="deleteMyComment(item)">删除</span>
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

    <el-dialog title="发布作品" :visible.sync="showUploadDialog" width="600px" custom-class="modern-dialog">
      <el-form ref="uploadForm" :model="uploadForm" label-position="top">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="标题">
              <el-input v-model="uploadForm.title" placeholder="取个吸引人的标题吧"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分区">
              <el-select v-model="uploadForm.categoryId" placeholder="选择分区">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="简介">
          <el-input type="textarea" :rows="3" v-model="uploadForm.description" placeholder="简单介绍一下视频内容..."></el-input>
        </el-form-item>

        <div class="upload-row">
          <div class="upload-box video-uploader" @click="triggerFileInput" :class="{hasFile: !!uploadForm.videoFile}">
            <i class="el-icon-video-camera-solid" v-if="!uploadForm.videoFile"></i>
            <div class="upload-text" v-if="!uploadForm.videoFile">上传视频文件</div>
            <div class="file-name" v-else>
              <i class="el-icon-check"></i> {{ uploadForm.videoFile.name }}
            </div>
            <input type="file" ref="fileInput" accept="video/mp4" hidden @change="handleFileSelect">
          </div>

          <div class="upload-box cover-uploader" @click="triggerCoverInput" :style="coverStyle">
            <i class="el-icon-picture" v-if="!uploadForm.coverPreview"></i>
            <div class="upload-text" v-if="!uploadForm.coverPreview">上传封面</div>
            <input type="file" ref="coverInput" accept="image/*" hidden @change="handleCoverSelect">
          </div>
        </div>
      </el-form>
      <div slot="footer">
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">立即发布</el-button>
      </div>
    </el-dialog>

    <el-dialog title="编辑个人资料" :visible.sync="showEditDialog" width="500px" custom-class="modern-dialog">
      <div class="edit-avatar-center">
        <el-avatar :size="80" :src="editForm.avatarUrl || userInfo.avatarUrl || defaultAvatar"></el-avatar>
        <el-button type="text" @click="$refs.avatarInput.click()">更换头像</el-button>
        <input type="file" ref="avatarInput" hidden accept="image/*" @change="handleAvatarSelect">
      </div>
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname"></el-input>
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="editForm.realName" placeholder="实名认证使用的姓名"></el-input>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="0">女</el-radio>
            <el-radio :label="2">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="简介">
          <el-input type="textarea" v-model="editForm.bio" :rows="3"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProfile" :loading="uploading">保存</el-button>
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

  </div>
</template>

<script>
import { userVideoApi } from '@/api/user'

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
        { key: 'works', label: '我的作品', icon: 'el-icon-video-camera' },
        { key: 'history', label: '播放历史', icon: 'el-icon-time' },
        { key: 'favorites', label: '收藏夹', icon: 'el-icon-star-off' },
        { key: 'interactions', label: '互动记录', icon: 'el-icon-chat-dot-square' }
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

      // Forms
      uploadForm: { title: '', description: '', categoryId: null, videoFile: null, coverFile: null, coverPreview: null },
      editForm: { nickname: '', bio: '', avatarUrl: '', gender: 2, realName: '' },
      accountForm: { username: '', phone: '', email: '' },
      pwdForm: { oldPassword: '', newPassword: '', confirmPassword: '' },

      pwdRules: {
        oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
        newPassword: [{ validator: validatePass, trigger: 'blur' }],
        confirmPassword: [{ validator: validatePass2, trigger: 'blur' }]
      },

      categories: [],

      // Follow Lists
      showFollowingList: false,
      showFansList: false
    }
  },
  computed: {
    coverStyle() {
      return this.uploadForm.coverPreview ? { backgroundImage: `url(${this.uploadForm.coverPreview})`, backgroundSize: 'cover' } : {}
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
        // 初始化编辑表单
        this.editForm.nickname = res.data.nickname
        this.editForm.bio = res.data.bio
        this.editForm.avatarUrl = res.data.avatarUrl
        this.editForm.gender = res.data.gender
        this.editForm.realName = res.data.realName
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
        this.$confirm('确定删除作品?','提示',{type:'warning'}).then(async () => {
          await userVideoApi.deleteMyVideo(item.id)
          this.$message.success('已删除')
          this.loadMyWorks()
        })
      }
    },
    async cancelCollect(item) {
      await userVideoApi.uncollectVideo(item.id)
      this.$message.success('取消收藏')
      this.loadFavorites()
    },
    async cancelLike(item) {
      await userVideoApi.unlikeVideo(item.id)
      this.loadLikes()
    },
    async deleteHistoryItem(item) {
      this.$confirm('确认删除这条记录?', '提示', { type: 'warning' }).then(async () => {
        try {
          await userVideoApi.deletePlayHistoryItem(item.id)
          this.playHistoryList = this.playHistoryList.filter(i => i.id !== item.id)
          this.$message.success('已删除')
        } catch (e) {
          console.warn(e)
        }
      })
    },
    async clearHistory() {
      this.$confirm('确定清空所有播放历史?', '提示', { type: 'warning' }).then(async () => {
        try {
          await userVideoApi.clearPlayHistory()
          this.playHistoryList = []
          this.$message.success('历史已清空')
        } catch (e) {
          console.warn('清空失败', e)
        }
      })
    },
    async deleteMyComment(item) {
      await userVideoApi.deleteComment(item.id)
      this.loadComments()
    },

    // --- Upload ---
    openUploadDialog() { this.showUploadDialog = true },
    triggerFileInput() { this.$refs.fileInput.click() },
    triggerCoverInput() { this.$refs.coverInput.click() },
    handleFileSelect(e) { this.uploadForm.videoFile = e.target.files[0] },
    handleCoverSelect(e) {
      const file = e.target.files[0]
      if(file) {
        this.uploadForm.coverFile = file
        this.uploadForm.coverPreview = URL.createObjectURL(file)
      }
    },
    async submitUpload() {
      if(!this.uploadForm.title || !this.uploadForm.videoFile) return this.$message.warning('请填写标题并选择视频')
      this.uploading = true
      try {
        const fd = new FormData()
        fd.append('file', this.uploadForm.videoFile)
        fd.append('title', this.uploadForm.title)
        fd.append('categoryId', this.uploadForm.categoryId || 1)
        if(this.uploadForm.description) fd.append('description', this.uploadForm.description)
        if(this.uploadForm.coverFile) fd.append('coverFile', this.uploadForm.coverFile)

        await userVideoApi.uploadVideo(fd)
        this.$message.success('投稿成功！请等待审核通过后显示')
        this.showUploadDialog = false
        if (this.currentTab === 'works') {
          this.loadMyWorks()
        } else {
          this.switchTab('works')
        }
      } catch(e) { this.$message.error('上传失败') }
      finally { this.uploading = false }
    },

    // --- Edit Profile ---
    openEditProfile() {
      this.showEditDialog = true
      this.editForm.avatarFile = null
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
      this.uploading = true
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
        this.uploading = false
      }
    },

    // --- Account Settings ---
    openAccountSettings() {
      this.showAccountDialog = true
      this.accountActiveTab = 'info'
      // 回显数据
      this.accountForm.username = this.userInfo.username
      this.accountForm.phone = this.userInfo.phone
      this.accountForm.email = this.userInfo.email
      // 重置密码表单
      this.pwdForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
      if(this.$refs.pwdForm) this.$refs.pwdForm.resetFields()
    },

    async saveAccountSettings() {
      this.uploading = true
      try {
        if (this.accountActiveTab === 'info') {
          // 更新基本账号信息 (手机/邮箱)
          await userVideoApi.updateProfile({
            phone: this.accountForm.phone,
            email: this.accountForm.email
          })
          this.$message.success('账号信息已更新')
          this.loadUserInfo() // 刷新全局信息
        } else {
          // 修改密码
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
    goToVideo(id) { this.$router.push(`/main/video/${id}`) },
    async loadCategories() {
      const res = await userVideoApi.getCategories()
      this.categories = res.data || []
    },
    formatDuration(s) {
      if(!s) return '00:00'
      const m = Math.floor(s/60); const sec=Math.floor(s%60)
      return `${m}:${sec<10?'0'+sec:sec}`
    },
    formatNumber(n) { return n > 10000 ? (n/10000).toFixed(1)+'w' : n || 0 },
    formatTime(t) { return t ? t.replace('T', ' ').substring(0,16) : '' },
    formatTimeAgo(t) { return this.formatTime(t).split(' ')[0] },
    async loadVideoTitles() {
      const ids = [...new Set(this.myComments.map(c => c.videoId))]
      for(let id of ids) {
        if(!this.videoTitleCache[id]) {
          try {
            const res = await userVideoApi.getVideoById(id)
            this.$set(this.videoTitleCache, id, res.data.title)
          } catch(e) {
            console.warn('获取视频标题失败', e)
          }
        }
      }
    },
    getVideoTitle(id) { return this.videoTitleCache[id] || `视频ID:${id}` }
  }
}
</script>

<style scoped>
/* * 现代极简设计 CSS
 * 核心：去边框、大阴影、圆角、留白
 */

.profile-page {
  min-height: 100vh;
  background: #f6f7f8;
}

/* 1. Banner */
.profile-banner {
  height: 240px;
  background-image: url('https://picsum.photos/1920/300?blur=2'); /* 默认背景图 */
  background-size: cover;
  background-position: center;
  position: relative;
}
.banner-mask {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 80px;
  background: linear-gradient(to top, rgba(0,0,0,0.4), transparent);
}

.main-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 20px;
  position: relative;
  margin-top: -60px; /* 悬浮效果关键 */
  z-index: 10;
}

/* 2. 用户卡片 */
.user-info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}

.info-left {
  display: flex;
  gap: 20px;
}

.avatar-wrapper {
  position: relative;
  margin-top: -40px; /* 头像上浮 */
}
.main-avatar {
  border: 4px solid #fff;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  background: #fff;
  font-size: 32px;
}
.vip-badge {
  position: absolute;
  bottom: 5px; right: 5px;
  background: #FB7299;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  border: 2px solid #fff;
}

.text-content {
  padding-bottom: 5px;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.nickname {
  font-size: 24px;
  font-weight: bold;
  color: #222;
  margin: 0;
}
.level-tag {
  background: #eee;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  color: #666;
}
.bio {
  font-size: 14px;
  color: #999;
  max-width: 400px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
}

.info-right {
  display: flex;
  align-items: center;
  gap: 40px;
}
.stat-box {
  text-align: center;
  cursor: pointer;
}
.stat-box:hover .stat-num { color: #409EFF; }
.stat-num { font-size: 20px; font-weight: bold; color: #222; }
.stat-label { font-size: 12px; color: #999; margin-top: 4px; }

.action-group {
  display: flex;
  gap: 10px;
}
.edit-btn {
  padding: 10px 24px;
  font-weight: 600;
}
.edit-avatar-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

/* 3. 导航栏 */
.nav-bar-sticky {
  margin-top: 20px;
  background: transparent;
  border-bottom: 1px solid #e7e7e7;
}
.nav-list {
  display: flex;
  gap: 40px;
}
.nav-item {
  font-size: 16px;
  color: #666;
  padding: 12px 0;
  cursor: pointer;
  position: relative;
  transition: color 0.3s;
}
.nav-item:hover { color: #409EFF; }
.nav-item.active { color: #409EFF; font-weight: 600; }
.nav-item.active .active-line {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 3px;
  background: #409EFF;
  border-radius: 3px;
}

/* 4. 内容区 */
.content-wrapper {
  margin-top: 20px;
  padding-bottom: 60px;
}
.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.view-header h2 { margin: 0; font-size: 20px; color: #222; font-weight: 500; }
.sub-text { font-size: 13px; color: #999; margin-left: 10px; }
.text-danger { color: #F56C6C; }

/* 我的作品 - 列表布局样式 */
.works-list-container {
  background: #fff;
  border-radius: 8px;
}

.work-list {
  display: flex;
  flex-direction: column;
}

.work-item {
  display: flex;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
  gap: 20px;
  transition: background-color 0.2s;
}

.work-item:hover {
  background-color: #fafafa;
}

.work-item:last-child {
  border-bottom: none;
}

/* 左侧封面 */
.work-cover {
  width: 190px;
  height: 107px; /* 16:9 比例 */
  position: relative;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  background: #000;
}

.work-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.work-cover:hover img {
  transform: scale(1.05);
}

.work-cover .duration {
  position: absolute; bottom: 6px; right: 6px;
  background: rgba(0,0,0,0.6); color: #fff;
  font-size: 12px; padding: 2px 4px; border-radius: 4px;
}

.work-cover .status-tag {
  position: absolute; top: 6px; left: 6px;
  font-size: 12px; padding: 2px 6px; border-radius: 4px; color: #fff;
}
.status-tag.success { background: #67C23A; }
.status-tag.warning { background: #E6A23C; }
.status-tag.error { background: #F56C6C; }

/* 右侧详情 */
.work-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0; /* 防止文本溢出 */
}

.work-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.work-title {
  font-size: 16px;
  font-weight: 600;
  color: #222;
  margin: 0;
  cursor: pointer;
  line-height: 1.4;
  /* 标题最多显示2行 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.work-title:hover {
  color: #409EFF;
}

.action-icon {
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 4px;
}
.action-icon:hover {
  color: #409EFF;
}

.work-desc {
  font-size: 13px;
  color: #666;
  margin-top: 8px;
  line-height: 1.5;
  /* 描述最多显示2行 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.work-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
  margin-top: 10px;
}

.work-footer .stats span {
  margin-left: 20px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.video-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.video-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
}
.card-cover {
  width: 100%;
  padding-top: 56.25%; /* 16:9 */
  position: relative;
  background: #000;
}
.card-cover img {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover;
}
.duration {
  position: absolute; bottom: 6px; right: 6px;
  background: rgba(0,0,0,0.6); color: #fff;
  font-size: 12px; padding: 2px 4px; border-radius: 4px;
}
.status-tag {
  position: absolute; top: 6px; left: 6px;
  font-size: 12px; padding: 2px 6px; border-radius: 4px; color: #fff;
}
.status-tag.success { background: #67C23A; }
.status-tag.warning { background: #E6A23C; }
.status-tag.error { background: #F56C6C; }

.hover-overlay {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.2s;
}
.hover-overlay.red { background: rgba(0,0,0,0.5); }
.video-card:hover .hover-overlay { opacity: 1; }
.hover-overlay i { font-size: 40px; color: #fff; }

.card-info { padding: 12px; }
.title {
  margin: 0 0 8px 0; font-size: 14px; color: #222;
  line-height: 20px; height: 40px;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.meta-row, .data-row {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: #999;
}
.more-actions:hover { color: #409EFF; }

/* 历史时间轴 */
.history-timeline {
  background: #fff; border-radius: 12px; padding: 20px;
}
.timeline-item {
  display: flex; gap: 20px; padding: 15px 0; border-bottom: 1px solid #f0f0f0;
}
.item-card {
  display: flex; gap: 15px; flex: 1;
}
.item-card .cover {
  width: 160px; height: 90px; border-radius: 6px; overflow: hidden;
}
.item-card .cover img { width: 100%; height: 100%; object-fit: cover; }
.item-card .detail { flex: 1; }
.item-card h3 { margin: 0 0 8px 0; font-size: 16px; color: #222; cursor: pointer; }
.item-card h3:hover { color: #409EFF; }
.view-time { font-size: 12px; color: #999; }
.author-row { font-size: 12px; color: #666; margin-top: 10px; }

/* 收藏夹布局 */
.fav-container { display: flex; gap: 20px; }
.fav-menu { width: 220px; background: #fff; border-radius: 8px; padding: 15px 0; height: fit-content; }
.menu-title { padding: 0 20px 10px; font-weight: 600; color: #999; font-size: 12px; }
.menu-item {
  padding: 12px 20px; cursor: pointer; display: flex; align-items: center; justify-content: space-between; color: #333; font-size: 14px;
}
.menu-item:hover { background: #f1f2f3; }
.menu-item.active { background: #409EFF; color: #fff; }
.menu-item.disabled { color: #ccc; cursor: not-allowed; }
.fav-body { flex: 1; }

/* 评论记录 */
.comment-timeline { background: #fff; padding: 20px; border-radius: 8px; }
.comment-card-item {
  padding: 20px 0; border-bottom: 1px solid #eee;
}
.comment-quote {
  font-size: 13px; color: #999; margin-bottom: 8px;
}
.comment-content {
  font-size: 15px; color: #222; line-height: 1.5;
}
.comment-footer {
  margin-top: 8px; font-size: 12px; color: #999;
}
.delete-link {
  margin-left: 15px; cursor: pointer; display: none;
}
.comment-card-item:hover .delete-link { display: inline-block; color: #F56C6C; }

/* 上传弹窗样式优化 */
.upload-row { display: flex; gap: 20px; margin-top: 10px; }
.upload-box {
  flex: 1; border: 2px dashed #d9d9d9; border-radius: 6px;
  height: 120px; display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; color: #8c939d; transition: all 0.3s;
  background-size: cover; background-position: center;
}
.upload-box:hover { border-color: #409EFF; color: #409EFF; }
.upload-box i { font-size: 28px; margin-bottom: 8px; }
.upload-box.hasFile { border-style: solid; border-color: #67C23A; color: #67C23A; }

.empty-placeholder {
  grid-column: 1 / -1;
  text-align: center; padding: 60px 0; color: #999;
}
.empty-img { width: 160px; margin-bottom: 20px; }

/* 动画 */
.fade-enter { animation: fadeIn 0.4s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

/* 响应式 */
@media (max-width: 768px) {
  .user-info-card { flex-direction: column; align-items: flex-start; }
  .info-right { margin-top: 20px; width: 100%; justify-content: space-between; }
  .fav-container { flex-direction: column; }
  .fav-menu { width: 100%; }
}
</style>