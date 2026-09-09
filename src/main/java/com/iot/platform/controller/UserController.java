package com.iot.platform.controller;

import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.Result;
import com.iot.platform.common.ResultCode;
import com.iot.platform.config.AuthInterceptor;
import com.iot.platform.model.dto.UserCreateDTO;
import com.iot.platform.model.entity.User;
import com.iot.platform.model.enums.RoleEnum;
import com.iot.platform.model.vo.UserVO;
import com.iot.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 Controller
 * <p>
 * 提供用户创建、查询、删除及启用/禁用等接口，内含 RBAC 分级权限控制。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户创建、查询、删除及状态管理接口")
public class UserController {

    private final UserService userService;

    @Operation(summary = "创建用户", description = "RBAC：超管只能创建系统管理员，系统管理员只能创建普通用户，普通用户不能创建")
    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto, HttpServletRequest request) {
        String currentRole = (String) request.getAttribute(AuthInterceptor.CURRENT_ROLE);
        return Result.success(userService.createUser(dto, currentRole));
    }

    @Operation(summary = "用户列表", description = "超管和系统管理员可查看所有用户")
    @GetMapping("/list")
    public Result<List<UserVO>> listUsers(HttpServletRequest request) {
        String currentRole = (String) request.getAttribute(AuthInterceptor.CURRENT_ROLE);
        RoleEnum operatorRole = RoleEnum.of(currentRole);
        if (operatorRole == null || operatorRole == RoleEnum.USER) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "普通用户无权查看用户列表");
        }
        return Result.success(userService.listUsers());
    }

    @Operation(summary = "删除用户", description = "只能删除比自己低级的用户，不能删除自己")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            HttpServletRequest request) {
        String currentRole = (String) request.getAttribute(AuthInterceptor.CURRENT_ROLE);
        String currentUsername = (String) request.getAttribute(AuthInterceptor.CURRENT_USERNAME);
        // 查询当前操作者用户ID
        User currentUser = userService.getByUsername(currentUsername);
        Long currentUserId = currentUser == null ? null : currentUser.getId();
        userService.deleteUser(id, currentUserId, currentRole);
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户", description = "修改用户启用状态，只能操作比自己低级的用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "目标状态：0-禁用 1-启用") @RequestParam Integer status,
            HttpServletRequest request) {
        String currentRole = (String) request.getAttribute(AuthInterceptor.CURRENT_ROLE);
        userService.updateStatus(id, status, currentRole);
        return Result.success();
    }
}
