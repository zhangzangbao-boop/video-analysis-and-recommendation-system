<template>
  <div class="admin-container">
    <div class="dashboard-control">
      <span class="dashboard-title">数据决策中心</span>
      <div class="control-right">
        <el-radio-group v-model="dateRange" size="small" style="margin-right: 15px;" @input="handleDateChange">
          <el-radio-button label="week">近7天</el-radio-button>
          <el-radio-button label="month">近30天</el-radio-button>
          <el-radio-button label="year">全年</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="customDate"
          type="daterange"
          size="small"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 240px;">
        </el-date-picker>
        <el-button type="primary" size="small" icon="el-icon-refresh" style="margin-left: 10px;" @click="refreshData">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="20" class="panel-group">
      <el-col :xs="12" :sm="12" :lg="6" v-for="(item, index) in cardData" :key="index">
        <div class="card-panel">
          <div class="card-panel-icon-wrapper" :class="item.colorClass">
            <i :class="item.icon" class="card-panel-icon"></i>
          </div>
          <div class="card-panel-description">
            <div class="card-panel-text">{{ item.label }}</div>
            <count-to :key="item.value" :start-val="0" :end-val="item.value" :duration="2000" class="card-panel-num"/>
            <div class="card-panel-trend">
              同比 <span :class="item.trend > 0 ? 'text-up' : 'text-down'">
                <i :class="item.trend > 0 ? 'el-icon-caret-top' : 'el-icon-caret-bottom'"></i> {{ Math.abs(item.trend) }}%
              </span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :xs="24" :sm="24" :lg="16">
        <div class="chart-wrapper">
          <div class="chart-header">
            <span class="chart-title">流量与用户增长趋势 ({{ dateRangeLabel }})</span>
            <el-tooltip content="柱状图为新增用户，折线图为视频播放量" placement="top">
              <i class="el-icon-info"></i>
            </el-tooltip>
          </div>
          <div id="mix-chart" style="width: 100%; height: 380px;"></div>
        </div>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <div class="chart-header">
            <span class="chart-title">内容转化漏斗</span>
          </div>
          <div id="funnel-chart" style="width: 100%; height: 380px;"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :lg="8">
        <div class="chart-wrapper">
          <div class="chart-header"><span class="chart-title">用户兴趣画像</span></div>
          <div id="radar-chart" style="width: 100%; height: 320px;"></div>
        </div>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="8">
        <div class="chart-wrapper">
          <div class="chart-header"><span class="chart-title">热门标签排行 ({{ dateRangeLabel }})</span></div>
          <div id="bar-chart" style="width: 100%; height: 320px;"></div>
        </div>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="8">
        <el-card shadow="never" class="creator-card" :body-style="{ padding: '0px' }">
          <div slot="header" class="clearfix">
            <span style="font-weight: bold;">🏆 本周优秀创作者</span>
            <el-button style="float: right; padding: 3px 0" type="text">查看全部</el-button>
          </div>
          <el-table :data="creatorList" :show-header="false" style="width: 100%">
            <el-table-column width="50" align="center">
              <template slot-scope="scope">
                <span class="rank-badge" :class="'rank-' + (scope.$index + 1)">{{ scope.$index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column show-overflow-tooltip>
              <template slot-scope="scope">
                <div style="display: flex; align-items: center;">
                  <el-avatar size="small" :src="scope.row.avatar"></el-avatar>
                  <span style="margin-left: 10px; font-weight: 500;">{{ scope.row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="score" width="80" align="right">
              <template slot-scope="scope">
                <span style="color: #f56c6c; font-weight: bold;">{{ scope.row.score }}</span> <span style="font-size: 12px; color: #999;">指数</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import CountTo from 'vue-count-to';

// --- 模拟数据源 (Mock Data) ---
const MOCK_DATA = {
  week: {
    cards: [892300, 45200, 320, 125800],
    trends: [12.5, 5.2, -2.1, 8.4],
    mixChart: {
      xAxis: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      user: [20, 49, 70, 23, 25, 76, 135],
      view: [2000, 4900, 7000, 2300, 2500, 7600, 13500]
    },
    barChart: {
      yAxis: ['Vlog', '猫咪', 'Python', 'Vue3', '健身', '旅行', '探店', '测评', 'LOL', '职场'],
      data: [320, 302, 301, 334, 390, 450, 420, 480, 500, 550]
    }
  },
  month: {
    cards: [3580000, 128000, 1050, 480000],
    trends: [8.5, 3.1, 12.0, 5.5],
    mixChart: {
      xAxis: ['1日', '5日', '10日', '15日', '20日', '25日', '30日'],
      user: [150, 230, 224, 218, 135, 147, 260],
      view: [15000, 23000, 22400, 21800, 13500, 14700, 26000]
    },
    barChart: {
      yAxis: ['美妆', '穿搭', '数码', '情感', '剧情', '科普', '新闻', '音乐', '舞蹈', '生活'],
      data: [1200, 1300, 1400, 1500, 1600, 1800, 2100, 2400, 2800, 3000]
    }
  },
  year: {
    cards: [45000000, 890000, 12000, 5600000],
    trends: [25.5, 18.2, 30.5, 40.1],
    mixChart: {
      xAxis: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
      user: [2000, 3500, 4000, 3800, 5000, 6000, 7500, 8000, 7000, 6500, 9000, 10000],
      view: [200000, 350000, 400000, 380000, 500000, 600000, 750000, 800000, 700000, 650000, 900000, 1000000]
    },
    barChart: {
      yAxis: ['年度大赏', '春节', '世界杯', '双11', '毕业季', '开学', '暑假', '寒假', '国庆', '五一'],
      data: [50000, 52000, 55000, 60000, 65000, 70000, 80000, 85000, 90000, 100000]
    }
  }
};

export default {
  name: 'AdminStats',
  components: { CountTo },
  data() {
    return {
      dateRange: 'week', // 默认选中近7天
      customDate: '',
      mixChart: null,
      funnelChart: null,
      radarChart: null,
      barChart: null,
      // 核心数据 (默认值)
      cardData: [
        { label: '总播放量 (PV)', value: 892300, trend: 12.5, icon: 'el-icon-video-play', colorClass: 'icon-blue' },
        { label: '日活跃用户 (DAU)', value: 45200, trend: 5.2, icon: 'el-icon-user-solid', colorClass: 'icon-green' },
        { label: '新增创作者', value: 320, trend: -2.1, icon: 'el-icon-camera', colorClass: 'icon-purple' },
        { label: '广告总收入 (元)', value: 125800, trend: 8.4, icon: 'el-icon-coin', colorClass: 'icon-red' }
      ],
      creatorList: [
        { name: '极客阿辉', avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', score: 98.2 },
        { name: '美妆小皇后', avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', score: 95.6 },
        { name: '旅行日记', avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png', score: 92.1 },
        { name: '萌宠集中营', avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', score: 89.5 },
        { name: 'Java教学', avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', score: 88.3 }
      ]
    }
  },
  computed: {
    dateRangeLabel() {
      const map = { week: '近7天', month: '近30天', year: '全年' };
      return map[this.dateRange];
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initCharts();
      window.addEventListener('resize', this.resizeCharts);
      // 初始化加载默认数据
      this.handleDateChange('week');
    });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    this.disposeCharts();
  },
  methods: {
    // --- 核心：切换时间范围的逻辑 ---
    handleDateChange(val) {
      this.$message.info(`正在加载${this.dateRangeLabel}数据...`);
      
      const data = MOCK_DATA[val];
      if (!data) return;

      // 1. 更新顶部卡片数据
      this.cardData.forEach((item, index) => {
        item.value = data.cards[index];
        item.trend = data.trends[index];
      });

      // 2. 更新混合图表 (Mix Chart)
      if (this.mixChart) {
        this.mixChart.setOption({
          xAxis: { data: data.mixChart.xAxis },
          series: [
            { data: data.mixChart.user }, // 新增用户
            { data: data.mixChart.view }  // 播放量
          ]
        });
      }

      // 3. 更新条形图 (Bar Chart)
      if (this.barChart) {
        this.barChart.setOption({
          yAxis: { data: data.barChart.yAxis },
          series: [{ data: data.barChart.data }]
        });
      }
    },
    
    refreshData() {
      this.$message.success('数据已刷新');
      this.handleDateChange(this.dateRange); // 重新加载当前选中时间段的数据
    },
    resizeCharts() {
      this.mixChart && this.mixChart.resize();
      this.funnelChart && this.funnelChart.resize();
      this.radarChart && this.radarChart.resize();
      this.barChart && this.barChart.resize();
    },
    disposeCharts() {
      this.mixChart && this.mixChart.dispose();
      this.funnelChart && this.funnelChart.dispose();
      this.radarChart && this.radarChart.dispose();
      this.barChart && this.barChart.dispose();
    },
    initCharts() {
      // 1. 混合图：流量 vs 用户
      this.mixChart = echarts.init(document.getElementById('mix-chart'));
      this.mixChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
        legend: { data: ['播放量', '新增用户'], bottom: 0 },
        grid: { left: '3%', right: '4%', bottom: '10%', top: '15%', containLabel: true },
        xAxis: [{ 
          type: 'category', 
          data: [], // 数据由 handleDateChange 填充
          axisPointer: { type: 'shadow' }
        }],
        yAxis: [
          { type: 'value', name: '播放量', min: 0, axisLabel: { formatter: '{value}' } },
          { type: 'value', name: '新增用户', min: 0, axisLabel: { formatter: '{value} 人' } }
        ],
        series: [
          {
            name: '新增用户', type: 'bar', yAxisIndex: 1,
            itemStyle: { color: '#34bfa3', borderRadius: [4, 4, 0, 0] },
            barWidth: '30%',
            data: []
          },
          {
            name: '播放量', type: 'line', smooth: true,
            itemStyle: { color: '#409EFF' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{offset: 0, color: 'rgba(64,158,255,0.3)'}, {offset: 1, color: 'rgba(64,158,255,0)'}])
            },
            data: []
          }
        ]
      });

      // 2. 漏斗图 (静态，不随时间变化太剧烈，暂时保持静态)
      this.funnelChart = echarts.init(document.getElementById('funnel-chart'));
      this.funnelChart.setOption({
        tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c}%' },
        series: [
          {
            name: '转化漏斗',
            type: 'funnel',
            left: '10%', top: 20, bottom: 20, width: '80%',
            min: 0, max: 100,
            minSize: '0%', maxSize: '100%',
            sort: 'descending',
            gap: 2,
            label: { show: true, position: 'inside' },
            itemStyle: { borderColor: '#fff', borderWidth: 1 },
            data: [
              { value: 100, name: '曝光展示', itemStyle: { color: '#409EFF' } },
              { value: 60, name: '点击播放', itemStyle: { color: '#67C23A' } },
              { value: 30, name: '有效完播', itemStyle: { color: '#E6A23C' } },
              { value: 10, name: '互动(赞/评)', itemStyle: { color: '#F56C6C' } },
              { value: 2, name: '分享转发', itemStyle: { color: '#909399' } }
            ]
          }
        ]
      });

      // 3. 雷达图 (静态)
      this.radarChart = echarts.init(document.getElementById('radar-chart'));
      this.radarChart.setOption({
        tooltip: {},
        radar: {
          indicator: [
            { name: '搞笑', max: 100 },
            { name: '科技', max: 100 },
            { name: '生活', max: 100 },
            { name: '美食', max: 100 },
            { name: '游戏', max: 100 },
            { name: '萌宠', max: 100 }
          ],
          center: ['50%', '50%'], radius: '65%'
        },
        series: [{
          name: '用户兴趣分布',
          type: 'radar',
          areaStyle: { opacity: 0.2 },
          data: [
            { value: [90, 50, 80, 70, 60, 85], name: '男性用户', itemStyle: { color: '#409EFF' } },
            { value: [70, 30, 95, 90, 40, 95], name: '女性用户', itemStyle: { color: '#F56C6C' } }
          ]
        }]
      });

      // 4. 横向柱状图 (随时间变化)
      this.barChart = echarts.init(document.getElementById('bar-chart'));
      this.barChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'value', show: false },
        yAxis: { 
          type: 'category', 
          data: [], // 动态填充
          axisTick: { show: false },
          axisLine: { show: false }
        },
        series: [
          {
            name: '热度', type: 'bar',
            data: [], // 动态填充
            itemStyle: {
              color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [{offset: 0, color: '#83bff6'}, {offset: 0.5, color: '#188df0'}, {offset: 1, color: '#188df0'}])
            },
            label: { show: true, position: 'right' }
          }
        ]
      });
    }
  }
}
</script>

