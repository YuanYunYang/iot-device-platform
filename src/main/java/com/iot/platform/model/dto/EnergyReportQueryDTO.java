package com.iot.platform.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 能耗报表查询请求 DTO
 * <p>
 * 用于按仪表、报表类型及日期范围筛选能耗报表记录。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "能耗报表查询请求")
public class EnergyReportQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "仪表ID", example = "1")
    private Long meterId;

    @Schema(description = "报表类型：DAILY/WEEKLY/MONTHLY", example = "DAILY")
    private String reportType;

    @Schema(description = "起始日期（yyyy-MM-dd 或 yyyy-MM）", example = "2026-01-01")
    private String startDate;

    @Schema(description = "结束日期（yyyy-MM-dd 或 yyyy-MM）", example = "2026-01-31")
    private String endDate;
}
