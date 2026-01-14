<template>
  <div class="admin-logs">
    <el-card shadow="never">
      <div class="filter-container">
        <el-input 
          placeholder="搜索操作人或内容..." 
          v-model="query.keyword" 
          style="width: 200px; margin-right: 10px;"
          size="small"
        ></el-input>
        
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
          style="margin-right: 10px;"
        >
        </el-date-picker>

        <el-button type="primary" icon="el-icon-search" size="small" @click="handleSearch">查询日志</el-button>
        
        <el-button icon="el-icon-download" size="small" @click="exportToExcel">导出Excel</el-button>
      </div>

      <el-table :data="logList" border stripe style="width: 100%; margin-top: 20px;">
        <el-table-column prop="id" label="日志ID" width="80"></el-table-column>
        <el-table-column prop="operator" label="操作人" width="120"></el-table-column>
        <el-table-column prop="module" label="所属模块" width="120">
          <template slot-scope="scope">
            <el-tag size="small" :type="getModuleType(scope.row.module)">{{ scope.row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作内容" min-width="200"></el-table-column>
        <el-table-column prop="ip" label="操作IP" width="140"></el-table-column>
        <el-table-column prop="time" label="操作时间" width="180"></el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag type="success" v-if="scope.row.status === '成功'">成功</el-tag>
            <el-tag type="danger" v-else>失败</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; text-align: right;">
        <el-pagination background layout="prev, pager, next" :total="100"></el-pagination>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'AdminLogs',
  data() {
    return {
      query: {
        keyword: '',
        dateRange: ''
      },
      // 模拟的日志数据
      logList: [
        { id: 501, operator: 'admin', module: '用户管理', action: '冻结用户 [User_李四]', ip: '192.168.1.101', time: '2026-01-12 14:30:00', status: '成功' },
        { id: 502, operator: 'admin', module: '视频管理', action: '驳回视频 [Vue教学]', ip: '192.168.1.101', time: '2026-01-12 14:28:15', status: '成功' },
        { id: 503, operator: 'editor', module: '视频管理', action: '设为热门 [萌宠合集]', ip: '192.168.1.105', time: '2026-01-12 11:10:05', status: '成功' },
        { id: 504, operator: 'admin', module: '系统登录', action: '登录后台系统', ip: '192.168.1.101', time: '2026-01-12 09:00:00', status: '成功' },
        { id: 505, operator: 'unknown', module: '系统登录', action: '尝试登录失败 (密码错误)', ip: '114.22.10.5', time: '2026-01-11 23:15:00', status: '失败' }
      ]
    }
  },
  methods: {
    handleSearch() {
      this.$message.info('正在查询日志...');
    },
    getModuleType(module) {
      if (module === '系统登录') return 'info';
      if (module === '用户管理') return 'warning';
      if (module === '视频管理') return 'primary';
      return '';
    },
    
    // --- 修改点2：新增 exportToExcel 函数 ---
    exportToExcel() {
      this.$confirm('确定要导出当前页面的日志数据吗?', '提示').then(() => {
        // 1. 定义表头 (CSV格式，用逗号分隔)
        const headers = ['日志ID,操作人,所属模块,操作内容,操作IP,操作时间,状态'];
        
        // 2. 格式化数据 (将对象数组转为 CSV 字符串)
        const data = this.logList.map(item => {
          return `${item.id},${item.operator},${item.module},${item.action},${item.ip},${item.time},${item.status}`;
        });
        
        // 3. 合并内容 (\n 表示换行)
        const csvContent = headers.concat(data).join('\n');
        
        // 4. 创建 Blob 对象 (为了解决中文乱码，必须加 \uFEFF BOM头)
        const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
        
        // 5. 创建下载链接并模拟点击
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `系统操作日志_${new Date().getTime()}.csv`; // 文件名带时间戳
        link.click();
        
        // 6. 释放资源
        URL.revokeObjectURL(link.href);
        
        this.$message.success('导出成功！请查看浏览器下载栏');
      }).catch(() => {
        // 用户点击取消，不做任何事
      });
    }
  }
}
</script>

<style scoped>
.filter-container { display: flex; align-items: center; }
</style>