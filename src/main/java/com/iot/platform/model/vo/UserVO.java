package com.iot.platform.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户展示 VO
 * <p>
 * 返回给前端的用户视图对象，不包含密码字段。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "用户信息视图")
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色：SUPER_ADMIN/SYSTEM_ADMIN/USER")
    private String role;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
