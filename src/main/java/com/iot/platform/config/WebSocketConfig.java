package com.iot.platform.config;

import com.iot.platform.websocket.DeviceWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 * <p>
 * 注册设备状态推送的 WebSocket 端点，前端通过该端点订阅设备实时状态变更。
 * 端点路径：ws://host:port/iot/ws/device
 *
 * @author iot-platform
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /** WebSocket 端点路径 */
    public static final String DEVICE_WS_PATH = "/ws/device";

    private final DeviceWebSocketHandler deviceWebSocketHandler;

    public WebSocketConfig(DeviceWebSocketHandler deviceWebSocketHandler) {
        this.deviceWebSocketHandler = deviceWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册设备状态推送处理器，允许所有源跨域访问
        registry.addHandler(deviceWebSocketHandler, DEVICE_WS_PATH)
                .setAllowedOrigins("*");
    }
}
