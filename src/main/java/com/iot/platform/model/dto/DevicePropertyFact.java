package com.iot.platform.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备属性事实对象（Drools 规则评估的事实）
 * <p>
 * 规则引擎在评估告警规则时，将设备上报的属性数据封装为本 Fact 注入 KieSession。
 * 规则触发后通过 {@link #getResults()} 收集告警结果。
 *
 * @author iot-platform
 */
@Data
public class DevicePropertyFact implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private String deviceId;

    /** 产品ID */
    private Long productId;

    /** 租户ID */
    private Long tenantId;

    /** 设备上报的属性键值对 */
    private Map<String, Object> properties;

    /** 数据采集时间 */
    private LocalDateTime timestamp;

    /** 规则评估产生的告警结果（规则触发后写入） */
    private java.util.List<AlarmResult> results;

    /**
     * 获取数值型属性（兼容 Number 与字符串）
     *
     * @param key 属性标识
     * @return Double 值，不存在或非数值返回 null
     */
    public Double getDouble(String key) {
        if (properties == null) {
            return null;
        }
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取整型属性
     *
     * @param key 属性标识
     * @return Integer 值，不存在或非数值返回 null
     */
    public Integer getInteger(String key) {
        Double d = getDouble(key);
        return d == null ? null : d.intValue();
    }

    /**
     * 获取字符串属性
     *
     * @param key 属性标识
     * @return 字符串值，不存在返回 null
     */
    public String getString(String key) {
        if (properties == null) {
            return null;
        }
        Object value = properties.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 添加告警结果（规则触发时调用）
     */
    public void addResult(AlarmResult result) {
        if (results == null) {
            results = new ArrayList<>();
        }
        results.add(result);
    }

    /**
     * 确保结果列表已初始化
     */
    public java.util.List<AlarmResult> ensureResults() {
        if (results == null) {
            results = new ArrayList<>();
        }
        return results;
    }

    /**
     * 初始化空属性 Map，避免 NPE
     */
    public Map<String, Object> ensureProperties() {
        if (properties == null) {
            properties = new ConcurrentHashMap<>();
        }
        return properties;
    }
}
