# 🎨 个人健身管理系统 - Vue3 前端

基于 Vue3 + Vite + Element Plus + Pinia 的健身管理前端项目

## 📋 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - 基于 Vue 3 的组件库
- **Pinia** - Vue 的状态管理方案
- **Vue Router** - Vue.js 的官方路由
- **Axios** - 基于 Promise 的 HTTP 客户端
- **ECharts** - 可视化图表库
- **Day.js** - 轻量级日期处理库

## 📁 项目结构

```
fitness-frontend/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API 接口
│   │   ├── user.js        # 用户相关接口
│   │   ├── food.js        # 饮食记录接口
│   │   ├── exercise.js    # 运动记录接口
│   │   ├── plan.js        # 健身计划接口
│   │   ├── statistics.js  # 数据统计接口
│   │   └── nutrition.js   # AI 营养咨询接口
│   ├── components/        # 公共组件
│   ├── router/            # 路由配置
│   │   └── index.js
│   ├── stores/            # Pinia 状态管理
│   │   └── user.js
│   ├── utils/             # 工具函数
│   │   ├── request.js     # Axios 封装
│   │   └── auth.js        # Token 管理
│   ├── views/             # 页面组件
│   │   ├── Login.vue      # 登录页
│   │   ├── Register.vue   # 注册页
│   │   ├── Layout.vue     # 主布局
│   │   ├── Dashboard.vue  # 首页仪表盘
│   │   ├── Food.vue       # 饮食记录
│   │   ├── Exercise.vue   # 运动记录
│   │   ├── Plan.vue       # 健身计划
│   │   ├── Statistics.vue # 数据统计
│   │   ├── AI.vue         # AI 营养咨询
│   │   ├── Profile.vue    # 个人中心
│   │   └── NotFound.vue   # 404页面
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── index.html
├── package.json
├── vite.config.js
└── README.md
```

## 🚀 快速开始

### 1. 安装依赖

```bash
cd fitness-frontend
npm install
```

### 2. 配置代理

编辑 `vite.config.js`：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端地址
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),
    },
  },
}
```

### 3. 启动开发服务器

```bash
npm run dev
```

访问：http://localhost:3000

### 4. 构建生产环境

```bash
npm run build
```

构建后的文件在 `dist` 目录

---

## 📱 功能模块

### ✅ 已完成

- [x] 用户登录/注册
- [x] JWT Token 认证
- [x] 首页仪表盘（今日数据概览）
- [x] 饮食记录管理
- [x] 拍照识别食物（调用后端百度 API）
- [x] 运动记录管理
- [x] 健身计划管理
- [x] AI 营养咨询（DeepSeek）
- [x] 个人中心
- [x] 数据统计（基础版）

### 🔄 待完善

- [ ] 数据可视化图表（ECharts）
- [ ] 运动打卡提醒
- [ ] 响应式移动端适配
- [ ] 主题切换

---

## 🔗 接口文档

前端调用后端接口，详见 `src/api/` 目录

### 主要接口

| 模块 | 接口路径 | 说明 |
|------|---------|------|
| 用户 | /user/login | 登录 |
| 用户 | /user/register | 注册 |
| 用户 | /user/info | 获取用户信息 |
| 饮食 | /foodRecord/list | 饮食记录列表 |
| 饮食 | /foodRecord/recognize | 拍照识别 |
| 运动 | /exercise/list | 运动记录列表 |
| 计划 | /plan/list | 健身计划列表 |
| AI | /nutrition/upload | 上传图片 |
| AI | /nutrition/ask | AI 咨询 |

---

## 🎨 UI 组件

使用 Element Plus 组件库，主要组件：

- **布局**：Container、Header、Aside、Main
- **导航**：Menu、Breadcrumb、Dropdown
- **数据展示**：Table、Card、Tag、Progress
- **表单**：Form、Input、Select、DatePicker
- **反馈**：Message、MessageBox、Loading
- **其他**：Dialog、Button、Icon

---

## 📝 开发规范

### 组件命名
- 页面组件：大驼峰（如 `Dashboard.vue`）
- 公共组件：大驼峰（如 `UserCard.vue`）

### API 封装
- 所有接口统一放在 `src/api/` 目录
- 使用 `request.js` 统一处理请求和响应

### 状态管理
- 使用 Pinia 管理全局状态
- 用户状态存储在 `stores/user.js`

---

## 🔧 常见问题

### Q: 端口被占用
**A:** 修改 `vite.config.js` 中的 `port`

### Q: 跨域问题
**A:** 确保后端 CORS 配置正确，或检查代理配置

### Q: 依赖安装失败
**A:** 使用淘宝镜像
```bash
npm config set registry https://registry.npmmirror.com
```

---

## 📞 联系方式

如有问题，请提交 Issue

---

**Enjoy coding!** 💪
