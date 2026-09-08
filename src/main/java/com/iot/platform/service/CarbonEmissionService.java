package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.dto.CarbonEmissionQueryDTO;
import com.iot.platform.model.entity.CarbonEmission;
import com.iot.platform.model.entity.EmissionFactor;
import com.iot.platform.model.vo.CarbonEmissionVO;

import java.util.List;

/**
 * 碳排放核算服务接口
 * <p>
 * 提供碳排放量计算、排放记录查询、排放因子管理及默认因子初始化等能力。
 *
 * @author iot-platform
 */
public interface CarbonEmissionService extends IService<CarbonEmission> {

    /**
     * 计算指定日期范围内的碳排放
     * <p>
     * 查询该范围内所有能耗报表，按电力消耗 × 电网排放因子计算 CO2 排放量，
     * 生成 Scope 2 碳排放记录并持久化，返回汇总视图。
     *
     * @param startDate 起始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 碳排放汇总视图
     */
    CarbonEmissionVO calculateEmission(String startDate, String endDate);

    /**
     * 查询碳排放记录列表
     *
     * @param query 查询条件
     * @return 碳排放明细列表
     */
    List<CarbonEmission> getEmissionRecords(CarbonEmissionQueryDTO query);

    /**
     * 初始化默认排放因子
     * <p>
     * 内置中国电网平均及常见化石燃料排放因子：
     * 电力 0.5810 kgCO2/kWh、天然气 2.1622 kgCO2/m³、柴油 2.7301 kgCO2/L、煤炭 1.9776 kgCO2/kg。
     */
    void initDefaultFactors();

    /**
     * 新增排放因子
     *
     * @param factor 排放因子信息
     * @return 保存后的排放因子
     */
    EmissionFactor addEmissionFactor(EmissionFactor factor);

    /**
     * 查询当前租户的排放因子列表
     *
     * @return 排放因子列表
     */
    List<EmissionFactor> listEmissionFactors();
}
