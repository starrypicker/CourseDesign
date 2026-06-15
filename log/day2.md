# Day2 开发日志 —— 后端全部接口开发 & 前端页面框架搭建

**日期：2026-06-04**  
**参与人员：尹领航（后端）、欧行（前端）**

---

## 一、今日目标

- 后端：完成所有实体类、Mapper、Service、Controller 的编写，实现全部 REST API
- 后端：集成 Redis 缓存、Redisson 分布式锁、JWT 认证、AES 数据加密
- 后端：添加定时任务（库存预警、订单超时）
- 前端：完成登录注册页面、前台布局、后台布局
- 前端：开始编写各业务页面

---

## 二、尹领航 —— 后端完整开发

### 2.1 项目分层设计与开发流程

采用经典的四层架构：**Controller → Service → Mapper → Entity**

开发流程是**自底向上**的：先写 Entity（实体类），再写 Mapper（数据访问），再写 Service（业务逻辑），最后写 Controller（接口暴露）。

### 2.2 Entity 实体类

对应 7 张表写了 7 个实体类：

| 实体类 | 对应表 | 要点 |
|--------|--------|------|
| `Manufacturer` | manufacturer | 厂家代码、厂家名称、联系人、电话、地址 |
| `Product` | product | 商品编码、关联厂家、单价、库存量、最低库存线、状态 |
| `Customer` | customer | 顾客代码、名称、联系人、地址、邮编、电话、邮箱、密码、状态 |
| `Orders` | orders | 订单号、顾客代码、运输要求、能否供货、运费、总金额、付款/发货/订单状态 |
| `OrderItem` | order_item | 细则编号、关联订单、关联商品、数量、单价、金额 |
| `PurchaseRecord` | purchase_record | 进货ID、商品编码、厂家编码、数量、单价、状态 |
| `SysUser` | sys_user | 用户ID、用户名、密码、角色、关联顾客代码 |

全部使用 Lombok 注解 `@Data`、`@NoArgsConstructor`、`@AllArgsConstructor` 简化代码。

### 2.3 Mapper 层

每个实体类对应一个 Mapper 接口 + 一个 MyBatis XML 映射文件。

Mapper 接口统一加 `@Mapper` 注解，放在 `mapper/` 包下。

XML 映射文件放在 `resources/mapper/` 目录下，配置 `mybatis.mapper-locations=classpath:mapper/*.xml`。

**关键 SQL 设计思路**：

- **商品查询**：支持多条件动态搜索（商品名称模糊查询、厂家筛选、状态筛选、价格区间），用了 MyBatis 的 `<where>` + `<if>` 动态 SQL
- **订单创建**：需要同时插入 `orders` 表和 `order_item` 表，并且要扣减库存，所以必须加 `@Transactional` 保证事务一致性
- **订单查询**：关联 `customer` 表查顾客名称，关联 `order_item` + `product` 查商品明细，用 LEFT JOIN + 分页
- **进货记录**：确认入库时要同时更新 `purchase_record.status` 和 `product.stock_quantity`，同样需要事务

**分页处理**：定义了通用的 `PageResult<T>` 类（total + list），MyBatis 分页直接在 SQL 里用 `LIMIT #{offset}, #{pageSize}`，offset 参数在 Service 层计算。

### 2.4 Service 层

Service 层是业务逻辑的核心。定义了接口 + 实现类：

| Service | 核心方法 | 业务要点 |
|---------|---------|---------|
| `CustomerServiceImpl` | list, add, update, delete, getByCode | 新增/修改时用 AES 加密手机号和邮箱；删除前检查是否有订单关联 |
| `ProductServiceImpl` | list, add, update, delete, updateStatus | 新增时校验厂家是否存在；删除前检查是否有订单关联 |
| `OrderServiceImpl` | create, confirm, pay, ship, complete, cancel | **核心流程**：创建订单→扣库存→确认→付款→发货→签收，每步状态校验 |
| `PurchaseRecordServiceImpl` | list, add, confirm | 确认入库→更新库存，加 Redisson 分布式锁防重复入库 |
| `ManufacturerServiceImpl` | list, add, update, delete | 删除前检查是否有商品关联 |

**为什么订单模块最复杂？**

订单有几个状态流转：
```
待确认(0) → 已确认(1) → 已付款 → 已发货 → 已签收 → 已完成(2)
                                                ↘ 已取消(3)
```

每一步操作都要校验当前状态是否允许该操作。比如"已取消"的订单不能再确认，"未付款"的订单不能发货。

