<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?logo=java" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-green?logo=springboot" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/MQTT-EMQX-blue?logo=eclipsemosquitto" alt="MQTT"/>
  <img src="https://img.shields.io/badge/Kafka-3.x-red?logo=apachekafka" alt="Kafka"/>
  <img src="https://img.shields.io/badge/Drools-8.44-purple" alt="Drools"/>
  <img src="https://img.shields.io/badge/Docker-K8s-blue?logo=docker" alt="Docker"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License"/>
</p>

# IoT 设备接入管控平台

面向企业的 **SaaS 级物联网设备接入与管控平台**：多租户隔离、MQTT 接入、Kafka 削峰、规则引擎告警、OTA 升级、能源与碳排管理。适合二次开发、私有化部署与商业定制。

---

## 商业合作 / 定制开发

本项目开源核心能力，同时提供企业级定制与交付服务：

- 私有化部署与生产环境落地
- 多租户 / 多园区 / 行业场景定制
- 设备协议对接、规则引擎与看板定制
- 能源管理、碳核算、负荷调度等扩展模块
- 性能调优、高可用与运维支持

**商务咨询 QQ：`2448041958`**  
添加好友请备注来意（如：定制开发 / 私有化部署 / 合作咨询）。

---

## 核心能力

| 能力 | 实现 | 说明 |
|------|------|------|
| 多租户隔离 | MyBatis-Plus TenantLine + `tenant_id` | 行级隔离，SQL 自动注入租户条件 |
| 消息削峰 | Kafka 异步消费 | 设备数据先入 Kafka 再落库 |
| 读写分离 | dynamic-datasource | 读从 / 写主 |
| 水平扩展 | K8s HPA + MQTT 共享订阅 | 支持多实例扩缩容 |
| 安全防护 | JWT + CORS + XSS + 限流 | 接口鉴权与防刷 |
| 规则引擎 | Drools 热加载 | 告警与能源规则可动态更新 |
| OTA 升级 | 固件 / 任务 / 设备进度 | MQTT 下发与进度回传 |
| 能源碳排 | 峰谷平、Scope1/2、负荷调度 | 报表与导出 |
| 可观测性 | Actuator + Prometheus | JVM / 接口 / 业务指标 |

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 · Spring Boot 3.2.5 · MyBatis-Plus |
| 消息 | MQTT (EMQX / Mosquitto) · Kafka |
| 存储 | MySQL 8 · Redis · InfluxDB 2 |
| 规则 | Drools 8.44 |
| 前端 | 纯 HTML/CSS/JS 管理台 + 移动端 H5 |
| 部署 | Docker · docker-compose · Kubernetes |

---

## 快速开始

### 1. 准备环境变量

```bash
cp .env.example .env
# 编辑 .env，修改 DB / Redis / JWT / Influx 等密钥
```

### 2. 启动基础设施

```bash
# 轻量本地依赖（MySQL / Redis / MQTT / InfluxDB）
docker compose --env-file .env up -d

# 或完整开发编排（含 Kafka、应用容器、Nginx）
docker compose -f docker-compose.dev.yml --env-file .env up -d
```

### 3. 初始化数据库

仓库仅保留 **表结构**，不含业务演示数据。

```bash
# Flyway 启动时会自动执行 V1 迁移；也可手动执行：
mysql -u root -p < src/main/resources/sql/init.sql

# 创建首个租户与超级管理员（示例脚本，请先阅读并修改）
mysql -u root -p < src/main/resources/sql/bootstrap_admin.sql.example
```

示例管理员账号（**务必登录后立即修改**）：

| 用户名 | 初始密码 |
|--------|----------|
| `admin` | `ChangeMe@2026` |

### 4. 启动后端

```bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5. 访问

| 服务 | 地址 |
|------|------|
| API | http://localhost:8080 |
| API 文档 | http://localhost:8080/doc.html |
| PC 管理台 | http://localhost:8080/frontend/index.html |
| 移动端 H5 | http://localhost:8080/frontend/mobile.html |

---

## 生产部署

```bash
# 镜像构建
docker build -t iot-platform:1.0 .

# 运行（密钥全部走环境变量）
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=mysql-host \
  -e DB_PASSWORD=*** \
  -e REDIS_HOST=redis-host \
  -e REDIS_PASSWORD=*** \
  -e KAFKA_SERVERS=kafka:9092 \
  -e MQTT_HOST=tcp://emqx:1883 \
  -e JWT_SECRET=*** \
  -e INFLUX_TOKEN=*** \
  iot-platform:1.0
```

Kubernetes：

```bash
# 先编辑 k8s/deployment.yaml 中 Secret 占位符
kubectl apply -f k8s/deployment.yaml
```

生产编排：`docker compose -f docker-compose-prod.yml --env-file .env up -d`（要求 `.env` 中已设置全部密钥）。

---

## API 概览

| 模块 | 路径 |
|------|------|
| 设备管理 | `/api/device` |
| 用户认证 | `/api/auth` |
| 告警管理 | `/api/alarm` |
| 产品管理 | `/api/product` |
| 监控数据 | `/api/monitor` |
| MQTT 认证 | `/iot/mqtt` |
| OTA 升级 | `/api/ota` |
| 规则引擎 | `/api/rule` |
| 能源 / 碳排 / 负荷 | `/api/energy/*` · `/api/carbon/*` · `/api/load` |
| 数据导出 | `/api/export` |

完整 OpenAPI 以运行后的 Knife4j 为准。

---

## MQTT Topic

| Topic | 方向 | 说明 |
|-------|------|------|
| `iot/device/{deviceId}/property/post` | 设备→平台 | 属性上报 |
| `iot/device/{deviceId}/command` | 平台→设备 | 指令下发 |
| `iot/device/{deviceId}/event` | 设备→平台 | 事件上报 |
| `iot/device/{deviceId}/ota/command` | 平台→设备 | OTA 指令 |
| `iot/device/{deviceId}/ota/progress` | 设备→平台 | OTA 进度 |
| `iot/device/{deviceId}/energy` | 设备→平台 | 能源数据 |

设备认证说明见 [docs/emqx-auth-setup.md](docs/emqx-auth-setup.md)。

---

## 项目结构

```
iot-device-platform/
├── src/main/java/com/iot/platform/   # 核心业务代码
├── src/main/resources/
│   ├── db/migration/                 # Flyway 表结构
│   ├── sql/                          # init + 管理员引导示例
│   ├── rules/                        # Drools 规则
│   └── application*.yml
├── frontend/                         # PC / 移动端界面
├── k8s/                              # Kubernetes 清单
├── deploy/                           # Nginx / Mosquitto 配置
├── docker-compose.yml                # 本地基础设施
├── docker-compose.dev.yml            # 开发编排
├── docker-compose-prod.yml           # 生产编排
├── .env.example                      # 环境变量模板
└── pom.xml
```

---

## 安全说明

- 仓库 **不包含** 业务演示种子数据与真实环境密钥
- 请使用 `.env` / K8s Secret / 密钥托管注入配置，勿提交真实口令
- 首次创建的管理员请立即修改密码
- JWT Secret 请使用足够长的随机字符串（≥ 32 字节）

---

## 贡献

欢迎提交 Issue 与 PR。提交前请确保 `mvn -q -DskipTests compile` 通过。

## License

MIT License
