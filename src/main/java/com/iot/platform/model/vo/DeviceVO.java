package com.iot.platform.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备展示 VO
 * <p>
 * 返回给前端的设备视图对象，融合了设备基础信息与状态。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "设备信息视图")
public class DeviceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "产品Key")
    private String productKey;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "设备状态：ONLINE/OFFLINE/UNKNOWN")
    private String status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "固件版本")
    private String firmwareVersion;

    @Schema(description = "最后连接IP")
    private String ipAddress;

    @Schema(description = "最后上线时间")
    private LocalDateTime lastOnlineTime;

    @Schema(description = "最后心跳时间")
    private LocalDateTime lastHeartbeatTime;

    @Schema(description = "设备位置")
    private String location;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
