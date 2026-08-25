package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警实体
 * <p>
 * 对应数据库 alarm 表，记录平台产生的告警事件。
 * 告警来源包括：物模型阈值触发、设备生命周期事件（离线）、自定义告警等。
 *
 * @author iot-platform
 */
@Data
@TableName("alarm")
public class Alarm implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备ID */
    private String deviceId;

    /** 产品ID */
    private Long productId;

    /** 告警等级：INFO/WARNING/CRITICAL */
    private String alarmLevel;

    /** 告警类型：THRESHOLD/LIFECYCLE/CUSTOM */
    private String alarmType;

    /** 告警标题 */
    private String alarmTitle;

    /** 告警内容 */
    private String alarmContent;

    /** 触发告警的属性标识 */
    private String propertyIdentifier;

    /** 触发告警的数值 */
    private Double alarmValue;

    /** 告警状态：ACTIVE/RESOLVED */
    private String status;

    /** 告警发生时间 */
    private LocalDateTime alarmTime;

    /** 告警解除时间 */
    private LocalDateTime resolvedTime;
}
