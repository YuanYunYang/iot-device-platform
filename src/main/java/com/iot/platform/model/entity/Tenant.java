package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户实体
 * <p>
 * SaaS 多租户架构核心表，每个租户代表一个独立组织/企业。
 * 租户下的所有数据（设备、产品、告警、用户）通过 tenant_id 隔离。
 *
 * @author iot-platform
 */
@Data
@TableName("tenant")
public class Tenant implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户编码（唯一，用于 JWT 中标识租户） */
    private String tenantCode;

    /** 租户名称 */
    private String tenantName;

    /** 租户类型：TRIAL-试用 / STANDARD-标准版 / ENTERPRISE-企业版 */
    private String tenantType;

    /** 租户状态：0-禁用 1-启用 */
    private Integer status;

    /** 设备数量上限（根据套餐分配） */
    private Integer deviceLimit;

    /** 用户数量上限 */
    private Integer userLimit;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 到期时间 */
    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
