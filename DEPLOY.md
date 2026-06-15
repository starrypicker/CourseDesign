# 体育用品销售系统 — CentOS 7.9 + 宝塔面板部署方案

## 一、环境概览

| 组件 | 版本 | 用途 |
|------|------|------|
| CentOS | 7.9 x64 | 操作系统 |
| 宝塔面板 | 最新稳定版 | 运维管理 |
| JDK | 17 / 21 | 运行 Spring Boot |
| MySQL | 8.0 | 数据库 |
| Redis | 7.x | 缓存 + 分布式锁 (Redisson) |
| Nginx | 1.24+ | 反向代理 + 前端静态资源 |
| Node.js | 18 LTS | 前端构建 |

---

## 二、从零开始操作步骤

### 步骤 1：安装 CentOS 7.9

```bash
# 安装完成后，以 root 登录，先更新系统
yum update -y
yum install -y wget curl net-tools vim
```

### 步骤 2：安装宝塔面板

```bash
# 官方安装脚本（CentOS 7）
yum install -y wget && wget -O install.sh https://download.bt.cn/install/install_lts.sh && sh install.sh ed8484bec
```

安装完成后会输出：
```
外网面板地址: http://你的IP:随机端口/随机路径
内网面板地址: http://你的IP:随机端口/随机路径
username: 随机用户名
password: 随机密码
```

**⚠️ 记录这些信息！** 然后用浏览器打开面板地址登录。

### 步骤 3：宝塔面板安装软件

登录宝塔面板 → 软件商店，一键安装：

#### 3.1 安装 MySQL 8.0
- 软件商店 → 搜索 `MySQL` → 选择 `MySQL 8.0` → 安装
- 安装完成后 → 数据库 → root密码 → 修改为安全密码
- 添加数据库：`数据库名: sports_sales`，`用户名: sports_sales`，`密码: 你的密码`

#### 3.2 安装 Redis
- 软件商店 → 搜索 `Redis` → 安装
- 安装完成后 → 点击 Redis → 设置 `requirepass` 密码
- 默认端口 6379，建议保持

#### 3.3 安装 Nginx
- 软件商店 → 搜索 `Nginx` → 安装 1.24+
- 默认端口 80，后续配置反向代理

#### 3.4 安装 JDK 17（手动安装，宝塔无此软件）

```bash
# SSH 连接到服务器，执行以下命令

# 下载 JDK 17（如果下载慢，可先下载到本地再上传）
cd /usr/local
wget https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz

# 解压
tar -xzf jdk-17_linux-x64_bin.tar.gz
mv jdk-17.* jdk17

# 配置环境变量
cat >> /etc/profile << 'EOF'

# JDK 17
export JAVA_HOME=/usr/local/jdk17
export PATH=$JAVA_HOME/bin:$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
EOF

# 生效
source /etc/profile

# 验证
java -version
# 输出: openjdk version "17.0.x" ...
```

#### 3.5 安装 Node.js 18（用于前端构建）

```bash
# 方式一：通过宝塔 PM2 管理器安装
# 软件商店 → 搜索 PM2 → 安装 → 自动安装 Node.js

# 方式二：手动安装
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs

# 验证
node -v   # v18.x.x
npm -v    # 9.x.x
```

### 步骤 4：导入数据库

#### 4.1 在开发机器上导出数据库

```bash
# 在本地开发环境导出
mysqldump -u root -p sports_sales > sports_sales.sql
```

#### 4.2 在服务器上导入

```bash
# 方式一：宝塔面板 → 数据库 → sports_sales → 导入 → 上传 .sql 文件

# 方式二：命令行导入
mysql -u sports_sales -p sports_sales < /path/to/sports_sales.sql
```

### 步骤 5：部署后端 (Spring Boot)

#### 5.1 打包

```bash
# 在本地项目根目录
cd "后端代码"
mvn clean package -DskipTests
# 生成的 jar 包在 target/sports-sales-*.jar
```

#### 5.2 配置生产环境

在 jar 包同级目录创建 `application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sports_sales?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: sports_sales
    password: 你的数据库密码
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  data:
    redis:
      host: localhost
      port: 6379
      password: 你的Redis密码
      database: 0

# 日志路径
logging:
  file:
    path: /www/wwwroot/sports-sales/logs
```

#### 5.3 上传并启动

```bash
# 上传 jar 包到服务器
# 方式一：宝塔面板 → 文件 → /www/wwwroot/sports-sales/ → 上传
# 方式二：使用 scp 命令
scp target/sports-sales-*.jar root@你的服务器IP:/www/wwwroot/sports-sales/

# SSH 连接服务器
ssh root@你的服务器IP

# 创建应用目录
mkdir -p /www/wwwroot/sports-sales/logs

# 测试启动（前台运行，验证配置正确）
cd /www/wwwroot/sports-sales
java -jar sports-sales-*.jar --spring.profiles.active=prod
# 看到 Started Application 则配置正确，Ctrl+C 停止

# 后台运行（使用 nohup）
nohup java -jar sports-sales-*.jar --spring.profiles.active=prod > logs/app.log 2>&1 &

# 或使用宝塔 PM2 管理器（推荐，支持开机自启和崩溃重启）
# 宝塔 → 软件商店 → PM2管理器 → 添加项目
#   启动文件: /www/wwwroot/sports-sales/sports-sales-*.jar
#   运行目录: /www/wwwroot/sports-sales
#   项目名称: sports-sales-api
#   启动参数: --spring.profiles.active=prod
```

