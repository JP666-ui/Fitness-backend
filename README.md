# 🏋️ 个人健身管理系统

基于 **SpringBoot 3.2 + Vue3 + DeepSeek AI + 百度 AI** 的个人健身管理系统。

支持饮食记录与拍照识别热量、运动记录、每日体重记录、健身计划、AI 营养咨询、数据统计等功能。

---

## 🚀 快速启动

### 方式一：一键启动（推荐）

双击项目根目录下的 **`启动系统.bat`**，脚本会自动完成：

1. 检查 Java / Maven / Node.js 环境
2. 可选：初始化数据库（首次运行请选 Y）
3. 前端依赖缺失时自动 `npm install`
4. 启动后端（独立窗口运行在 8080）
5. 等待后端就绪后启动前端（独立窗口运行在 3000）
6. 自动打开浏览器访问 http://localhost:3000

### 方式二：手动启动

**1) 初始化数据库**（首次运行）

用 Navicat 执行根目录的 `fitness_db_complete.sql`，或命令行执行：

```bash
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS fitness_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p123456 --default-character-set=utf8mb4 fitness_db < fitness_db_complete.sql
```

**2) 启动后端**

```bash
mvnw.cmd spring-boot:run
```

后端地址：http://localhost:8080

**3) 启动前端**

```bash
cd fitness-frontend
npm install     # 首次运行
npm run dev
```

前端地址：http://localhost:3000

---

## 🔑 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 普通用户 | `test` | `123456` |
| 管理员 | `admin` | `123456` |

> 管理员登录后可进入「用户管理」页面管理其他用户。

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| SpringBoot | 3.2.0 | 主框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0 | 数据库（utf8mb4） |
| JJWT | 0.12.3 | JWT 认证 |
| EasyExcel | 3.3.2 | Excel 导出 |
| 百度 AI | - | 菜品识别 / OCR |
| 智谱 GLM | glm-4-flash | AI 营养咨询（免费） |

### 前端

| 技术 | 说明 |
|------|------|
| Vue 3 | 组合式 API |
| Vite | 构建工具（端口 3000） |
| Element Plus | UI 组件库 |
| Pinia | 状态管理 |
| Axios | HTTP 请求 |
| ECharts | 数据可视化 |

---

## 📁 项目结构

```
Fitness-backend/
├── 启动系统.bat                  # 一键启动脚本
├── fitness_db_complete.sql       # 完整数据库脚本（建表 + 初始数据）
├── init_db.ps1                   # 数据库初始化 PowerShell 脚本
├── pom.xml                       # Maven 配置
│
├── src/main/java/com/fitness/
│   ├── common/                   # 通用返回结果 Result
│   ├── config/                   # CORS、JWT 拦截器、WebMvc 配置
│   ├── controller/               # 控制器层
│   │   └── admin/                # 管理员接口
│   ├── entity/                   # 实体类
│   │   └── export/               # Excel 导出实体
│   ├── mapper/                   # MyBatis-Plus Mapper
│   ├── service/                  # 业务接口 + impl 实现
│   └── util/                     # JWT、百度识别、Excel 导出工具
│
├── src/main/resources/
│   └── application.yml           # 数据库 / API Key / JWT 配置
│
└── fitness-frontend/             # Vue3 前端
    ├── vite.config.js            # 端口 3000，代理 /api → 8080
    └── src/
        ├── api/                  # 接口封装
        ├── router/               # 路由配置
        ├── stores/               # Pinia 状态
        ├── utils/                # axios 封装
        └── views/                # 页面组件
```

---

## ✨ 功能模块

### 👤 用户模块
- 注册 / 登录（JWT 认证）
- 个人信息查看与修改
- BMI 自动计算与健康评估

### 🍽️ 饮食记录
- 手动添加（**151 种常见食物**，7 大分类下拉选择，自动带出热量）
- 拍照识别（百度菜品识别 API）
- 按食物重量自动计算热量
- 当日总热量统计
- 仅保留最近一个月的记录
- 导出 CSV 记录

**食物库分类**：主食（含包子、油条、煎饼果子等）· 肉类 · 炒菜（西红柿炒鸡蛋、鱼香肉丝、宫保鸡丁等）· 蔬菜 · 水果 · 饮品 · 零食

### 🏃 运动记录
- 添加运动（跑步 / 骑行 / 游泳 / 健身 / 瑜伽等）
- 根据体重和时长自动计算消耗热量
- 运动记录列表管理

### ⚖️ 每日体重记录
- **每天记录一次**（`user_id + record_date` 唯一，重复记录自动覆盖）
- 自动计算并保存记录时的 BMI
- 记录体重后**自动同步**到用户当前体重，联动影响热量缺口
- 趋势折线图（含目标体重参考线、最高/最低点标注）
- 变化概览：当前体重、区间变化、距目标、记录天数
- 目标达成进度条 + 随进度变化的鼓励话语
- 快捷微调（±0.1 / ±0.5）与「用上次体重」

### 📋 健身计划

**设置目标体重后自动计算每日缺口**

