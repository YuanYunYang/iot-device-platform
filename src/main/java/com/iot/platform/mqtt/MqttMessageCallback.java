package com.iot.platform.mqtt;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.iot.platform.model.dto.MqttMessageDTO;
import com.iot.platform.service.MqttMessageService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MQTT 消息回调处理器
 * <p>
 * 实现 Paho 的 MqttCallbackExtended 接口，处理：
 * <ul>
 *     <li>messageArrived：解析上行消息并路由到业务服务</li>
 *     <li>connectionLost：连接断开通知</li>
 *     <li>connectComplete：连接建立（含重连）后重新订阅</li>
 * </ul>
 *
 * @author iot-platform
 */
@Slf4j
@Component
public class MqttMessageCallback implements MqttCallbackExtended {

    private final MqttTopicHandler topicHandler;
    private final MqttMessageService messageService;
    private final MqttClientManager clientManager;

    /**
     * 构造函数。
     * <p>
     * 注意：{@code MqttClientManager} 与本回调存在相互依赖（Manager 注入 Callback 作为消息回调，
     * Callback 在重连时调用 Manager 重新订阅），为打破 Spring 构造期循环依赖，
     * 此处对 {@code MqttClientManager} 使用 {@code @Lazy} 注入代理。
     */
    @Autowired
    public MqttMessageCallback(MqttTopicHandler topicHandler,
                               MqttMessageService messageService,
                               @Lazy MqttClientManager clientManager) {
        this.topicHandler = topicHandler;
        this.messageService = messageService;
        this.clientManager = clientManager;
    }

    /**
     * 收到消息时回调
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.debug("收到 MQTT 消息: topic={}, payload={}", topic, payload);

        String deviceId = topicHandler.parseDeviceId(topic);
        String messageType = topicHandler.parseMessageType(topic);

        if (deviceId == null || messageType == null) {
            log.warn("无法解析 Topic，丢弃消息: {}", topic);
            return;
        }

        try {
            // 解析 JSON 负载
            Map<String, Object> payloadMap = parsePayload(payload);

            MqttMessageDTO dto = MqttMessageDTO.builder()
                    .deviceId(deviceId)
                    .messageType(messageType)
                    .topic(topic)
                    .payload(payloadMap)
                    .receiveTime(LocalDateTime.now())
                    .messageId(getValue(payloadMap, "messageId", String.class))
                    .version(getValue(payloadMap, "version", String.class))
                    .build();

            // 交由业务服务异步处理，避免阻塞 MQTT 回调线程
            messageService.handleMessage(dto);
        } catch (Exception e) {
            log.error("处理 MQTT 消息异常: topic={}, payload={}, error={}", topic, payload, e.getMessage(), e);
        }
    }

    /**
     * 连接断开回调
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT 连接断开: {}", cause.getMessage(), cause);
        // 依靠 Paho 自动重连机制恢复连接
    }

    /**
     * 消息投递完成回调
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("消息投递完成: {}", token.isComplete());
    }

    /**
     * 连接建立完成回调（含重连）
     * <p>
     * 重连后需要重新订阅设备主题，因为 cleanSession 可能丢失订阅。
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            log.info("MQTT 重连成功，重新订阅设备主题: {}", serverURI);
            clientManager.subscribeDeviceTopics();
        } else {
            log.info("MQTT 首次连接成功: {}", serverURI);
        }
    }

    /**
     * 解析消息负载为 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            JSONObject json = JSONUtil.parseObj(payload);
            // 转换为普通 Map 便于后续业务处理
            return json.toBean(Map.class);
        } catch (Exception e) {
            log.warn("消息负载非 JSON 格式，按原始文本处理: {}", payload);
            return Map.of("raw", payload);
        }
    }

    /**
     * 从 Map 中取值并做类型转换
     */
    private <T> T getValue(Map<String, Object> map, String key, Class<T> clazz) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        try {
            return clazz.cast(value);
        } catch (ClassCastException e) {
            return clazz.cast(String.valueOf(value));
        }
    }
}
