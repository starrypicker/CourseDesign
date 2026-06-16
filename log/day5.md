# Day5 开发日志 —— 三大新功能 & 全面修复

**日期：2026-06-15**  
**参与人员：尹领航（后端）、欧行（前端）**

---

## 一、今日目标

前四天系统基本可用了，但还缺几个重要的业务功能：
- **客服聊天**：顾客和商家之间需要实时沟通
- **发票系统**：批发订单需要开发票
- **退换货**：已签收的订单需要支持退换货
- **数据库关联问题**：多表关联导致无法删除的 bug 需要彻底解决
- **各种细节打磨**

---

## 二、尹领航 —— 后端三大新模块

### 2.1 客服聊天系统（WebSocket 实时通信）

#### 2.1.1 设计思路

传统的 HTTP 请求-响应模式不适合聊天场景——消息需要实时推送，不能让客户端轮询（浪费资源）。所以选用 **WebSocket** 协议，全双工通信，一条连接双向收发。

Spring Boot 原生支持 WebSocket，不需要额外引入 Netty 等重量级框架。

#### 2.1.2 后端架构

新增了以下文件：

| 文件 | 职责 |
|------|------|
| `WebSocketConfig.java` | 注册 WebSocket 端点 `/ws/chat`，配置握手拦截器 |
| `ChatWebSocketHandler.java` | 核心处理器：接收消息 → 存储 → 转发给目标用户 |
| `WebSocketSessionManager.java` | 会话管理器：维护在线用户 → WebSocket 连接映射 |
| `ChatHandshakeInterceptor.java` | 握手拦截器：WebSocket 握手时从请求参数中提取 Token 验证身份 |
| `ChatMessage.java` (Entity) | 聊天消息实体：发送者、接收者、角色、内容、时间 |
| `ChatMessageMapper.java/XML` | 消息持久化 Mapper |
| `ChatMessageService.java / ChatMessageServiceImpl.java` | 消息业务逻辑 |
| `ChatMessageController.java` | REST 接口：获取历史聊天记录、获取未读消息 |

#### 2.1.3 消息流转

```
顾客浏览器 ──WebSocket──→ 后端 ChatWebSocketHandler
                              │
                     ┌───────┴────────┐
                     │  存储到 MySQL   │
                     │  (chat_message) │
                     └───────┬────────┘
                              │
              ┌───────────────┴────────────────┐
              │  查找管理员在线连接              │
              │  (WebSocketSessionManager)      │
              └───────────────┬────────────────┘
                              │
              管理员浏览器 ←─WebSocket── 转发消息
```

**关键设计决策**：
- 顾客只能给管理员发消息（不能顾客互发），管理员可以给任意顾客回复
- WebSocket 连接建立时通过 `ChatHandshakeInterceptor` 验证 JWT Token，拒绝未认证的连接
- 消息 JSON 格式：`{"receiverCode": "admin", "content": "你好，我的订单什么时候发货？"}`
- 每条消息入库，即使用户离线后也能看到历史记录

#### 2.1.4 WebSocket 连接管理

```java
// WebSocketSessionManager 维护 ConcurrentHashMap
// key = "admin" 或 "C001" → value = WebSocketSession
// 管理员有多个连接时用 List<WebSocketSession> 存储
// 断开时自动移除
```

### 2.2 退换货系统

#### 2.2.1 设计思路

退换货是批发系统的重要功能。设计为以下流程：

```
顾客申请（填原因、类型：退货退款/换货）
  → 管理员审核（通过/拒绝 + 备注）
    → 通过 → 管理员确认完成 → 库存恢复 + 订单状态变更 + 退款处理
    → 拒绝 → 流程结束
```

#### 2.2.2 新增文件

| 文件 | 职责 |
|------|------|
| `ReturnRequest.java` (Entity) | 退换货实体：订单号、类型(0退货退款/1换货)、原因、状态(0待审核/1通过/2拒绝/3已完成)、退款金额、处理备注 |
| `ReturnRequestMapper.java/XML` | Mapper，含统计方法（统计进行中和已完成的申请数）|
| `ReturnRequestService.java / ReturnRequestServiceImpl.java` | 核心业务 |
| `ReturnRequestController.java` | REST API |

#### 2.2.3 关键业务逻辑

**创建退换货申请**时的校验链：

1. 校验订单是否存在且属于当前顾客
2. 已取消的订单不可申请
3. **必须已签收且已完成**的订单才能申请（`orderStatus == 2 && shippingStatus == 2`）
4. 同一订单不能有重复申请（检查进行中的申请）
5. 同一订单不能重复完成退换货（防止库存重复回滚）
6. **发票检查**：如果该订单已开票或已申请开票（且发票未作废），提示"请先联系管理员作废发票"——防止二次申请

