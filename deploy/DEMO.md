# 演示环境快速说明（服务器路径：/opt/iot-platform）

## 启动 / 停止

```bash
cd /opt/iot-platform
docker compose -f docker-compose-demo.yml up -d
docker compose -f docker-compose-demo.yml ps
docker compose -f docker-compose-demo.yml logs -f app
```

重启后由 systemd 单元 `iot-platform.service` 自动拉起。

## 访问地址

将 `SERVER_IP` 替换为公网 IP：

- PC 管理台: http://SERVER_IP/frontend/index.html
- 移动端 H5: http://SERVER_IP/frontend/mobile.html
- API 文档: http://SERVER_IP/iot/doc.html
- 健康检查: http://SERVER_IP/iot/actuator/health

## 演示账号

- 用户名: `admin`
- 密码: `admin123`

PC 页右上角点「登录」即可获取 JWT 并加载数据。

## 组件

MySQL / Redis / Mosquitto / InfluxDB / Kafka / App / Nginx（针对约 2G 内存轻量编排）
