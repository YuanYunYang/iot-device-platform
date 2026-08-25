package com.iot.platform.model.enums;

import lombok.Getter;

/**
 * 告警等级枚举
 * <p>
 * 告警按严重程度分为三级，对应不同的处置策略：
 * <ul>
 *     <li>INFO：提示信息，无需紧急处理</li>
 *     <li>WARNING：警告，建议关注并排查</li>
 *     <li>CRITICAL：严重告警，需立即处理</li>
 * </ul>
 *
 * @author iot-platform
 */
@Getter
public enum AlarmLevelEnum {

    /** 提示信息 */
    INFO("INFO", "提示", 1),
    /** 警告 */
    WARNING("WARNING", "警告", 2),
    /** 严重 */
    CRITICAL("CRITICAL", "严重", 3);

    /** 等级编码（存库值） */
    private final String code;

    /** 等级描述 */
    private final String desc;

    /** 排序权重（越大越严重） */
    private final int weight;

    AlarmLevelEnum(String code, String desc, int weight) {
        this.code = code;
        this.desc = desc;
        this.weight = weight;
    }

    /**
     * 根据编码获取枚举，默认 INFO
     */
    public static AlarmLevelEnum of(String code) {
        if (code == null) {
            return INFO;
        }
        for (AlarmLevelEnum level : values()) {
            if (level.code.equalsIgnoreCase(code)) {
                return level;
            }
        }
        return INFO;
    }
}
