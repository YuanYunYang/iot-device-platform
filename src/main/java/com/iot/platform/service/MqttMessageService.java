package com.iot.platform.service;

import com.iot.platform.model.dto.MqttMessageDTO;
import com.iot.platform.model.vo.MonitorDataVO;

import java.util.List;

/**
 * MQTT 消息处理服务接口
 * <p>
 * 平台 MQTT 通信的核心业务入口，负责处理所有上行消息：
 * 设备连接、属性上报、事件上报、生命周期、心跳、控制指令响应。
 *
 * @author iot-platform
 */
public interface MqttMessageService {

    /**
     * 处理上行 MQTT 消息（统一分发入口）
     *
     * @param dto 解析后的消息对象
     */
    void handleMessage(MqttMessageDTO dto);

    /**
     * 处理设备连接消息
     *
     * @param dto 消息对象
     */
    void handleConnect(MqttMessageDTO dto);

    /**
     * 处理属性数据上报
     *
     * @param dto 消息对象
     */
    void handleProperty(MqttMessageDTO dto);

    /**
     * 处理设备事件上报
     *
     * @param dto 消息对象
     */
    void handleEvent(MqttMessageDTO dto);

    /**
     * 处理设备生命周期消息（离线/上线通知）
     *
     * @param dto 消息对象
     */
    void handleLifecycle(MqttMessageDTO dto);

    /**
     * 处理设备心跳消息
     *
     * @param dto 消息对象
     */
    void handleHeartbeat(MqttMessageDTO dto);

    /**
     * 处理控制指令响应
     *
     * @param dto 消息对象
     */
    void handleCommandResponse(MqttMessageDTO dto);

    /**
     * 查询设备历史监控数据（从 InfluxDB 读取）
     *
     * @param deviceId   设备ID
     * @param identifier 属性标识
     * @param limit      返回条数
     * @return 监控数据列表
     */
    List<MonitorDataVO> queryHistory(String deviceId, String identifier, int limit);
}
