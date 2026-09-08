package com.iot.platform.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 能源数据上报 DTO
 * <p>
 * 设备通过 MQTT 上报能耗数据时携带的载荷结构，
 * 由 EnergyMeterService.handleEnergyData 解析处理。
 *
 * @author iot-platform
 */
@Data
public class EnergyDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 能源表编号 */
    private String meterCode;

    /** 能源表类型：ELECTRICITY/WATER/GAS */
    private String meterType;

    /** 数据采集时间戳（epoch 毫秒） */
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
}
