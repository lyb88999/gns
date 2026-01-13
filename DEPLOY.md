# 部署指南 - Ubuntu Server

本指南将指导您将 GNS 系统部署到一台 Ubuntu 服务器上。

**前置假设**：
*   服务器已安装 Ubuntu (20.04/22.04)。
*   服务器上已安装 MySQL (3306) 和 Redis (6379)。
*   数据库账号密码与本地一致（root / Lyb1217..），数据库名 `gns` 已创建。

---

## 第一步：本地构建 (Build)

在您的开发机（Mac）上执行打包操作。

### 1. 后端打包 (Java)
```bash
# 在项目根目录执行
./mvnw clean package -DskipTests
```
*   产物位置：`target/general-notification-system-0.0.1-SNAPSHOT.jar`
*   我们要把它重命名简单一点，比如 `gns-api.jar`。

### 2. 前端打包 (Vue)
```bash
# 进入前端目录
cd frontend
npm install
npm run build
```
*   产物位置：`frontend/dist/` (这是一个文件夹，里面有 index.html 和 assets/)

---

## 第二步：服务器环境准备 (Server Setup)

登录您的 Ubuntu 服务器。

### 1. 安装 Java 17
后端需要 JDK 17 才能运行。
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
# 验证
java -version
```

### 2. 安装 Nginx
Nginx 用于托管前端静态页面，并将 API 请求转发给后端。
```bash
sudo apt install -y nginx
# 启动并设置开机自启
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 3. 创建部署目录
```bash
# 创建一个目录存放项目
sudo mkdir -p /var/www/gns/html
sudo mkdir -p /var/www/gns/backend
# 赋予权限（根据您的实际用户名，这里假设是 ubuntu）
sudo chown -R $USER:$USER /var/www/gns
```

---

## 第三步：上传文件

在您的开发机（Mac）上，使用 `scp` 命令将文件上传到服务器。
*(请将 `user@your-server-ip` 替换为您的实际服务器地址)*

```bash
# 1. 上传后端 Jar 包
scp target/general-notification-system-0.0.1-SNAPSHOT.jar user@your-server-ip:/var/www/gns/backend/gns-api.jar

# 2. 上传前端 dist 目录 (注意 -r)
scp -r frontend/dist/* user@your-server-ip:/var/www/gns/html/
```

---

## 第四步：启动后端服务

为了让 Java 程序在后台稳定运行，我们使用 `systemd`。

### 1. 创建服务文件
```bash
sudo nano /etc/systemd/system/gns-api.service
```

### 2. 粘贴以下内容
*(注意修改 `/var/www/gns/backend/gns-api.jar` 如果路径不同)*

```ini
[Unit]
Description=GNS Backend Service
After=syslog.target network.target mysql.service redis.service

[Service]
User=root
ExecStart=/usr/bin/java -jar /var/www/gns/backend/gns-api.jar
SuccessExitStatus=143
Restart=always

[Install]
WantedBy=multi-user.target
```

### 3. 启动并监控
```bash
# 重新加载配置
sudo systemctl daemon-reload
# 启动服务
sudo systemctl start gns-api
#设置开机自启
sudo systemctl enable gns-api
# 查看日志验证是否启动成功
sudo journalctl -u gns-api -f
```
*如果看到 "Started GeneralNotificationSystemApplication..." 说明启动成功。*

---

## 第五步：配置 Nginx (前端 + 反向代理)

我们需要配置 Nginx，让访问 `http://your-server-ip/` 时显示前端，访问 `/api` 时转发给后端的 8080 端口。

### 1. 修改配置
```bash
sudo nano /etc/nginx/sites-available/default
```

### 2. 替换为以下内容
*(清空原内容，粘贴这个)*

```nginx
server {
    listen 80;
    server_name _;  # 如果有域名，这里填域名

    # 1. 前端静态资源
    location / {
        root /var/www/gns/html;
        index index.html index.htm;
        try_files $uri $uri/ /index.html; # Vue Router History模式必需
    }

    # 2. 后端 API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 3. 重启 Nginx
```bash
# 检查配置语法
sudo nginx -t
# 重启
sudo systemctl restart nginx
```

---

## 第六步：验证

打开浏览器访问 `http://your-server-ip/`。

1.  应该能看到 GNS 的登录页面。
2.  输入 `admin` / `Lyb1217..`。
3.  点击登录，如果能成功跳转到仪表盘，说明：
    *   前端加载成功 (Nginx工作正常)
    *   后端接口连通 (Proxy转发正常)
    *   数据库连接正常 (Backend工作正常)

🎉 部署完成！
