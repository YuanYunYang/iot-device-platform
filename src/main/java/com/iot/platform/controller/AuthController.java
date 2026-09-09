package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.config.AuthInterceptor;
import com.iot.platform.model.dto.LoginDTO;
import com.iot.platform.model.vo.LoginVO;
import com.iot.platform.model.vo.UserVO;
import com.iot.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 * <p>
 * 提供用户登录与当前登录用户信息查询接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、当前用户信息接口")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户登录", description = "校验用户名密码，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        log.info("用户登录请求: username={}", dto.getUsername());
        return Result.success(userService.login(dto));
    }

    @Operation(summary = "获取当前登录用户信息", description = "根据 Token 解析当前登录用户")
    @GetMapping("/current")
    public Result<UserVO> currentUser(HttpServletRequest request) {
        String username = (String) request.getAttribute(AuthInterceptor.CURRENT_USERNAME);
        return Result.success(userService.getCurrentUser(username));
    }
}
