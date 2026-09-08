package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 * <p>
 * 对应数据库 sys_user 表，记录平台用户账号、角色及启用状态信息。
 *
 * @author iot-platform
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID（多租户隔离） */
    private Long tenantId;

    /** 用户名 */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 角色：SUPER_ADMIN/SYSTEM_ADMIN/USER */
    private String role;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
