# 个人健身管理系统

基于 **SpringBoot 3.2 + Vue3 + MySQL + 智谱 GLM** 的前后端分离健身管理系统。

围绕「饮食记录 → 运动消耗 → 体重追踪 → 计划规划 → 数据反馈」形成闭环，用医学公式（Mifflin-St Jeor）与代谢当量（MET）给出可解释的热量计算，而不是凭感觉估数。

---

## 目录

- [项目概况](#项目概况)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [核心功能](#核心功能)
- [核心算法](#核心算法)
- [技术亮点](#技术亮点)
- [数据库设计](#数据库设计)
- [接口清单](#接口清单)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [常见问题](#常见问题)
- [后续规划](#后续规划)

---

## 项目概况

| 项目 | 说明 |
|---|---|
| 形态 | 前后端分离 Web 应用 |
| 代码规模 | 后端 60 个 Java 文件 / 约 5300 行；前端 28 个文件 / 约 6000 行 |
| 接口数量 | 60 个 RESTful 接口，11 个 Controller |
| 数据库 | MySQL 5.7，8 张表，食物库预置 151 条 |
| 前端页面 | 12 个 Vue 页面 |

### 解决的问题

普通减脂人群普遍面临三个问题：

1. 不知道自己每天吃了多少热量
2. 不知道日常活动加运动消耗了多少
3. 更不知道「我到底该吃多少才能瘦」

市面上多数工具只做记录不做计算，或算法不透明。本项目把计算过程完全展开：基础代谢怎么算、活动消耗怎么估、缺口怎么定、为什么不能低于某个值，每一步都有依据。

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|---|---|---|
| SpringBoot | 3.2.0 | 应用框架 |
| MyBatis-Plus | 3.5.5 | 持久层 |
| MySQL | 5.7 | 数据库 |
| JJWT | 0.12.3 | JWT 认证 |
| EasyExcel | 3.3.2 | Excel 导出 |
| Fastjson2 | 2.0.32 | JSON 处理 |
| Lombok | - | 简化样板代码 |
| Spring AOP | - | 限流、幂等切面 |

### 前端

| 技术 | 版本 | 用途 |
|---|---|---|
| Vue | 3.4 | 框架（Composition API） |
| Vite | 5.0 | 构建工具 |
| Element Plus | 2.4 | UI 组件库 |
| Pinia | 2.1 | 状态管理 |
| Vue Router | 4.2 | 路由（history 模式） |
| Axios | 1.6 | HTTP 客户端 |
| ECharts | 5.4 | 数据可视化 |
| dayjs | 1.11 | 日期处理 |

### 外部服务

| 服务 | 用途 | 是否必需 |
|---|---|---|
| 智谱 GLM-4-Flash | AI 营养咨询 | 否，未配置则降级为内置知识库 |
| 百度 OCR | 拍照识别菜品 | 否，未配置则只能手动录入 |

---

## 系统架构

```
┌─────────────────────────────────────────────────────┐
│  表现层   Vue3 + Element Plus + ECharts              │
│  12 个页面 · 8 个 api 模块 · Pinia 状态管理           │
└──────────────────────┬──────────────────────────────┘
                       │  HTTP / JSON
                       │  Vite Proxy (开发) / Nginx (生产)
┌──────────────────────▼──────────────────────────────┐
│  接入层   JwtInterceptor · RateLimitAspect           │
│           IdempotentAspect · CorsConfig             │
├─────────────────────────────────────────────────────┤
│  控制层   11 个 Controller · 60 个接口                │
├─────────────────────────────────────────────────────┤
│  业务层   6 个 Service + 5 个 Impl                    │
│           CalorieCalculatorService（核心算法）        │
├─────────────────────────────────────────────────────┤
│  持久层   7 个 Mapper（MyBatis-Plus）                 │
├─────────────────────────────────────────────────────┤
│  基础层   LocalCache · TokenBucket · AiChatClient     │
│           ApiKeyCipher · JwtUtil · ExportUtil        │
└──────────────────────┬──────────────────────────────┘
                       │
                ┌──────▼──────┐
                │  MySQL 5.7  │
                └─────────────┘
```

### 前后端如何连接

开发阶段通过 Vite 代理把 `/api` 请求转发到后端：

```javascript
// fitness-frontend/vite.config.js
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),
    },
  },
}
```

请求链路：

```
浏览器  POST localhost:3000/api/user/login
   ↓  Vite 匹配 /api，rewrite 去掉前缀
后端    POST localhost:8080/user/login
   ↓
浏览器  收到响应（视角上同源，不触发跨域限制）
```

同时后端也配置了 CORS（`CorsConfig`）作为生产环境兜底。

---

## 核心功能

### 1. 用户与认证

- 注册：用户名唯一校验、密码加密存储
- 登录：校验通过后签发 JWT（24 小时有效）
- 鉴权：`JwtInterceptor` 解析 token，将 userId 注入 request
- 个人信息：身高、体重、目标体重、年龄、性别、活动系数

### 2. 饮食记录

- 手动录入：食物名 + 重量，按食物库每 100g 热量自动换算
- 食物库：151 种常见食物，7 大分类，支持关键词搜索
- 拍照识别：百度 OCR + 菜品识别（可选）
- 编辑删除：带越权校验，只能操作自己的记录
- 导出：EasyExcel 导出为 xlsx

### 3. 运动记录

- 运动类型：跑步、游泳、骑行、力量训练等
- 消耗计算：按 MET 代谢当量自动换算
- 时长记录：以分钟为单位

### 4. 体重记录

- 每日记录，同一用户同一天只保留一条
- 趋势统计：距目标差值、已减重量
- 基准对齐：以健身计划创建日为起点

### 5. 健身计划（核心）

- 自动计算基础代谢、每日总消耗
- 由目标体重反推每日热量缺口
- 安全评估：缺口过小提示效果不明显，过大则拒绝
- 进度追踪：时间进度、已过天数、剩余天数
- 不重复创建：已有进行中计划时自动复用

### 6. 数据统计

- 每日：摄入、运动消耗、基础消耗、总消耗、缺口、是否达标
- 周期：周 / 月 / 年聚合
- 起点对齐：统计区间自动从计划创建日开始
- 可视化：ECharts 趋势图 + 时间进度条

### 7. AI 营养咨询

- 多轮对话：保留最近 10 条上下文
- 个性化：将用户身高、体重、目标、当日数据注入 System Prompt
- 密钥管理：用户可填自己的 API Key，AES/GCM 加密存储
- 降级：无密钥或调用失败时使用内置知识库应答
- 限流：5 QPS / 用户

### 8. 管理接口

用户列表、详情、启用/禁用、删除、数据统计（后端提供接口，前端管理页暂未接入）。

---

## 核心算法

### 基础代谢率（BMR）

采用 Mifflin-St Jeor 公式（1990 年提出，目前营养学界公认精度较高）：

```
男性：BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 + 5
女性：BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 - 161
```

### 每日总消耗（TDEE）

```
TDEE = BMR × 活动系数
```

| 活动水平 | 系数 | 说明 |
|---|---|---|
| 久坐 | 1.2 | 办公室工作，几乎不运动 |
| 轻度活动 | 1.375 | 每周运动 1-3 次 |
| 中度活动 | 1.55 | 每周运动 3-5 次 |
| 高度活动 | 1.725 | 每周运动 6-7 次 |
| 极高活动 | 1.9 | 体力劳动者或运动员 |

### 热量缺口反推

```
需减重量 = 当前体重 - 目标体重
总缺口   = 需减重量 × 7700 kcal    （1 kg 脂肪约等于 7700 kcal）
每日缺口 = 总缺口 ÷ 计划天数
```

**约束条件：**

```
200 ≤ 每日缺口 ≤ 1000
每日摄入 ≥ BMR
```

**为什么限制上限 1000？**

每日缺口超过 1000 kcal 意味着极端节食，会带来肌肉流失、代谢适应（基础代谢下降，越减越难减）、营养缺乏等问题，不是可持续的方案。

**为什么摄入不能低于 BMR？**

BMR 是维持呼吸、心跳、体温等基本生命活动的最低能量需求。长期低于这个值，身体会启动「节能模式」，反而阻碍减脂。

### 运动消耗（MET）

```
消耗热量(kcal) = MET × 体重(kg) × 时长(小时)
```

| 运动 | MET 值 |
|---|---|
| 慢走 | 3.0 |
| 瑜伽 | 2.5 |
| 骑行（中速） | 6.0 |
| 游泳 | 6.0 |
| 跑步（8 km/h） | 8.0 |
| 跳绳 | 10.0 |

### 每日缺口

```
总消耗 = BMR × 1.2（基础活动） + 运动消耗
缺口   = 总消耗 - 摄入
```

缺口为正表示在减脂，为负表示热量盈余。

---

## 技术亮点

### 1. 接口限流（AOP + 令牌桶）

**问题**：AI 接口有调用成本，被刷会造成损失。

**方案**：注解式限流。

```java
@RateLimit(qps = 5, perUser = true, message = "AI 对话过于频繁")
@PostMapping("/chat")
public Result<Map<String, Object>> chat(...) { ... }
```

**令牌桶原理**：

```
桶容量 = qps，以 qps 速率持续补充令牌
请求到达 → 尝试取 1 个令牌
  有令牌 → 放行
  无令牌 → 拒绝（429）
```

**实现要点**：

- **惰性填充**：不起后台定时任务，每次请求时按时间差计算应补令牌数，避免无请求时 CPU 空转
- **`synchronized` 保证原子性**：「计算令牌 → 判断 → 扣减」必须原子
- **对比固定窗口**：固定窗口在窗口边界会有 2 倍流量突刺，令牌桶允许短时突发但长期速率受控

**实测**：20 个并发请求 → 5 个成功、15 个被限流，与 `qps=5` 精确吻合。

### 2. 本地缓存（LRU + TTL）

**问题**：食物库 151 条数据，每次下拉框都查库，浪费 IO。

**方案**：自研本地缓存。

```java
List<CommonFood> list = localCache.getOrLoad(cacheKey, 10 * 60 * 1000L,
        () -> commonFoodMapper.selectList(null));
```

**四个设计要点**：

| 要点 | 实现方式 |
|---|---|
| LRU 淘汰 | `LinkedHashMap(accessOrder=true)` + 重写 `removeEldestEntry` |
| TTL 过期 | value 包装过期时间戳，读取时惰性判断 |
| 读写锁 | `ReentrantReadWriteLock`，读并发、写互斥 |
| 防击穿 | `getOrLoad` 双重检查 + 写锁，同 key 只回源一次 |

**实测**：首次 175ms（查库），命中后 21ms，**提速约 8 倍**。

**为什么不用 Redis**：

| 对比项 | 本地缓存 | Redis |
|---|---|---|
| 访问耗时 | 约 100ns | 约 0.5-1ms（网络往返） |
| 多实例共享 | 不支持 | 支持 |
| 运维成本 | 零 | 需部署维护 |

数据量小、读多写少、允许短暂不一致、单机部署，本地缓存性价比更高。若需多实例共享则应换 Redis。

### 3. 幂等防重复提交

**问题**：用户连点，产生重复饮食记录。

**方案**：

```java
@Idempotent(seconds = 3, message = "请勿重复提交")
@PostMapping("/add")
public Result<FoodRecord> add(...) { ... }
```

**key 的构造是关键**：

```
key = "idem:" + userId + ":" + 类名.方法名 + ":" + 参数hash
```

- **加 userId**：否则 A 用户提交后，B 用户同一秒提交也会被误拦
- **加参数 hash**：同一用户 3 秒内提交两份**不同**饮食是合理的，只有完全相同才算重复
- **失败释放占位**：业务异常时立即清除 key，避免用户失败后要等窗口结束才能重试

**实测**：连续 4 次相同提交，仅第 1 次成功，其余返回 429；窗口过期后恢复。

### 4. 密钥安全（AES/GCM）

**问题**：用户自填 API Key，明文存库有泄露风险。

**方案**：AES/GCM 加密后落库，接口只回传掩码。

```
原始:  sk-abcd1234efgh
存储:  Base64(IV + 密文)
展示:  sk-a****efgh
```

**为什么选 GCM 而不是 ECB/CBC**：

| 模式 | 缺陷 |
|---|---|
| ECB | 相同明文块产生相同密文块，泄露数据模式 |
| CBC | 只保证机密性，不防篡改 |
| GCM | 机密性 + 完整性校验（AEAD），篡改可被发现 |

**关键细节**：

- IV 每次加密必须随机且不同（GCM 下 IV 复用会泄露密钥）
- IV 与密文一起存储（IV 无需保密，但解密需要）
- 使用 `SecureRandom` 而非 `Random`（后者可预测）

### 5. 分级异常处理

```java
@ExceptionHandler(BusinessException.class)                    // 业务异常，warn 级别
@ExceptionHandler(MethodArgumentNotValidException.class)      // 参数校验
@ExceptionHandler(MissingServletRequestParameterException.class)
@ExceptionHandler(Exception.class)                            // 兜底，error 级别
```

设计原则：

- **业务异常**（限流、幂等、参数非法）是流程的一部分，不打印堆栈
- **系统异常**（NPE、SQL 错误）是 bug，记录完整堆栈
- **兜底异常**防止堆栈信息泄露给前端

### 6. AI 降级

AI 是外部依赖，不可用不应拖垮核心功能。

```java
String answer = aiChatClient.chatWithHistory(...);
if (answer == null || answer.isEmpty()) {
    answer = localChatResponse(message, user, userId);   // 降级
    data.put("fallback", true);
}
```

用户记录饮食不应该因为大模型挂了而失败。

---

## 数据库设计

### 表结构

| 表名 | 用途 | 关键字段 |
|---|---|---|
| `user` | 用户信息 | 身高、体重、目标体重、活动系数 |
| `food_record` | 饮食记录 | 食物名、重量、热量、日期 |
| `exercise_record` | 运动记录 | 类型、时长、消耗热量 |
| `weight_record` | 体重记录 | 体重、日期 |
| `fitness_plan` | 健身计划 | 目标体重、每日缺口、起止日期 |
| `common_food` | 食物热量库 | 名称、每 100g 热量、分类 |
| `user_ai_config` | 用户 AI 密钥 | 加密后的 Key |
| `nutrition_consult` | 咨询历史 | 问题、回答 |

### 设计考量

- **三范式**：字段原子性、消除部分依赖与传递依赖
- **不建物理外键**：靠应用层保证，避免性能损耗与运维复杂度
- **索引**：`user_id`、`record_date` 建索引；体重表 `user_id + record_date` 唯一约束
- **DECIMAL 存体重**：不用 float，避免精度丢失
- **逻辑删除**：保留数据便于追溯

---

## 接口清单

### 用户 `/user`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/user/register` | 注册 |
| POST | `/user/login` | 登录 |
| GET | `/user/info` | 获取当前用户信息 |
| PUT | `/user/update` | 更新信息 |
| PUT | `/user/weight` | 更新体重 |
| GET | `/user/health` | 健康数据 |

### 饮食 `/foodRecord`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/foodRecord/list` | 列表 |
| POST | `/foodRecord/add` | 添加（带幂等保护） |
| PUT | `/foodRecord/update` | 更新 |
| DELETE | `/foodRecord/{id}` | 删除 |
| GET | `/foodRecord/export` | 导出 Excel |
| POST | `/foodRecord/recognize` | 拍照识别 |

### 运动 `/exercise`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/exercise/add` | 添加 |
| PUT | `/exercise/update/{id}` | 更新 |
| DELETE | `/exercise/{id}` | 删除 |
| GET | `/exercise/list` | 列表 |
| GET | `/exercise/today` | 今日汇总 |
| GET | `/exercise/statistics` | 统计 |
| POST | `/exercise/calculateCalorie` | 计算消耗 |

### 体重 `/weight`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/weight/add` | 添加记录 |
| GET | `/weight/list` | 列表 |
| GET | `/weight/statistics` | 统计（含距目标差值） |
| GET | `/weight/today` | 今日记录 |
| DELETE | `/weight/{id}` | 删除 |

### 计划 `/plan`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/plan/current` | 当前计划（无则自动创建） |
| GET | `/plan/calorie-analysis` | 热量分析 |
| GET | `/plan/deficit-preview` | 缺口预览 |
| POST | `/plan/get-or-create` | 获取或创建 |
| POST | `/plan/create` | 创建 |
| PUT | `/plan/update/{id}` | 更新 |
| GET | `/plan/list` | 列表 |
| GET | `/plan/active` | 进行中计划 |
| PUT | `/plan/complete/{id}` | 标记完成 |
| PUT | `/plan/cancel/{id}` | 取消 |
| DELETE | `/plan/{id}` | 删除 |
| GET | `/plan/types` | 计划类型 |

### 统计 `/statistics`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/statistics/today` | 今日 |
| GET | `/statistics/daily?days=N` | 每日 |
| GET | `/statistics/week` | 本周 |
| GET | `/statistics/month` | 本月 |
| GET | `/statistics/year` | 本年 |

### 食物库 `/common-food`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/common-food/list` | 列表（带缓存） |
| GET | `/common-food/categories` | 分类 |
| GET | `/common-food/calorie` | 查询热量 |
| GET | `/common-food/statistics` | 统计 |

### AI 咨询 `/consult`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/consult/chat` | 对话（带限流） |
| GET | `/consult/advice` | 营养建议 |
| GET | `/consult/recipes` | 食谱推荐 |

### AI 配置 `/ai-config`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ai-config` | 查询状态 |
| POST | `/ai-config` | 保存密钥 |
| DELETE | `/ai-config` | 删除密钥 |
| POST | `/ai-config/test` | 测试连通性 |
| GET | `/ai-config/status` | 密钥来源状态 |

### 管理 `/admin/user`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/user/list` | 分页列表 |
| GET | `/admin/user/all` | 全部用户 |
| GET | `/admin/user/{id}` | 用户详情 |
| PUT | `/admin/user/{id}/status` | 启用/禁用 |
| DELETE | `/admin/user/{id}` | 删除用户 |
| GET | `/admin/user/statistics` | 用户统计 |

### 监控 `/monitor`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/monitor/cache` | 缓存命中率 |
| GET | `/monitor/limiter` | 限流桶数量 |
| GET | `/monitor/health` | JVM 与综合状态 |

---

## 快速开始

### 环境要求

| 组件 | 版本要求 |
|---|---|
| JDK | 17+ |
| Maven | 3.6+（或用自带 mvnw） |
| Node.js | 16+ |
| MySQL | 5.7 或 8.0 |

### 方式一：一键启动（推荐）

双击项目根目录的 **`start-all.bat`**

脚本会自动：

1. 检查 MySQL 是否运行
2. 启动后端（8080，独立窗口）
3. 启动前端（3000，独立窗口，依赖缺失时自动 `npm install`）
4. 轮询检测两个端口，最多等 90 秒
5. 都就绪后自动打开浏览器

### 方式二：手动启动

#### 1. 初始化数据库（首次）

```bash
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS fitness_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p123456 --default-character-set=utf8mb4 fitness_db < fitness_db_complete.sql
```

也可用 Navicat 直接执行 `fitness_db_complete.sql`。

#### 2. 启动后端

```bash
# 打包
mvnw.cmd clean package -DskipTests

# 运行
java -jar target/Fitness-backend-0.0.1-SNAPSHOT.jar
```

或开发模式：

```bash
mvnw.cmd spring-boot:run
```

#### 3. 启动前端

```bash
cd fitness-frontend
npm.cmd install     # 首次
npm.cmd run dev
```

> **Windows 提示**：如果 PowerShell 报「禁止运行脚本」，请用 `npm.cmd` 而不是 `npm`，或执行 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`。

### 访问

| 地址 | 说明 |
|---|---|
| http://localhost:3000 | 前端页面 |
| http://localhost:8080 | 后端接口 |

### 测试账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `test` | `123456` | 普通用户 |
| `admin` | `123456` | 管理员 |

### 配置 AI（可选）

在 `src/main/resources/application-secret.yml` 中填写：

```yaml
ai:
  glm:
    api-key: 你的智谱APIKey

baidu:
  ocr:
    access-key: 你的百度AccessKey
    secret-key: 你的百度SecretKey
```

该文件已被 `.gitignore` 忽略，不会提交到仓库。也可通过环境变量注入：

```bash
set AI_GLM_API_KEY=你的密钥
```

未配置时 AI 功能自动降级为内置知识库，不影响其他功能。

---

## 项目结构

```
Fitness-backend/
├── src/main/java/com/fitness/
│   ├── common/
│   │   ├── annotation/      自定义注解（RateLimit、Idempotent）
│   │   ├── aspect/          AOP 切面（限流、幂等）
│   │   ├── cache/           本地缓存（LRU + TTL）
│   │   ├── exception/       业务异常
│   │   ├── limiter/         令牌桶限流器
│   │   └── Result.java      统一返回体
│   ├── config/              配置类（CORS、JWT、异常处理、MyBatis）
│   ├── controller/          11 个 Controller（含 admin 子包）
│   ├── dto/                 数据传输对象
│   ├── entity/              实体类
│   ├── mapper/              MyBatis-Plus Mapper
│   ├── service/             业务接口
│   │   └── impl/            业务实现
│   └── util/                工具类（JWT、加密、AI 客户端、导出）
├── src/main/resources/
│   ├── application.yml      主配置
│   ├── application-secret.yml           密钥配置（gitignored）
│   └── application-secret.yml.example   配置模板
├── fitness-frontend/
│   ├── src/
│   │   ├── api/             8 个接口模块
│   │   ├── views/           12 个页面
│   │   ├── router/          路由与守卫
│   │   ├── stores/          Pinia 状态
│   │   ├── utils/           axios 封装、token 管理
│   │   └── components/      公共组件
│   └── vite.config.js       含代理配置
├── fitness_db_complete.sql  数据库初始化脚本
├── common_food_data.sql     食物库数据（151 条）
├── start-all.bat            一键启动脚本
└── pom.xml
```

---

## 常见问题

### 后端启动失败：无法连接数据库

```bash
# 确认 MySQL 服务已启动
net start | findstr /I mysql

# 如未启动
net start MySQL57
```

### 前端页面能打开但接口全部报错

按顺序排查：

```bash
# 1. 后端是否在跑
netstat -ano | findstr :8080

# 2. 后端能否直接访问（绕过前端）
curl http://localhost:8080/user/login -X POST -H "Content-Type: application/json" -d "{\"username\":\"test\",\"password\":\"123456\"}"

# 3. 代理是否生效（通过前端）
curl http://localhost:3000/api/user/login -X POST -H "Content-Type: application/json" -d "{\"username\":\"test\",\"password\":\"123456\"}"
```

- 第 2 步成功、第 3 步失败 → 代理配置问题，检查 `vite.config.js`
- 第 2 步也失败 → 后端问题，查看后端控制台日志

### npm 报「禁止运行脚本」

```powershell
# 方案一：直接用 npm.cmd
npm.cmd run dev

# 方案二：调整执行策略
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

### 端口被占用

```bash
# 查看占用进程
netstat -ano | findstr :3000

# 结束进程（替换为实际 PID）
taskkill /PID <PID> /F
```

或修改 `vite.config.js` 的 `server.port`。

### AI 功能无响应

1. 检查是否配置了 API Key（访问 `/ai-config/status`）
2. 查看后端日志是否有调用失败记录
3. 未配置密钥时会自动降级为内置知识库，功能仍可用

### 刷新页面 404（生产环境）

Vue Router 使用 history 模式，需要服务器配置 fallback。Nginx 示例：

```nginx
location / {
    root /var/www/fitness-frontend/dist;
    try_files $uri $uri/ /index.html;
}
```

---

## 后续规划

| 方向 | 具体方案 | 预期收益 |
|---|---|---|
| Redis 缓存 | 替换本地缓存，支持多实例共享 | 可水平扩展 |
| 消息队列 | AI 请求异步化（RabbitMQ / RocketMQ） | 长请求不阻塞主线程 |
| 接口文档 | 接入 Swagger / Knife4j | 便于前后端协作 |
| 单元测试 | Service 层覆盖核心算法 | 保证计算正确性 |
| 微服务拆分 | 用户服务、记录服务、AI 服务 | 独立部署与伸缩 |
| Docker 部署 | docker-compose 一键起环境 | 环境一致性 |
| 前端优化 | 路由懒加载、组件按需引入 | 首屏加载更快 |
| 管理页面 | 补全前端用户管理界面 | 管理功能可用 |

---

## 说明

- `application-secret.yml` 包含真实密钥，已被 `.gitignore` 忽略，不会提交
- 首次克隆项目请复制 `application-secret.yml.example` 为 `application-secret.yml` 并填入自己的密钥
- 所有热量计算仅作参考，不构成医疗或营养建议
