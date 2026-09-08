package com.iot.platform.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 能源表创建请求 DTO
 * <p>
 * 前端调用创建能源表接口时提交的信息。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "能源表创建请求")
public class EnergyMeterCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "能源表编号（唯一）", example = "EM_001")
    @NotBlank(message = "能源表编号不能为空")
    private String meterCode;

    @Schema(description = "能源表名称", example = "1号变压器电表")
    @NotBlank(message = "能源表名称不能为空")
    private String meterName;

    @Schema(description = "能源表类型：ELECTRICITY/WATER/GAS", example = "ELECTRICITY")
    @NotBlank(message = "能源表类型不能为空")
    private String meterType;

    @Schema(description = "通信协议：MODBUS/MQTT", example = "MQTT")
    private String protocol;

    @Schema(description = "安装位置", example = "A栋配电室")
    private String location;

    @Schema(description = "关联产品ID", example = "1")
    private Long productId;

    @Schema(description = "关联设备ID", example = "device_001")
    private String deviceId;

    @Schema(description = "Modbus 通信地址", example = "1")
    private Integer modbusAddr;

    @Schema(description = "Modbus 通信端口", example = "502")
    private Integer modbusPort;

    @Schema(description = "电流互感器变比", example = "150")
    private Double ctRatio;

    @Schema(description = "电压互感器变比", example = "1")
    private Double ptRatio;
}