**决定把订单的状态机设计成三个维度**：
- `order_status`：订单生命周期状态（待确认/已确认/已完成/已取消）
- `payment_status`：付款状态（未付款/已付款/已退款）
- `shipping_status`：发货状态（未发货/已发货/已签收）

三个维度独立管理，比一个字段管所有状态更灵活。

### 2.5 Controller 层

全部返回统一格式 `Result<T>`（code + message + data），定义在 `common/Result.java` 中：

```java
// 成功：Result.success(data)
// 失败：Result.error("错误信息")
```

接口设计遵循 RESTful 风格：

| 模块 | 路径 | 方法 |
|------|------|------|
| 认证 | `POST /api/auth/login`、`POST /api/auth/register` | 登录返回 JWT Token |
| 商品 | `GET/POST/PUT/DELETE /api/product` | 增删改查 + 上/下架 |
| 订单 | `GET/POST/PUT /api/order` | 创建、查询、确认、付款、发货、取消 |
| 顾客 | `GET/POST/PUT/DELETE /api/customer` | 增删改查 + 启用/禁用 |
| 进货 | `GET/POST/PUT /api/purchase` | 进货记录 + 确认入库 |
| 厂家 | `GET/POST/PUT/DELETE /api/manufacturer` | 增删改查 |

### 2.6 安全与认证

**JWT 认证流程**：
1. 用户登录 → 后端验证用户名密码（BCrypt）→ 生成 JWT Token → 返回给前端
2. 前端后续请求在 Header 中携带 `Authorization: Bearer <token>`
3. `LoginInterceptor` 拦截所有 `/api/**` 请求（除登录注册），校验 Token 有效性
4. 从 Token 中解析出用户名和角色，放入 `ThreadLocal`，后续 Service 层可以获取当前用户

**AES 数据加密**：
- 顾客的手机号（phone）和邮箱（email），以及订单的收件人电话（recipient_phone）使用 AES-256-CBC 加密存储
- 加密工具类 `CryptoUtil`：提供 `encrypt()` 和 `decrypt()` 方法
- 为什么加密？这些是敏感个人信息，数据库中存储密文，即使数据库泄露也不会暴露真实联系方式
- 加密密钥配置在 `application.yml` 中

**实现细节**：
- 加密在 Service 层的 `add`/`update` 方法中自动处理
- 解密在 Service 层的查询方法中自动处理
- 对前端透明——前端收到的数据已经是解密后的明文，提交的数据也是明文，加密在后端完成

### 2.7 Redis 缓存

商品、顾客、订单查询加了 `@Cacheable` 缓存：

```java
@Cacheable(value = "product", key = "#productCode", unless = "#result == null")
public Product getByCode(String productCode) { ... }

@CacheEvict(value = "product", key = "#product.productCode")
public void update(Product product) { ... }
```

缓存策略：
- 查询时先查 Redis，有就直接返回，没有才查数据库并写入 Redis
- 更新/删除时清除对应缓存
- `unless = "#result == null"` 确保 null 值不入缓存（防止缓存穿透）

### 2.8 Redisson 分布式锁

订单创建时需要扣减库存，高并发场景下可能超卖。用 Redisson 的 `RLock` 加锁：

```java
RLock lock = redissonClient.getLock("stock:lock:" + productCode);
try {
    lock.lock(10, TimeUnit.SECONDS);
    // 检查库存 → 扣减库存 → 创建订单
} finally {
    lock.unlock();
}
```

**为什么按商品编码加锁？**

如果对整个订单创建加一把大锁，不同商品之间也会互相阻塞，并发性能差。按每个商品编码分别加锁，不同商品的订单可以并行创建，只有同一个商品才需要排队。同时，多个商品加锁时按编码排序，避免了死锁问题。

### 2.9 定时任务

| 定时任务 | 频率 | 逻辑 |
|----------|------|------|
| `StockMonitorTask` | 每天 8:00 | 查询所有 `stock_quantity < min_stock` 的商品，输出预警日志 |
| `OrderTimeoutTask` | 每 30 分钟 | 查询超过 24 小时仍为"待确认"的订单，自动取消并恢复库存 |

### 2.10 全局异常处理

`GlobalExceptionHandler` 用 `@RestControllerAdvice` 统一捕获异常，避免直接返回 500 错误给前端：

```java
@ExceptionHandler(Exception.class)
public Result<?> handleException(Exception e) {
    log.error("系统异常", e);
    return Result.error("系统异常: " + e.getMessage());
}
```

### 2.11 今日遇到并解决的问题

