package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 固件包实体
 * <p>
 * 对应数据库 firmware 表，管理 OTA 升级使用的固件包元信息与校验信息。
 * 固件上传后默认为 DRAFT 状态，发布（PUBLISHED）后方可被升级任务引用。
 *
 * @author iot-platform
 */
@Data
@TableName("firmware")
public class Firmware implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID（由 MyBatis-Plus 多租户插件自动注入） */
    private Long tenantId;

    /** 关联产品ID */
    private Long productId;

    /** 固件版本号 */
    private String version;

    /** 固件文件下载地址 */
    private String fileUrl;

    /** 固件文件大小（字节） */
    private Long fileSize;

    /** MD5 校验码 */
    private String checksumMd5;

    /** SHA256 校验码 */
    private String checksumSha256;

    /** 状态：DRAFT/PUBLISHED/DEPRECATED */
    private String status;

    /** 固件描述 */
    private String description;

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
