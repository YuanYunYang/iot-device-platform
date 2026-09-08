package com.iot.platform.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 负荷调度策略创建 DTO
 *
 * @author iot-platform
 */
@Data
public class LoadDispatchStrategyCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 策略名称 */
    @NotBlank(message = "策略名称不能为空")
    private String strategyName;

    /** 策略类型：PEAK_SHAVING/VALLEY_FILLING/DEMAND_RESPONSE */
    @NotBlank(message = "策略类型不能为空")
    private String strategyType;

    /** 目标设备ID */
    @NotBlank(message = "目标设备ID不能为空")
    private String targetDeviceId;

    /** 动作类型：AC_TEMP_ADJUST/EV_CHARGE_LIMIT/LIGHT_DIM/DEVICE_SHUTDOWN */
    @NotBlank(message = "动作类型不能为空")
    private String actionType;

    /** 动作参数（JSON） */
    private String actionParams;

    /** 触发条件（JSON） */
    private String triggerCondition;

    /** 优先级 */
    private Integer priority;
}