1. **MyBatis XML 没有自动扫描到** → 在 `application.yml` 中配置了 `mybatis.mapper-locations`
2. **JWT Token 过期后还返回 500 而不是 401** → 在拦截器和全局异常处理中加了 `ExpiredJwtException` 的专门处理
3. **AES 加密后字段太长超过数据库字段长度** → 把 `phone` 和 `email` 字段从 `VARCHAR(20)` 改成了 `VARCHAR(200)`
4. **Redisson 与 Spring Cache 共用 Redis 产生冲突** → 给 Redisson 和 Spring Cache 配置了不同的序列化方式

---

## 三、欧行 —— 前端页面框架搭建

### 3.1 Axios 封装

封装了 `utils/request.js`：

- **请求拦截器**：自动从 Vuex 中获取 Token 并添加到请求头 `Authorization: Bearer <token>`
- **响应拦截器**：统一处理错误码，Token 过期自动跳转登录页
- 设置了 `baseURL` 和 `timeout`

### 3.2 API 模块

按业务模块拆分了 API 文件：

| 文件 | 接口 |
|------|------|
| `api/auth.js` | login(), register() |
| `api/product.js` | list(), add(), update(), delete(), updateStatus() |
| `api/order.js` | list(), create(), confirm(), pay(), ship(), complete(), cancel() |
| `api/customer.js` | list(), add(), update(), delete(), updateStatus() |
| `api/purchase.js` | list(), add(), confirm() |
| `api/manufacturer.js` | list(), add(), update(), delete() |

每个函数返回 Promise，在组件中直接用 `async/await` 调用。

### 3.3 登录/注册页面

创建了 `views/login/index.vue`，包含两个子组件：
- `LoginForm.vue`：用户名 + 密码 + 角色选择（管理员/顾客）
- `RegisterForm.vue`：顾客注册表单（名称、联系人、地址、电话、邮箱、密码）

**设计思路**：
- 登录和注册在同一页面，用 tab 切换
- 登录成功后存储 Token 和用户信息到 Vuex 和 localStorage
- 根据角色跳转不同页面：管理员 → `/admin/dashboard`，顾客 → `/`

### 3.4 前台布局 FrontLayout.vue

顾客前台采用经典的"顶部导航 + 左侧侧边栏 + 右侧内容区"布局：

- **顶部导航**：Logo、搜索框、购物车图标、用户头像下拉菜单
- **左侧侧边栏**：首页、商品浏览、购物车、我的订单、个人中心
- **右侧内容区**：`<router-view />` 渲染子路由

用了 Element Plus 的 `el-container`、`el-header`、`el-aside`、`el-main` 组件。

### 3.5 后台布局 AdminLayout.vue

管理员后台采用同样的布局模式：

- **顶部**：系统标题"体育用品批发销售信息系统"、当前管理员名称、退出按钮
- **左侧菜单**：仪表盘、商品管理、订单管理、顾客管理、进货管理、厂家管理
- 菜单加图标（使用 Element Plus Icon）

### 3.6 路由配置

在 `router/index.js` 中配置了完整路由，包括路由守卫：

```javascript
// 全局前置守卫
router.beforeEach((to, from, next) => {
  const token = store.state.token
  if (to.path.startsWith('/admin') && !isAdmin) {
    next('/login')  // 非管理员访问后台 → 跳转登录
  }
  // ...
})
```

---

## 四、今日小结

今天是最"高产"的一天，后端所有接口全部完成，前端页面框架也搭好了：

| 模块 | 完成内容 | 负责人 |
|------|---------|--------|
| 后端 Entity | 7 个实体类 | 尹领航 |
| 后端 Mapper | 7 个 Mapper 接口 + 7 个 XML 映射文件 | 尹领航 |
| 后端 Service | 5 个 Service 接口 + 5 个实现类 | 尹领航 |
| 后端 Controller | 6 个 Controller，约 30+ 个 API 接口 | 尹领航 |
| 后端安全 | JWT + BCrypt + AES + 拦截器 | 尹领航 |
| 后端缓存/锁 | Redis 缓存 + Redisson 分布式锁 | 尹领航 |
| 后端定时任务 | 库存预警 + 订单超时取消 | 尹领航 |
| 前端封装 | Axios 封装 + API 模块拆分 | 欧行 |
| 前端登录 | 登录/注册页面 | 欧行 |
| 前端布局 | 前台 FrontLayout + 后台 AdminLayout | 欧行 |
| 前端路由 | 完整路由配置 + 导航守卫 | 欧行 |

明天目标：前后端联调，把各业务页面写完。
