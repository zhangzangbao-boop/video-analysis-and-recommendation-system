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
          <el-button type="success" size="small" icon="el-icon-plus">添加测试用户</el-button>
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
                <div style="font-weight: bold; color: #409EFF;">{{ scope.row.username }}</div>
                <div style="font-size: 12px; color: #909399;">{{ scope.row.phone }}</div>
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

        <el-table-column prop="regTime" label="注册时间" width="160" sortable align="center" show-overflow-tooltip></el-table-column>
        
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 'normal'" type="success" size="small" effect="dark">正常</el-tag>
            <el-tag v-else-if="scope.row.status === 'frozen'" type="danger" size="small" effect="dark">已冻结</el-tag>
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
                <el-dropdown-item command="freeze" style="color: #F56C6C;" v-if="scope.row.status !== 'frozen'">冻结账号</el-dropdown-item>
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
            <el-button type="danger" v-if="currentUser.status !== 'frozen'" icon="el-icon-lock" @click="handleCommand('freeze', currentUser)">冻结账号</el-button>
            <el-button type="success" v-else icon="el-icon-unlock" @click="handleCommand('unfreeze', currentUser)">解封账号</el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
export default {
  name: 'UserManage',
  data() {
    return {
      query: { keyword: '', status: '', level: '' },
      loading: false,
      drawerVisible: false,
      loadingDetail: false,
      currentUser: {},
      // 模拟更丰富的数据
      userList: [
        { id: 10001, username: '极客阿辉', realName: '张辉', gender: 'male', phone: '13800138000', email: 'hui@geek.com', avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', level: 3, balance: 1250.00, points: 5600, status: 'normal', regTime: '2025-05-12', fansCount: 12030, videoCount: 45, likeCount: 89000 },
        { id: 10002, username: '美妆小皇后', realName: '李莉', gender: 'female', phone: '13912345678', email: 'lili@beauty.com', avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', level: 2, balance: 88.50, points: 1200, status: 'normal', regTime: '2025-08-20', fansCount: 5600, videoCount: 12, likeCount: 23000 },
        { id: 10003, username: '暴躁老哥', realName: '王五', gender: 'male', phone: '15011112222', email: '', avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png', level: 1, balance: 0.00, points: 50, status: 'frozen', regTime: '2026-01-01', fansCount: 2, videoCount: 0, likeCount: 5 },
        { id: 10004, username: '匿名用户_9527', realName: '', gender: 'male', phone: '18899998888', email: 'test@qq.com', avatar: 'https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg', level: 1, balance: 12.00, points: 300, status: 'muted', regTime: '2026-01-10', fansCount: 10, videoCount: 2, likeCount: 120 }
      ]
    }
  },
  computed: {
    filteredList() {
      return this.userList.filter(user => {
        // 1. 关键词 (ID/手机/昵称)
        const keyword = this.query.keyword.toLowerCase();
        const matchKey = !keyword || 
                         String(user.id).includes(keyword) || 
                         user.phone.includes(keyword) || 
                         user.username.toLowerCase().includes(keyword);
        // 2. 状态
        const matchStatus = !this.query.status || user.status === this.query.status;
        // 3. 等级
        const matchLevel = !this.query.level || String(user.level) === this.query.level;

        return matchKey && matchStatus && matchLevel;
      });
    }
  },
  methods: {
    handleSearch() {
      this.loading = true;
      setTimeout(() => { this.loading = false; }, 300);
    },
    resetFilter() {
      this.query = { keyword: '', status: '', level: '' };
      this.handleSearch();
    },
    openDrawer(row) {
      this.drawerVisible = true;
      this.loadingDetail = true;
      this.currentUser = {}; // 先清空，防止闪烁旧数据
      
      // 模拟异步获取详情
      setTimeout(() => {
        this.currentUser = row;
        this.loadingDetail = false;
      }, 500);
    },
    handleCommand(command, row) {
      if (command === 'freeze' || command === 'unfreeze') {
        const isFreeze = command === 'freeze';
        const actionText = isFreeze ? '冻结' : '解封';
        const type = isFreeze ? 'error' : 'warning';
        
        this.$confirm(`确定要${actionText}用户 "${row.username}" 吗？此操作将影响用户登录。`, '高危操作警告', {
          confirmButtonText: `确定${actionText}`,
          cancelButtonText: '取消',
          type: type
        }).then(() => {
          // 更新本地状态
          const target = this.userList.find(u => u.id === row.id);
          if (target) {
            target.status = isFreeze ? 'frozen' : 'normal';
          }
          this.$message.success(`用户已成功${actionText}`);
          // 如果在抽屉里操作，同步更新 currentUser
          if (this.currentUser.id === row.id) {
            this.currentUser.status = target.status;
          }
        }).catch(() => {});
      } else if (command === 'resetPwd') {
        this.$confirm(`确定重置用户 "${row.username}" 的密码为默认密码 (123456) 吗？`, '提示').then(() => {
          this.$message.success('密码重置成功');
        });
      }
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