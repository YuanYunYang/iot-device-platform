package com.iot.platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MQTT 消息 DTO
 * <p>
 * 平台解析 MQTT 报文后的统一内部消息结构，由 MqttMessageCallback 解析原始报文生成，
 * 交由 MqttMessageService 做后续业务处理（存储、告警、推送）。
 *
 * @author iot-platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private String deviceId;

    /** 消息类型：connect/property/event/lifecycle/command_resp */
    private String messageType;

    /** 原始 Topic */
    private String topic;

    /** 消息负载（解析后的键值对） */
    private Map<String, Object> payload;

    /** 消息接收时间 */
    private LocalDateTime receiveTime;

    /** 报文版本 */
    private String version;

    /** 消息ID（设备上报的唯一标识，用于指令响应匹配） */
    private String messageId;
}
