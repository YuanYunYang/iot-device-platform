package com.iot.platform.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 负荷调度执行 DTO
 * <p>
 * 描述一次具体的负荷调度下发动作，包含目标设备、动作类型及动作参数。
 * 动作参数为 JSON 字符串，如 {"temp":26}（空调调温）、{"powerLimit":30}（充电限功率）。
 *
 * @author iot-platform
 */
@Data
public class LoadDispatchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 策略ID */
    private Long strategyId;

    /** 目标设备ID */
    private String deviceId;

    /** 动作类型：AC_TEMP_ADJUST/EV_CHARGE_LIMIT/LIGHT_DIM/DEVICE_SHUTDOWN */
    private String actionType;

    /** 动作参数（JSON），如 {"temp":26} 或 {"powerLimit":30} */
    private String actionParams;

    /** 触发原因 */
    private String reason;
}
