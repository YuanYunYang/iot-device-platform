package com.iot.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指令响应持有器
 * <p>
 * 解耦设备控制指令的下发与响应回收：
 * <ul>
 *     <li>DeviceControlService 下发指令时，以 messageId 注册一个等待 Future</li>
 *     <li>MqttMessageService 收到设备控制响应时，按 messageId 完成对应 Future</li>
 * </ul>
 * 使用 ConcurrentHashMap 保证多线程（MQTT 回调线程 vs REST 请求线程）下的线程安全。
 *
 * @author iot-platform
 */
@Slf4j
@Component
public class CommandResponseHolder {

    /** 挂起的指令响应 Future，key=messageId */
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingFutures = new ConcurrentHashMap<>();

    /**
     * 注册等待响应的 Future
     *
     * @param messageId 指令消息ID
     * @param future    等待 Future
     */
    public void register(String messageId, CompletableFuture<Map<String, Object>> future) {
        pendingFutures.put(messageId, future);
        log.debug("注册指令响应等待: messageId={}", messageId);
    }

    /**
     * 完成指令响应
     *
     * @param messageId 指令消息ID
     * @param response  设备响应内容
     * @return true 表示存在对应的等待 Future 且成功完成
     */
    public boolean complete(String messageId, Map<String, Object> response) {
        CompletableFuture<Map<String, Object>> future = pendingFutures.remove(messageId);
        if (future != null && !future.isDone()) {
            future.complete(response);
            log.debug("指令响应已完成: messageId={}", messageId);
            return true;
        }
        log.debug("未找到对应的指令等待: messageId={}", messageId);
        return false;
    }

    /**
     * 移除挂起的指令（超时或取消时调用）
     *
     * @param messageId 指令消息ID
     */
    public void remove(String messageId) {
        pendingFutures.remove(messageId);
    }

    /**
     * 获取当前挂起指令数量
     */
    public int getPendingCount() {
        return pendingFutures.size();
    }
}
