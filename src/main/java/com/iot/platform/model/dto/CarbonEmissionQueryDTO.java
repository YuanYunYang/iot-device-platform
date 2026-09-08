package com.iot.platform.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 碳排放记录查询请求 DTO
 * <p>
 * 用于按排放范围、排放源类型及日期范围筛选碳排放明细记录。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "碳排放记录查询请求")
public class CarbonEmissionQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "排放范围：SCOPE_1/SCOPE_2", example = "SCOPE_2")
    private String scope;

    @Schema(description = "排放源类型：ELECTRICITY/NATURAL_GAS/DIESEL/COAL", example = "ELECTRICITY")
    private String sourceType;

    @Schema(description = "起始日期（yyyy-MM-dd）", example = "2026-01-01")
    private String startDate;

    @Schema(description = "结束日期（yyyy-MM-dd）", example = "2026-01-31")
    private String endDate;
}
