# EMQX HTTP 认证配置指南

本文档说明如何配置 EMQX 的 HTTP Auth / ACL 插件，对接 IoT 平台的 per-device 身份认证与权限校验。

## 一、认证流程

```
设备 --(MQTT CONNECT, clientId/username/password)--> EMQX
EMQX --(POST /iot/mqtt/auth)--> IoT 平台
IoT 平台 --({"result":"allow"} | {"result":"deny"})--> EMQX
EMQX --(CONNACK)--> 设备
```

- **username**：设备 ID（deviceId）
- **password**：设备密钥（deviceSecret，设备注册时由平台生成）
- 平台校验 device 表中 `device_id` 与 `device_secret` 是否匹配。
- 认证结果缓存至 Redis（5 分钟 TTL），降低高频回调对数据库的压力。

## 二、鉴权（ACL）流程

```
设备 --(PUBLISH/SUBSCRIBE, topic)--> EMQX
EMQX --(POST /iot/mqtt/acl)--> IoT 平台
IoT 平台 --({"result":"allow"} | {"result":"deny"})--> EMQX
```

- 设备仅允许操作自身 Topic：`iot/device/{自己的 deviceId}/#`
- 禁止发布/订阅其它设备的 Topic，防止越权。

## 三、平台侧接口

| 接口 | 方法 | 说明 |
| ---- | ---- | ---- |
| `/iot/mqtt/auth` | POST | EMQX 认证回调，请求体 `{clientid, username, password}` |
| `/iot/mqtt/acl`  | POST | EMQX ACL 回调，请求体 `{clientid, username, topic, access}` |

> 注意：context-path 为 `/iot`，因此完整路径为 `/iot/mqtt/auth`、`/iot/mqtt/acl`。
> 该组路径已在 `InterceptorConfig` 中排除 JWT 鉴权拦截，供 EMQX 直接回调。

返回体统一为：

```json
{"result": "allow"}
```

或

```json
{"result": "deny"}
```

## 四、EMQX 控制台配置（EMQX 5.x）

### 1. 启用 HTTP 认证插件

在 EMQX Dashboard → Authentication → 新建认证 → 选择 `HTTP Server`。

### 2. 认证（HTTP Auth）配置

- **Method**：`POST`
- **URL**：`http://<platform-host>:8080/iot/mqtt/auth`
- **Headers**：`Content-Type: application/json`
- **Body**（JSON 模板）：

```json
{
  "clientid": "${clientid}",
  "username": "${username}",
  "password": "${password}"
}
```

- **连接超时**：5s
- **读取超时**：5s
- 并发较高时建议开启连接池，并适当调大平台 Tomcat 线程数。

### 3. ACL（授权）配置

在 EMQX Dashboard → Authorization → 新建授权 → 选择 `HTTP Server`。

- **Method**：`POST`
- **URL**：`http://<platform-host>:8080/iot/mqtt/acl`
- **Headers**：`Content-Type: application/json`
- **Body**（JSON 模板）：

```json
{
  "clientid": "${clientid}",
  "username": "${username}",
  "topic": "${topic}",
  "access": "${action}"
}
```

> EMQX 中 `action` 取值：`publish` 或 `subscribe`，平台统一按 deviceId 前缀校验，
> 对 publish/subscribe 采用同一放行策略。

## 五、EMQX 4.x（emqx_auth_http 插件）配置

编辑 `etc/plugins/emqx_auth_http.conf`：

```ini
## 认证
auth.http.auth_req = http://<platform-host>:8080/iot/mqtt/auth
auth.http.auth_req.method = post
auth.http.auth_req.headers.content-type = application/json
auth.http.auth_req.body.clientid = ${clientid}
auth.http.auth_req.body.username = ${username}
auth.http.auth_req.body.password = ${password}

## ACL
auth.http.acl_req = http://<platform-host>:8080/iot/mqtt/acl
auth.http.acl_req.method = post
auth.http.acl_req.headers.content-type = application/json
auth.http.acl_req.body.clientid = ${clientid}
auth.http.acl_req.body.username = ${username}
auth.http.acl_req.body.topic = ${topic}
auth.http.acl_req.body.access = ${access}

## 超时与重连
auth.http.request_timeout = 5s
auth.http.request_pool_size = 32
```

启用插件：

```bash
./bin/emqx_ctl plugins load emqx_auth_http
```

## 六、安全建议

1. EMQX 与平台间通信建议走内网或启用 mTLS，防止认证报文被窃听。
2. `deviceSecret` 在平台侧以 MD5 摘要存储，请勿明文传输；生产环境建议升级为按设备签名（HMAC）认证。
3. 认证结果 Redis 缓存 TTL 为 5 分钟，设备密钥变更后最多需 5 分钟生效；如需立即失效可手动删除 `iot:mqtt:auth:*` 缓存。
4. ACL 仅按 deviceId 前缀校验，确保设备上线即使用真实 deviceId（由认证环节保证）。

## 七、验证

1. 使用任意 MQTT 客户端（如 mosquitto_pub）：
   ```bash
   mosquitto_pub -h <emqx-host> -p 1883 \
     -u device_001 -P secret_001 \
     -t iot/device/device_001/property \
     -m '{"temperature":25.5}'
   ```
   预期：认证通过，消息被平台接收。
2. 越权测试（发布到他人 Topic）：
   ```bash
   mosquitto_pub -h <emqx-host> -p 1883 \
     -u device_001 -P secret_001 \
     -t iot/device/device_002/property \
     -m '{}'
   ```
   预期：ACL 拒绝（`result: deny`），消息被丢弃。
