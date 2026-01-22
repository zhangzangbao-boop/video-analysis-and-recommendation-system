<template>
  <div class="video-manage">
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="待审核视频" name="pending">
          <span slot="label"><i class="el-icon-time"></i> 待审核</span>
        </el-tab-pane>
        <el-tab-pane label="已发布视频" name="published">
          <span slot="label"><i class="el-icon-success"></i> 已发布</span>
        </el-tab-pane>
        <el-tab-pane label="已驳回视频" name="rejected">
          <span slot="label"><i class="el-icon-error"></i> 已驳回</span>
        </el-tab-pane>
      </el-tabs>

      <div style="margin-bottom: 20px; display: flex; justify-content: space-between;">
        <div>
          <el-input
              placeholder="搜索视频标题..."
              v-model="listQuery.keyword"
              style="width: 240px; margin-right: 10px;"
              size="small"
              prefix-icon="el-icon-search"
              clearable
              @clear="handleSearch"
              @keyup.enter.native="handleSearch"
          ></el-input>
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">查询</el-button>
        </div>
        <el-button type="success" size="small" icon="el-icon-refresh" @click="fetchList">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="180" align="center"></el-table-column>

        <el-table-column label="视频封面" width="140" align="center">
          <template slot-scope="scope">
            <div class="video-cover" @click="openVideoDrawer(scope.row)">
              <img :src="scope.row.coverUrl" alt="封面" style="width: 100%; height: 70px; object-fit: cover; border-radius: 4px; display: block;">
              <i class="el-icon-caret-right play-icon"></i>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="视频标题" min-width="200">
          <template slot-scope="scope">
            <span style="font-weight: bold; color: #303133;">{{ scope.row.title }}</span>
            <br>
            <el-tag size="mini" type="info" style="margin-top: 5px;">{{ scope.row.categoryName || '默认分类' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 'PENDING'" type="warning" size="small">待审核</el-tag>
            <el-tag v-else-if="scope.row.status === 'PASSED'" type="success" size="small">已发布</el-tag>
            <el-tag v-else-if="scope.row.status === 'REJECTED'" type="danger" size="small">已驳回</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="上传时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template slot-scope="scope">
            <div v-if="scope.row.status === 'PENDING'">
              <el-button size="mini" type="primary" plain @click="openVideoDrawer(scope.row)">审核 / 预览</el-button>
            </div>
            <div v-else>
              <el-button size="mini" type="text" @click="openVideoDrawer(scope.row)">查看详情</el-button>
              <el-button size="mini" type="text" style="color: #F56C6C;" @click="handleDelete(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; text-align: right;">
        <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="listQuery.page"
            :page-sizes="[10, 20, 50]"
            :page-size="listQuery.limit"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total">
        </el-pagination>
      </div>
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
              :src="currentVideo.videoUrl"
              controls
              autoplay
              style="width: 100%; height: 300px; background: #000; border-radius: 8px;"
          ></video>
        </div>

        <div class="video-meta" style="margin-top: 20px;">
          <h3>{{ currentVideo.title }}</h3>
          <p class="desc">{{ currentVideo.description || '暂无简介' }}</p>
          <div class="tags">
            <el-tag size="small">{{ currentVideo.categoryName || '未分类' }}</el-tag>
            <el-tag size="small" type="info">上传于: {{ formatTime(currentVideo.createTime) }}</el-tag>
          </div>
          <!-- AI审核建议 -->
          <div v-if="hasAiAudit()" style="margin-top: 15px;">
            <el-alert 
              :type="getAiAuditType()" 
              :closable="false"
              show-icon>
              <template slot="title">
                <span style="font-weight: bold;">🤖 AI审核建议</span>
                <div style="margin-top: 8px; font-size: 14px;">{{ getAiAuditMessage() }}</div>
              </template>
            </el-alert>
          </div>
          <!-- 人工审核意见 -->
          <div v-else-if="currentVideo.auditMsg && !hasAiAudit()" style="margin-top: 15px;">
            <el-alert :title="'审核意见: ' + currentVideo.auditMsg" type="info" :closable="false"></el-alert>
          </div>
        </div>

        <el-divider></el-divider>

        <!-- AI分析区域 -->
        <div style="margin-bottom: 20px; padding: 15px; background: #f0f9ff; border-radius: 4px; border: 1px solid #b3d8ff;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
            <h4 style="margin: 0;"><i class="el-icon-cpu"></i> AI智能分析</h4>
            <el-button 
              type="primary" 
              size="small" 
              icon="el-icon-search"
              :loading="aiAnalyzing"
              @click="triggerAiAudit">
              {{ aiAuditResult ? '重新分析' : '开始AI分析' }}
            </el-button>
          </div>
          
          <!-- AI分析结果展示 -->
          <div v-if="aiAuditResult" style="margin-top: 15px;">
            <el-alert 
              :type="getAiResultType()" 
              :closable="false"
              show-icon>
              <template slot="title">
                <div style="font-weight: bold; margin-bottom: 8px;">AI分析结果</div>
                <div style="font-size: 14px; line-height: 1.6;">
                  <div><strong>审核建议：</strong>{{ aiAuditResult.suggestion === 'PASS' ? '通过' : aiAuditResult.suggestion === 'REJECT' ? '驳回' : '人工审核' }}</div>
                  <div><strong>风险等级：</strong>
                    <el-tag :type="getRiskTagType()" size="small">{{ getRiskLevelText() }}</el-tag>
                    <span style="margin-left: 10px;">风险分数：{{ aiAuditResult.riskScore || 0 }}/100</span>
                  </div>
                  <div><strong>分析说明：</strong>{{ aiAuditResult.message }}</div>
                  
                  <!-- 违规详情 -->
                  <div v-if="aiAuditResult.violations && aiAuditResult.violations.length > 0" style="margin-top: 10px;">
                    <div style="font-weight: bold; color: #F56C6C; margin-bottom: 5px;">⚠️ 违规标注：</div>
                    <div v-for="(violation, index) in aiAuditResult.violations" :key="index" 
                         style="padding: 8px; background: #fff; border-radius: 4px; margin-bottom: 5px; border-left: 3px solid #F56C6C;">
                      <div><strong>{{ violation.typeName }}</strong> 
                        <el-tag size="mini" type="info" style="margin-left: 5px;">{{ violation.location }}</el-tag>
                        <el-tag size="mini" type="warning" style="margin-left: 5px;">置信度: {{ violation.confidence }}%</el-tag>
                      </div>
                      <div style="color: #666; font-size: 12px; margin-top: 3px;">{{ violation.description }}</div>
                    </div>
                  </div>
                </div>
              </template>
            </el-alert>
            
            <!-- 根据AI结果快速操作 -->
            <div style="margin-top: 15px;">
              <el-button 
                v-if="aiAuditResult.suggestion === 'PASS'"
                type="success" 
                size="small"
                :loading="auditLoading"
                @click="submitAudit('pass', '采纳AI分析结果：通过')">
                采纳AI建议 - 通过
              </el-button>
              <el-button 
                v-if="aiAuditResult.suggestion === 'REJECT'"
                type="danger" 
                size="small"
                :loading="auditLoading"
                @click="submitAudit('reject', aiAuditResult.message)">
                采纳AI建议 - 驳回
              </el-button>
              <el-button 
                v-if="aiAuditResult.suggestion === 'REVIEW'"
                type="warning" 
                size="small"
                :loading="auditLoading"
                @click="submitAudit('pass', '忽略AI建议，人工通过')">
                忽略AI建议，人工通过
              </el-button>
            </div>
          </div>
          
          <div v-else-if="!aiAnalyzing" style="color: #909399; font-size: 14px; text-align: center; padding: 20px;">
            点击"开始AI分析"按钮，对视频进行智能审核分析
          </div>
          <div v-else style="text-align: center; padding: 20px;">
            <i class="el-icon-loading" style="font-size: 20px;"></i>
            <span style="margin-left: 10px;">AI正在分析中，请稍候...</span>
          </div>
        </div>

        <el-divider></el-divider>

        <div v-if="currentVideo.status === 'PENDING'" class="audit-action">
          <!-- AI一键操作 -->
          <div v-if="hasAiAudit()" style="margin-bottom: 20px; padding: 15px; background: #f5f7fa; border-radius: 4px;">
            <h4 style="margin: 0 0 10px 0;"><i class="el-icon-cpu"></i> AI智能审核</h4>
            <div style="margin-bottom: 10px; color: #606266; font-size: 14px;">
              {{ getAiAuditMessage() }}
            </div>
            <el-button 
              v-if="getAiAuditAction() === 'PASS'" 
              type="success" 
              icon="el-icon-check" 
              size="small"
              :loading="auditLoading"
              @click="submitAudit('pass', '采纳AI建议：通过')">
              一键采纳AI建议（通过）
            </el-button>
            <el-button 
              v-if="getAiAuditAction() === 'REJECT'" 
              type="danger" 
              icon="el-icon-close" 
              size="small"
              :loading="auditLoading"
              @click="submitAudit('reject', getAiAuditMessage())">
              一键采纳AI建议（驳回）
            </el-button>
            <el-button 
              v-if="getAiAuditAction() === 'REVIEW'" 
              type="warning" 
              icon="el-icon-view" 
              size="small"
              :loading="auditLoading"
              @click="submitAudit('pass', '忽略AI建议，人工通过')">
              忽略AI建议，人工通过
            </el-button>
          </div>
          
          <h4><i class="el-icon-s-check"></i> 人工审核</h4>
          <el-form>
            <el-form-item label="审核意见">
              <el-input
                  type="textarea"
                  v-model="auditReason"
                  :rows="3"
                  placeholder="如果驳回，请务必填写驳回原因；如果通过，可不填。">
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="success" icon="el-icon-check" :loading="auditLoading" @click="submitAudit('pass')">通过</el-button>
              <el-button type="danger" icon="el-icon-close" :loading="auditLoading" @click="submitAudit('reject')">驳回</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-else class="status-info">
          <el-alert
              v-if="currentVideo.status === 'PASSED'"
              title="该视频已发布"
              type="success"
              show-icon
              :closable="false"
              description="视频状态正常，用户可见。">
          </el-alert>
          <el-alert
              v-if="currentVideo.status === 'REJECTED'"
              title="该视频已驳回"
              type="error"
              show-icon
              :closable="false"
              :description="'驳回原因：' + (currentVideo.auditMsg || '无')">
          </el-alert>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { videoApi } from '@/api/admin'

export default {
  name: 'VideoManage',
  data() {
    return {
      activeTab: 'pending', // pending, published, rejected
      list: [],
      total: 0,
      loading: false,
      auditLoading: false,

      listQuery: {
        page: 1,
        limit: 10,
        status: 'PENDING',
        keyword: ''
      },

      drawerVisible: false,
      auditReason: '',
      currentVideo: {},
      aiAuditResult: null, // AI分析结果
      aiAnalyzing: false // AI分析中
    }
  },
  created() {
    this.fetchList();
  },
  methods: {
    // 获取列表数据
    async fetchList() {
      this.loading = true;
      try {
        // 映射 tab 到 API 需要的状态参数
        let statusParam = 'PENDING';
        if (this.activeTab === 'published') statusParam = 'PASSED';
        if (this.activeTab === 'rejected') statusParam = 'REJECTED';

        this.listQuery.status = statusParam;

        const res = await videoApi.getVideoList(this.listQuery);
        if (res.code === 200) {
          // 🔥 核心修复：正确读取 list 字段
          this.list = res.data.list || res.data.records || [];
          this.total = Number(res.data.total) || 0;
        }
      } catch (error) {
        console.error(error);
        this.$message.error('获取视频列表失败');
      } finally {
        this.loading = false;
      }
    },

    handleTabClick() {
      this.listQuery.page = 1;
      this.fetchList();
    },

    handleSearch() {
      this.listQuery.page = 1;
      this.fetchList();
    },

    handleSizeChange(val) {
      this.listQuery.limit = val;
      this.fetchList();
    },

    handleCurrentChange(val) {
      this.listQuery.page = val;
      this.fetchList();
    },

    openVideoDrawer(row) {
      this.currentVideo = row;
      this.auditReason = ''; // 重置审核理由
      this.aiAuditResult = null; // 重置AI分析结果
      this.drawerVisible = true;
    },
    
    // 触发AI分析
    async triggerAiAudit() {
      if (!this.currentVideo || !this.currentVideo.id) {
        this.$message.warning('请先选择视频');
        return;
      }
      
      this.aiAnalyzing = true;
      try {
        const res = await videoApi.aiAuditVideo(this.currentVideo.id);
        if (res.code === 200) {
          this.aiAuditResult = res.data;
          this.$message.success('AI分析完成');
          // 刷新视频信息（更新audit_msg）
          this.fetchList();
          // 重新获取当前视频信息
          const videoRes = await videoApi.getVideoById(this.currentVideo.id);
          if (videoRes.code === 200) {
            this.currentVideo = videoRes.data;
          }
        } else {
          this.$message.error(res.msg || 'AI分析失败');
        }
      } catch (error) {
        console.error('AI分析失败', error);
        this.$message.error('AI分析请求失败，请稍后重试');
      } finally {
        this.aiAnalyzing = false;
      }
    },
    
    // 获取AI结果类型（用于el-alert）
    getAiResultType() {
      if (!this.aiAuditResult) return 'info';
      if (this.aiAuditResult.suggestion === 'PASS') return 'success';
      if (this.aiAuditResult.suggestion === 'REJECT') return 'error';
      return 'warning';
    },
    
    // 获取风险标签类型
    getRiskTagType() {
      if (!this.aiAuditResult) return 'info';
      const level = this.aiAuditResult.riskLevel;
      if (level === 'HIGH') return 'danger';
      if (level === 'MEDIUM') return 'warning';
      return 'success';
    },
    
    // 获取风险等级文本
    getRiskLevelText() {
      if (!this.aiAuditResult) return '未知';
      const level = this.aiAuditResult.riskLevel;
      if (level === 'HIGH') return '高风险';
      if (level === 'MEDIUM') return '中风险';
      if (level === 'LOW') return '低风险';
      return '未知';
    },

    handleCloseDrawer(done) {
      // 停止播放
      const video = document.querySelector('video');
      if (video) video.pause();
      this.currentVideo = {};
      done();
    },

    // 判断是否有AI审核建议
    hasAiAudit() {
      return this.currentVideo.auditMsg && this.currentVideo.auditMsg.includes('[AI审核]');
    },

    // 获取AI审核消息
    getAiAuditMessage() {
      if (!this.currentVideo.auditMsg) return '';
      const msg = this.currentVideo.auditMsg;
      if (msg.includes('[AI审核]')) {
        return msg.replace('[AI审核]', '').trim();
      }
      return msg;
    },

    // 获取AI审核类型（用于显示不同颜色的提示）
    getAiAuditType() {
      const msg = this.getAiAuditMessage();
      if (msg.includes('建议通过') || msg.includes('通过审核')) {
        return 'success';
      } else if (msg.includes('建议驳回') || msg.includes('驳回')) {
        return 'error';
      } else if (msg.includes('人工审核')) {
        return 'warning';
      }
      return 'info';
    },

    // 获取AI审核建议的操作
    getAiAuditAction() {
      const msg = this.getAiAuditMessage();
      if (msg.includes('建议通过') || msg.includes('通过审核')) {
        return 'PASS';
      } else if (msg.includes('建议驳回') || msg.includes('驳回')) {
        return 'REJECT';
      } else if (msg.includes('人工审核')) {
        return 'REVIEW';
      }
      return 'PASS';
    },

    // 提交审核
    async submitAudit(action, aiReason = null) {
      // 如果使用AI建议，使用AI的原因；否则使用手动输入的原因
      const reason = aiReason || this.auditReason;
      
      if (action === 'reject' && !reason || !reason.trim()) {
        this.$message.warning('驳回操作必须填写审核意见');
        return;
      }

      this.auditLoading = true;
      try {
        const payload = {
          videoId: this.currentVideo.id,
          action: action,
          reason: reason || '通过审核'
        };

        const res = await videoApi.auditVideo(payload);
        if (res.code === 200) {
          this.$message.success('操作成功');
          this.drawerVisible = false;
          this.fetchList(); // 刷新列表
        } else {
          this.$message.error(res.msg || '操作失败');
        }
      } catch (error) {
        this.$message.error('审核请求失败');
      } finally {
        this.auditLoading = false;
      }
    },

    // 删除视频
    handleDelete(row) {
      this.$confirm('确定要删除该视频吗? 此操作不可恢复', '警告', { type: 'warning' }).then(async () => {
        try {
          const res = await videoApi.deleteVideo(row.id);
          if (res.code === 200) {
            this.$message.success('视频已删除');
            this.fetchList();
          }
        } catch (error) {
          this.$message.error('删除失败');
        }
      });
    },

    formatTime(timeStr) {
      if (!timeStr) return '';
      return timeStr.replace('T', ' ');
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
.drawer-content { padding: 20px; height: 100%; overflow-y: auto; }
.video-meta { margin-top: 15px; }
.video-meta h3 { margin: 0 0 10px 0; }
.desc { color: #666; font-size: 14px; margin-bottom: 15px; line-height: 1.5; }
.tags .el-tag { margin-right: 10px; }
</style>