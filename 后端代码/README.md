# 体育用品批发销售信息系统 - 后端


## 项目简介

本系统是一家体育用品批发商店的销售信息管理系统后端，提供顾客管理、商品管理、订单管理、厂家管理、进货管理等功能，支持前台顾客购物和后台管理员管理。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 核心框架 |
| MyBatis | 3.0.3 | ORM框架 |
| MySQL | 8.x | 数据库 |
| Redis | - | 缓存 |
| Redisson | 3.27.2 | 分布式锁 |
| Druid | 1.2.22 | 数据库连接池 |
| Lombok | 1.18.34 | 代码简化 |
| Hutool | 5.8.26 | 工具库 |
| JDK | 17 | 运行环境 |

## 加分项实现

| 加分项 | 实现方式 | 涉及文件 |
|--------|---------|---------|
| 事务管理 | `@Transactional` 注解，订单创建/取消时库存自动回滚 | Service实现类 |
| Redis缓存 | `@Cacheable/@CacheEvict` 缓存商品、顾客、订单数据 | RedisConfig, Service实现类 |
| 分布式锁 | Redisson `RLock`，订单创建和库存补货时加锁 | OrderServiceImpl, ProductServiceImpl |
| HTTPS | Spring Boot SSL配置 + keystore.p12证书 | HttpsConfig, application.yml |
| 多线程 | 线程池 + `@Async` + `CompletableFuture`，定时任务异步执行 | ThreadPoolConfig, StockMonitorTask, OrderTimeoutTask |
| 敏感数据加解密 | AES/CBC/PKCS5Padding，顾客电话/邮箱/收件人电话自动加解密 | CryptoUtil |

## 环境要求

- JDK 17
- MySQL 8.x
- Redis
- Maven 3.x

## 快速开始

### 1. 安装依赖

确保已安装 JDK 17、MySQL 8.x、Redis。

### 2. 初始化数据库

执行 `数据库/main.sql` 脚本创建数据库和表：

```bash
mysql -u root -p < 数据库/main.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`，修改数据库和Redis连接信息：

```yaml
spring:
  datasource:
    username: root
    password: 你的MySQL密码
  redis:
    password: 你的Redis密码
```

### 4. 编译运行

```bash
mvn clean package -DskipTests
java -jar target/sports-sales-1.0.0.jar
```

或在 IntelliJ IDEA 中直接运行 `SportsSalesApplication.java`。

### 5. 验证启动

访问 `https://localhost/api/product/list`（浏览器提示证书不安全时点击继续访问），返回JSON数据即启动成功。

## 项目结构

```
src/main/java/com/sports/sales/
├── SportsSalesApplication.java    # 启动类
├── common/                        # 统一响应封装
│   ├── Result.java                # 统一返回结果
│   └── PageResult.java            # 分页结果
├── config/                        # 配置类
│   ├── RedisConfig.java           # Redis缓存配置
│   ├── RedissonConfig.java        # Redisson分布式锁配置
│   ├── ThreadPoolConfig.java      # 线程池配置
│   ├── CorsConfig.java            # 跨域配置
│   └── HttpsConfig.java           # HTTPS配置
├── controller/                    # 控制层
│   ├── CustomerController.java    # 顾客管理
│   ├── ManufacturerController.java # 厂家管理
│   ├── ProductController.java     # 商品管理
│   ├── OrderController.java       # 订单管理
│   └── PurchaseRecordController.java # 进货管理
├── dto/                           # 数据传输对象
│   ├── OrderCreateDTO.java        # 创建订单请求
│   ├── OrderQueryDTO.java         # 订单查询参数
│   ├── ProductQueryDTO.java       # 商品查询参数
│   └── CustomerQueryDTO.java      # 顾客查询参数
├── entity/                        # 实体类
│   ├── Customer.java              # 顾客
│   ├── Manufacturer.java          # 厂家
│   ├── Product.java               # 商品
│   ├── Orders.java                # 订单
│   ├── OrderItem.java             # 订单细则
│   └── PurchaseRecord.java        # 进货记录
├── exception/                     # 异常处理
│   └── GlobalExceptionHandler.java # 全局异常处理器
├── mapper/                        # MyBatis Mapper接口
├── service/                       # 业务接口
│   └── impl/                      # 业务实现
├── task/                          # 定时任务
│   ├── StockMonitorTask.java      # 库存预警（每天8:00）
│   └── OrderTimeoutTask.java      # 订单超时检查（每30分钟）
└── util/                          # 工具类
    ├── CryptoUtil.java            # AES加解密工具
    └── KeyStoreGenerator.java     # HTTPS证书生成工具

src/main/resources/
├── application.yml                # 应用配置
├── logback-spring.xml             # 日志配置
├── keystore.p12                   # HTTPS证书
└── mapper/                        # MyBatis XML映射文件
```

