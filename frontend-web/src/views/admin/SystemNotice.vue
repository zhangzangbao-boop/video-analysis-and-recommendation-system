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
            <el-radio label="系统通知">系统通知</el-radio>
            <el-radio label="活动推广">活动推广</el-radio>
            <el-radio label="安全警告">安全警告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="消息内容">
          <el-input type="textarea" v-model="form.content" rows="6" placeholder="请输入要推送给所有用户的内容..."></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-s-promotion" @click="sendPush">立即推送</el-button>
          <el-button icon="el-icon-document-add">存为草稿</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 20px;">
      <div slot="header">历史推送记录</div>
      <el-table :data="history" border stripe>
        <el-table-column prop="title" label="标题" min-width="200"></el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template slot-scope="scope">
            <el-tag size="small" :type="getTypeColor(scope.row.type)">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="推送时间" width="180"></el-table-column>
        <el-table-column prop="count" label="触达人数" width="120" align="center"></el-table-column>
        <el-table-column label="状态" width="100" align="center">
           <template><el-tag type="success" size="mini">已发送</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'SystemNotice',
  data() {
    return {
      form: { title: '', type: '系统通知', content: '' },
      history: [
        { title: '关于打击违规视频的公告', type: '安全警告', time: '2026-01-10 10:00', count: 4520 },
        { title: '新版本功能上线通知', type: '系统通知', time: '2026-01-01 09:00', count: 4200 },
        { title: '春节短视频创作大赛开启', type: '活动推广', time: '2025-12-25 12:00', count: 3800 }
      ]
    }
  },
  methods: {
    getTypeColor(type) {
      if (type === '安全警告') return 'danger';
      if (type === '活动推广') return 'warning';
      return 'primary';
    },
    sendPush() {
      if (!this.form.title || !this.form.content) {
        return this.$message.warning('请完整填写标题和内容');
      }
      this.$confirm('确定要向全平台用户发送此消息吗？', '推送确认', { type: 'warning' })
        .then(() => {
          this.$message.success('推送任务已提交，系统正在排队发送中...');
          // 模拟添加到列表
          this.history.unshift({
            title: this.form.title,
            type: this.form.type,
            time: new Date().toLocaleString(),
            count: 4520 + Math.floor(Math.random() * 100)
          });
          this.form.title = '';
          this.form.content = '';
        });
    }
  }
}
</script>

<style scoped>
.notice-container { padding: 20px; }
</style>