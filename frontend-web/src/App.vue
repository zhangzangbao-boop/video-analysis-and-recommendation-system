<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<style>
/* 1. 全局重置 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "微软雅黑", Arial, sans-serif;
  background-color: #f0f2f5; /* 柔和的灰背景，让白色卡片更突出 */
  -webkit-font-smoothing: antialiased; /*字体抗锯齿*/
}

/* 2. 美化滚动条 (Chrome/Edge) */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 4px;
}
::-webkit-scrollbar-track {
  background: #f1f1f1;
}

/* 3. 让所有卡片默认带圆角和柔和阴影 */
.el-card {
  border: none !important; /* 去掉生硬的边框 */
  border-radius: 8px !important; /* 更大的圆角 */
  box-shadow: 0 1px 4px rgba(0,21,41,.08) !important; /* 极简阴影 */
  transition: all 0.3s;
}
/* 悬浮时卡片轻微浮起 */
.el-card:hover {
  box-shadow: 0 4px 16px rgba(0,21,41,.12) !important;
  transform: translateY(-2px);
}
</style>