## API接口

### 顾客管理 `/api/customer`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/customer/list | 顾客列表（分页） |
| GET | /api/customer/{customerCode} | 查询顾客详情 |
| POST | /api/customer | 添加顾客 |
| PUT | /api/customer | 更新顾客 |
| DELETE | /api/customer/{customerCode} | 删除顾客 |

### 厂家管理 `/api/manufacturer`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/manufacturer/list | 厂家列表 |
| GET | /api/manufacturer/{manufacturerCode} | 查询厂家详情 |
| POST | /api/manufacturer | 添加厂家 |
| PUT | /api/manufacturer | 更新厂家 |
| DELETE | /api/manufacturer/{manufacturerCode} | 删除厂家 |

### 商品管理 `/api/product`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/product/list | 商品列表（分页） |
| GET | /api/product/{productCode} | 查询商品详情 |
| POST | /api/product | 添加商品 |
| PUT | /api/product | 更新商品 |
| DELETE | /api/product/{productCode} | 删除商品 |
| GET | /api/product/low-stock | 库存预警列表 |
| POST | /api/product/replenish | 商品补货 |

### 订单管理 `/api/order`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/order/list | 订单列表（分页） |
| GET | /api/order/{orderId} | 查询订单详情 |
| POST | /api/order | 创建订单 |
| PUT | /api/order/confirm/{orderId} | 确认订单 |
| PUT | /api/order/pay/{orderId} | 订单付款 |
| PUT | /api/order/ship/{orderId} | 订单发货 |
| PUT | /api/order/complete/{orderId} | 完成订单 |
| PUT | /api/order/cancel/{orderId} | 取消订单（自动回滚库存） |
| GET | /api/order/unpaid | 未付款订单报表 |
| GET | /api/order/unshipped | 未发货订单报表 |
| GET | /api/order/completed | 已完成订单列表 |

### 进货管理 `/api/purchase`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/purchase/list | 进货记录列表 |
| GET | /api/purchase/{id} | 查询进货详情 |
| POST | /api/purchase | 添加进货记录 |
| PUT | /api/purchase/confirm/{id} | 确认进货（自动入库） |

## 创建订单示例

```json
POST /api/order
Content-Type: application/json

{
  "customerCode": "C001",
  "shippingRequirement": "标准快递",
  "recipientName": "张三",
  "recipientAddress": "北京市朝阳区xxx",
  "recipientPhone": "13800138000",
  "paymentMethod": "支付宝",
  "items": [
    {"productCode": "P001", "quantity": 2},
    {"productCode": "P003", "quantity": 1}
  ]
}
```

## 日志

日志文件位于项目上级目录的 `log/` 文件夹下：

| 文件 | 说明 |
|------|------|
| sports-sales.log | 全量日志，按天滚动，保留30天 |
| sports-sales-error.log | 错误日志，仅ERROR级别 |
| sports-sales-biz.log | 业务操作日志（订单/库存等关键操作） |

## HTTPS配置

默认启用HTTPS（443端口），HTTP（80端口）自动重定向到HTTPS。

如需关闭HTTPS，修改 `application.yml`：

```yaml
server:
  ssl:
    enabled: false
```
