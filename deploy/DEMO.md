# 客户演示环境

## 访问

- PC 管理台: http://SERVER_IP/frontend/index.html
- 移动端: http://SERVER_IP/frontend/mobile.html
- API 文档: http://SERVER_IP/iot/doc.html

## 登录账号（推荐租户管理员）

| 用户名 | 密码 | 角色 |
|--------|------|------|
| tenant_admin | admin123 | 租户管理员（推荐演示） |
| admin | admin123 | 超级管理员 |
| operator | admin123 | 运维用户 |

打开页面后使用用户名/密码登录（无需再手动粘贴 JWT）。

## 演示模块

总览看板、设备管理、产品物模型、告警中心、OTA、规则引擎、能源管控、碳排放、负荷调度、用户权限。

## 运维

```bash
cd /opt/iot-platform
docker compose -f docker-compose-demo.yml ps
docker compose -f docker-compose-demo.yml logs -f app
systemctl status iot-platform
```
