package com.iot.platform.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OTA 升级任务创建 DTO
 *
 * @author iot-platform
 */
@Data
public class OtaTaskCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联固件ID */
    @NotNull(message = "固件ID不能为空")
    private Long firmwareId;

    /** 任务名称 */
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    /** 目标范围：ALL/SELECTED/BY_PRODUCT */
    private String targetType;

    /** 目标产品标识（targetType=BY_PRODUCT 时使用） */
    private String targetProductKey;

    /** 目标设备ID列表（targetType=SELECTED 时使用） */
    private List<String> targetDeviceIds;

    /** 下发策略：IMMEDIATE/SCHEDULED */
    private String strategy;

    /** 定时执行时间（strategy=SCHEDULED 时使用） */
    private LocalDateTime scheduledTime;
}
