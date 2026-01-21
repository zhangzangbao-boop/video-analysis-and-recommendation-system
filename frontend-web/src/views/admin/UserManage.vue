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
          :data="userList"
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
        <el-pagination
            background
            layout="total, prev, pager, next"
            :total="total"
            :current-page.sync="query.page"
            :page-size="query.pageSize"
            @current-change="handleSearch"
        ></el-pagination>
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
            <el-timeline-item
                v-for="(activity, index) in activities"
                :key="index"
                :timestamp="activity.timestamp"
                placement="top"
                :color="activity.color">
              {{ activity.content }}
            </el-timeline-item>

            <el-timeline-item v-if="activities.length === 0" timestamp="暂无数据" color="#909399">
              近期无活跃记录
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
import request from '@/utils/request' // 引入 request 以便直接调用动态接口

export default {
  name: 'UserManage',
  data() {
    return {
      query: { keyword: '', status: '', level: '', page: 1, pageSize: 10 },
      loading: false,
      drawerVisible: false,
      loadingDetail: false,
      currentUser: {},
      activities: [], // 新增：用户动态列表
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
  mounted() {
    this.loadUserList()
  },
  methods: {
    // 加载用户列表
    async loadUserList() {
      this.loading = true
      try {
        const response = await userApi.getUserList(this.query)
        if (!response || !response.data) {
          this.userList = []
          this.total = 0
          return
        }
        if (response.data) {
          this.userList = response.data.list || []

          // 【修改这里】强制转换为数字，解决 Vue warn: Expected Number with value 24, got String
          this.total = Number(response.data.total) || 0

          this.userList.forEach(user => {
            if (user.status === 1) user.statusStr = 'normal'
            else if (user.status === 0) user.statusStr = 'frozen'
            else if (user.status === 2) user.statusStr = 'muted'

            if (user.createTime) {
              user.regTime = this.formatDateTime(user.createTime)
            }
            if (!user.avatarUrl) {
              user.avatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
            } else {
              user.avatar = user.avatarUrl
            }
          })
        }
      } catch (error) {
        console.error('加载用户列表失败:', error)
        this.userList = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    // 处理搜索和翻页
    handleSearch(val) {
      // 如果 val 是数字，说明是翻页组件触发的
      // 如果 val 不是数字，说明是点击查询按钮触发的，重置为第1页
      if (typeof val !== 'number') {
        this.query.page = 1
      }
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
      this.activities = []

      try {
        // 1. 获取用户详情 (这里走的是 admin.js 里的接口，默认就是 /api/admin/user)
        const response = await userApi.getUserById(row.id)
        if (response.data) {
          const user = response.data
          // ... (处理 user 数据, statusStr 等) ...

          if (user.status === 1) user.statusStr = 'normal'
          else if (user.status === 0) user.statusStr = 'frozen'
          else if (user.status === 2) user.statusStr = 'muted'

          user.regTime = user.createTime ? this.formatDateTime(user.createTime) : ''
          user.lastLogin = user.lastLogin ? this.formatDateTime(user.lastLogin) : ''
          user.avatar = user.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

          this.currentUser = user
        }

        // 2. 获取用户动态
        try {
          // 【修改这里】去掉 /v1，确保路径正确
          const actRes = await request({
            url: `/api/admin/user/${row.id}/activities`,
            method: 'get'
          })
          if (actRes.code === 200) {
            this.activities = actRes.data
          }
        } catch (e) {
          console.warn('获取动态失败', e)
        }

      } catch (error) {
        this.$message.error('加载详情失败')
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

        this.$confirm(`确定要${actionText}用户 "${row.username}" 吗？`, '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: type
        }).then(async () => {
          try {
            await userApi.updateUserStatus(row.id, newStatus)
            this.$message.success(`用户已${actionText}`)
            // 更新当前列表项
            const target = this.userList.find(u => u.id === row.id)
            if (target) {
              target.statusStr = newStatus
              target.status = isFreeze ? 0 : 1
            }
            if (this.currentUser.id === row.id) {
              this.currentUser.statusStr = newStatus
              this.currentUser.status = isFreeze ? 0 : 1
            }
          } catch (error) {
            this.$message.error('操作失败')
          }
        }).catch(() => {})
      } else if (command === 'resetPwd') {
        this.$confirm(`确定重置密码为 123456 吗？`, '提示').then(async () => {
          try {
            await userApi.resetPassword(row.id)
            this.$message.success('密码重置成功')
          } catch (error) {
            this.$message.error('操作失败')
          }
        })
      }
    },
    showAddUserDialog() {
      this.addUserForm = { username: '', password: '', phone: '', nickname: '', email: '' }
      this.addUserDialogVisible = true
      this.$nextTick(() => { if (this.$refs.addUserForm) this.$refs.addUserForm.clearValidate() })
    },
    async handleAddUser() {
      this.$refs.addUserForm.validate(async (valid) => {
        if (valid) {
          this.loading = true
          try {
            await userApi.createUser(this.addUserForm)
            this.$message.success('用户创建成功')
            this.addUserDialogVisible = false
            this.loadUserList()
          } catch (error) {
            this.$message.error('创建失败：' + (error.message || '网络错误'))
          } finally {
            this.loading = false
          }
        }
      })
    },
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