package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 能耗报表实体
 * <p>
 * 对应数据库 energy_report 表，按仪表维度记录日报表/月报表的能耗汇总数据，
 * 含尖峰平谷分项电量、最大需量、平均功率因数、同环比及电费，
 * 作为能耗趋势分析与碳排放折算的基础数据。
 *
 * @author iot-platform
 */
@Data
@TableName("energy_report")
public class EnergyReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 仪表ID */
    private Long meterId;

    /** 仪表编号 */
    private String meterCode;

    /** 报表类型：DAILY/WEEKLY/MONTHLY */
    private String reportType;

    /** 报表日期（DAILY: yyyy-MM-dd，MONTHLY: yyyy-MM） */
    private String reportDate;

    /** 总能耗（kWh） */
    private Double totalEnergy;

    /** 尖峰能耗（kWh） */
    private Double peakEnergy;

    /** 谷时段能耗（kWh） */
    private Double valleyEnergy;

    /** 平时段能耗（kWh） */
    private Double flatEnergy;

    /** 最大需量（kW） */
    private Double maxDemand;

    /** 平均功率因数 */
    private Double avgPowerFactor;

    /** 同比（%） */
    private Double yoyRatio;

    /** 环比（%） */
    private Double momRatio;

    /** 电费（元） */
    private Double cost;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
