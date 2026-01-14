<template>
  <div class="video-manage">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="待审核视频" name="pending">
          <span slot="label"><i class="el-icon-time"></i> 待审核 <el-badge :value="pendingCount" class="item" type="danger" v-if="pendingCount>0"/></span>
        </el-tab-pane>
        <el-tab-pane label="已发布视频" name="published">
          <span slot="label"><i class="el-icon-success"></i> 已发布</span>
        </el-tab-pane>
      </el-tabs>

      <div style="margin-bottom: 20px; display: flex; justify-content: space-between;">
        <div>
          <el-input 
            placeholder="搜索视频标题..." 
            v-model="searchQuery" 
            style="width: 240px; margin-right: 10px;"
            size="small"
            prefix-icon="el-icon-search"
            clearable
          ></el-input>
          <el-button type="primary" size="small" icon="el-icon-search">查询</el-button>
        </div>
        <el-button type="success" size="small" icon="el-icon-upload2">模拟上传</el-button>
      </div>

      <el-table :data="filteredList" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        
        <el-table-column label="视频封面" width="140" align="center">
          <template slot-scope="scope">
            <div class="video-cover" @click="openVideoDrawer(scope.row)">
              <img :src="scope.row.cover" alt="封面" style="width: 100%; height: 70px; object-fit: cover; border-radius: 4px; display: block;">
              <i class="el-icon-caret-right play-icon"></i>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="视频标题" min-width="200">
          <template slot-scope="scope">
            <span style="font-weight: bold; color: #303133;">{{ scope.row.title }}</span>
            <br>
            <el-tag size="mini" type="info" style="margin-top: 5px;">{{ scope.row.category }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="author" label="发布者" width="120" align="center">
          <template slot-scope="scope">
            <el-avatar size="small" style="background: #409EFF">{{ scope.row.author.substring(0,1) }}</el-avatar>
            <div style="font-size: 12px; color: #666;">{{ scope.row.author }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="uploadTime" label="上传时间" width="160" align="center" sortable></el-table-column>
        
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template slot-scope="scope">
            <div v-if="activeTab === 'pending'">
              <el-button size="mini" type="primary" plain @click="openVideoDrawer(scope.row)">审核 / 预览</el-button>
            </div>
            <div v-else>
              <el-button size="mini" type="text" @click="openVideoDrawer(scope.row)">查看详情</el-button>
              <el-button size="mini" type="text" style="color: #F56C6C;" @click="handleDelete(scope.row)">下架</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer
      :title="currentVideo.title || '视频详情'"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="600px"
      :before-close="handleCloseDrawer">
      
      <div class="drawer-content" v-if="currentVideo.id">
        <div class="player-wrapper">
          <video 
            :src="currentVideo.url" 
            controls 
            autoplay 
            style="width: 100%; height: 340px; background: #000; border-radius: 8px;"
          ></video>
        </div>

        <div class="video-meta">
          <h3>{{ currentVideo.title }}</h3>
          <p class="desc">{{ currentVideo.description || '暂无简介' }}</p>
          <div class="tags">
            <el-tag size="small">{{ currentVideo.category }}</el-tag>
            <el-tag size="small" type="warning">{{ currentVideo.duration }}</el-tag>
            <el-tag size="small" type="info">发布于: {{ currentVideo.uploadTime }}</el-tag>
          </div>
        </div>

        <el-divider></el-divider>

        <div v-if="currentVideo.status === 'pending'" class="audit-action">
           <h4><i class="el-icon-s-check"></i> 审核操作</h4>
           <el-form>
             <el-form-item label="审核意见">
               <el-input type="textarea" v-model="auditReason" placeholder="如果驳回，请填写原因..."></el-input>
             </el-form-item>
             <el-form-item>
               <el-button type="success" icon="el-icon-check" @click="submitAudit('pass')">通过发布</el-button>
               <el-button type="danger" icon="el-icon-close" @click="submitAudit('reject')">驳回视频</el-button>
             </el-form-item>
           </el-form>
        </div>
        
        <div v-else class="status-info">
          <el-alert title="该视频状态正常" type="success" show-icon :closable="false" description="视频已发布并展示在用户端推荐流中。"></el-alert>
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
      activeTab: 'pending',
      searchQuery: '',
      drawerVisible: false,
      auditReason: '',
      currentVideo: {}, // 当前选中的视频对象
      
      // --- 真实测试数据 (带有可播放的 url) ---
      allVideos: [
        { 
          id: 101, 
          title: '超治愈！海边日落Vlog', 
          author: 'User_林克', 
          category: '生活/Vlog',
          uploadTime: '2026-01-14 10:00',
          duration: '0:46',
          status: 'pending',
          cover: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?ixlib=rb-1.2.1&auto=format&fit=crop&w=500&q=60',
          // 真实视频链接
          url: 'https://vjs.zencdn.net/v/oceans.mp4' 
        },
        { 
          id: 102, 
          title: '3D动画制作过程揭秘', 
          author: 'User_极客', 
          category: '科技/动画',
          uploadTime: '2026-01-13 15:30',
          duration: '0:52',
          status: 'pending',
          // 🔴 修改点：换成了 Picsum 的随机图片链接，更稳定
          cover: 'https://picsum.photos/id/237/500/300', 
          url: 'https://media.w3.org/2010/05/sintel/trailer.mp4'
        },
        { 
          id: 103, 
          title: '可爱的Big Buck Bunny', 
          author: 'User_C', 
          category: '动漫/搞笑',
          uploadTime: '2026-01-12 09:20',
          duration: '1:00',
          status: 'published',
          cover: 'https://picsum.photos/500/300?random=103',
          url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'
        }
      ]
    }
  },
  computed: {
    filteredList() {
      return this.allVideos.filter(item => {
        const matchTab = item.status === this.activeTab;
        const matchSearch = !this.searchQuery || item.title.includes(this.searchQuery);
        return matchTab && matchSearch;
      });
    },
    pendingCount() {
      return this.allVideos.filter(v => v.status === 'pending').length;
    }
  },
  methods: {
    // 打开抽屉并播放
    openVideoDrawer(row) {
      this.currentVideo = row;
      this.auditReason = '';
      this.drawerVisible = true;
    },
    handleCloseDrawer(done) {
      // 关闭时清空视频，防止声音继续播放
      this.currentVideo = {};
      done();
    },
    submitAudit(action) {
      const msg = action === 'pass' ? '审核通过，已发布' : '已驳回该视频';
      const type = action === 'pass' ? 'success' : 'warning';
      
      this.$message({ type, message: msg });
      this.drawerVisible = false;
      
      // 更新本地数据状态
      const target = this.allVideos.find(v => v.id === this.currentVideo.id);
      if (target) {
        target.status = action === 'pass' ? 'published' : 'rejected';
      }
    },
    handleDelete(row) {
      this.$confirm('确定要下架该视频吗?', '警告', { type: 'warning' }).then(() => {
        this.allVideos = this.allVideos.filter(v => v.id !== row.id);
        this.$message.success('视频已下架');
      });
    }
  }
}
</script>

<style scoped>
.video-manage { padding: 20px; }
.video-cover { position: relative; cursor: pointer; overflow: hidden; border-radius: 4px; }
.video-cover:hover .play-icon { opacity: 1; transform: scale(1.1); }
.play-icon {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  color: #fff; font-size: 24px; opacity: 0.8; transition: all 0.3s;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}
/* 抽屉内容样式 */
.drawer-content { padding: 20px; height: 100%; overflow-y: auto; }
.video-meta { margin-top: 15px; }
.video-meta h3 { margin: 0 0 10px 0; }
.desc { color: #666; font-size: 14px; margin-bottom: 15px; line-height: 1.5; }
.tags .el-tag { margin-right: 10px; }
.audit-action { margin-top: 20px; }
</style>