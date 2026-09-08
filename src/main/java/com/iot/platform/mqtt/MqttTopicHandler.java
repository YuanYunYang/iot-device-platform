package com.iot.platform.mqtt;

import com.iot.platform.service.OtaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MQTT 主题路由处理器
 * <p>
 * 解析设备上报的 Topic，提取设备ID与消息类型，交由业务层分发处理。
 * <p>
 * Topic 规划：
 * <pre>
 * iot/device/{deviceId}/connect          设备上线连接
 * iot/device/{deviceId}/property          属性数据上报
 * iot/device/{deviceId}/event             事件上报（如故障）
 * iot/device/{deviceId}/lifecycle         生命周期（上线/离线）
 * iot/device/{deviceId}/heartbeat         心跳
 * iot/device/{deviceId}/command           平台下发控制指令（下行）
 * iot/device/{deviceId}/command/resp      设备控制指令响应（上行）
 * iot/device/{deviceId}/ota/command       平台下发 OTA 升级指令（下行）
 * iot/device/{deviceId}/ota/progress      设备上报 OTA 升级进度（上行）
 * </pre>
 *
 * @author iot-platform
 */
@Slf4j
@Component
public class MqttTopicHandler {

    /** Topic 前缀 */
    private static final String TOPIC_PREFIX = "iot/device/";

    /**
     * OTA 服务（懒加载，打破与 MQTT 客户端之间的构造期循环依赖）。
     * <p>
     * OTA 进度上报由本处理器的 {@link #handleOtaProgress} 分支处理，
     * 而 OTA 服务又依赖 MqttClientManager 下发指令，形成环路，故使用 {@code @Lazy}。
     */
    @Autowired
    @Lazy
    private OtaService otaService;

    /**
     * 解析 Topic，提取设备ID
     *
     * @param topic 原始 Topic
     * @return 设备ID，解析失败返回 null
     */
    public String parseDeviceId(String topic) {
        if (topic == null || !topic.startsWith(TOPIC_PREFIX)) {
            log.warn("非法的设备 Topic，无法解析设备ID: {}", topic);
            return null;
        }
        // 去掉前缀 "iot/device/"
        String remain = topic.substring(TOPIC_PREFIX.length());
        int slashIndex = remain.indexOf('/');
        if (slashIndex <= 0) {
            log.warn("Topic 缺少设备ID或消息类型: {}", topic);
            return null;
        }
        return remain.substring(0, slashIndex);
    }

    /**
     * 解析 Topic，提取消息类型
     *
     * @param topic 原始 Topic
     * @return 消息类型，如 property、command/resp，解析失败返回 null
     */
    public String parseMessageType(String topic) {
        if (topic == null || !topic.startsWith(TOPIC_PREFIX)) {
            return null;
        }
        String remain = topic.substring(TOPIC_PREFIX.length());
        int slashIndex = remain.indexOf('/');
        if (slashIndex < 0 || slashIndex == remain.length() - 1) {
            return null;
        }
        return remain.substring(slashIndex + 1);
    }

    /**
     * 构建下行控制指令 Topic
     *
     * @param deviceId 设备ID
     * @return iot/device/{deviceId}/command
     */
    public String buildCommandTopic(String deviceId) {
        return TOPIC_PREFIX + deviceId + "/command";
    }

    /**
     * 构建设备控制响应 Topic（设备订阅接收）
     *
     * @param deviceId 设备ID
     * @return iot/device/{deviceId}/command/resp
     */
    public String buildCommandRespTopic(String deviceId) {
        return TOPIC_PREFIX + deviceId + "/command/resp";
    }

    /**
     * 构建 OTA 升级指令下行 Topic
     *
     * @param deviceId 设备ID
     * @return iot/device/{deviceId}/ota/command
     */
    public String buildOtaCommandTopic(String deviceId) {
        return TOPIC_PREFIX + deviceId + "/ota/command";
    }

    /**
     * 构建 OTA 进度上报上行 Topic
     *
     * @param deviceId 设备ID
     * @return iot/device/{deviceId}/ota/progress
     */
    public String buildOtaProgressTopic(String deviceId) {
        return TOPIC_PREFIX + deviceId + "/ota/progress";
    }

    /**
     * 处理设备上报的 OTA 升级进度（消息处理分支）
     * <p>
     * 当解析到消息类型为 {@code ota/progress} 时调用，委托 OTA 服务更新进度与任务统计。
     *
     * @param deviceId 设备ID
     * @param payload  上报负载（taskId、status、errorMsg 等）
     */
    public void handleOtaProgress(String deviceId, Map<String, Object> payload) {
        try {
            otaService.handleDeviceProgress(deviceId, payload);
        } catch (Exception e) {
            log.error("处理 OTA 进度上报异常: deviceId={}, error={}", deviceId, e.getMessage(), e);
        }
    }
}