**为什么退换货和发票要互斥？**

已开发票的订单如果退换货了，会涉及税务问题。先作废发票 → 再退换货，流程更规范。

**退款金额计算**：
- 退货退款时，默认退款金额 = 订单总金额
- 管理员审核时可以修改退款金额

**库存恢复**：
- 审核通过后，管理员"确认完成"时，将该订单的商品库存逐一恢复
- 加 Redisson 分布式锁，防止并发操作导致库存数据不一致

#### 2.2.4 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/return/list` | 退换货列表（管理员看全部，顾客只看自己的）|
| GET | `/api/return/{id}` | 查看详情 |
| POST | `/api/return` | 顾客提交退换货申请 |
| PUT | `/api/return/{id}/approve` | 管理员审核通过 |
| PUT | `/api/return/{id}/reject` | 管理员审核拒绝 |
| PUT | `/api/return/{id}/complete` | 管理员确认完成（恢复库存）|

### 2.3 发票系统

#### 2.3.1 设计思路

批发订单通常需要开具发票。设计流程：

```
顾客申请开票（发票抬头、税号等）
  → 管理员审核 → 生成发票号码 → 标记已开票
  → 顾客查看/打印
  → 支持取消（未开票时）和作废（已开票时）
```

#### 2.3.2 新增文件

| 文件 | 职责 |
|------|------|
| `Invoice.java` (Entity) | 发票实体：发票抬头、税号、金额、发票号码、状态(0待开票/1已开票/2已作废/3已取消)|
| `InvoiceMapper.java/XML` | Mapper |
| `InvoiceService.java / InvoiceServiceImpl.java` | 业务逻辑 |
| `InvoiceController.java` | REST API |

#### 2.3.3 关键设计

- 每个订单只能申请一次发票（唯一约束）
- 金额默认从订单的 `total_amount` 读取
- 已作废(`status=2`)的发票不阻止退换货
- 发票号码由管理员手动输入（模拟真实发票号），系统不做自动生成

### 2.4 数据库外键关联删除问题修复

#### 2.4.1 问题描述

之前删除厂家、商品、顾客时，经常因为外键约束报错（如：`Cannot delete or update a parent row: a foreign key constraint fails`）。错误信息直接报给前端很难看。

#### 2.4.2 解决方案

为每个删除操作增加**预先关联检查**，给用户返回友好的错误提示：

| 删除目标 | 检查的表 | 提示信息 |
|----------|----------|----------|
| 厂家 | `product`（商品表）、`purchase_record`（进货表）| "该厂家下存在关联商品，无法删除" |
| 商品 | `order_item`（订单明细）、`purchase_record`、`return_request` | "该商品存在关联订单/进货记录/退换货申请，无法删除" |
| 顾客 | `orders`（订单表）、`sys_user`、`return_request`、`invoice` | "该顾客存在关联订单，无法删除" |

**实现方式**：在 Mapper XML 中新增了 `countByCode` 方法，在 Service 层删除前先调用检查，有关联数据的返回友好提示而不是数据库异常。

- `ManufacturerServiceImpl.delete()` → 先调用 `productMapper.countByManufacturerCode()` + `purchaseRecordMapper.countByManufacturerCode()`
- `ProductServiceImpl.delete()` → 先调用 `orderMapper.countByProductCode()` + `purchaseRecordMapper.countByProductCode()`
- `CustomerServiceImpl.delete()` → 先调用 `orderMapper.countByCustomerCode()` + `invoiceMapper.countByCustomerCode()` + `returnRequestMapper.countByCustomerCode()`

### 2.5 其他后端增强

#### 2.5.1 DataInitializer 扩展

增加了发票测试数据的初始化，确保启动时有一定量的测试数据方便演示。

#### 2.5.2 定时任务增强

- `OrderTimeoutTask`：增加了更详细的日志输出，记录每次检查的订单数和取消数
- `StockMonitorTask`：输出格式改为更直观的表格，方便运维人员查看

#### 2.5.3 订单表字段扩展

`Orders.java` 新增字段：
- `returnRequested`：是否有退换货申请（方便订单列表快速判断）

#### 2.5.4 AuthController 增强

`POST /api/auth/register` 支持了更完善的注册参数校验。

---

## 三、欧行 —— 前端三大新功能页面

### 3.1 客服聊天页面

#### 3.1.1 顾客前台聊天（`views/front/Chat.vue`）

完整的聊天界面：

- **顶部**：标题 + "在线"标签
- **中间**：消息列表，自动滚到底部
  - 顾客消息（右侧气泡，蓝色）
  - 客服消息（左侧气泡，灰色）
  - 每条消息显示发送者和时间
