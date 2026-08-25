package com.iot.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.model.entity.Alarm;
import com.iot.platform.model.enums.AlarmLevelEnum;
import com.iot.platform.repository.AlarmMapper;
import com.iot.platform.service.AlarmService;
import com.iot.platform.websocket.DeviceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警服务实现
 * <p>
 * 生成告警记录并持久化，同时通过 WebSocket 实时推送到前端。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl extends ServiceImpl<AlarmMapper, Alarm> implements AlarmService {

    private final AlarmMapper alarmMapper;
    private final DeviceWebSocketHandler webSocketHandler;

    @Override
    public Alarm createAlarm(String deviceId, Long productId, String level, String type,
                             String title, String content,
                             String propertyIdentifier, Double alarmValue) {
        Alarm alarm = new Alarm();
        alarm.setDeviceId(deviceId);
        alarm.setProductId(productId);
        alarm.setAlarmLevel(level);
        alarm.setAlarmType(type);
        alarm.setAlarmTitle(title);
        alarm.setAlarmContent(content);
        alarm.setPropertyIdentifier(propertyIdentifier);
        alarm.setAlarmValue(alarmValue);
        alarm.setStatus("ACTIVE");
        alarm.setAlarmTime(LocalDateTime.now());

        alarmMapper.insert(alarm);
        log.warn("生成告警: deviceId={}, level={}, title={}", deviceId, level, title);

        // 通过 WebSocket 实时推送告警到前端
        Map<String, Object> pushData = Map.of(
                "alarmId", alarm.getId(),
                "deviceId", deviceId,
                "level", level,
                "levelDesc", AlarmLevelEnum.of(level).getDesc(),
                "title", title,
                "content", content,
                "value", alarmValue == null ? "" : alarmValue,
                "time", alarm.getAlarmTime().toString()
        );
        webSocketHandler.sendToDevice(deviceId, "alarm", pushData);
        webSocketHandler.broadcast("alarm", pushData);

        return alarm;
    }

    @Override
    public List<Alarm> listAlarmsByDevice(String deviceId, String level, int page, int size) {
        int offset = (page - 1) * size;
        if (offset < 0) {
            offset = 0;
        }
        return alarmMapper.selectAlarmsByDeviceId(deviceId, level, offset, size);
    }

    @Override
    public List<Alarm> listRecentAlarms(int limit) {
        LambdaQueryWrapper<Alarm> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Alarm::getAlarmTime);
        wrapper.last("LIMIT " + limit);
        return alarmMapper.selectList(wrapper);
    }
}
