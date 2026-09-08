package com.iot.platform.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 规则评估结果（Drools 规则触发后产生）
 * <p>
 * 规则在 RHS（then）中构造本对象并写入 {@link DevicePropertyFact#getResults()}，
 * 由规则引擎服务统一回收并调用 {@code AlarmService.createAlarm} 生成告警。
 *
 * @author iot-platform
 */
@Data
public class AlarmResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private String deviceId;

    /** 告警等级：INFO/WARNING/CRITICAL */
    private String level;

    /** 告警类型：THRESHOLD/COMPOSITE/TIME_WINDOW/CUSTOM */
    private String type;

    /** 告警标题 */
    private String title;

    /** 告警内容 */
    private String content;

    /** 触发属性标识 */
    private String propertyIdentifier;

    /** 触发数值 */
    private Double alarmValue;
}
