<template>
  <div class="video-manage">
    <el-card shadow="never">
      <div class="filter-wrapper">
        <div class="filter-left">
          <el-input 
            placeholder="搜索视频标题/ID..." 
            v-model="query.keyword" 
            style="width: 200px; margin-right: 10px;"
            size="small"
            prefix-icon="el-icon-search"
            clearable
          ></el-input>
          
          <el-select v-model="query.category" placeholder="全部分类" size="small" style="width: 120px; margin-right: 10px;" clearable>
            <el-option label="生活Vlog" value="vlog"></el-option>
            <el-option label="科技数码" value="tech"></el-option>
            <el-option label="搞笑娱乐" value="funny"></el-option>
            <el-option label="萌宠动物" value="pet"></el-option>
          </el-select>

          <el-date-picker
            v-model="query.date"
            type="daterange"
            range-separator="-"
            start-placeholder="发布开始"
            end-placeholder="发布结束"
            size="small"
            style="width: 220px; margin-right: 10px;">
          </el-date-picker>

          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button size="small" icon="el-icon-refresh-right" @click="resetFilter">重置</el-button>
        </div>

        <div class="filter-right" v-if="multipleSelection.length > 0">
          <el-alert :title="`已选择 ${multipleSelection.length} 项`" type="info" show-icon :closable="false" style="display:inline-block; width:auto; padding: 5px 10px; margin-right: 10px;"></el-alert>
          <el-button type="success" size="small" icon="el-icon-check" @click="handleBatchAction('pass')">批量通过</el-button>
          <el-button type="danger" size="small" icon="el-icon-delete" @click="handleBatchAction('delete')">批量下架</el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-click="handleSearch" style="margin-top: 10px;">
        <el-tab-pane label="全部视频" name="all"></el-tab-pane>
        <el-tab-pane name="pending">
          <span slot="label">待审核 <el-badge :value="3" class="tab-badge" type="danger" /></span>
        </el-tab-pane>
        <el-tab-pane label="已发布" name="published"></el-tab-pane>
        <el-tab-pane label="已下架" name="removed"></el-tab-pane>
      </el-tabs>

      <el-table 
        :data="filteredList" 
        border 
        style="width: 100%; margin-top: 15px;" 
        v-loading="loading"
        @selection-change="handleSelectionChange">
        
        <el-table-column type="selection" width="55" align="center"></el-table-column>
        <el-table-column prop="id" label="ID" width="80" align="center" sortable></el-table-column>
        
        <el-table-column label="视频信息" min-width="250">
          <template slot-scope="scope">
            <div style="display: flex;">
              <div class="video-thumb" @click="openDrawer(scope.row)">
                <i class="el-icon-caret-right play-icon"></i>
                <div :style="{background: scope.row.color}" style="width: 80px; height: 50px; border-radius: 4px;"></div>
                <span class="video-duration">{{ scope.row.duration }}</span>
              </div>
              <div style="margin-left: 10px; display: flex; flex-direction: column; justify-content: space-between;">
                <div class="video-title" @click="openDrawer(scope.row)">{{ scope.row.title }}</div>
                <div>
                  <el-tag size="mini" type="info">{{ scope.row.categoryLabel }}</el-tag>
                  <el-tag v-if="scope.row.isHot" size="mini" type="danger" effect="plain" style="margin-left: 5px;">热门</el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="作者" width="150">
          <template slot-scope="scope">
            <div style="display: flex; align-items: center;">
              <el-avatar size="small" icon="el-icon-user-solid"></el-avatar>
              <div style="margin-left: 8px;">
                <div style="font-size: 13px;">{{ scope.row.author }}</div>
                <div style="font-size: 12px; color: #999;">ID: {{ scope.row.authorId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="数据指标" width="180">
          <template slot-scope="scope">
            <div class="stats-row"><i class="el-icon-video-play"></i> {{ scope.row.views }}</div>
            <div class="stats-row"><i class="el-icon-star-off"></i> {{ scope.row.likes }}</div>
            <div class="stats-row"><i class="el-icon-chat-line-square"></i> {{ scope.row.comments }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small" effect="dark">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="uploadTime" label="发布时间" width="160" sortable align="center"></el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="openDrawer(scope.row)">审核/详情</el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, scope.row)" style="margin-left: 10px;">
              <span class="el-dropdown-link" style="color: #409EFF; cursor: pointer; font-size: 12px;">
                更多 <i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="hot" v-if="scope.row.status === 'published'">{{ scope.row.isHot ? '取消热门' : '设为热门' }}</el-dropdown-item>
                <el-dropdown-item command="delete" style="color: #F56C6C;">删除/下架</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; text-align: right;">
        <el-pagination background layout="total, prev, pager, next" :total="100"></el-pagination>
      </div>
    </el-card>

    <el-drawer
      :title="currentVideo.title || '视频详情'"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="500px">
      <div class="drawer-content" v-if="currentVideo.id">
        <div class="video-player-placeholder" :style="{background: currentVideo.color}">
          <i class="el-icon-video-play" style="font-size: 60px; color: #fff; opacity: 0.8;"></i>
          <div style="color: #fff; margin-top: 10px;">(此处为视频播放区域)</div>
        </div>
        
        <div class="drawer-section">
          <h4>基础信息</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="视频ID">{{ currentVideo.id }}</el-descriptions-item>
            <el-descriptions-item label="发布作者">{{ currentVideo.author }} (ID: {{currentVideo.authorId}})</el-descriptions-item>
            <el-descriptions-item label="所属分类">{{ currentVideo.categoryLabel }}</el-descriptions-item>
            <el-descriptions-item label="发布时间">{{ currentVideo.uploadTime }}</el-descriptions-item>
            <el-descriptions-item label="视频简介">{{ currentVideo.desc || '这是一段为了模拟展示效果的视频简介，通常包含视频内容的详细说明。' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="drawer-section" v-if="currentVideo.status === 'pending'">
          <h4 style="color: #E6A23C;"><i class="el-icon-s-check"></i> 审核操作</h4>
          <el-form label-position="top">
            <el-form-item label="审核意见">
               <el-input type="textarea" v-model="auditReason" rows="3" placeholder="如果驳回，请务必填写原因..."></el-input>
            </el-form-item>
            <el-form-item>
               <el-button type="success" icon="el-icon-check" @click="submitAudit('pass')" style="width: 120px;">通过发布</el-button>
               <el-button type="danger" icon="el-icon-close" @click="submitAudit('reject')" style="width: 120px;">驳回</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <div class="drawer-section" v-else>
          <el-alert title="该视频已审核完成" type="info" :description="'当前状态：' + getStatusLabel(currentVideo.status)" show-icon :closable="false"></el-alert>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
export default {
  name: 'VideoManage',
  data() {
    return {
      activeTab: 'all',
      loading: false,
      drawerVisible: false,
      auditReason: '',
      currentVideo: {},
      multipleSelection: [],
      query: {
        keyword: '',
        category: '',
        date: [] // 修复点：初始化为数组，避免日期选择器报错
      },
      // 模拟丰富的数据
      tableData: [
        { id: 1001, title: '2026年第一场雪 Vlog', author: '林克', authorId: 8821, categoryLabel: '生活Vlog', duration: '05:20', color: '#845EC2', views: 12030, likes: 3200, comments: 450, status: 'published', isHot: true, uploadTime: '2026-01-13 10:00' },
        { id: 1002, title: 'Vue3 源码深度解析', author: '极客阿辉', authorId: 9932, categoryLabel: '科技数码', duration: '20:15', color: '#D65DB1', views: 890, likes: 120, comments: 30, status: 'pending', isHot: false, uploadTime: '2026-01-13 09:30' },
        { id: 1003, title: '我家猫咪会后空翻', author: '萌宠君', authorId: 1024, categoryLabel: '萌宠动物', duration: '00:45', color: '#FF9671', views: 56000, likes: 8900, comments: 1200, status: 'published', isHot: false, uploadTime: '2026-01-12 18:20' },
        { id: 1004, title: '涉嫌违规的宣传视频', author: '匿名用户', authorId: 1000, categoryLabel: '其他', duration: '01:00', color: '#FFC75F', views: 20, likes: 0, comments: 0, status: 'removed', isHot: false, uploadTime: '2026-01-11 11:00' },
        { id: 1005, title: '搞笑段子合集 Vol.3', author: '开心麻瓜', authorId: 3321, categoryLabel: '搞笑娱乐', duration: '03:30', color: '#F9F871', views: 3400, likes: 560, comments: 88, status: 'pending', isHot: false, uploadTime: '2026-01-10 14:00' },
        { id: 1006, title: '如何制作美味的红烧肉', author: '美食家小王', authorId: 5566, categoryLabel: '生活Vlog', duration: '08:12', color: '#00D2FC', views: 4500, likes: 670, comments: 120, status: 'pending', isHot: false, uploadTime: '2026-01-09 12:30' }
      ]
    }
  },
  computed: {
    // 前端模拟筛选逻辑
    filteredList() {
      return this.tableData.filter(item => {
        // 1. Tab页签筛选
        if (this.activeTab !== 'all' && item.status !== this.activeTab) return false;
        // 2. 搜索关键词
        if (this.query.keyword && !item.title.includes(this.query.keyword)) return false;
        // 3. 分类筛选
        // 注意：这里简单模拟，实际应用需根据 value 对应
        if (this.query.category) {
            const map = { 'vlog': '生活Vlog', 'tech': '科技数码', 'funny': '搞笑娱乐', 'pet': '萌宠动物' };
            if (item.categoryLabel !== map[this.query.category]) return false;
        }
        return true;
      });
    }
  },
  methods: {
    handleSearch() {
      this.loading = true;
      setTimeout(() => { this.loading = false; }, 300);
    },
    resetFilter() {
      this.query = { keyword: '', category: '', date: [] };
      this.handleSearch();
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    handleBatchAction(action) {
      this.$confirm(`确定要批量${action === 'pass' ? '通过' : '下架'} ${this.multipleSelection.length} 个视频吗？`, '提示', { type: 'warning' })
        .then(() => {
          this.$message.success('批量操作成功');
          // 模拟前端删除
          if (action === 'delete') {
            const ids = this.multipleSelection.map(i => i.id);
            this.tableData = this.tableData.filter(i => !ids.includes(i.id));
          }
          this.multipleSelection = []; 
        }).catch(() => {});
    },
    getStatusType(status) {
      const map = { published: 'success', pending: 'warning', removed: 'danger' };
      return map[status] || 'info';
    },
    getStatusLabel(status) {
      const map = { published: '已发布', pending: '待审核', removed: '已下架' };
      return map[status] || status;
    },
    openDrawer(row) {
      this.currentVideo = row;
      this.auditReason = '';
      this.drawerVisible = true;
    },
    submitAudit(type) {
      this.$message.success(type === 'pass' ? '审核已通过' : '已驳回该视频');
      this.drawerVisible = false;
      // 更新本地数据状态
      const target = this.tableData.find(v => v.id === this.currentVideo.id);
      if (target) {
        target.status = type === 'pass' ? 'published' : 'removed';
      }
    },
    handleCommand(command, row) {
      if (command === 'hot') {
        row.isHot = !row.isHot;
        this.$message.success(row.isHot ? '已设为热门' : '已取消热门');
      } else if (command === 'delete') {
        this.$confirm('确认删除吗？', '警告').then(() => {
          this.tableData = this.tableData.filter(v => v.id !== row.id);
          this.$message.success('删除成功');
        });
      }
    }
  }
}
</script>

<style scoped>
.filter-wrapper { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.filter-left, .filter-right { display: flex; align-items: center; }

/* 视频列表样式 */
.video-thumb { position: relative; cursor: pointer; display: flex; align-items: center; }
.video-thumb:hover .play-icon { opacity: 1; transform: scale(1.2); }
.play-icon { position: absolute; left: 30px; top: 15px; font-size: 24px; color: #fff; opacity: 0; transition: all 0.3s; z-index: 2; pointer-events: none; }
.video-duration { position: absolute; right: 4px; bottom: 4px; background: rgba(0,0,0,0.6); color: #fff; font-size: 10px; padding: 1px 4px; border-radius: 2px; }
.video-title { font-weight: bold; color: #303133; cursor: pointer; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; height: 40px; line-height: 20px; font-size: 14px; }
.video-title:hover { color: #409EFF; }
.stats-row { font-size: 12px; color: #606266; line-height: 18px; display: flex; align-items: center; }
.stats-row i { margin-right: 5px; width: 14px; }
.tab-badge >>> .el-badge__content { margin-top: 8px; }

/* 抽屉内容 */
.drawer-content { padding: 20px; height: 100%; overflow-y: auto; }
.video-player-placeholder { width: 100%; height: 200px; background: #000; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 8px; margin-bottom: 20px; }
.drawer-section { margin-bottom: 20px; }
.drawer-section h4 { border-left: 4px solid #409EFF; padding-left: 10px; margin-bottom: 15px; color: #303133; }
</style>