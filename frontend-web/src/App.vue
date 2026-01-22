<template>
  <div id="app">
    <!-- 渐变背景层 -->
    <div class="gradient-background"></div>
    <!-- 粒子动效层 -->
    <div class="particles-container" ref="particlesContainer"></div>
    <!-- 角落飘浮装饰 -->
    <div class="floating-decoration" ref="floatingDecoration" @click="explodeDecoration">
      <div class="decoration-icon" :class="currentDecorationType">
        <i v-if="currentDecorationType === 'star'" class="el-icon-star-on"></i>
        <i v-else class="el-icon-price-tag"></i>
      </div>
    </div>
    <!-- 内容层 -->
    <div class="content-layer">
      <router-view/>
    </div>
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

/* 页面层级结构 */
#app {
  position: relative;
  min-height: 100vh;
  overflow-x: hidden;
}

/* 渐变背景层 - 最底层 */
.gradient-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -2;
  background: linear-gradient(135deg,
    #f0e6ff 0%,
    #e6f7ff 33.33%,
    #ffe6f0 66.66%,
    #f0e6ff 100%
  );
  background-size: 400% 400%;
  animation: gradientShift 10s ease-in-out infinite;
}

@keyframes gradientShift {
  0%, 100% {
    background-position: 0% 0%;
  }
  33.33% {
    background-position: 100% 0%;
  }
  66.66% {
    background-position: 100% 100%;
  }
}

/* 粒子动效层 - 中间层 */
.particles-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  pointer-events: none;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.8) 0%, transparent 70%);
  pointer-events: none;
  will-change: transform;
  animation: particleFloat linear infinite;
}

@keyframes particleFloat {
  from {
    transform: translate(0, 0);
  }
  to {
    transform: translate(var(--move-x), var(--move-y));
  }
}

/* 内容层 - 最上层 */
.content-layer {
  position: relative;
  z-index: 1;
  min-height: 100vh;
}

/* 角落飘浮装饰 */
.floating-decoration {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
  cursor: pointer;
  animation: floatAround 2s ease-in-out infinite;
}

@keyframes floatAround {
  0%, 100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(10px, -10px);
  }
  50% {
    transform: translate(-5px, -5px);
  }
  75% {
    transform: translate(-10px, 10px);
  }
}

.decoration-icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b6b, #ffb347);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.decoration-icon.star {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.decoration-icon:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
}

/* 爆炸碎片 */
.explosion-fragment {
  position: fixed;
  pointer-events: none;
  z-index: 999;
  animation: fragmentExplode 1s ease-out forwards;
}

