<template>
  <div class="notice-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix">
        <span style="font-weight: bold; font-size: 16px;">全站消息推送中心</span>
        <el-tag size="small" type="danger" style="float: right;">仅限管理员</el-tag>
      </div>
      
      <el-form label-width="100px" style="max-width: 800px; margin-top: 20px;">
        <el-form-item label="消息标题">
          <el-input v-model="form.title" placeholder="例如：系统维护通知"></el-input>
        </el-form-item>
        <el-form-item label="消息类型">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">系统公告</el-radio>
            <el-radio :label="2">活动通知</el-radio>
            <el-radio :label="3">系统维护</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="推送范围">
          <el-radio-group v-model="form.targetType">
            <el-radio :label="0">全部用户</el-radio>
            <el-radio :label="1">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="消息内容">
          <el-input type="textarea" v-model="form.content" rows="6" placeholder="请输入要推送给所有用户的内容..."></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-s-promotion" @click="sendPush" :loading="loading">立即推送</el-button>
          <el-button icon="el-icon-document-add" @click="saveDraft" :loading="loading">存为草稿</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 20px;">
      <div slot="header">历史推送记录</div>
      <el-table :data="history" border stripe>
        <el-table-column prop="title" label="标题" min-width="200"></el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template slot-scope="scope">
            <el-tag size="small" :type="getTypeColor(scope.row.type)">{{ getTypeName(scope.row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="推送时间" width="180">
          <template slot-scope="scope">
            {{ scope.row.time ? scope.row.time.substring(0, 19).replace('T', ' ') : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="count" label="触达人数" width="120" align="center"></el-table-column>
        <el-table-column label="状态" width="100" align="center">
           <template><el-tag type="success" size="mini">已发送</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { adminApi } from '@/api/admin'

export default {
  name: 'SystemNotice',
  data() {
    return {
      form: { 
        title: '', 
        type: 1, // 1-系统公告, 2-活动通知, 3-系统维护
        content: '',
        targetType: 0 // 0-全部用户, 1-指定用户
      },
      history: [],
      loading: false
    }
  },
  created() {
    this.loadHistory()
  },
  methods: {
    getTypeColor(type) {
      if (type === 3) return 'danger'; // 系统维护
      if (type === 2) return 'warning'; // 活动通知
      return 'primary'; // 系统公告
    },
    getTypeName(type) {
      const typeMap = { 1: '系统公告', 2: '活动通知', 3: '系统维护' }
      return typeMap[type] || '系统通知'
    },
    async sendPush() {
      if (!this.form.title || !this.form.content) {
        return this.$message.warning('请完整填写标题和内容');
      }
      this.$confirm('确定要向全平台用户发送此消息吗？', '推送确认', { type: 'warning' })
        .then(async () => {
          this.loading = true
          try {
            const notice = {
              title: this.form.title,
              content: this.form.content,
              type: this.form.type,
              targetType: this.form.targetType
            }
            const res = await adminApi.publishNotice(notice)
            if (res.code === 200) {
              this.$message.success('推送成功！');
              this.form.title = '';
              this.form.content = '';
              this.loadHistory()
            } else {
              this.$message.error(res.message || '推送失败');
            }
          } catch (error) {
            console.error('推送失败:', error)
            this.$message.error('推送失败，请重试');
          } finally {
            this.loading = false
          }
        });
    },
    async saveDraft() {
      if (!this.form.title || !this.form.content) {
        return this.$message.warning('请完整填写标题和内容');
      }
      this.loading = true
      try {
        const notice = {
          title: this.form.title,
          content: this.form.content,
          type: this.form.type,
          targetType: this.form.targetType,
          status: 0 // 草稿
        }
        const res = await adminApi.createNotice(notice)
        if (res.code === 200) {
          this.$message.success('已保存为草稿');
          this.loadHistory()
        } else {
          this.$message.error(res.message || '保存失败');
        }
      } catch (error) {
        console.error('保存失败:', error)
        this.$message.error('保存失败，请重试');
      } finally {
        this.loading = false
      }
    },
    async loadHistory() {
      try {
        const res = await adminApi.getPublishedNotices()
        if (res.code === 200) {
          this.history = (res.data || []).map(item => ({
            ...item,
            type: item.type,
            time: item.publishTime || item.createTime,
            count: item.readCount || 0
          }))
        }
      } catch (error) {
        console.error('加载历史记录失败:', error)
      }
    }
  }
}
</script>

<style scoped>
.notice-container { padding: 20px; }
</style>