# IoT 设备接入管控平台 (IoT Device Platform)

> 面向中小企业的物联网设备接入与管控平台，支持设备快速注册、MQTT 协议接入、实时状态监控、阈值告警推送与远程控制指令下发。展示"硬件设备快速接入智能化平台，通过平台管控设备"的全链路能力。

本项目是一个 Java 开发者用于展示 IoT 行业经验的 Portfolio 项目，涵盖了物联网平台最核心的设备生命周期管理、消息通信、时序数据存储与实时推送等典型场景，代码结构清晰、注释完整、可直接编译运行。

---

## 目录

- [架构总览](#架构总览)
- [核心功能](#核心功能)
- [技术栈](#技术栈)
- [快速启动](#快速启动)
- [API 概览](#api-概览)
- [MQTT Topic 规划](#mqtt-topic-规划)
- [物模型示例](#物模型示例)
- [业务流程](#业务流程)
- [项目结构](#项目结构)
- [截图说明](#截图说明)

---

## 架构总览

平台采用分层架构，设备通过 MQTT 协议接入，平台作为 MQTT 客户端代理所有设备通信，实现数据上报、状态管控与指令下发的闭环。

```
                          ┌─────────────────────────────────────────────┐
                          │                  前端 / 客户端                 │
                          │   (REST API 请求 + WebSocket 实时订阅)         │
                          └───────────┬───────────────────┬─────────────┘
                                      │ REST               │ WebSocket 推送
                                      ▼                    ▲
┌──────────────┐   MQTT     ┌───────────────────────────────────────────┐
│   IoT 设备    │◄──────────►│          IoT 设备接入管控平台               │
│ (温湿度传感器 │  上行/下行  │  ┌─────────────┐    ┌──────────────────┐   │
│  智能开关等)  │           │  │ Controller  │    │ WebSocketHandler │   │
└──────────────┘           │  └──────┬──────┘    └────────▲─────────┘   │
        ▲                   │         ▼                    │             │
        │ 控制指令下发        │  ┌─────────────┐    ┌────────┴─────────┐   │
        │                   │  │   Service   │───►│ AlarmService 告警 │   │
        │                   │  │ (业务逻辑)   │    └──────────────────┘   │
        │                   │  └──┬───┬──┬──┘                           │
        │                   │     │   │  │   ┌────────────────┐         │
        │                   │     │   │  └──►│ MqttClientMgr  │─────────┼──► 下行指令
        │                   │     │   │      └────────────────┘         │
        │                   │     │   ▼                                 │
        │                   │     │ ┌────────────┐  ┌──────────────┐    │
        │                   │     │ │ InfluxDB   │  │   Redis      │    │
        │                   │     │ │ (时序数据)  │  │ (在线状态缓存) │    │
        │                   │     │ └────────────┘  └──────────────┘    │
        │                   │     ▼                                     │
        │                   │ ┌────────────┐                            │
        └───────────────────┤ │  MySQL     │ (设备/产品/告警关系数据)        │
                            │ └────────────┘                            │
                            └───────────────────────────────────────────┘
                                         ▲
                                         │ MQTT Broker (Mosquitto)
```

### 数据流说明

| 方向 | 通道 | 说明 |
|------|------|------|
| 设备 → 平台 | MQTT 上行 | 设备连接、属性上报、事件、心跳、指令响应 |
| 平台 → 设备 | MQTT 下行 | 控制指令下发 |
| 平台 → 前端 | WebSocket | 设备状态变更、实时属性数据、告警事件实时推送 |
| 前端 → 平台 | REST API | 设备注册、查询、控制、告警查询 |

---

## 核心功能

1. **设备接入管理**：设备注册（关联产品）、设备列表/详情查询、设备删除；设备通过 MQTT 连接后自动上线，状态实时同步至 Redis 缓存。
2. **物模型管理**：以产品为载体定义物模型（属性 property、事件 event、服务 service），设备注册时继承物模型，平台据此进行数据校验与阈值告警。
3. **数据上报处理**：设备通过 MQTT 上报属性数据，平台解析后校验物模型，写入 InfluxDB 时序存储，并通过 WebSocket 推送给前端。
4. **告警引擎**：基于物模型阈值配置（min/max + alarmLevel），上报数据超阈值自动生成告警（INFO/WARNING/CRITICAL 三级），告警实时推送前端。
5. **远程控制**：前端调用 REST API 下发控制指令，平台通过 MQTT 推送到设备，同步等待设备执行响应（默认超时 30 秒）。
6. **心跳检测**：定时任务每 60 秒扫描设备心跳，超过 90 秒未上报的设备自动置为 OFFLINE 并产生离线告警。

---

## 技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言/框架 | Java + Spring Boot | 17 / 3.2 | 核心框架 |
| 设备通信 | Eclipse Paho MQTT | 1.2.5 | MQTT 客户端，设备接入协议 |
| 关系型数据库 | MyBatis-Plus + MySQL | 3.5.5 / 8.0 | 设备/产品/告警数据存储 |
| 时序数据库 | InfluxDB 2.0 | 2.7 | 设备上报数据时序存储 |
| 实时推送 | WebSocket (Spring) | - | 设备状态实时推送到前端 |
| 缓存 | Redis | 7 | 设备在线状态、心跳缓存 |
| API 文档 | Knife4j (OpenAPI 3) | 4.5.0 | 接口文档 |
| 工具库 | Lombok / Hutool | 5.8.27 | 简化代码与工具集 |
| 容器化 | Docker Compose | - | 基础设施一键启动 |

---

## 快速启动

### 前置条件

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. 启动基础设施（一键拉起全部依赖）

```bash
cd iot-device-platform
docker-compose up -d
```

该命令会启动以下服务：

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL 8.0 | 3306 | 关系型数据库（自动执行 `init.sql` 建表与初始化数据） |
| Redis | 6379 | 缓存 |
| Mosquitto (MQTT Broker) | 1883 / 9001 | MQTT 消息代理 |
| InfluxDB 2.0 | 8086 | 时序数据库 |

验证服务状态：

```bash
docker-compose ps
```

### 2. 编译并运行平台

```bash
mvn clean package -DskipTests
java -jar target/iot-device-platform-1.0.0.jar
```

或使用 Maven 直接运行：

```bash
mvn spring-boot:run
```

### 3. 验证启动

- API 文档（Knife4j）：http://localhost:8080/iot/doc.html
- Swagger UI：http://localhost:8080/iot/swagger-ui.html

控制台输出以下信息即表示启动成功：

```
====================================================
  IoT 设备接入管控平台 启动成功
  API 文档: http://localhost:8080/iot/doc.html
  Swagger: http://localhost:8080/iot/swagger-ui.html
====================================================
```

### 4. 模拟设备接入（可选）

使用 `mosquitto_pub` / `mosquitto_sub` 命令行工具模拟设备上报：

```bash
# 模拟设备上线（向 iot/device/device_001/connect 发布连接消息）
mosquitto_pub -h localhost -p 1883 -t "iot/device/device_001/connect" \
  -m '{"ip":"192.168.1.100","firmwareVersion":"v1.0.0"}'

# 模拟设备上报属性（温度 45℃ 触发严重告警）
mosquitto_pub -h localhost -p 1883 -t "iot/device/device_001/property" \
  -m '{"temperature":45,"humidity":55,"battery":80}'

# 模拟设备心跳
mosquitto_pub -h localhost -p 1883 -t "iot/device/device_001/heartbeat" \
  -m '{"timestamp":1710000000000}'
```

---

## API 概览

所有接口统一前缀：`/iot`，统一返回 `Result` 结构 `{code, message, data, timestamp}`。

### 设备管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/iot/device/register` | 注册设备（关联产品，继承物模型） |
| GET | `/iot/device/{deviceId}` | 查询设备详情及实时状态 |
| GET | `/iot/device/list` | 分页查询设备列表（支持状态过滤） |
| DELETE | `/iot/device/{deviceId}` | 删除设备 |
| POST | `/iot/device/control` | 远程控制设备（同步等待响应） |

### 产品物模型管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/iot/product` | 创建产品 |
| GET | `/iot/product/list` | 查询产品列表 |
| GET | `/iot/product/{productKey}` | 查询产品详情 |
| POST | `/iot/product/{productId}/thing-model` | 保存物模型定义 |
| GET | `/iot/product/{productId}/thing-model` | 查询物模型定义 |

### 告警查询

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/iot/alarm/device/{deviceId}` | 按设备查询告警（支持等级过滤） |
| GET | `/iot/alarm/recent` | 查询最近告警列表 |

### 实时监控

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/iot/monitor/history/{deviceId}` | 查询设备历史监控数据（InfluxDB） |

### WebSocket 实时推送

| 端点 | 说明 |
|------|------|
| `ws://localhost:8080/iot/ws/device` | 订阅全局设备事件 |
| `ws://localhost:8080/iot/ws/device?deviceId=device_001` | 订阅指定设备的实时数据 |

推送消息格式：

```json
{
  "type": "property",
  "deviceId": "device_001",
  "data": { "properties": { "temperature": 45.0, "humidity": 55.0 } },
  "timestamp": 1710000000000
}
```

`type` 取值：`status`（状态变更）、`property`（属性数据）、`alarm`（告警事件）。

---

## MQTT Topic 规划

Topic 命名规范：`iot/device/{deviceId}/{消息类型}`

| 方向 | Topic | 说明 |
|------|-------|------|
| 上行 | `iot/device/{deviceId}/connect` | 设备上线连接，携带 IP、固件版本 |
| 上行 | `iot/device/{deviceId}/property` | 属性数据上报（温度、湿度等） |
| 上行 | `iot/device/{deviceId}/event` | 事件上报（如故障事件） |
| 上行 | `iot/device/{deviceId}/lifecycle` | 生命周期事件（上线/离线通知） |
| 上行 | `iot/device/{deviceId}/heartbeat` | 心跳上报 |
| 上行 | `iot/device/{deviceId}/command/resp` | 控制指令响应（设备执行后回复） |
| 下行 | `iot/device/{deviceId}/command` | 平台下发控制指令 |

平台订阅通配符主题 `iot/device/#` 接收所有设备上行消息，由 `MqttTopicHandler` 按 Topic 路由到对应处理器。

### 上报报文示例

**属性上报**（`iot/device/device_001/property`）：

```json
{
  "temperature": 45.0,
  "humidity": 55.0,
  "battery": 80,
  "messageId": "msg-uuid-xxx",
  "version": "v1.0.0"
}
```

**控制指令下发**（`iot/device/device_002/command`）：

```json
{
  "messageId": "cmd-uuid-xxx",
  "command": "switch",
  "params": { "state": true },
  "timestamp": 1710000000000
}
```

**控制指令响应**（`iot/device/device_002/command/resp`）：

```json
{
  "messageId": "cmd-uuid-xxx",
  "code": 0,
  "message": "success",
  "data": { "state": true }
}
```

---

## 物模型示例

物模型以产品为载体，定义设备的属性、事件、服务。以下是"温湿度传感器"产品的物模型定义 JSON（通过 `POST /iot/product/{productId}/thing-model` 保存）：

```json
[
  {
    "identifier": "temperature",
    "name": "温度",
    "type": "property",
    "dataType": "float",
    "unit": "℃",
    "minValue": -20,
    "maxValue": 40,
    "alarmLevel": "CRITICAL",
    "description": "环境温度，超过40℃触发严重告警"
  },
  {
    "identifier": "humidity",
    "name": "湿度",
    "type": "property",
    "dataType": "float",
    "unit": "%",
    "minValue": 0,
    "maxValue": 100,
    "alarmLevel": "WARNING",
    "description": "环境湿度，超过100%触发警告"
  },
  {
    "identifier": "battery",
    "name": "电量",
    "type": "property",
    "dataType": "int",
    "unit": "%",
    "minValue": 20,
    "maxValue": null,
    "alarmLevel": "WARNING",
    "description": "电池电量百分比，低于20%触发告警（min作为告警下限）"
  }
]
```

**阈值告警规则说明**：
- 当 `maxValue` 配置时，上报值超过 `maxValue` 触发告警（如温度 > 40℃）。
- 当仅配置 `minValue`（`maxValue` 为空）时，上报值低于 `minValue` 触发告警（如电量 < 20%）。
- 告警等级由 `alarmLevel` 决定（INFO/WARNING/CRITICAL）。

---

## 业务流程

### 设备接入流程

```
1. 管理员通过 REST API 注册设备（关联产品，继承物模型）
        ▼
2. 设备通过 MQTT 连接 Broker，发布 iot/device/{deviceId}/connect
        ▼
3. MqttMessageCallback 接收消息 → MqttMessageService.handleConnect
        ▼
4. 更新设备状态为 ONLINE，缓存到 Redis，记录 IP/固件版本/连接时间
        ▼
5. 通过 WebSocket 推送上线事件到前端
```

### 数据上报与告警流程

```
1. 设备上报属性 → iot/device/{deviceId}/property
        ▼
2. MqttMessageService.handleProperty 异步处理
        ▼
3. 刷新心跳 → 加载物模型定义 → 校验数据类型
        ▼
4. 写入 InfluxDB 时序存储
        ▼
5. 阈值判定：超阈值 → AlarmService 生成告警记录
        ▼
6. WebSocket 推送实时数据 + 告警事件到前端
```

### 远程控制流程

```
1. 前端 POST /iot/device/control 下发控制指令
        ▼
2. DeviceControlService 校验设备在线 → 生成 messageId
        ▼
3. 注册等待 Future 到 CommandResponseHolder
        ▼
4. 通过 MQTT 下发 iot/device/{deviceId}/command
        ▼
5. 阻塞等待响应（超时30秒）
        ▼
6. 设备执行后发布 iot/device/{deviceId}/command/resp
        ▼
7. MqttMessageService 收到响应 → holder.complete(messageId) 唤醒等待
        ▼
8. 返回设备执行结果给前端
```

---

## 项目结构

```
iot-device-platform/
├── README.md                          项目说明文档
├── pom.xml                            Maven 依赖配置
├── docker-compose.yml                 基础设施一键启动
├── .gitignore
├── src/main/java/com/iot/platform/
│   ├── IotPlatformApplication.java     启动类（启用异步与定时任务）
│   ├── config/                         配置类
│   │   ├── MqttConfig.java             MQTT 连接配置
│   │   ├── WebSocketConfig.java        WebSocket 端点配置
│   │   ├── RedisConfig.java            Redis 序列化配置
│   │   ├── KnifeConfig.java            API 文档配置
│   │   ├── InfluxDBConfig.java         InfluxDB 客户端配置
│   │   └── MyBatisMetaObjectHandler.java 自动填充时间字段
│   ├── controller/                    REST 接口层
│   ├── service/                        业务接口与实现
│   ├── repository/                    MyBatis-Plus Mapper
│   ├── model/                         数据模型（entity/dto/vo/enums）
│   ├── mqtt/                           MQTT 通信核心
│   │   ├── MqttClientManager.java      MQTT 客户端管理（线程安全）
│   │   ├── MqttTopicHandler.java       Topic 路由解析
│   │   └── MqttMessageCallback.java    消息回调处理
│   ├── websocket/                      WebSocket 实时推送
│   ├── timer/                          定时任务（心跳检测）
│   └── common/                         公共组件（统一返回/异常处理）
├── src/main/resources/
│   ├── application.yml                完整配置
│   ├── mapper/                        MyBatis XML 映射
│   └── sql/init.sql                   建表与初始数据
└── src/test/java/                     单元测试
```

---

## 截图说明

> 以下为运行效果截图占位，实际使用时替换为真实截图：

- **API 文档首页**（Knife4j）：`docs/images/api-doc.png` — 展示分组化的接口文档页面
- **设备列表接口调试**：`docs/images/device-api.png` — 设备注册与列表查询调试
- **WebSocket 实时数据推送**：`docs/images/websocket.png` — 浏览器接收到设备实时属性推送
- **告警记录查询**：`docs/images/alarm.png` — 阈值超限后的告警记录列表

可通过任意 WebSocket 客户端（如浏览器控制台）连接验证实时推送：

```javascript
let ws = new WebSocket("ws://localhost:8080/iot/ws/device?deviceId=device_001");
ws.onmessage = (event) => console.log("收到推送:", JSON.parse(event.data));
```

---

## License

MIT License - 仅供学习与 Portfolio 展示用途。
