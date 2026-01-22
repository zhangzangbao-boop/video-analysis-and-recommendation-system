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

      <el-table :data="logList" border stripe style="width: 100%; margin-top: 20px;" v-loading="loading">
        <el-table-column prop="id" label="日志ID" width="80"></el-table-column>
        <el-table-column prop="operator" label="操作人" width="120"></el-table-column>
        <el-table-column prop="module" label="所属模块" width="120">
          <template slot-scope="scope">
            <el-tag size="small" :type="getModuleType(scope.row.module)">{{ scope.row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作内容" min-width="200"></el-table-column>
        <el-table-column prop="ipAddress" label="操作IP" width="140"></el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="180">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag type="success" v-if="scope.row.status === '成功'">成功</el-tag>
            <el-tag type="danger" v-else>失败</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; text-align: right;">
        <el-pagination 
          background 
          layout="total, sizes, prev, pager, next, jumper" 
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        ></el-pagination>
      </div>
    </el-card>
  </div>
</template>

<script>
import { logsApi } from '@/api/admin'

export default {
  name: 'AdminLogs',
  data() {
    return {
      loading: false,
      query: {
        keyword: '',
        dateRange: ''
      },
      logList: [],
      total: 0,
      currentPage: 1,
      pageSize: 10
    }
  },
  mounted() {
    this.fetchLogList()
  },
  methods: {
    async fetchLogList() {
      this.loading = true
      try {
        const params = {
          page: this.currentPage,
          pageSize: this.pageSize,
          keyword: this.query.keyword || undefined
        }
        
        // 处理日期范围
        if (this.query.dateRange && this.query.dateRange.length === 2) {
          params.startDate = this.query.dateRange[0]
          params.endDate = this.query.dateRange[1]
        }
        
        const res = await logsApi.getLogList(params)
        if (res.code === 200) {
          this.logList = res.data.list || res.data.records || []
          this.total = Number(res.data.total) || 0
        } else {
          this.$message.error(res.message || '获取日志列表失败')
        }
      } catch (error) {
        console.error('获取日志列表失败:', error)
        this.$message.error('获取日志列表失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    
    handleSearch() {
      this.currentPage = 1
      this.fetchLogList()
    },
    
    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
      this.fetchLogList()
    },
    
    handlePageChange(val) {
      this.currentPage = val
      this.fetchLogList()
    },
    
    getModuleType(module) {
      if (module === '系统登录' || module?.includes('登录')) return 'info'
      if (module === '用户管理' || module?.includes('用户')) return 'warning'
      if (module === '视频管理' || module?.includes('视频')) return 'primary'
      return ''
    },
    
    formatTime(time) {
      if (!time) return ''
      // 处理 LocalDateTime 格式：2024-01-12T14:30:00
      if (typeof time === 'string') {
        return time.replace('T', ' ').substring(0, 19)
      }
      return time
    },
    
    exportToExcel() {
      this.$confirm('确定要导出当前页面的日志数据吗?', '提示').then(() => {
        // 1. 定义表头 (CSV格式，用逗号分隔)
        const headers = ['日志ID,操作人,所属模块,操作内容,操作IP,操作时间,状态']
        
        // 2. 格式化数据 (将对象数组转为 CSV 字符串)
        const data = this.logList.map(item => {
          const time = this.formatTime(item.createTime)
          return `${item.id},${item.operator},${item.module},${item.action},${item.ipAddress || ''},${time},${item.status}`
        })
        
        // 3. 合并内容 (\n 表示换行)
        const csvContent = headers.concat(data).join('\n')
        
        // 4. 创建 Blob 对象 (为了解决中文乱码，必须加 \uFEFF BOM头)
        const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
        
        // 5. 创建下载链接并模拟点击
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = `系统操作日志_${new Date().getTime()}.csv` // 文件名带时间戳
        link.click()
        
        // 6. 释放资源
        URL.revokeObjectURL(link.href)
        
        this.$message.success('导出成功！请查看浏览器下载栏')
      }).catch(() => {
        // 用户点击取消，不做任何事
      })
    }
  }
}
</script>

<style scoped>
.filter-container { display: flex; align-items: center; }
</style>