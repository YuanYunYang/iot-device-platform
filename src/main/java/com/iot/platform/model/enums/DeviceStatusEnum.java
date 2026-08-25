package com.iot.platform.model.enums;

import lombok.Getter;

/**
 * 设备状态枚举
 * <p>
 * 描述设备在平台中的连接状态：
 * <ul>
 *     <li>ONLINE：设备已连接，正常上报数据</li>
 *     <li>OFFLINE：设备断开连接或心跳超时</li>
 *     <li>UNKNOWN：设备未激活或状态未知（新注册设备默认状态）</li>
 * </ul>
 *
 * @author iot-platform
 */
@Getter
public enum DeviceStatusEnum {

    /** 在线 */
    ONLINE("ONLINE", "在线"),
    /** 离线 */
    OFFLINE("OFFLINE", "离线"),
    /** 未知（未激活） */
    UNKNOWN("UNKNOWN", "未知");

    /** 状态编码（存库值） */
    private final String code;

    /** 状态描述 */
    private final String desc;

    DeviceStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举
     */
    public static DeviceStatusEnum of(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (DeviceStatusEnum status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