#### 5.4 验证后端

```bash
curl http://localhost:8080/api/product/list?pageNum=1&pageSize=10
# 返回 JSON 数据表示成功
```

### 步骤 6：部署前端 (Vue 3)

#### 6.1 构建前端

```bash
# 在本地项目根目录
cd "前端代码"

# 修改 API 地址为生产环境
# 编辑 .env.production 文件（如没有则创建）
echo "VITE_API_BASE_URL=http://你的服务器IP:8080/api" > .env.production

# 构建
npm install
npm run build
# 生成 dist/ 目录
```

#### 6.2 上传到服务器

```bash
# 方式一：打包后上传
tar -czf dist.tar.gz dist/
scp dist.tar.gz root@你的服务器IP:/www/wwwroot/sports-sales/

# SSH 解压
ssh root@你的服务器IP
cd /www/wwwroot/sports-sales
tar -xzf dist.tar.gz

# 方式二：宝塔面板 → 文件 → /www/wwwroot/sports-sales/dist/ → 上传 dist 目录所有文件
```

### 步骤 7：配置 Nginx 反向代理

宝塔面板 → 网站 → 添加站点：

- **域名**：你的域名 或 服务器IP
- **根目录**：`/www/wwwroot/sports-sales/dist`
- **PHP版本**：纯静态

创建站点后 → 点击站点 → 配置文件 → 粘贴以下配置：

```nginx
server {
    listen       80;
    server_name  你的域名或IP;
    root         /www/wwwroot/sports-sales/dist;
    index        index.html;

    # 前端静态资源
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到后端
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
    }
}
```

保存配置 → 重载 Nginx。

### 步骤 8：防火墙配置

#### 服务器防火墙（宝塔面板操作）

宝塔面板 → 安全：
- 放行端口：`80`（HTTP）
- 放行端口：`443`（HTTPS，如需要）
- 放行端口：`8080`（后端直接访问，仅内网/调试用）
- 放行端口：`22`（SSH）

#### 云服务器安全组（如阿里云/腾讯云）

登录云控制台 → 安全组 → 入方向规则 → 添加：
- 端口 `80`，来源 `0.0.0.0/0`
- 端口 `443`，来源 `0.0.0.0/0`（如需要 HTTPS）
- 端口 `22`，来源 `你的IP/32`（限制 SSH 访问）

### 步骤 9：验证部署

浏览器访问 `http://你的服务器IP`：
- 应看到前端登录页面
- 登录后功能正常
- API 调用成功（打开浏览器 DevTools → Network 确认无 404/500）

### 步骤 10：配置 HTTPS（可选）

宝塔面板 → 网站 → 你的站点 → SSL → Let's Encrypt → 申请证书 → 开启强制HTTPS。

然后修改 Nginx 配置中的 API 代理，增加 WebSocket 支持（如需要）：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

---

## 三、日常运维命令

```bash
# 查看后端日志
tail -f /www/wwwroot/sports-sales/logs/app.log

# 重启后端（PM2方式）
# 宝塔 → PM2管理器 → sports-sales-api → 重启

# 重启后端（nohup方式）
ps aux | grep java
kill -9 PID
cd /www/wwwroot/sports-sales
nohup java -jar sports-sales-*.jar --spring.profiles.active=prod > logs/app.log 2>&1 &

# 查看 Redis 状态
redis-cli -a 你的密码 ping

# 查看 MySQL 状态
systemctl status mysqld

# 查看端口占用
netstat -tlnp | grep -E '80|8080|3306|6379'

# 更新前端（新版本）
# 1. 本地重新 npm run build
# 2. 上传 dist/ 覆盖 /www/wwwroot/sports-sales/dist/
# 3. 无需重启 Nginx

# 更新后端（新版本）
# 1. 本地重新 mvn package
# 2. 上传新 jar 包覆盖
# 3. PM2 → 重启 sports-sales-api
```

---

## 四、常见问题排查

| 问题 | 检查项 |
|------|--------|
| 前端页面空白 | Nginx 配置 `try_files` 是否正确；`dist/` 文件是否完整 |
| API 请求 404 | Nginx `proxy_pass` URL 末尾是否有 `/`；后端是否启动 |
| 数据库连接失败 | MySQL 是否启动；用户名密码是否正确；防火墙是否放行 3306 |
| Redis 连接失败 | Redis 是否启动；`application-prod.yml` 中密码是否正确 |
| 登录后跳转异常 | 检查前端 `VITE_API_BASE_URL` 是否正确 |
| 上传文件过大 | Nginx `client_max_body_size 50M;` |
