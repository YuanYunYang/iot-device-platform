package com.iot.platform.model.vo;

import com.iot.platform.model.entity.EnergyReading;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 能源表展示 VO
 * <p>
 * 返回给前端的能源表视图对象，融合了能源表基础信息、在线状态及最新读数。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "能源表信息视图")
public class EnergyMeterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "能源表编号")
    private String meterCode;

    @Schema(description = "能源表名称")
    private String meterName;

    @Schema(description = "能源表类型：ELECTRICITY/WATER/GAS")
    private String meterType;

    @Schema(description = "通信协议：MODBUS/MQTT")
    private String protocol;

    @Schema(description = "安装位置")
    private String location;

    @Schema(description = "关联产品ID")
    private Long productId;

    @Schema(description = "关联设备ID")
    private String deviceId;

    @Schema(description = "Modbus 通信地址")
    private Integer modbusAddr;

    @Schema(description = "Modbus 通信端口")
    private Integer modbusPort;

    @Schema(description = "电流互感器变比")
    private Double ctRatio;

    @Schema(description = "电压互感器变比")
    private Double ptRatio;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "在线状态：0-离线 1-在线")
    private Integer onlineStatus;

    @Schema(description = "最新读数")
    private EnergyReading latestReading;
}
