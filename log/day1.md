# Day1 开发日志 —— 项目初始化与基础搭建

**日期：2026-06-01**  
**参与人员：尹领航（后端）、欧行（前端）**

---

## 一、今日目标

- 确定项目选题：**体育用品批发销售信息系统**
- 设计数据库表结构并编写建表 SQL
- 搭建 Spring Boot 后端项目骨架
- 搭建 Vue 3 前端项目骨架
- 配置前后端基础运行环境

---

## 二、尹领航 —— 后端开发

### 2.1 项目选题分析

我们选的题目是"体育用品批发销售信息系统"。分析下来，系统需要管理以下几块核心数据：

- **生产厂家**：体育用品有明确的品牌/厂家（李宁、安踏、耐克等），需要一张厂家表
- **库存商品**：商品信息，关联厂家，需要库存管理
- **顾客**：批发的客户（体育用品店、运动商城等），不是普通个人消费者
- **订单**：顾客下单，包含订单头 + 订单细则（一对多）
- **进货记录**：从厂家补货的记录
- **系统用户**：登录认证，区分管理员和顾客

### 2.2 数据库设计

按照上面的分析，设计了 **7 张表**：

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `manufacturer` | 生产厂家信息表 | manufacturer_code(PK), manufacturer_name, contact_person, phone, address |
| `product` | 库存商品信息表 | product_code(PK), manufacturer_code(FK), unit_price, stock_quantity, min_stock, status |
| `customer` | 顾客信息表 | customer_code(PK), customer_name, phone, email, password, status |
| `orders` | 订单信息表 | order_id(PK,自增), customer_code(FK), total_amount, payment_status, shipping_status, order_status |
| `order_item` | 订单细则表 | item_id(PK,自增), order_id(FK), product_code(FK), quantity, unit_price, total_amount |
| `purchase_record` | 进货记录表 | id(PK,自增), product_code(FK), manufacturer_code(FK), quantity, unit_price, status |
| `sys_user` | 用户认证表 | id(PK,自增), username(唯一), password(BCrypt加密), role(admin/customer) |

**设计思路**：
- 商品和厂家用**业务编码**做主键（如 M001、P001），而不是自增 ID，因为体育用品行业本身就有编码体系
- 订单用**自增 ID** 做主键，因为有大量并发创建需求
- `phone` 和 `email` 字段故意设为 `VARCHAR(200)`，而不是常规的 `VARCHAR(20)`，因为后续计划用 AES 加密存储，密文比明文长很多
- 订单表拆成 `orders` + `order_item`，遵循数据库范式，一个订单可以包含多个商品
- 增加了 `sys_user` 表做认证隔离，而不是直接在 customer 表加角色字段，方便后续扩展管理员账号

写好了 `main.sql`，包含建库、建表、外键约束、索引和测试数据（4个厂家、8个商品、3个顾客）。

### 2.3 后端项目搭建

用 Spring Initializr（阿里云镜像）创建 Spring Boot 3.2.5 项目：

- **JDK**：17
- **构建工具**：Maven
- **核心依赖**：Spring Web、MyBatis、MySQL Driver、Lombok
- **后续添加的依赖**：Redis、Redisson、Druid、Hutool、JWT

项目初始结构：
```
后端代码/
├── pom.xml
├── src/main/java/com/sports/sales/
│   └── SportsSalesApplication.java    # 启动类
└── src/main/resources/
    ├── application.yml                 # 主配置
    └── mapper/                         # MyBatis XML映射文件
```

**启动类配置**：
- `@SpringBootApplication`
- `@MapperScan("com.sports.sales.mapper")` —— 扫描 MyBatis Mapper 接口
- 留了 `@EnableScheduling` 和 `@EnableTransactionManagement` 的位置，后续再加

**application.yml 配置要点**：
- MySQL 连接：`sports_sales` 数据库，Druid 连接池
- Redis 基础配置
- 预留了 JWT 密钥和 AES 加密密钥的配置项

配置完成后运行 `main` 方法，启动成功，端口 8080，访问 `localhost:8080` 出现 Whitelabel Error Page 说明 Spring Boot 跑起来了。

### 2.4 Git 初始化

- 初始化 Git 仓库
- 配置 `.gitignore`（排除 node_modules、target、*.log 等）
- 关联 Gitee 远程仓库（SSH 方式）
- 第一次提交：`Initial commit`

---

## 三、欧行 —— 前端开发

### 3.1 技术选型

- **Vue 3**（Composition API）+ **Vite** 构建
- **Element Plus** 组件库（企业级 UI，表格、表单、对话框都现成的）
- **Vue Router 4** 路由管理
- **Vuex 4** 状态管理（购物车、用户信息）
- **Axios** HTTP 请求
- **Sass** CSS 预处理

### 3.2 项目初始化

```bash
npm create vite@latest 前端代码 -- --template vue
cd 前端代码
npm install
npm install element-plus vue-router@4 vuex@4 axios sass
```

### 3.3 目录结构规划

```
前端代码/src/
├── main.js              # 应用入口，挂载App、Router、Store、ElementPlus
├── App.vue              # 根组件，只放 <router-view />
├── api/                 # API请求模块（auth.js, product.js, order.js等）
├── layouts/             # 布局组件（前台布局FrontLayout、后台布局AdminLayout）
├── router/              # 路由配置
├── store/               # Vuex（token、userInfo、cart）
├── styles/              # 全局样式
├── utils/               # 工具函数（request.js Axios封装）
└── views/               # 页面视图
    ├── login/           # 登录/注册
    ├── front/           # 顾客前台页面
    └── admin/           # 管理员后台页面
```

### 3.4 路由设计

规划了两套路由：

**前台路由**（顾客视角）：
- `/` → 首页
- `/products` → 商品浏览
- `/cart` → 购物车
- `/orders` → 我的订单
- `/profile` → 个人中心

**后台路由**（管理员视角）：
- `/admin/dashboard` → 仪表盘
- `/admin/products` → 商品管理
- `/admin/orders` → 订单管理
- `/admin/users` → 顾客管理
- `/admin/purchase` → 进货管理
- `/admin/manufacturers` → 厂家管理

### 3.5 今日遇到的坑

- Vite 创建项目默认不带 `vue-router` 和 `vuex`，需要手动安装
- Element Plus 默认是英文的，后续需要配置中文语言包
- `.env.development` 中的 `VITE_API_BASE_URL` 需要在 `vite.config.js` 中配置代理，否则跨域请求会被拦截

---

## 四、今日小结

今天完成了项目的"地基"工作：

| 模块 | 完成内容 | 负责人 |
|------|---------|--------|
| 数据库 | 7张表设计 + SQL脚本 + 测试数据 | 尹领航 |
| 后端 | Spring Boot 项目搭建 + 基础配置 + 启动验证 | 尹领航 |
| 前端 | Vue 3 项目搭建 + 依赖安装 + 目录结构 + 路由规划 | 欧行 |
| 版本控制 | Git 初始化 + 远程仓库关联 + 首次提交 | 尹领航 |

整个项目框架已经搭好，明天开始具体功能开发。