在「个人中心」设置目标体重并保存后，系统会立即：
1. 按 Mifflin-St Jeor 公式算出 BMR 与 TDEE
2. 由「当前体重 − 目标体重」推算需减总热量
3. 除以健康周期得出每日热量缺口
4. 自动创建或更新计划（**不会重复创建**）

实测：仅修改目标体重为 75kg，系统自动返回

```
需减 9.2kg × 7700 = 70840 kcal
70840 ÷ 129 天 = 549 kcal/天
建议每日摄入 = 2586 − 549 = 2037 kcal
```

**由计划体重反推每日热量缺口**

核心公式：

```
需减体重(kg)     = 当前体重 − 目标体重
需减总热量(kcal) = 需减体重 × 7700        （1kg 脂肪 ≈ 7700 kcal）
每日热量缺口     = 需减总热量 ÷ 计划天数
建议每日摄入     = 每日总消耗(TDEE) − 每日热量缺口
```

实测示例（85kg → 75kg）：

| 步骤 | 公式 | 结果 |
|---|---|---|
| ① 需减体重 | 85.0 − 75.0 | **10.0 kg** |
| ② 需减总热量 | 10.0 × 7700 | **77000 kcal** |
| ③ 每日热量缺口 | 77000 ÷ 140 天 | **550 kcal/天** |
| ④ 建议每日摄入 | 2597 − 550 | **2047 kcal/天** |

页面上以卡片形式逐步展示上述推导，用户能看清每个数字怎么来的。

**三种节奏对比**（可直接选择）

| 节奏 | 每周减重 | 每日缺口 | 建议摄入 | 预计耗时 |
|---|---|---|---|---|
| 温和 | 0.25 kg | 275 kcal | 2322 kcal | 280 天 |
| 标准（推荐） | 0.5 kg | 550 kcal | 2047 kcal | 140 天 |
| 快速 | 0.64 kg | 708 kcal | 1889 kcal | 109 天 |

> 「快速」方案实际只能减 0.64kg/周 —— 因为摄入会触及基础代谢下限，
> 系统自动做保护并按**真实缺口**重新计算速度与天数，不虚标。

**反向计算**

调整目标时输入期望缺口，实时预览：需多少天、建议摄入多少、是否触及保护线。

**不用每次重新创建**
- 进入页面自动加载当前计划，已有计划直接沿用（幂等接口）
- 仅在确无计划时才自动生成

### 📊 数据统计

**最小统计单位为「日」**
- 每日摄入、每日总消耗、每日运动消耗、每日热量缺口
- 支持近 7 / 14 / 30 / 90 天切换

**热量缺口算法（科学版）**
```
每日总消耗 = 基础代谢 × 1.2（日常活动） + 运动消耗
每日热量缺口 = 每日总消耗 − 每日摄入
```

**可视化**
- 每日热量趋势图（摄入 / 总消耗 / 目标线）
- 每日热量缺口柱状图
- **坚持进度条**：按「达标天数 / 统计天数」计算，配随进度变化的鼓励话语
  （准备出发 → 迈出第一步 → 渐入佳境 → 已经过半 → 胜利在望 → 最后冲刺 → 圆满完成）
- 每日明细表格，标注达标 / 超标

### 🤖 AI 营养咨询

与 AI 营养师自由对话，**不需要上传任何图片**。

**对话能力**
- 直接提问饮食、营养、健身问题，支持多轮上下文
- AI 自动读取身高体重、健身目标、今日摄入与消耗，给出个性化建议
- 快捷提问、今日热量概览、营养小知识

**密钥策略（客户可自选）**
- **不填密钥** → 自动使用系统内置的免费模型（智谱 GLM-4-Flash），开箱即用
- **填自己的密钥** → 优先使用客户自己的额度，密钥 AES 加密存储，页面仅回显掩码
- 提供「测试连接」按钮，实时验证密钥是否可用
- 可随时「清除我的密钥」恢复使用系统免费模型

> 申请免费 Key：https://open.bigmodel.cn （注册后在「API Keys」页面创建）
>
> AI 服务不可用时（未配置 Key 或调用失败），对话自动降级为内置知识库回复，
> 并在页面顶部提示具体原因，功能不中断。

### 🔧 管理员功能
- 用户列表查询
- 用户数据统计
- 启用 / 禁用用户
- 删除用户

---

## ⚙️ 配置说明

项目采用**公开配置 + 私密配置分离**的方式，避免 API Key 泄露到版本库。

### 配置文件结构

| 文件 | 是否提交 | 用途 |
|------|---------|------|
| `application.yml` | ✅ 提交 | 公开配置，密钥位置用占位符 |
| `application-secret.yml` | ❌ 忽略 | 存放你的真实 API Key |
| `application-secret.yml.example` | ✅ 提交 | 配置模板，供参考 |

### 配置步骤

**1. 复制模板文件**

```bash
cd src/main/resources
cp application-secret.yml.example application-secret.yml
```

**2. 填入你的密钥**

