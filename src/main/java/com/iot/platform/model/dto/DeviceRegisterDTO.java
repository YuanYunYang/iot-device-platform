package com.iot.platform.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备注册请求 DTO
 * <p>
 * 前端调用注册接口时提交的设备信息。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "设备注册请求")
public class DeviceRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "设备唯一标识（建议使用设备序列号）", example = "device_003")
    @NotBlank(message = "设备ID不能为空")
    @Size(max = 64, message = "设备ID长度不能超过64")
    private String deviceId;

    @Schema(description = "设备名称", example = "仓库温湿度传感器")
    @Size(max = 128, message = "设备名称长度不能超过128")
    private String deviceName;

    @Schema(description = "关联产品Key", example = "temp_hum_sensor")
    @NotBlank(message = "产品Key不能为空")
    private String productKey;

    @Schema(description = "设备位置", example = "B栋1楼仓库")
    @Size(max = 128, message = "位置信息长度不能超过128")
    private String location;

    @Schema(description = "备注")
    @Size(max = 256, message = "备注长度不能超过256")
    private String remark;
}
