package com.iot.platform.mqtt;

import com.iot.platform.config.MqttConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;

/**
 * MQTT 客户端管理器
 * <p>
 * 线程安全的 MQTT 客户端封装，负责：
 * <ol>
 *     <li>与 Broker 建立连接（支持自动重连）</li>
 *     <li>订阅设备 Topic（通配符订阅所有设备消息）</li>
 *     <li>发布控制指令到指定设备</li>
 * </ol>
 * 平台作为唯一的 MQTT 客户端，代理所有设备的上下行通信。
 *
 * @author iot-platform
 */
@Slf4j
@Component
public class MqttClientManager {

    private final MqttConfig mqttConfig;
    private final MqttMessageCallback messageCallback;

    /** Paho MQTT 客户端实例（线程安全） */
    private MqttClient mqttClient;

    public MqttClientManager(MqttConfig mqttConfig, MqttMessageCallback messageCallback) {
        this.mqttConfig = mqttConfig;
        this.messageCallback = messageCallback;
    }

    /**
     * 初始化 MQTT 客户端并连接 Broker
     */
    @PostConstruct
    public void init() {
        try {
            // 使用内存持久化（平台不持久化离线消息）
            mqttClient = new MqttClient(mqttConfig.getHost(),
                    mqttConfig.getClientId(),
                    new MemoryPersistence());

            // 注册消息回调
            mqttClient.setCallback(messageCallback);

            MqttConnectOptions options = buildConnectOptions();
            log.info("正在连接 MQTT Broker: {}, clientId={}", mqttConfig.getHost(), mqttConfig.getClientId());
            mqttClient.connect(options);
            log.info("MQTT Broker 连接成功");

            // 连接成功后订阅设备主题
            subscribeDeviceTopics();
        } catch (MqttException e) {
            // 连接失败不阻断应用启动，依靠自动重连机制恢复
            log.error("MQTT 客户端初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 构建连接参数
     */
    private MqttConnectOptions buildConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(mqttConfig.getConnectionTimeout());
        options.setKeepAliveInterval(mqttConfig.getKeepAliveInterval());
        options.setCleanSession(mqttConfig.isCleanSession());
        options.setAutomaticReconnect(mqttConfig.isAutomaticReconnect());
        // 失败重连最大间隔（秒）
        options.setMaxReconnectDelay(60);
        if (mqttConfig.getUsername() != null && !mqttConfig.getUsername().isBlank()) {
            options.setUserName(mqttConfig.getUsername());
            options.setPassword(mqttConfig.getPassword().toCharArray());
        }
        return options;
    }

    /**
     * 订阅所有设备的上行 Topic
     * <p>
     * 使用通配符订阅 iot/device/# 以接收所有设备消息，
     * 再由 MqttTopicHandler 根据 Topic 细分消息类型进行路由。
     */
    public void subscribeDeviceTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("MQTT 客户端未连接，跳过订阅");
            return;
        }
        try {
            String topic = mqttConfig.getTopicPrefix();
            int qos = mqttConfig.getQos();
            mqttClient.subscribe(topic, qos);
            log.info("已订阅设备主题: {}, QoS={}", topic, qos);
        } catch (MqttException e) {
            log.error("订阅设备主题失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发布消息到指定 Topic（线程安全）
     *
     * @param topic   目标主题
     * @param payload 消息内容
     * @param qos     服务质量等级
     * @param retained 是否保留消息
     */
    public void publish(String topic, String payload, int qos, boolean retained) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                log.error("MQTT 客户端未连接，无法发布消息到 {}", topic);
                throw new IllegalStateException("MQTT 客户端未连接");
            }
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            message.setRetained(retained);
            mqttClient.publish(topic, message);
            log.debug("消息已发布: topic={}, payload={}", topic, payload);
        } catch (MqttException e) {
            log.error("发布 MQTT 消息失败: topic={}, error={}", topic, e.getMessage(), e);
            throw new RuntimeException("MQTT 消息发布失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发布消息（默认 QoS，非保留）
     */
    public void publish(String topic, String payload) {
        publish(topic, payload, mqttConfig.getQos(), false);
    }

    /**
     * 判断客户端是否已连接
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 销毁时断开 MQTT 连接
     */
    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("MQTT 客户端已断开连接");
            }
            if (mqttClient != null) {
                mqttClient.close();
            }
        } catch (MqttException e) {
            log.error("关闭 MQTT 客户端异常: {}", e.getMessage(), e);
        }
    }
}
