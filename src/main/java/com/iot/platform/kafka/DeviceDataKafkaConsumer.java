package com.iot.platform.kafka;

import com.iot.platform.model.dto.MqttMessageDTO;
import com.iot.platform.service.MqttMessageService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 设备数据 Kafka 消费者
 * <p>
 * 批量消费 Kafka 中的设备消息，异步处理属性上报、事件上报等，
 * 将 MQTT 消息处理与数据库写入解耦。
 * 即使数据库短暂不可用也不丢消息（Kafka 持久化 + 手动 offset 提交）。
 *
 * @author iot-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceDataKafkaConsumer {

    private final MqttMessageService mqttMessageService;
    private final MeterRegistry meterRegistry;

    /**
     * 批量消费设备数据
     */
    @KafkaListener(topics = "iot-device-data", groupId = "iot-platform-group")
    public void handleDeviceData(List<MqttMessageDTO> messages, Acknowledgment ack) {
        log.debug("批量消费 {} 条设备消息", messages.size());

        Counter counter = Counter.builder("iot.kafka.consumed.total")
                .tag("topic", "iot-device-data")
                .register(meterRegistry);

        try {
            for (MqttMessageDTO message : messages) {
                // 异步处理：写入 InfluxDB + 触发告警 + WebSocket 推送
                mqttMessageService.handleMessage(message);
                counter.increment();
            }
            // 手动提交 offset
            ack.acknowledge();
        } catch (Exception e) {
            log.error("批量消费设备数据失败, 消息数={}", messages.size(), e);
            // 不提交 offset，Kafka 会重试
        }
    }
}
