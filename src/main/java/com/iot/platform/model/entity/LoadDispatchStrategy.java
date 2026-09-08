package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 负荷调度策略实体
 * <p>
 * 对应数据库 load_dispatch_strategy 表，定义峰谷平削峰填谷、需求响应等
 * 负荷调度策略，可在满足触发条件时自动下发设备控制指令（如空调温度调节、
 * 充电桩限功率、照明调光、设备关停）。
 *
 * @author iot-platform
 */
@Data
@TableName("load_dispatch_strategy")
public class LoadDispatchStrategy implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 策略名称 */
    private String strategyName;

    /** 策略类型：PEAK_SHAVING(削峰)/VALLEY_FILLING(填谷)/DEMAND_RESPONSE(需求响应) */
    private String strategyType;

    /** 目标设备ID */
    private String targetDeviceId;

    /** 动作类型：AC_TEMP_ADJUST/EV_CHARGE_LIMIT/LIGHT_DIM/DEVICE_SHUTDOWN */
    private String actionType;

    /** 动作参数（JSON），如 {"temp":26} 或 {"powerLimit":30} */
    private String actionParams;

    /** 触发条件（JSON），如 {"activePower":">800","peakFlag":"==1"} */
    private String triggerCondition;

    /** 是否启用：0-禁用 1-启用 */
    private Integer enabled;

    /** 优先级（数值越大优先级越高） */
    private Integer priority;

    /** 最后执行时间 */
    private LocalDateTime lastExecuteTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
