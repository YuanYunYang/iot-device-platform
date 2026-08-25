package com.iot.platform.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录返回 VO
 * <p>
 * 登录成功后返回 JWT token 及当前登录用户信息。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "登录返回视图")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色")
    private String role;
}
