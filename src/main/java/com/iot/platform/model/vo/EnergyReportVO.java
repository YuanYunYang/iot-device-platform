package com.iot.platform.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 能耗报表展示 VO
 * <p>
 * 在能耗报表基础上扩展尖峰平谷分项电费及占比信息，用于前端可视化展示。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "能耗报表视图")
public class EnergyReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "仪表ID")
    private Long meterId;

    @Schema(description = "仪表编号")
    private String meterCode;

    @Schema(description = "报表类型：DAILY/WEEKLY/MONTHLY")
    private String reportType;

    @Schema(description = "报表日期")
    private String reportDate;

    @Schema(description = "总能耗（kWh）")
    private Double totalEnergy;

    @Schema(description = "尖峰能耗（kWh）")
    private Double peakEnergy;

    @Schema(description = "谷时段能耗（kWh）")
    private Double valleyEnergy;

    @Schema(description = "平时段能耗（kWh）")
    private Double flatEnergy;

    @Schema(description = "最大需量（kW）")
    private Double maxDemand;

    @Schema(description = "平均功率因数")
    private Double avgPowerFactor;

    @Schema(description = "同比（%）")
    private Double yoyRatio;

    @Schema(description = "环比（%）")
    private Double momRatio;

    @Schema(description = "电费（元）")
    private Double cost;

    @Schema(description = "尖峰电费（元）")
    private Double peakCost;

    @Schema(description = "谷时段电费（元）")
    private Double valleyCost;

    @Schema(description = "平时段电费（元）")
    private Double flatCost;

    @Schema(description = "尖峰能耗占比（%）")
    private Double peakRatio;

    @Schema(description = "谷时段能耗占比（%）")
    private Double valleyRatio;

    @Schema(description = "平时段能耗占比（%）")
    private Double flatRatio;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
