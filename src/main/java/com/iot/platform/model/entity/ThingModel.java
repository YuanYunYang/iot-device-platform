package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物模型定义实体
 * <p>
 * 对应数据库 thing_model 表，定义产品下的属性（property）、事件（event）、服务（service）。
 * 平台收到设备上报数据后，依据物模型进行：
 * <ol>
 *     <li>数据类型校验</li>
 *     <li>阈值告警判定（min_value/max_value + alarm_level）</li>
 * </ol>
 * 其中 min_value/max_value 既用于取值范围校验，也作为告警阈值。
 *
 * @author iot-platform
 */
@Data
@TableName("thing_model")
public class ThingModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联产品ID */
    private Long productId;

    /** 属性标识符，如 temperature */
    private String identifier;

    /** 属性名称，如 温度 */
    private String name;

    /** 功能类型：property/event/service */
    private String type;

    /** 数据类型：int/float/string/bool/enum */
    private String dataType;

    /** 单位，如 ℃、% */
    private String unit;

    /** 最小值（阈值校验下限，如电量低于此值触发告警） */
    private Double minValue;

    /** 最大值（阈值告警上限，如温度超过此值触发告警） */
    private Double maxValue;

    /** 超阈值告警等级：INFO/WARNING/CRITICAL */
    private String alarmLevel;

    /** 属性描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;
}