<style scoped>
.admin-container { padding: 20px; background-color: #f0f2f5; min-height: 100vh; }
.dashboard-control { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; background: #fff; padding: 15px 20px; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
.dashboard-title { font-size: 20px; font-weight: bold; color: #303133; }
.control-right { display: flex; align-items: center; }

/* 卡片样式 */
.panel-group { margin-bottom: 10px; }
.card-panel { height: 108px; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05); display: flex; align-items: center; padding: 0 20px; transition: all 0.3s; margin-bottom: 20px; }
.card-panel:hover { transform: translateY(-5px); box-shadow: 0 8px 16px rgba(0,0,0,0.1); }
.card-panel-icon-wrapper { width: 60px; height: 60px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-right: 20px; font-size: 32px; }
.icon-blue { color: #36a3f7; background: #e6f7ff; }
.icon-green { color: #34bfa3; background: #e1f3d8; }
.icon-purple { color: #8e44ad; background: #f4ecf7; }
.icon-red { color: #f56c6c; background: #fde2e2; }
.card-panel-description { flex: 1; }
.card-panel-text { color: rgba(0,0,0,0.45); font-size: 14px; margin-bottom: 8px; }
.card-panel-num { font-size: 24px; font-weight: bold; color: #303133; }
.card-panel-trend { font-size: 12px; margin-top: 5px; color: #909399; }
.text-up { color: #f56c6c; font-weight: bold; }
.text-down { color: #67c23a; font-weight: bold; }

/* 图表容器 */
.chart-wrapper { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05); margin-bottom: 20px; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 1px solid #f0f0f0; padding-bottom: 10px; }
.chart-title { font-size: 16px; font-weight: bold; color: #303133; border-left: 4px solid #409EFF; padding-left: 10px; }

/* 榜单样式 */
.creator-card { height: 400px; overflow-y: auto; }
.rank-badge { display: inline-block; width: 20px; height: 20px; line-height: 20px; text-align: center; border-radius: 50%; background: #f0f2f5; color: #606266; font-weight: bold; font-size: 12px; }
.rank-1 { background: #f56c6c; color: #fff; }
.rank-2 { background: #E6A23C; color: #fff; }
.rank-3 { background: #409EFF; color: #fff; }
</style>