# 体育用品批发销售信息系统

基于 Vue 3 + Spring Boot 的体育用品批发销售信息管理系统。支持顾客前台购物和管理员后台管理。

## 项目结构

```
├── README.md                # 项目说明文档
├── .gitignore               # Git忽略配置
├── 数据库/                   # 数据库脚本
│   └── main.sql             # 建库建表 + 测试数据
├── 后端代码/                 # Spring Boot 后端
│   ├── src/main/java/       # Java源文件
│   ├── src/main/resources/  # 配置文件 + MyBatis XML
│   ├── pom.xml              # Maven配置
│   └── README.md            # 后端说明文档
├── 前端代码/                 # Vue 3 前端
│   ├── src/                 # Vue源代码
│   ├── public/              # 静态资源
│   ├── package.json         # 依赖配置
│   ├── vite.config.js       # Vite构建配置
│   └── index.html           # 入口HTML
└── log/                     # 运行日志
    ├── sports-sales.log     # 全量日志
    ├── sports-sales-error.log  # 错误日志
    └── sports-sales-biz.log    # 业务日志
```

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | Vue 3 + Vite | SPA单页应用 |
| UI组件 | Element Plus | 企业级UI组件库 |
| 状态管理 | Vuex 4 | 购物车/用户状态 |
| 路由 | Vue Router 4 | 前台+后台路由 |
| HTTP | Axios | 请求拦截/响应处理 |
| 后端框架 | Spring Boot 3.2 | REST API |
| ORM | MyBatis 3 | 数据库映射 |
| 数据库 | MySQL 8.x | 关系型数据库 |
| 缓存 | Redis + Spring Cache | 数据缓存 |
| 分布式锁 | Redisson | 订单/库存并发控制 |
| 安全 | BCrypt + AES + JWT | 密码加密/数据加密/身份认证 |
| 连接池 | Druid | 数据库连接池 |

## 功能模块

### 顾客前台
- 商品浏览与搜索
- 购物车管理
- 订单创建/付款/确认收货
- 个人信息管理
- 密码修改

### 管理员后台
- 仪表盘（销售统计/库存预警）
- 商品管理（添加/编辑/上下架/删除）
- 订单管理（确认/发货/取消）
- 顾客管理（添加/编辑/启用禁用/删除）
- 进货管理（进货记录/确认入库）

## 环境要求

- **JDK** 17+
- **MySQL** 8.x
- **Redis** (用于缓存和分布式锁)
- **Node.js** 18+ (前端开发)
- **Maven** 3.x (后端构建)

## 快速部署

### 1. 初始化数据库

```bash
mysql -u root -p < 数据库/main.sql
```

### 2. 配置环境变量

```bash
# 数据库配置
export DB_USERNAME=root
export DB_PASSWORD=你的MySQL密码

# Redis配置（如无密码可不设置）
export REDIS_HOST=localhost
export REDIS_PORT=6379
# export REDIS_PASSWORD=你的Redis密码
```

### 3. 启动后端

```bash
cd 后端代码
mvn clean package -DskipTests
java -jar target/sports-sales-1.0.0.jar
```

或在 IntelliJ IDEA 中直接运行 `SportsSalesApplication.java`。

后端默认运行在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd 前端代码
npm install
npm run dev
```

前端开发服务器默认运行在 `http://localhost:5173`，已配置代理转发 `/api` 请求到后端。

### 5. 访问系统

| 入口 | 地址 | 账号 | 密码 |
|------|------|------|------|
| 管理员后台 | 管理员登录 | `admin` | `123456` |
| 顾客前台 | 顾客登录 | `sun` | `123456` |

## API接口概览

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证 | `/api/auth` | 登录/注册 |
| 顾客 | `/api/customer` | 顾客CRUD |
| 商品 | `/api/product` | 商品CRUD + 库存 |
| 订单 | `/api/order` | 订单全流程 |
| 进货 | `/api/purchase` | 进货管理 |
| 厂家 | `/api/manufacturer` | 厂家信息 |

## 关键设计

### 数据安全
- 密码：BCrypt 哈希存储
- 敏感数据：AES-256-CBC 加密（手机号/邮箱/收件人电话）
- 认证：JWT Token + 请求拦截器
- 权限隔离：顾客只能访问自己的订单和信息

### 并发控制
- 订单创建：按商品编码排序加分布式锁，防止超卖
- 库存补货：分布式锁保护库存更新
- 进货确认：分布式锁防止重复入库

### 缓存策略
- 商品/顾客/订单查询缓存（Redis）
- 更新时自动清除缓存（`@CacheEvict`）
- null值不入缓存（`unless = "#result == null"`）

### 定时任务
- 库存预警：每天8:00自动检查并创建补货建议
- 订单超时：每30分钟检查，24小时未确认自动取消

## 开发说明

- 前端开发时需创建 `前端代码/.env.development` 配置后端地址
- 后端HTTPS默认关闭，使用HTTP 8080端口
- 加密密钥在 `application.yml` 中配置，生产环境需通过环境变量覆盖
- 日志文件按天滚动，保留30天
