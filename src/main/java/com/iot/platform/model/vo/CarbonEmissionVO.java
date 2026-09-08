package com.iot.platform.model.vo;

import com.iot.platform.model.entity.CarbonEmission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 碳排放汇总展示 VO
 * <p>
 * 汇总指定日期范围内的碳排放总量、按排放范围分类（Scope 1/2）小计、
 * 按排放源类型分组的排放明细列表及统计分布。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "碳排放汇总视图")
public class CarbonEmissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "碳排放总量（kgCO2）")
    private Double totalEmission;

    @Schema(description = "Scope 1 直接排放（kgCO2）")
    private Double scope1Emission;

    @Schema(description = "Scope 2 间接排放（kgCO2）")
    private Double scope2Emission;

    @Schema(description = "碳排放明细列表")
    private List<CarbonEmission> emissionList;

    @Schema(description = "按排放源类型分组的排放量（kgCO2）")
    private Map<String, Double> emissionByType;
}
