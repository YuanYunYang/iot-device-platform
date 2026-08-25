package com.iot.platform.service;

import com.iot.platform.model.dto.DeviceControlDTO;

import java.util.Map;

/**
 * 设备远程控制服务接口
 * <p>
 * 负责通过 MQTT 下发控制指令到设备，并同步等待设备执行响应。
 *
 * @author iot-platform
 */
public interface DeviceControlService {

    /**
     * 下发控制指令并同步等待响应
     *
     * @param dto 控制指令请求
     * @return 设备响应内容（成功时为设备返回的数据）
     */
    Map<String, Object> sendCommand(DeviceControlDTO dto);

    /**
     * 完成指令响应（由 MqttMessageService 收到设备响应后回调）
     *
     * @param messageId 指令消息ID
     * @param response  设备响应内容
     */
    void completeCommand(String messageId, Map<String, Object> response);

    /**
     * 超时清理挂起的指令（避免内存泄漏）
     *
     * @param messageId 指令消息ID
     */
    void removePendingCommand(String messageId);
}
