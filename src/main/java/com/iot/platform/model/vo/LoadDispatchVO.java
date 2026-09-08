package com.iot.platform.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 负荷调度策略视图
 *
 * @author iot-platform
 */
@Data
public class LoadDispatchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 策略ID */
    private Long id;

    /** 策略名称 */
    private String strategyName;

    /** 策略类型：PEAK_SHAVING/VALLEY_FILLING/DEMAND_RESPONSE */
    private String strategyType;

    /** 目标设备ID */
    private String targetDeviceId;

    /** 动作类型：AC_TEMP_ADJUST/EV_CHARGE_LIMIT/LIGHT_DIM/DEVICE_SHUTDOWN */
    private String actionType;

    /** 动作参数（JSON） */
    private String actionParams;

    /** 触发条件（JSON） */
    private String triggerCondition;

    /** 是否启用：0-禁用 1-启用 */
    private Integer enabled;

    /** 优先级 */
    private Integer priority;

    /** 最后执行时间 */
    private LocalDateTime lastExecuteTime;

    /** 累计执行次数 */
    private Integer executeCount;
}
