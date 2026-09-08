package com.iot.platform.service;

import java.util.Map;

/**
 * MQTT 设备认证鉴权服务接口
 * <p>
 * 为 EMQX Broker 的 HTTP 认证/鉴权插件提供回调能力：
 * <ol>
 *     <li>认证（auth）：根据设备ID + 设备密钥校验设备身份</li>
 *     <li>ACL 鉴权（acl）：校验设备是否有权发布/订阅特定 Topic</li>
 * </ol>
 * 认证结果使用 Redis 缓存（5分钟TTL）以降低数据库压力。
 *
 * @author iot-platform
 */
public interface MqttAuthService {

    /**
     * EMQX 认证回调：校验设备身份
     * <p>
     * EMQX 在设备建立 MQTT 连接前调用本接口，传入 clientid、username、password。
     * 平台以 username 作为 deviceId、password 作为 deviceSecret 进行校验。
     *
     * @param params EMQX 透传的认证参数（clientid、username、password）
     * @return true-允许连接 false-拒绝连接
     */
    boolean authenticate(Map<String, Object> params);

    /**
     * EMQX ACL 回调：校验设备发布/订阅权限
     * <p>
     * 设备仅允许发布自身 Topic：iot/device/{自己的 deviceId}/#，
     * 禁止发布/订阅其它设备的 Topic，防止越权。
     *
     * @param params EMQX 透传的鉴权参数（clientid、username、topic、access）
     * @return true-允许 false-拒绝
     */
    boolean checkAcl(Map<String, Object> params);
}
