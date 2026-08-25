package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备实体
 * <p>
 * 对应数据库 device 表，记录接入平台的每台设备信息及其连接状态。
 *
 * @author iot-platform
 */
@Data
@TableName("device")
public class Device implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备唯一标识（业务ID，MQTT Topic 中使用） */
    private String deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 关联产品ID */
    private Long productId;

    /** 冗余产品标识，便于 MQTT 路由时快速获取物模型 */
    private String productKey;

    /** 设备密钥（鉴权用） */
    private String deviceSecret;

    /** 设备状态：ONLINE/OFFLINE/UNKNOWN */
    private String status;

    /** 固件版本 */
    private String firmwareVersion;

    /** 最后连接IP */
    private String ipAddress;

    /** 最后上线时间 */
    private LocalDateTime lastOnlineTime;

    /** 最后离线时间 */
    private LocalDateTime lastOfflineTime;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeatTime;

    /** 设备位置描述 */
    private String location;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
