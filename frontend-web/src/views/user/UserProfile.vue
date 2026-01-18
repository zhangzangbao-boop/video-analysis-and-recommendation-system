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
              <div class="stat-card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                <div class="stat-icon"><i class="el-icon-video-camera"></i></div>
                <div class="stat-content">
                  <div class="stat-number">{{ worksTotal }}</div>
                  <div class="stat-label">我的作品</div>
                </div>
              </div>
              <div class="stat-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                <div class="stat-icon"><i class="el-icon-star-on"></i></div>
                <div class="stat-content">
                  <div class="stat-number">{{ userStats.following || 0 }}</div>
                  <div class="stat-label">关注</div>
                </div>
              </div>
              <div class="stat-card" style="background: linear-gradient(135deg, #5ee7df 0%, #b490ca 100%);">
                <div class="stat-icon"><i class="el-icon-user"></i></div>
                <div class="stat-content">
                  <div class="stat-number">{{ userStats.followers || 0 }}</div>
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
              <h3><i class="el-icon-video-camera"></i> 我的作品</h3>
              <p>管理您上传的视频，查看审核状态</p>
            </div>

            <div v-loading="loadingWorks">
              <el-table :data="myWorksList" style="width: 100%" v-if="myWorksList.length > 0" border stripe>
                <el-table-column label="封面" width="140" align="center">
                  <template slot-scope="scope">
                    <img :src="scope.row.coverUrl" style="width: 100px; height: 56px; object-fit: cover; border-radius: 4px;">
                  </template>
                </el-table-column>

                <el-table-column prop="title" label="标题" min-width="200">
                  <template slot-scope="scope">
                    <span style="font-weight: bold;">{{ scope.row.title }}</span>
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
                    <el-button type="text" style="color: #F56C6C" icon="el-icon-delete" @click="handleDeleteWork(scope.row)">删除</el-button>
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

          <div v-show="['history', 'likes', 'comments'].includes(activeTab)" class="tab-pane enhanced">
            <div class="empty-state">
              <div class="empty-icon"><i class="el-icon-coffee"></i></div>
              <h4>功能开发中</h4>
              <p>该模块即将上线，敬请期待</p>
            </div>
          </div>

          <div v-show="activeTab === 'settings'" class="tab-pane enhanced">
            <div class="content-header"><h3>账号设置</h3></div>
            <div class="empty-state"><p>暂无设置选项</p></div>
          </div>
        </div>
      </el-card>
    </div>
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
        bio: '暂无简介'
      },
      userStats: { following: 0, followers: 0 },

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
      }
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
    }
  },
  methods: {
    handleTabClick(tabName) {
      this.activeTab = tabName;
      if (tabName === 'works') {
        this.fetchMyWorks();
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
.stat-card { flex: 1; padding: 15px; border-radius: 10px; color: white; display: flex; align-items: center; gap: 10px; box-shadow: 0 4px 10px rgba(0,0,0,0.1); }
.stat-number { font-size: 24px; font-weight: bold; }
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

/* 响应式适配 */
@media (max-width: 768px) {
  .profile-header-enhanced { flex-direction: column; text-align: center; }
  .stats-cards { flex-direction: column; }
  .custom-tabs { overflow-x: auto; }
}
</style>