```yaml
# application-secret.yml
ai:
  glm:
    api-key: 你的智谱API_KEY      # https://open.bigmodel.cn

baidu:
  ocr:
    access-key: 你的百度API_KEY   # https://console.bce.baidu.com/ai/
    secret-key: 你的百度SECRET_KEY
```

**3. 配置数据库**（可选）

默认连接 `root / 123456`。如需修改，用环境变量或写入 secret 文件：

```yaml
spring:
  datasource:
    password: 你的MySQL密码
```

### 也支持环境变量

不想用配置文件时，设置环境变量即可：

```bash
AI_GLM_API_KEY=你的密钥
BAIDU_OCR_ACCESS_KEY=你的密钥
BAIDU_OCR_SECRET_KEY=你的密钥
DB_USERNAME=root
DB_PASSWORD=你的密码
JWT_SECRET=你的JWT密钥
```

### 关于 `ai.glm.api-key` 的两种部署方式

| 场景 | 配置 | 效果 |
|------|------|------|
| 你提供免费额度 | 填入你的 Key | 所有用户开箱即用，无需配置 |
| 用户自备额度 | 留空 | 用户在「密钥设置」中填自己的 Key |

> 无论哪种方式，用户都可以在页面上填自己的 Key 覆盖系统配置。
> 百度 OCR 的 Key 未配置时，拍照识别会返回提示，不影响其他功能。

> ⚠️ **安全提示**：`application-secret.yml` 已在 `.gitignore` 中，
> 不会被提交。请勿把真实密钥写进 `application.yml` 或直接提交到仓库。

---

## 🔌 主要接口

| 模块 | 接口 | 说明 |
|------|------|------|
| 用户 | `POST /user/login` | 登录 |
| 用户 | `POST /user/register` | 注册 |
| 饮食 | `GET /foodRecord/list` | 当前用户饮食记录 |
| 饮食 | `POST /foodRecord/add` | 添加饮食记录 |
| 饮食 | `GET /foodRecord/export` | 导出饮食记录 |
| 运动 | `GET /exercise/list` | 当前用户运动记录 |
| 运动 | `POST /exercise/add` | 添加运动记录 |
| 体重 | `POST /weight/add` | 记录体重（每天一条，重复覆盖） |
| 体重 | `GET /weight/list?days=30` | 体重记录列表 |
| 体重 | `GET /weight/statistics?days=30` | 体重变化统计与达标进度 |
| 体重 | `GET /weight/today` | 今日是否已记录 |
| 体重 | `DELETE /weight/{id}` | 删除体重记录 |
| 统计 | `GET /statistics?timeRange=week` | 数据统计 |
| 统计 | `GET /statistics/today` | 今日统计 |
| 咨询 | `POST /consult/chat` | AI 自由对话（支持历史上下文） |
| 咨询 | `GET /consult/advice` | 营养建议（含 BMR、AI 可用状态） |
| 咨询 | `GET /consult/recipes?type=loss` | 推荐食谱 |
| AI配置 | `GET /ai-config` | 获取用户 AI 配置（密钥掩码） |
| AI配置 | `POST /ai-config` | 保存用户 API Key |
| AI配置 | `DELETE /ai-config` | 清除密钥，恢复系统免费模型 |
| AI配置 | `POST /ai-config/test` | 测试 AI 连接 |
| 食物库 | `GET /common-food/list?category=主食` | 常见食物列表 |
| 食物库 | `GET /common-food/categories` | 食物分类 |
| 食物库 | `GET /common-food/calorie?name=包子` | 查询单个食物热量 |
| 统计 | `GET /statistics/daily?days=7` | 每日统计（摄入/消耗/缺口） |
| 统计 | `GET /statistics/today` | 今日统计 |
| 计划 | `GET /plan/current` | 当前计划（自动加载） |
| 计划 | `POST /plan/get-or-create` | 获取或创建（幂等） |
| 计划 | `GET /plan/calorie-analysis` | 热量缺口分析（含推导步骤） |
| 计划 | `GET /plan/deficit-preview` | 按缺口反推所需天数 |
| 计划 | `PUT /plan/update/{id}` | 调整目标（自动重算缺口） |
| 计划 | `GET /plan/list` | 历史计划列表 |
| 计划 | `GET /plan/types` | 计划类型 |
| 管理 | `GET /admin/user/list` | 用户列表（管理员） |

---

## ❓ 常见问题

**Q：前端页面提示「获取数据失败」？**
A：确认后端窗口已出现 `Started FitnessBackendApplication`，且端口 8080 未被占用。

**Q：后端报错 `Table 'fitness_db.xxx' doesn't exist`？**
A：数据库未初始化，重新运行 `启动系统.bat` 并选择初始化数据库，或用 Navicat 执行 `fitness_db_complete.sql`。

**Q：8080 / 3000 端口被占用？**
A：脚本会自动检测并跳过已占用的服务。可用 `netstat -ano | findstr ":8080"` 查看占用进程。

**Q：拍照识别不出结果？**
A：需在 `application.yml` 中配置有效的百度 API Key；未配置时会返回模拟数据。

---

## 📄 许可证

本项目仅供学习交流使用。