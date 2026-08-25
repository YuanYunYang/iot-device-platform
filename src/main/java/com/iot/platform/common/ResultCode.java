package com.iot.platform.common;

import lombok.Getter;

/**
 * 业务状态码枚举
 * <p>
 * 统一管理平台所有业务返回状态码，便于前端按码处理。
 * 编码规则：6 位整数，前 3 位为模块号，后 3 位为具体错误号。
 *
 * @author iot-platform
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(200000, "操作成功"),

    /** 通用失败 */
    FAILED(500000, "操作失败"),

    /** 参数校验失败 */
    PARAM_ERROR(400001, "参数校验失败"),

    /** 资源不存在 */
    NOT_FOUND(404001, "资源不存在"),

    /** 设备不存在 */
    DEVICE_NOT_FOUND(404002, "设备不存在"),

    /** 产品不存在 */
    PRODUCT_NOT_FOUND(404003, "产品不存在"),

    /** 设备重复注册 */
    DEVICE_ALREADY_EXISTS(409001, "设备已存在，请勿重复注册"),

    /** 设备未在线，无法控制 */
    DEVICE_OFFLINE(409002, "设备未在线，无法下发控制指令"),

    /** 控制指令响应超时 */
    CONTROL_TIMEOUT(504001, "设备控制指令响应超时"),

    /** MQTT 连接异常 */
    MQTT_ERROR(500001, "MQTT 通信异常"),

    /** 系统内部错误 */
    INTERNAL_ERROR(500500, "系统内部错误");

    /** 状态码 */
    private final int code;

    /** 提示信息 */
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