- **底部**：输入框 + 发送按钮（Enter 发送）
- 空状态提示："暂无消息，开始和客服聊天吧！"

**技术实现**：

```javascript
// api/chat.js 封装了 WebSocket 连接
createChatSocket(token) → 返回 WebSocket 实例
// 收到消息自动滚动到底部
watch(messages, () => nextTick(() => scrollToBottom()))
// 页面卸载时关闭 WebSocket 连接
onBeforeUnmount(() => socket.close())
```

- 页面加载时先调 `getChatHistory()` 获取历史消息
- 然后建立 WebSocket 连接，实时接收新消息
- 发送的消息本地立即显示（乐观更新），不需要等服务器返回

#### 3.1.2 管理员后台聊天（`views/admin/Chat.vue`）

- 管理员可以看到所有顾客发来的消息
- 左侧顾客列表（显示有消息的顾客）
- 右侧消息区域
- 支持切换不同顾客回复

### 3.2 退换货页面

#### 3.2.1 顾客前台退换货（`views/front/Returns.vue`）

- **列表页**：申请编号、订单号、类型（退货退款/换货）、原因、状态、退款金额、管理员备注、申请时间
- 状态用 Tag 颜色区分：待审核=灰色、已通过=蓝色、已完成=绿色、已拒绝=红色
- **新建申请**：弹窗选择订单号（只显示已签收已完成的订单）、选择类型、填写原因
- **查看详情**：弹窗展示完整信息

#### 3.2.2 管理员后台退换货（`views/admin/Returns.vue`）

- 支持按状态筛选（待审核/已通过/已拒绝/已完成）
- 支持按类型筛选（退货退款/换货）
- 操作按钮按状态动态显示：
  - 待审核 → "通过" / "拒绝" 按钮
  - 已通过 → "确认完成" 按钮
  - 已完成 / 已拒绝 → 无操作按钮
- 审核弹窗：填写管理备注、修改退款金额

### 3.3 发票页面

#### 3.3.1 顾客前台发票（`views/front/Invoices.vue`）

- **列表页**：编号、关联订单号、发票抬头、金额、发票号码、状态、申请时间
- 状态：待开票(0)、已开票(1)、已作废(2)、已取消(3)
- **申请开票**：弹窗选择订单号、填写发票抬头、税号等信息
- **操作按钮**：
  - 待开票 → "取消申请"
  - 已开票 → "打印发票"（弹窗预览打印效果）
  - 已作废/已取消 → 无操作

#### 3.3.2 管理员后台发票（`views/admin/Invoices.vue`）

- 支持按状态筛选
- 待开票的申请 → "开票"按钮（弹窗输入发票号码）
- 已开票的 → "作废"按钮
- 作废时弹出确认对话框

---

## 四、欧行 —— 新增管理页面 & 功能增强

### 4.1 库存预警页面（`views/admin/LowStock.vue`）

原来库存预警只在 Dashboard 里显示一个小列表，太简陋。现在做了独立页面：

- 顶部：标题 + "刷新"按钮 + "一键检查库存"按钮
- 红色 Tag 显示库存不足的商品数量
- **低库存表格**：
  - 商品编码、商品名称、厂家、当前库存（红色/橙色）、最低库存线、单价
  - **建议补货量**列：公式 `max(0, minStock × 3 - stockQuantity)`，蓝色加粗显示
- **缺货表格**：库存为 0 的商品单独列出
- **快捷补货按钮**：点击跳转到进货管理页面，自动填入商品编码

**设计思路**：把库存预警和进货操作联动，看到预警 → 一键跳转进货，减少操作路径。

### 4.2 订单报表页面（`views/admin/Reports.vue`）

独立报表页面，用 `el-tabs` 分三个 tab：

- **已完成订单** tab：已完成订单列表，含商品明细、金额、付款/发货状态、运输信息
- **已取消订单** tab：被取消的订单汇总（含取消原因备注），方便分析取消原因
- **统计汇总** tab：
  - 总订单数、已完成订单数、取消订单数
  - 总销售额
  - 按订单状态饼状图（CSS 实现）

### 4.3 购物车增强（`views/front/Cart.vue`）

在 Day4 的购物车基础上进一步优化：

- **库存校验**：结算时检查购物车中每件商品的数量是否超过当前库存，超过则提示并限制
- **价格变动提示**：商品价格可能在加入购物车后变动，结算时对比购物车单价和当前单价，如有变动提示用户
- **空购物车建议**：购物车为空时展示热门商品推荐（从商品列表 API 中取前 4 个）
- **结算优化**：收件信息表单增加地址选择器（省/市/区三级联动），电话号码格式校验

### 4.4 订单增强

