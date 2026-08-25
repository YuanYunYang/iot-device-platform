package com.iot.platform.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建用户请求 DTO
 * <p>
 * 管理员创建下级用户时提交的用户名、密码及角色信息。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "创建用户请求")
public class UserCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名", example = "system_admin_01")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "角色：SYSTEM_ADMIN/USER", example = "SYSTEM_ADMIN")
    @NotBlank(message = "角色不能为空")
    @NotNull(message = "角色不能为空")
    private String role;
}
