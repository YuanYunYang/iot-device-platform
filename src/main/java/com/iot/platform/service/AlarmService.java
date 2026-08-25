package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.entity.Alarm;

import java.util.List;

/**
 * 告警服务接口
 * <p>
 * 提供告警记录的生成与查询能力。
 *
 * @author iot-platform
 */
public interface AlarmService extends IService<Alarm> {

    /**
     * 创建告警记录并推送
     *
     * @param deviceId           设备ID
     * @param productId          产品ID
     * @param level              告警等级：INFO/WARNING/CRITICAL
     * @param type               告警类型：THRESHOLD/LIFECYCLE/CUSTOM
     * @param title              告警标题
     * @param content            告警内容
     * @param propertyIdentifier 触发属性标识（可空）
     * @param alarmValue         触发数值（可空）
     * @return 告警实体
     */
    Alarm createAlarm(String deviceId, Long productId, String level, String type,
                      String title, String content,
                      String propertyIdentifier, Double alarmValue);

    /**
     * 按设备ID分页查询告警
     *
     * @param deviceId 设备ID
     * @param level    告警等级过滤（可空）
     * @param page     页码（从1开始）
     * @param size     每页条数
     * @return 告警列表
     */
    List<Alarm> listAlarmsByDevice(String deviceId, String level, int page, int size);

    /**
     * 查询最近告警列表
     *
     * @param limit 条数
     * @return 告警列表
     */
    List<Alarm> listRecentAlarms(int limit);
}
