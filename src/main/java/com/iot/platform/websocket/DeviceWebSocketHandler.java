package com.iot.platform.websocket;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 设备 WebSocket 推送处理器
 * <p>
 * 维护前端订阅会话，将设备状态变更、实时数据、告警事件实时推送到前端。
 * <p>
 * 订阅模型：前端连接时通过查询参数 ?deviceId=xxx 指定订阅设备，
 * 未指定 deviceId 则订阅全局事件（如所有设备状态变更）。
 *
 * @author iot-platform
 */
@Slf4j
@Component
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    /** 全局订阅会话（未指定 deviceId） */
    private final Set<WebSocketSession> globalSessions = new CopyOnWriteArraySet<>();

    /** 按设备ID分组的订阅会话 */
    private final Map<String, Set<WebSocketSession>> deviceSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String deviceId = extractDeviceId(session);
        if (deviceId != null) {
            deviceSessions.computeIfAbsent(deviceId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("前端订阅设备 [{}] 实时数据, sessionId={}", deviceId, session.getId());
        } else {
            globalSessions.add(session);
            log.info("前端订阅全局设备事件, sessionId={}", session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 暂不处理前端上行消息，仅做日志
        log.debug("收到前端 WebSocket 消息: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String deviceId = extractDeviceId(session);
        if (deviceId != null) {
            Set<WebSocketSession> sessions = deviceSessions.get(deviceId);
            if (sessions != null) {
                sessions.remove(session);
                log.info("前端取消订阅设备 [{}], sessionId={}", deviceId, session.getId());
            }
        } else {
            globalSessions.remove(session);
            log.info("前端断开全局订阅, sessionId={}", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输异常, sessionId={}, error={}", session.getId(), exception.getMessage());
    }

    /**
     * 向订阅了指定设备的前端推送消息
     *
     * @param deviceId 设备ID
     * @param type     消息类型：status/property/alarm
     * @param data     推送数据
     */
    public void sendToDevice(String deviceId, String type, Object data) {
        String json = buildMessage(type, deviceId, data);
        // 推送给订阅该设备的会话
        Set<WebSocketSession> sessions = deviceSessions.get(deviceId);
        if (sessions != null) {
            sendMessageToSessions(sessions, json);
        }
        // 同时推送给全局订阅会话
        sendMessageToSessions(globalSessions, json);
    }

    /**
     * 向所有前端推送全局消息
     *
     * @param type 消息类型
     * @param data 推送数据
     */
    public void broadcast(String type, Object data) {
        String json = buildMessage(type, null, data);
        sendMessageToSessions(globalSessions, json);
        deviceSessions.values().forEach(sessions -> sendMessageToSessions(sessions, json));
    }

    /**
     * 构建推送 JSON 报文
     */
    private String buildMessage(String type, String deviceId, Object data) {
        Map<String, Object> message = new ConcurrentHashMap<>();
        message.put("type", type);
        message.put("deviceId", deviceId);
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());
        return JSONUtil.toJsonStr(message);
    }

    /**
     * 向一组会话发送消息
     */
    private void sendMessageToSessions(Set<WebSocketSession> sessions, String json) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("WebSocket 消息推送失败, sessionId={}, error={}", session.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * 从会话 URI 中提取 deviceId 查询参数
     */
    private String extractDeviceId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        String query = uri.getQuery();
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if ("deviceId".equals(kv[0]) && kv.length == 2) {
                return kv[1];
            }
        }
        return null;
    }

    /**
     * 获取当前在线会话总数（监控用）
     */
    public int getSessionCount() {
        int count = globalSessions.size();
        for (Set<WebSocketSession> sessions : deviceSessions.values()) {
            count += sessions.size();
        }
        return count;
    }
}
