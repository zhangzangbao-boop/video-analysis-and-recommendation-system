<template>
  <div class="user-manage">
    <el-card shadow="never">
      <div class="filter-wrapper">
        <div class="filter-left">
          <el-input 
            v-model="query.keyword" 
            placeholder="搜索昵称 / ID / 手机号" 
            style="width: 240px; margin-right: 10px;"
            size="small"
            prefix-icon="el-icon-search"
            clearable
            @clear="handleSearch"
          ></el-input>
          
          <el-select v-model="query.status" placeholder="账号状态" size="small" style="width: 120px; margin-right: 10px;" clearable>
            <el-option label="正常" value="normal"></el-option>
            <el-option label="冻结" value="frozen"></el-option>
            <el-option label="禁言" value="muted"></el-option>
          </el-select>

          <el-select v-model="query.level" placeholder="用户等级" size="small" style="width: 120px; margin-right: 10px;" clearable>
            <el-option label="LV.1" value="1"></el-option>
            <el-option label="LV.2" value="2"></el-option>
            <el-option label="LV.3" value="3"></el-option>
          </el-select>

          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button size="small" icon="el-icon-refresh-right" @click="resetFilter">重置</el-button>
        </div>
        
        <div class="filter-right">
          <el-button type="success" size="small" icon="el-icon-plus" @click="showAddUserDialog">添加新用户</el-button>
        </div>
      </div>

      <el-table 
        :data="filteredList" 
        border 
        stripe 
        style="width: 100%; margin-top: 15px;"
        v-loading="loading"
      >
        <el-table-column prop="id" label="UID" width="80" align="center" sortable></el-table-column>
        
        <el-table-column label="用户" min-width="180">
          <template slot-scope="scope">
            <div style="display: flex; align-items: center; cursor: pointer;" @click="openDrawer(scope.row)">
              <el-avatar shape="square" size="medium" :src="scope.row.avatar"></el-avatar>
              <div style="margin-left: 10px;">
                <div style="font-weight: bold; color: #409EFF;">{{ scope.row.username || scope.row.nickname }}</div>
                <div style="font-size: 12px; color: #909399;">{{ scope.row.phone || '未绑定' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="level" label="等级" width="100" align="center">
          <template slot-scope="scope">
             <el-tag size="mini" effect="plain" type="info">Lv.{{ scope.row.level }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="资产数据" width="150">
          <template slot-scope="scope">
            <div style="font-size: 12px;">余额: <span style="color: #F56C6C;">￥{{ scope.row.balance }}</span></div>
            <div style="font-size: 12px;">积分: <span style="color: #E6A23C;">{{ scope.row.points }}</span></div>
          </template>
        </el-table-column>

        <el-table-column prop="regTime" label="注册时间" width="160" sortable align="center" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ scope.row.regTime || (scope.row.createTime ? formatDateTime(scope.row.createTime) : '') }}
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.statusStr === 'normal' || scope.row.status === 1" type="success" size="small" effect="dark">正常</el-tag>
            <el-tag v-else-if="scope.row.statusStr === 'frozen' || scope.row.status === 0" type="danger" size="small" effect="dark">已冻结</el-tag>
            <el-tag v-else type="warning" size="small" effect="dark">禁言中</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="small" icon="el-icon-document" @click="openDrawer(scope.row)">画像详情</el-button>
            <el-divider direction="vertical"></el-divider>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, scope.row)">
              <span class="el-dropdown-link" style="color: #409EFF; cursor: pointer; font-size: 12px;">
                管理 <i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="resetPwd">重置密码</el-dropdown-item>
                <el-dropdown-item command="freeze" style="color: #F56C6C;" v-if="scope.row.statusStr !== 'frozen' && scope.row.status !== 0">冻结账号</el-dropdown-item>
                <el-dropdown-item command="unfreeze" style="color: #67C23A;" v-else>解封账号</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; text-align: right;">
        <el-pagination background layout="total, prev, pager, next" :total="filteredList.length"></el-pagination>
      </div>
    </el-card>

    <el-drawer
      :title="currentUser.username + ' 的用户画像'"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="500px">
      
      <div class="drawer-content">
        <el-skeleton :rows="5" animated v-if="loadingDetail" />
        
        <div v-else>
          <div class="user-header">
            <el-avatar :size="70" :src="currentUser.avatar" shape="circle" style="border: 2px solid #fff; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);"></el-avatar>
            <div class="user-header-info">
              <h3 style="margin: 0;">{{ currentUser.username }} <i v-if="currentUser.gender === 'male'" class="el-icon-male" style="color: #409EFF;"></i><i v-else class="el-icon-female" style="color: #F56C6C;"></i></h3>
              <p style="color: #999; margin: 5px 0; font-size: 13px;">UID: {{ currentUser.id }} | {{ currentUser.bio || '这个人很懒，什么都没写' }}</p>
              <div>
                <el-tag size="mini" type="info">IP: 192.168.1.101</el-tag>
                <el-tag size="mini" type="warning" style="margin-left: 5px;">北京</el-tag>
              </div>
            </div>
          </div>

          <div class="stats-cards">
            <div class="stat-box">
              <div class="num">{{ currentUser.fansCount || 0 }}</div>
              <div class="label">粉丝数</div>
            </div>
             <div class="stat-box">
              <div class="num">{{ currentUser.videoCount || 0 }}</div>
              <div class="label">发布作品</div>
            </div>
             <div class="stat-box">
              <div class="num">{{ currentUser.likeCount || 0 }}</div>
              <div class="label">获赞总量</div>
            </div>
          </div>

          <el-divider></el-divider>

          <div class="section-title"><i class="el-icon-postcard"></i> 详细档案</div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="真实姓名">{{ currentUser.realName || '未实名' }}</el-descriptions-item>
            <el-descriptions-item label="手机号码">{{ currentUser.phone }}</el-descriptions-item>
            <el-descriptions-item label="电子邮箱">{{ currentUser.email || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ currentUser.regTime }}</el-descriptions-item>
            <el-descriptions-item label="最后登录">{{ currentUser.lastLogin || '2026-01-13 12:00:00' }}</el-descriptions-item>
          </el-descriptions>

          <br>
          
          <div class="section-title"><i class="el-icon-time"></i> 近期动态</div>
          <el-timeline>
            <el-timeline-item timestamp="2026-01-13" placement="top" color="#409EFF">
              发布了视频《{{ currentUser.username }}的Vlog》
            </el-timeline-item>
            <el-timeline-item timestamp="2026-01-12" placement="top" color="#67C23A">
              点赞了视频《搞笑合集》
            </el-timeline-item>
            <el-timeline-item timestamp="2026-01-10" placement="top">
              注册成为新用户
            </el-timeline-item>
          </el-timeline>
          
          <div class="drawer-footer">
            <el-button type="warning" plain icon="el-icon-message-solid">发送通知</el-button>
            <el-button type="danger" v-if="currentUser.statusStr !== 'frozen' && currentUser.status !== 0" icon="el-icon-lock" @click="handleCommand('freeze', currentUser)">冻结账号</el-button>
            <el-button type="success" v-else icon="el-icon-unlock" @click="handleCommand('unfreeze', currentUser)">解封账号</el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 添加用户对话框 -->
    <el-dialog
      title="添加新用户"
      :visible.sync="addUserDialogVisible"
      width="500px"
      :close-on-click-modal="false">
      <el-form 
        :model="addUserForm" 
        :rules="addUserFormRules" 
        ref="addUserForm" 
        label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addUserForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="addUserForm.password" 
            type="password" 
            placeholder="请输入密码"
            show-password></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addUserForm.phone" placeholder="请输入手机号（可选）"></el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="addUserForm.nickname" placeholder="请输入昵称（可选）"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="addUserForm.email" placeholder="请输入邮箱（可选）"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="addUserDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddUser" :loading="loading">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { userApi } from '@/api/admin'

