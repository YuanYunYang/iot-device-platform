package com.iot.platform.model.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 * <p>
 * 描述平台用户的分级角色权限：
 * <ul>
 *     <li>SUPER_ADMIN：超级管理员，拥有最高权限，可创建系统管理员</li>
 *     <li>SYSTEM_ADMIN：系统管理员，可创建普通用户</li>
 *     <li>USER：普通用户，仅可访问基础业务功能，不能创建用户</li>
 * </ul>
 *
 * @author iot-platform
 */
@Getter
public enum RoleEnum {

    /** 超级管理员 */
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员"),
    /** 系统管理员 */
    SYSTEM_ADMIN("SYSTEM_ADMIN", "系统管理员"),
    /** 普通用户 */
    USER("USER", "普通用户");

    /** 角色编码（存库值） */
    private final String code;

    /** 角色描述 */
    private final String description;

    RoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 角色编码
     * @return 角色枚举，未匹配返回 null
     */
    public static RoleEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (RoleEnum role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }
}
