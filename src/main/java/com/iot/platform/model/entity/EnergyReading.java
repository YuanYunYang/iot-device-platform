package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 能源读数实体（时序数据）
 * <p>
 * 能源仪表实时采集的运行参数，将持久化到 InfluxDB 时序数据库，
 * 同时支持写入 MySQL energy_reading 表用于数据导出与汇总统计。
 *
 * @author iot-platform
 */
@Data
@TableName("energy_reading")
public class EnergyReading implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 能源表ID */
    private Long meterId;

    /** 能源表编号 */
    private String meterCode;

    /** 能源表类型：ELECTRICITY/WATER/GAS */
    private String meterType;

    /** 数据采集时间戳（epoch 毫秒） */
    @TableField("reading_time")
    private Long timestamp;

    /** 电压（V） */
    private Double voltage;

    /** 电流（A） */
    private Double current;

    /** 有功功率（kW） */
    private Double activePower;

    /** 无功功率（kVar） */
    private Double reactivePower;

    /** 功率因数 */
    private Double powerFactor;

    /** 频率（Hz） */
    private Double frequency;

    /** 本周期用电量（kWh） */
    private Double energyConsumption;

    /** 累计用电量（kWh） */
    private Double cumulativeEnergy;

    /** 峰谷标志：0-平 1-峰 2-谷 */
    private Integer peakFlag;
}