- **Orders.vue（前台）**：增加"申请发票"和"申请退换货"快捷按钮（仅对已完成的订单显示）
- **Orders.vue（后台）**：订单详情中显示关联的发票和退换货信息
- 订单状态步骤条（`el-steps`）增加了退换货状态节点

### 4.5 个人中心增强（`views/front/Profile.vue`）

- 增加"我的发票"快捷入口
- 增加"我的退换货"快捷入口
- 同时显示订单统计：总订单、待付款、已完成

### 4.6 注册表单增强（`views/login/components/RegisterForm.vue`）

- 增加手机号 11 位校验
- 增加邮箱格式校验
- 密码强度提示（弱/中/强）
- 确认密码校验

### 4.7 路由更新

新增路由：

| 路径 | 页面 | 权限 |
|------|------|------|
| `/chat` | 顾客聊天 | 顾客 |
| `/returns` | 我的退换货 | 顾客 |
| `/invoices` | 我的发票 | 顾客 |
| `/admin/chat` | 客服聊天（管理端）| 管理员 |
| `/admin/returns` | 退换货管理 | 管理员 |
| `/admin/invoices` | 发票管理 | 管理员 |
| `/admin/low-stock` | 库存预警 | 管理员 |
| `/admin/reports` | 报表 | 管理员 |

侧边栏菜单同步更新（AdminLayout.vue、FrontLayout.vue）。

---

## 五、前后端协作修复

### 5.1 发票 Bug 修复

**Bug**：退换货申请时检查发票状态逻辑有误，`InvoiceServiceImpl` 中存在空指针异常。

**修复**：
- `InvoiceServiceImpl.java`：修复 `getByOrderId()` 返回值 null 时的处理
- `ReturnRequestServiceImpl.java`：发票检查增加 null 安全判断

### 5.2 顾客删除 Bug 修复

**Bug**：删除顾客时没有检查 `sys_user` 表关联，导致用户记录残留。

**修复**：
- `CustomerServiceImpl.delete()` 增加 `sysUserMapper` 检查
- 存在用户记录的 → 同时删除用户（或提示）
- `SysUserMapper.xml` 新增查询方法

### 5.3 Vite 配置更新

- `vite.config.js` 增加了 WebSocket 代理配置，前端 WebSocket 请求 `ws://localhost:5173/ws/chat` 代理到后端 `ws://localhost:8080/ws/chat`

---

## 六、今日提交记录

| 时间 | Commit | 内容 |
|------|--------|------|
| 11:14 | `908bfc0` | 加入客服聊天（WebSocket）、发票系统、退换货系统 |
| 13:54 | `265e971` | 修复发票和退换货关联的 bug |
| 14:41 | `e3db422` | LowStock 页面、Reports 报表页面、购物车增强、订单增强、DataInitializer 扩展、定时任务增强 |
| 16:45 | `db80bbc` | 修复顾客删除时用户关联残留的 bug |
| 21:27 | `956fb45` | 解决数据库相互关联导致不能删除的问题（多表关系检查） |

---

## 七、今日小结

| 模块 | 完成内容 | 负责人 |
|------|---------|--------|
| 后端 | WebSocket 客服聊天系统（7个新文件） | 尹领航 |
| 后端 | 退换货系统（含发票联动检查、库存恢复、分布式锁） | 尹领航 |
| 后端 | 发票系统（申请→开具→作废 完整流程） | 尹领航 |
| 后端 | 数据库外键删除问题全面修复（多 Mapper 增加关联检查） | 尹领航 |
| 后端 | DataInitializer 扩展、定时任务增强 | 尹领航 |
| 前端 | 顾客聊天页面 + 管理员聊天页面（WebSocket 实时通信） | 欧行 |
| 前端 | 顾客退换货页面（申请 + 查看） | 欧行 |
| 前端 | 管理员退换货页面（审核 + 完成） | 欧行 |
| 前端 | 顾客发票页面（申请 + 打印） | 欧行 |
| 前端 | 管理员发票页面（开具 + 作废） | 欧行 |
| 前端 | 库存预警独立页面（低库存 + 缺货 + 一键补货） | 欧行 |
| 前端 | 订单报表页面（已完成/已取消/统计汇总） | 欧行 |
| 前端 | 购物车增强、订单增强、个人中心增强、注册增强 | 欧行 |
| 前后端 | 发票/退换货联动逻辑修复 | 欧行 + 尹领航 |
| 前后端 | 数据库关联删除全面修复 + Vite WebSocket 代理配置 | 欧行 + 尹领航 |

**今天是最"高产"的一天——三大新功能模块（客服聊天、发票、退换货）+ 两个独立管理页面（库存预警、报表）+ 数据库外键问题的彻底解决，系统功能更加完整。**
