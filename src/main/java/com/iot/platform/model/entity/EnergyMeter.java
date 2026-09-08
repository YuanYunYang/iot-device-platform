package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 能耗仪表实体
 * <p>
 * 对应数据库 energy_meter 表，记录接入能耗分析模块的每块计量仪表信息。
 * 仪表通过 deviceId 关联平台设备，按小时上报用电量、功率及功率因数等数据。
 *
 * @author iot-platform
 */
@Data
@TableName("energy_meter")
public class EnergyMeter implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 仪表编号（业务唯一标识） */
    private String meterCode;

    /** 仪表名称 */
    private String meterName;

    /** 关联设备ID（device 表的 deviceId 字段） */
    private String deviceId;

    /** 仪表类型：ELECTRICITY/WATER/GAS */
    private String meterType;

    /** 通信协议：MODBUS/MQTT */
    private String protocol;

    /** 安装位置 */
    private String location;

    /** 关联产品ID */
    private Long productId;

    /** Modbus 通信地址 */
    private Integer modbusAddr;

    /** Modbus 通信端口 */
    private Integer modbusPort;

    /** 电流互感器变比 */
    private Double ctRatio;

    /** 电压互感器变比 */
    private Double ptRatio;

    /** 额定容量（kW） */
    private Double ratedPower;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

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
