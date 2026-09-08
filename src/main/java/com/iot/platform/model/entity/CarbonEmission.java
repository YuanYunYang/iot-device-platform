package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 碳排放记录实体
 * <p>
 * 对应数据库 carbon_emission 表，逐条记录各排放源的碳排放核算结果，
 * 含排放范围（Scope 1 直接 / Scope 2 间接）、排放源类型、消耗量、
 * 排放因子及折算 CO2 排放量，作为碳排放报告自动生成的基础数据。
 *
 * @author iot-platform
 */
@Data
@TableName("carbon_emission")
public class CarbonEmission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 排放范围：SCOPE_1(直接排放)/SCOPE_2(间接排放) */
    private String scope;

    /** 排放源类型：ELECTRICITY/NATURAL_GAS/DIESEL/COAL */
    private String sourceType;

    /** 排放源名称 */
    private String sourceName;

    /** 消耗量 */
    private Double consumption;

    /** 消耗量单位，如 kWh、m³、L、kg */
    private String consumptionUnit;

    /** 排放因子（kgCO2/单位） */
    private Double emissionFactor;

    /** CO2 排放量（kgCO2） */
    private Double co2Emission;

    /** 核算日期 */
    private LocalDate calculationDate;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
