package com.iot.platform.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 设备控制指令 DTO
 * <p>
 * 前端调用控制接口下发指令到设备。
 * params 为指令参数键值对，由具体产品物模型定义其内容。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "设备控制指令请求")
public class DeviceControlDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目标设备ID", example = "device_002")
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    @Schema(description = "指令标识（对应物模型 service 的 identifier）", example = "switch")
    @NotBlank(message = "指令标识不能为空")
    private String command;

    @Schema(description = "指令参数", example = "{\"state\": true}")
    private Map<String, Object> params;
}
