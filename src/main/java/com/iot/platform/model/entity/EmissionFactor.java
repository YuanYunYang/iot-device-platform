package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 碳排放因子实体
 * <p>
 * 对应数据库 emission_factor 表，维护各能源类型的碳排放因子（单位能耗对应的 CO2 排放量），
 * 用于碳排放核算计算。系统内置中国电网平均排放因子，支持租户自定义。
 *
 * @author iot-platform
 */
@Data
@TableName("emission_factor")
public class EmissionFactor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 排放源类型：ELECTRICITY/NATURAL_GAS/DIESEL/COAL */
    private String sourceType;

    /** 排放源名称 */
    private String sourceName;

    /** 因子值（kgCO2/单位） */
    private Double factorValue;

    /** 单位 */
    private String unit;

    /** 描述 */
    private String description;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