export default {
  name: 'UserManage',
  data() {
    return {
      query: { keyword: '', status: '', level: '', page: 1, pageSize: 10 },
      loading: false,
      drawerVisible: false,
      loadingDetail: false,
      currentUser: {},
      userList: [],
      total: 0,
      addUserDialogVisible: false,
      addUserForm: {
        username: '',
        password: '',
        phone: '',
        nickname: '',
        email: ''
      },
      addUserFormRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
        email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }]
      }
    }
  },
  computed: {
    filteredList() {
      // 前端筛选（如果后端不支持筛选，可以在这里做）
      return this.userList.filter(user => {
        const keyword = this.query.keyword.toLowerCase();
        const matchKey = !keyword || 
                         String(user.id).includes(keyword) || 
                         (user.phone && user.phone.includes(keyword)) || 
                         (user.username && user.username.toLowerCase().includes(keyword));
        const matchStatus = !this.query.status || user.statusStr === this.query.status;
        const matchLevel = !this.query.level || String(user.level) === this.query.level;
        return matchKey && matchStatus && matchLevel;
      });
    }
  },
  mounted() {
    this.loadUserList()
  },
  methods: {
    // 加载用户列表
    async loadUserList() {
      this.loading = true
      try {
        const response = await userApi.getUserList(this.query)
        // 检查响应结构
        if (!response || !response.data) {
          console.warn('响应数据格式异常:', response)
          this.userList = []
          this.total = 0
          return
        }
        if (response.data) {
          this.userList = response.data.list || []
          this.total = response.data.total || 0
          // 转换状态：1=normal, 0=frozen, 2=muted
          this.userList.forEach(user => {
            if (user.status === 1) user.statusStr = 'normal'
            else if (user.status === 0) user.statusStr = 'frozen'
            else if (user.status === 2) user.statusStr = 'muted'
            // 格式化时间
            if (user.createTime) {
              user.regTime = this.formatDateTime(user.createTime)
            }
            // 设置默认头像
            if (!user.avatarUrl) {
              user.avatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
            } else {
              user.avatar = user.avatarUrl
            }
          })
        }
      } catch (error) {
        // 错误信息已经在request.js中显示，这里只记录日志
        console.error('加载用户列表失败:', error)
        console.error('错误详情:', {
          message: error.message,
          response: error.response?.data,
          code: error.code
        })
        // 如果是连接错误，提供更详细的提示
        if (error.code === 'ECONNREFUSED' || error.message?.includes('ERR_CONNECTION_REFUSED')) {
          this.$message({
            message: '无法连接到后端服务，请确保后端服务已启动在 http://localhost:8090',
            type: 'error',
            duration: 5000
          })
        }
        // 设置空列表，避免页面显示异常
        this.userList = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.query.page = 1
      this.loadUserList()
    },
    resetFilter() {
      this.query = { keyword: '', status: '', level: '', page: 1, pageSize: 10 }
      this.loadUserList()
    },
    async openDrawer(row) {
      this.drawerVisible = true
      this.loadingDetail = true
      this.currentUser = {}
      
      try {
        const response = await userApi.getUserById(row.id)
        if (response.data) {
          const user = response.data
          // 转换状态
          if (user.status === 1) user.statusStr = 'normal'
          else if (user.status === 0) user.statusStr = 'frozen'
          else if (user.status === 2) user.statusStr = 'muted'
          
          // 格式化数据
          user.regTime = user.createTime ? this.formatDateTime(user.createTime) : ''
          user.lastLogin = user.lastLogin ? this.formatDateTime(user.lastLogin) : ''
          user.avatar = user.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
          
          this.currentUser = user
        }
      } catch (error) {
        this.$message.error('加载用户详情失败：' + (error.message || '网络错误'))
        console.error('加载用户详情失败:', error)
        // 如果API失败，使用行数据
        this.currentUser = { ...row }
      } finally {
        this.loadingDetail = false
      }
    },
    async handleCommand(command, row) {
      if (command === 'freeze' || command === 'unfreeze') {
        const isFreeze = command === 'freeze'
        const actionText = isFreeze ? '冻结' : '解封'
        const newStatus = isFreeze ? 'frozen' : 'normal'
        const type = isFreeze ? 'error' : 'warning'
        
        this.$confirm(`确定要${actionText}用户 "${row.username}" 吗？此操作将影响用户登录。`, '高危操作警告', {
          confirmButtonText: `确定${actionText}`,
          cancelButtonText: '取消',
          type: type
        }).then(async () => {
          try {
            await userApi.updateUserStatus(row.id, newStatus)
            this.$message.success(`用户已成功${actionText}`)
            // 更新本地状态
            const target = this.userList.find(u => u.id === row.id)
            if (target) {
              target.statusStr = newStatus
              target.status = isFreeze ? 0 : 1
            }
            // 如果在抽屉里操作，同步更新 currentUser
            if (this.currentUser.id === row.id) {
              this.currentUser.statusStr = newStatus
              this.currentUser.status = isFreeze ? 0 : 1
            }
            // 重新加载列表
            this.loadUserList()
          } catch (error) {
            this.$message.error(`${actionText}用户失败：` + (error.message || '网络错误'))
            console.error(`${actionText}用户失败:`, error)
          }
        }).catch(() => {})
      } else if (command === 'resetPwd') {
        this.$confirm(`确定重置用户 "${row.username}" 的密码为默认密码 (123456) 吗？`, '提示').then(async () => {
          try {
            await userApi.resetPassword(row.id)
            this.$message.success('密码重置成功')
          } catch (error) {
            this.$message.error('密码重置失败：' + (error.message || '网络错误'))
            console.error('密码重置失败:', error)
          }
        })
      }
    },
    // 显示添加用户对话框
    showAddUserDialog() {
      this.addUserForm = {
        username: '',
        password: '',
        phone: '',
        nickname: '',
        email: ''
      }
      this.addUserDialogVisible = true
      // 清除表单验证
      this.$nextTick(() => {
        if (this.$refs.addUserForm) {
          this.$refs.addUserForm.clearValidate()
        }
      })
    },
    // 添加用户
    async handleAddUser() {
      this.$refs.addUserForm.validate(async (valid) => {
        if (valid) {
          this.loading = true
          try {
            await userApi.createUser(this.addUserForm)
            this.$message.success('用户创建成功')
            this.addUserDialogVisible = false
            // 重新加载用户列表
            this.loadUserList()
          } catch (error) {
            this.$message.error('创建用户失败：' + (error.message || '网络错误'))
            console.error('创建用户失败:', error)
          } finally {
            this.loading = false
          }
        }
      })
    },
    // 格式化日期时间
    formatDateTime(dateTime) {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }
  }
}
</script>

<style scoped>
.filter-wrapper { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.filter-left { display: flex; align-items: center; }

/* 抽屉样式 */
.drawer-content { padding: 20px; height: 100%; overflow-y: auto; padding-bottom: 60px; }
.user-header { display: flex; align-items: center; margin-bottom: 20px; }
.user-header-info { margin-left: 15px; }

/* 统计数据卡片 */
.stats-cards { display: flex; justify-content: space-between; margin-bottom: 20px; }
.stat-box { flex: 1; text-align: center; background: #f8f9fa; padding: 15px; margin: 0 5px; border-radius: 8px; }
.stat-box .num { font-size: 20px; font-weight: bold; color: #303133; }
.stat-box .label { font-size: 12px; color: #909399; margin-top: 5px; }

.section-title { font-weight: bold; margin-bottom: 15px; color: #303133; border-left: 4px solid #409EFF; padding-left: 10px; }

.drawer-footer { position: absolute; bottom: 0; left: 0; width: 100%; padding: 15px 20px; background: #fff; border-top: 1px solid #e8e8e8; text-align: right; box-sizing: border-box; }
</style>