@keyframes fragmentExplode {
  0% {
    opacity: 1;
    transform: scale(1) rotate(0deg);
  }
  100% {
    opacity: 0;
    transform: scale(0) rotate(360deg) translate(var(--move-x), var(--move-y));
  }
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

<script>
export default {
  name: 'App',
  data() {
    return {
      particles: [],
      particleCount: 60, // 根据屏幕尺寸自适应
      animationFrame: null,
      // 飘浮装饰相关
      currentDecorationType: 'star', // 'star' 或 'tag'
      decorationTypes: ['star', 'tag'],
      decorationTimer: null
    }
  },
  mounted() {
    this.initParticles()
    this.animateParticles()
    this.startDecorationCycle()
    // 监听窗口大小变化，重新初始化粒子
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    if (this.animationFrame) {
      cancelAnimationFrame(this.animationFrame)
    }
    if (this.decorationTimer) {
      clearInterval(this.decorationTimer)
    }
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    initParticles() {
      // 清空现有粒子
      this.particles.forEach(particle => {
        if (particle.element && particle.element.parentNode) {
          particle.element.parentNode.removeChild(particle.element)
        }
      })
      this.particles = []

      const container = this.$refs.particlesContainer
      if (!container) return

      const rect = container.getBoundingClientRect()
      const colors = [
        '#ff0000', '#ff9900', '#ffff00', '#00ff00', '#0099ff', '#9900ff' // 彩虹色系
      ]

      for (let i = 0; i < this.particleCount; i++) {
        const size = Math.random() * 3 + 2 // 2-5px
        const color = colors[Math.floor(Math.random() * colors.length)]
        const opacity = Math.random() * 0.3 + 0.6 // 0.6-0.9

        const particle = {
          id: i,
          x: Math.random() * rect.width,
          y: Math.random() * rect.height,
          vx: (Math.random() - 0.5) * 2, // -1 to 1
          vy: (Math.random() - 0.5) * 2, // -1 to 1
          size,
          color,
          opacity,
          baseOpacity: opacity,
          scale: 1,
          collisionTimer: 0
        }

        this.particles.push(particle)
        this.createParticleElement(particle)
      }
    },

    createParticleElement(particle) {
      const container = this.$refs.particlesContainer
      if (!container) return

      const element = document.createElement('div')
      element.className = 'particle'
      element.style.width = `${particle.size}px`
      element.style.height = `${particle.size}px`
      element.style.background = `radial-gradient(circle, ${this.hexToRgba(particle.color, particle.opacity)} 0%, transparent 70%)`
      element.style.left = `${particle.x}px`
      element.style.top = `${particle.y}px`
      element.style.transform = `scale(${particle.scale})`

      container.appendChild(element)
      particle.element = element
    },

    hexToRgba(hex, opacity) {
      const r = parseInt(hex.slice(1, 3), 16)
      const g = parseInt(hex.slice(3, 5), 16)
      const b = parseInt(hex.slice(5, 7), 16)
      return `rgba(${r}, ${g}, ${b}, ${opacity})`
    },

    animateParticles() {
      const container = this.$refs.particlesContainer
      if (!container) return

      const rect = container.getBoundingClientRect()

      this.particles.forEach((particle, index) => {
        // 更新位置
        particle.x += particle.vx
        particle.y += particle.vy

        // 边界反弹
        if (particle.x <= 0 || particle.x >= rect.width - particle.size) {
          particle.vx *= -1
          particle.x = Math.max(0, Math.min(particle.x, rect.width - particle.size))
        }
        if (particle.y <= 0 || particle.y >= rect.height - particle.size) {
          particle.vy *= -1
          particle.y = Math.max(0, Math.min(particle.y, rect.height - particle.size))
        }

        // 粒子间碰撞检测
        this.particles.forEach((otherParticle, otherIndex) => {
          if (index !== otherIndex) {
            const dx = particle.x - otherParticle.x
            const dy = particle.y - otherParticle.y
            const distance = Math.sqrt(dx * dx + dy * dy)

            if (distance < (particle.size + otherParticle.size) / 2) {
              // 碰撞处理
              if (particle.collisionTimer <= 0) {
                // 改变方向
                const tempVx = particle.vx
                const tempVy = particle.vy
                particle.vx = otherParticle.vx
                particle.vy = otherParticle.vy
                otherParticle.vx = tempVx
                otherParticle.vy = tempVy

                // 放大效果
                particle.scale = 1.2
                otherParticle.scale = 1.2
                particle.collisionTimer = 30 // 30帧的放大效果
                otherParticle.collisionTimer = 30
              }
            }
          }
        })

        // 碰撞效果衰减
        if (particle.collisionTimer > 0) {
          particle.collisionTimer--
          if (particle.collisionTimer <= 0) {
            particle.scale = 1
          }
        }

        // 更新DOM元素
        if (particle.element) {
          particle.element.style.left = `${particle.x}px`
          particle.element.style.top = `${particle.y}px`
          particle.element.style.transform = `scale(${particle.scale})`
        }
      })

      this.animationFrame = requestAnimationFrame(this.animateParticles)
    },

    handleResize() {
      // 窗口大小改变时重新初始化粒子
      this.initParticles()
    },

    // 开始装饰循环切换
    startDecorationCycle() {
      this.decorationTimer = setInterval(() => {
        const currentIndex = this.decorationTypes.indexOf(this.currentDecorationType)
        const nextIndex = (currentIndex + 1) % this.decorationTypes.length
        this.currentDecorationType = this.decorationTypes[nextIndex]
      }, 5000) // 每5秒切换一次装饰类型
    },

    // 爆炸装饰效果
    explodeDecoration() {
      const decoration = this.$refs.floatingDecoration
      if (!decoration) return

      const rect = decoration.getBoundingClientRect()
      const centerX = rect.left + rect.width / 2
      const centerY = rect.top + rect.height / 2

      // 隐藏原装饰
      decoration.style.display = 'none'

      // 生成爆炸碎片
      const fragmentCount = Math.floor(Math.random() * 10) + 20 // 20-30个碎片
      const colors = ['#ff6b6b', '#ffb347', '#ffff66', '#66ff66', '#66b3ff', '#b366ff']

      for (let i = 0; i < fragmentCount; i++) {
        const fragment = document.createElement('div')
        fragment.className = 'explosion-fragment'

        // 随机形状
        const shapes = ['circle', 'triangle', 'star']
        const shape = shapes[Math.floor(Math.random() * shapes.length)]

        if (shape === 'circle') {
          fragment.style.borderRadius = '50%'
          fragment.style.width = `${Math.random() * 4 + 2}px`
          fragment.style.height = fragment.style.width
        } else if (shape === 'triangle') {
          fragment.style.width = '0'
          fragment.style.height = '0'
          fragment.style.borderLeft = `${Math.random() * 4 + 2}px solid transparent`
          fragment.style.borderRight = `${Math.random() * 4 + 2}px solid transparent`
          fragment.style.borderBottom = `${Math.random() * 4 + 2}px solid ${colors[Math.floor(Math.random() * colors.length)]}`
          fragment.style.background = 'none'
        } else {
          fragment.style.width = `${Math.random() * 4 + 2}px`
          fragment.style.height = fragment.style.width
          fragment.style.background = colors[Math.floor(Math.random() * colors.length)]
          fragment.style.clipPath = 'polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%)'
        }

        if (shape !== 'triangle') {
          fragment.style.background = colors[Math.floor(Math.random() * colors.length)]
        }

        // 设置位置
        fragment.style.left = `${centerX}px`
        fragment.style.top = `${centerY}px`

        // 随机运动方向和距离
        const angle = Math.random() * Math.PI * 2
        const distance = Math.random() * 100 + 50
        const moveX = Math.cos(angle) * distance
        const moveY = Math.sin(angle) * distance

        fragment.style.setProperty('--move-x', `${moveX}px`)
        fragment.style.setProperty('--move-y', `${moveY}px`)

        // 设置动画时长
        const duration = Math.random() * 0.5 + 0.5 // 0.5-1秒
        fragment.style.animationDuration = `${duration}s`

        document.body.appendChild(fragment)

        // 动画结束后移除碎片
        setTimeout(() => {
          if (fragment.parentNode) {
            fragment.parentNode.removeChild(fragment)
          }
        }, duration * 1000)
      }

      // 3秒后重新显示装饰
      setTimeout(() => {
        decoration.style.display = 'block'
      }, 3000)
    }
  }
}
</script>