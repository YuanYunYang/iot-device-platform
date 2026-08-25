package com.iot.platform.service.impl;

import com.iot.platform.config.InfluxDBConfig;
import com.iot.platform.model.dto.MqttMessageDTO;
import com.iot.platform.model.entity.Device;
import com.iot.platform.model.entity.ThingModel;
import com.iot.platform.model.enums.AlarmLevelEnum;
import com.iot.platform.model.enums.DeviceStatusEnum;
import com.iot.platform.model.vo.MonitorDataVO;
import com.iot.platform.repository.DeviceMapper;
import com.iot.platform.repository.ThingModelMapper;
import com.iot.platform.service.AlarmService;
import com.iot.platform.service.CommandResponseHolder;
import com.iot.platform.service.DeviceService;
import com.iot.platform.service.MqttMessageService;
import com.iot.platform.service.ProductService;
import com.iot.platform.websocket.DeviceWebSocketHandler;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT 消息处理服务实现（平台核心业务）
 * <p>
 * 处理设备所有上行消息，是设备接入管控平台的核心枢纽：
 * <ol>
 *     <li>连接消息：设备上线，更新状态并缓存</li>
 *     <li>属性上报：校验物模型 -> 写入 InfluxDB -> 阈值告警判定 -> WebSocket 推送</li>
 *     <li>事件上报：设备故障等事件处理</li>
 *     <li>生命周期：上线/离线通知</li>
 *     <li>心跳：刷新心跳时间</li>
 *     <li>控制响应：回收设备指令执行结果</li>
 * </ol>
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttMessageServiceImpl implements MqttMessageService {

    private final DeviceService deviceService;
    private final AlarmService alarmService;
    private final ProductService productService;
    private final DeviceMapper deviceMapper;
    private final ThingModelMapper thingModelMapper;
    private final DeviceWebSocketHandler webSocketHandler;
    private final CommandResponseHolder commandResponseHolder;
    private final InfluxDBClient influxDBClient;
    private final InfluxDBConfig influxDBConfig;

    /**
     * 统一消息分发入口（异步执行，避免阻塞 MQTT 回调线程）
     */
    @Async
    @Override
    public void handleMessage(MqttMessageDTO dto) {
        if (dto == null || dto.getMessageType() == null) {
            return;
        }
        log.info("处理设备消息: deviceId={}, type={}", dto.getDeviceId(), dto.getMessageType());
        switch (dto.getMessageType()) {
            case "connect" -> handleConnect(dto);
            case "property" -> handleProperty(dto);
            case "event" -> handleEvent(dto);
            case "lifecycle" -> handleLifecycle(dto);
            case "heartbeat" -> handleHeartbeat(dto);
            case "command/resp", "command_resp" -> handleCommandResponse(dto);
            default -> log.warn("未知的消息类型，忽略: type={}, deviceId={}", dto.getMessageType(), dto.getDeviceId());
        }
    }

    /**
     * 处理设备连接消息
     */
    @Override
    public void handleConnect(MqttMessageDTO dto) {
        String deviceId = dto.getDeviceId();
        Map<String, Object> payload = dto.getPayload();

        Device device = deviceMapper.selectByDeviceId(deviceId);
        if (device == null) {
            log.warn("收到未注册设备的连接消息，忽略: {}", deviceId);
            return;
        }

        // 更新设备状态为在线
        String clientIp = getString(payload, "ip");
        deviceService.updateDeviceStatus(deviceId, DeviceStatusEnum.ONLINE.getCode(), clientIp);

        // 记录固件版本
        String firmware = getString(payload, "firmwareVersion");
        if (firmware != null) {
            Device update = new Device();
            update.setId(device.getId());
            update.setFirmwareVersion(firmware);
            deviceMapper.updateById(update);
        }

        log.info("设备已上线: deviceId={}, ip={}, firmware={}", deviceId, clientIp, firmware);

        // 推送上线事件到前端
        Map<String, Object> pushData = new ConcurrentHashMap<>();
        pushData.put("deviceId", deviceId);
        pushData.put("status", DeviceStatusEnum.ONLINE.getCode());
        pushData.put("statusDesc", DeviceStatusEnum.ONLINE.getDesc());
        pushData.put("time", LocalDateTime.now().toString());
        webSocketHandler.sendToDevice(deviceId, "status", pushData);
    }

    /**
     * 处理属性数据上报（核心流程）
     */
    @Override
    public void handleProperty(MqttMessageDTO dto) {
        String deviceId = dto.getDeviceId();
        Map<String, Object> payload = dto.getPayload();
        if (payload == null || payload.isEmpty()) {
            log.warn("属性上报数据为空: deviceId={}", deviceId);
            return;
        }

        Device device = deviceMapper.selectByDeviceId(deviceId);
        if (device == null) {
            log.warn("收到未注册设备的属性上报，忽略: {}", deviceId);
            return;
        }

        // 刷新心跳
        deviceService.updateHeartbeat(deviceId);

        // 获取物模型定义，用于校验与告警判定
        List<ThingModel> thingModels = thingModelMapper.selectByProductId(device.getProductId());

        // 写入 InfluxDB 并校验阈值
        writePropertyToInfluxDB(deviceId, device.getProductId(), payload, thingModels);

        // 推送实时数据到前端
        Map<String, Object> pushData = new ConcurrentHashMap<>();
        pushData.put("deviceId", deviceId);
        pushData.put("properties", payload);
        pushData.put("time", LocalDateTime.now().toString());
        webSocketHandler.sendToDevice(deviceId, "property", pushData);

        log.debug("属性数据处理完成: deviceId={}, properties={}", deviceId, payload.keySet());
    }

    /**
     * 处理设备事件上报（如故障事件）
     */
    @Override
    public void handleEvent(MqttMessageDTO dto) {
        String deviceId = dto.getDeviceId();
        Map<String, Object> payload = dto.getPayload();
        String eventType = getString(payload, "eventType");
        String eventName = getString(payload, "eventName");

        // 事件通常作为告警记录
        Device device = deviceMapper.selectByDeviceId(deviceId);
        Long productId = device == null ? null : device.getProductId();

        alarmService.createAlarm(
                deviceId, productId,
                AlarmLevelEnum.WARNING.getCode(),
                "CUSTOM",
                "设备事件: " + (eventName == null ? eventType : eventName),
                "设备上报事件: " + payload,
                eventType, null);

        log.info("设备事件已处理: deviceId={}, eventType={}", deviceId, eventType);
    }

    /**
     * 处理设备生命周期消息
     */
    @Override
    public void handleLifecycle(MqttMessageDTO dto) {
        String deviceId = dto.getDeviceId();
        Map<String, Object> payload = dto.getPayload();
        String action = getString(payload, "action");

        if ("offline".equalsIgnoreCase(action) || "disconnect".equalsIgnoreCase(action)) {
            deviceService.updateDeviceStatus(deviceId, DeviceStatusEnum.OFFLINE.getCode(), null);

            Device device = deviceMapper.selectByDeviceId(deviceId);
            Long productId = device == null ? null : device.getProductId();
            // 离线产生 INFO 级别告警
            alarmService.createAlarm(
                    deviceId, productId,
                    AlarmLevelEnum.INFO.getCode(),
                    "LIFECYCLE",
                    "设备离线",
                    "设备 " + deviceId + " 已离线",
                    null, null);

            // 推送离线状态到前端
            Map<String, Object> pushData = new ConcurrentHashMap<>();
            pushData.put("deviceId", deviceId);
            pushData.put("status", DeviceStatusEnum.OFFLINE.getCode());
            pushData.put("statusDesc", DeviceStatusEnum.OFFLINE.getDesc());
            pushData.put("time", LocalDateTime.now().toString());
            webSocketHandler.sendToDevice(deviceId, "status", pushData);

            log.info("设备已离线: deviceId={}", deviceId);
        }
    }

    /**
     * 处理设备心跳消息
     */
    @Override
    public void handleHeartbeat(MqttMessageDTO dto) {
        deviceService.updateHeartbeat(dto.getDeviceId());
        log.debug("设备心跳: deviceId={}", dto.getDeviceId());
    }

    /**
     * 处理控制指令响应（回收设备执行结果）
     */
    @Override
    public void handleCommandResponse(MqttMessageDTO dto) {
        String messageId = dto.getMessageId();
        Map<String, Object> payload = dto.getPayload();
        if (messageId == null) {
            // 尝试从 payload 获取
            messageId = getString(payload, "messageId");
        }
        if (messageId == null) {
            log.warn("控制指令响应缺少 messageId: deviceId={}", dto.getDeviceId());
            return;
        }
        boolean completed = commandResponseHolder.complete(messageId, payload);
        if (!completed) {
            log.warn("未找到对应的指令等待或已超时: messageId={}", messageId);
        }
    }

    /**
     * 查询设备历史监控数据（从 InfluxDB 读取）
     */
    @Override
    public List<MonitorDataVO> queryHistory(String deviceId, String identifier, int limit) {
        if (limit <= 0 || limit > 1000) {
            limit = 100;
        }
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: -7d) " +
                        "|> filter(fn: (r) => r._measurement == \"device_data\") " +
                        "|> filter(fn: (r) => r[\"deviceId\"] == \"%s\") " +
                        (identifier != null && !identifier.isBlank()
                                ? "|> filter(fn: (r) => r[\"_field\"] == \"" + identifier + "\") "
                                : "") +
                        "|> sort(columns: [\"_time\"], desc: true) " +
                        "|> limit(n: " + limit + ")",
                influxDBConfig.getBucket(), deviceId);

        List<MonitorDataVO> result = new ArrayList<>();
        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, influxDBConfig.getOrgName());
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    MonitorDataVO vo = new MonitorDataVO();
                    vo.setDeviceId(deviceId);
                    vo.setIdentifier(record.getField());
                    vo.setValue(record.getValue());
                    Instant time = record.getTime();
                    if (time != null) {
                        vo.setTime(LocalDateTime.ofInstant(time, ZoneOffset.of("+8")));
                    }
                    vo.setSource("history");
                    result.add(vo);
                }
            }
        } catch (Exception e) {
            log.error("查询 InfluxDB 历史数据失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
        }
        return result;
    }

    /**
     * 将属性数据写入 InfluxDB，并执行阈值告警判定
     *
     * @param deviceId     设备ID
     * @param productId    产品ID
     * @param payload      属性键值对
     * @param thingModels  物模型定义（用于阈值与单位）
     */
    private void writePropertyToInfluxDB(String deviceId, Long productId,
                                         Map<String, Object> payload, List<ThingModel> thingModels) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi()) {
            Point point = Point.measurement("device_data")
                    .addTag("deviceId", deviceId)
                    .addTag("productId", productId == null ? "unknown" : productId.toString())
                    .time(Instant.now(), WritePrecision.NS);

            // 构建物模型查找索引（按 identifier）
            Map<String, ThingModel> modelIndex = new ConcurrentHashMap<>();
            if (thingModels != null) {
                for (ThingModel model : thingModels) {
                    modelIndex.put(model.getIdentifier(), model);
                }
            }

            // 逐个属性写入并校验阈值
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                // 跳过元数据字段
                if ("messageId".equals(key) || "version".equals(key) || "timestamp".equals(key)) {
                    continue;
                }
                // 写入 InfluxDB 字段
                if (value instanceof Number) {
                    point.addField(key, ((Number) value).doubleValue());
                } else {
                    point.addField(key, String.valueOf(value));
                }

                // 阈值告警判定
                ThingModel model = modelIndex.get(key);
                if (model != null && model.getAlarmLevel() != null) {
                    checkThreshold(deviceId, productId, model, value);
                }
            }

            writeApi.writePoint(influxDBConfig.getBucket(), influxDBConfig.getOrgName(), point);
            log.debug("属性数据已写入 InfluxDB: deviceId={}", deviceId);
        } catch (Exception e) {
            log.error("写入 InfluxDB 失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 阈值告警判定
     * <p>
     * 当 maxValue 配置时，超过 maxValue 触发告警（如温度过高）；
     * 当 minValue 配置但 maxValue 为空时，低于 minValue 触发告警（如电量过低）。
     */
    private void checkThreshold(String deviceId, Long productId, ThingModel model, Object value) {
        Double numValue = toDouble(value);
        if (numValue == null) {
            return;
        }

        String level = model.getAlarmLevel();
        boolean alarm = false;
        String direction = "";

        if (model.getMaxValue() != null && numValue > model.getMaxValue()) {
            alarm = true;
            direction = "超过上限 " + model.getMaxValue();
        } else if (model.getMinValue() != null
                && (model.getMaxValue() == null)
                && numValue < model.getMinValue()) {
            alarm = true;
            direction = "低于下限 " + model.getMinValue();
        }

        if (alarm) {
            String unit = model.getUnit() == null ? "" : model.getUnit();
            String content = String.format("设备 %s 的 %s 当前值 %.2f%s，%s",
                    deviceId, model.getName(), numValue, unit, direction);
            alarmService.createAlarm(
                    deviceId, productId,
                    level,
                    "THRESHOLD",
                    "阈值告警: " + model.getName(),
                    content,
                    model.getIdentifier(),
                    numValue);
        }
    }

    /**
     * 安全地将值转为 Double
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从 Map 中安全获取字符串
     */
    private String getString(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}
