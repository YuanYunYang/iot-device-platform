package com.iot.platform.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.iot.platform.model.dto.DeviceControlDTO;
import com.iot.platform.model.entity.Device;
import com.iot.platform.model.enums.DeviceStatusEnum;
import com.iot.platform.mqtt.MqttClientManager;
import com.iot.platform.mqtt.MqttTopicHandler;
import com.iot.platform.repository.DeviceMapper;
import com.iot.platform.service.CommandResponseHolder;
import com.iot.platform.service.DeviceControlService;
import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 设备远程控制服务实现
 * <p>
 * 通过 MQTT 向设备下发控制指令，并同步等待设备执行响应（默认超时 30 秒）。
 * <p>
 * 同步等待机制：
 * <ol>
 *     <li>生成 messageId，将等待 Future 注册到 CommandResponseHolder</li>
 *     <li>通过 MQTT 下发指令到 iot/device/{deviceId}/command</li>
 *     <li>阻塞等待 Future 完成，设备执行后通过 command/resp 上报结果</li>
 *     <li>MqttMessageService 收到响应后调用 holder.complete() 唤醒等待</li>
 * </ol>
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceControlServiceImpl implements DeviceControlService {

    private final MqttClientManager mqttClientManager;
    private final MqttTopicHandler topicHandler;
    private final DeviceMapper deviceMapper;
    private final CommandResponseHolder commandResponseHolder;

    /** 控制指令响应超时时间（秒），从配置读取 */
    @Value("${iot.control.response-timeout:30}")
    private int responseTimeout;

    @Override
    public Map<String, Object> sendCommand(DeviceControlDTO dto) {
        String deviceId = dto.getDeviceId();

        // 校验设备存在
        Device device = deviceMapper.selectByDeviceId(deviceId);
        if (device == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.DEVICE_NOT_FOUND, "设备不存在: " + deviceId);
        }

        // 校验设备在线
        if (!DeviceStatusEnum.ONLINE.getCode().equalsIgnoreCase(device.getStatus())) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.DEVICE_OFFLINE, "设备未在线，当前状态: " + device.getStatus());
        }

        // 生成指令消息ID
        String messageId = IdUtil.fastSimpleUUID();

        // 构建指令报文
        Map<String, Object> commandPayload = new HashMap<>();
        commandPayload.put("messageId", messageId);
        commandPayload.put("command", dto.getCommand());
        commandPayload.put("params", dto.getParams());
        commandPayload.put("timestamp", System.currentTimeMillis());

        // 注册等待 Future
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        commandResponseHolder.register(messageId, future);

        // 下发指令
        String topic = topicHandler.buildCommandTopic(deviceId);
        String payload = JSONUtil.toJsonStr(commandPayload);
        try {
            mqttClientManager.publish(topic, payload);
            log.info("控制指令已下发: deviceId={}, command={}, messageId={}", deviceId, dto.getCommand(), messageId);
        } catch (Exception e) {
            commandResponseHolder.remove(messageId);
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.MQTT_ERROR, "指令下发失败: " + e.getMessage());
        }

        // 同步等待响应
        try {
            Map<String, Object> response = future.get(responseTimeout, TimeUnit.SECONDS);
            log.info("控制指令响应已收到: deviceId={}, messageId={}", deviceId, messageId);
            if (response == null) {
                response = Map.of("result", "success");
            }
            return response;
        } catch (TimeoutException e) {
            commandResponseHolder.remove(messageId);
            log.error("控制指令响应超时: deviceId={}, messageId={}", deviceId, messageId);
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.CONTROL_TIMEOUT, "设备控制指令响应超时（" + responseTimeout + "秒）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            commandResponseHolder.remove(messageId);
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FAILED, "指令等待被中断");
        } catch (ExecutionException e) {
            commandResponseHolder.remove(messageId);
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FAILED, "指令执行异常: " + e.getMessage());
        }
    }

    @Override
    public void completeCommand(String messageId, Map<String, Object> response) {
        commandResponseHolder.complete(messageId, response);
    }

    @Override
    public void removePendingCommand(String messageId) {
        commandResponseHolder.remove(messageId);
    }
}
