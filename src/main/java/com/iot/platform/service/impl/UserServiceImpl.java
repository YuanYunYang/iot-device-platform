package com.iot.platform.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.JwtUtils;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.dto.LoginDTO;
import com.iot.platform.model.dto.UserCreateDTO;
import com.iot.platform.model.entity.User;
import com.iot.platform.model.enums.RoleEnum;
import com.iot.platform.model.vo.LoginVO;
import com.iot.platform.model.vo.UserVO;
import com.iot.platform.repository.UserMapper;
import com.iot.platform.service.UserService;
import com.iot.platform.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 * <p>
 * 实现用户登录、创建、查询、删除、状态更新等核心逻辑，内置 RBAC 分级权限校验。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 登录时无租户上下文，需忽略租户隔离查询用户
        TenantContext.setIgnore(true);
        try {
            // 根据用户名查询用户
            User user = getByUsername(dto.getUsername());
            if (user == null) {
                throw new GlobalExceptionHandler.BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
            }

            // 校验账号状态
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new GlobalExceptionHandler.BusinessException(ResultCode.USER_DISABLED);
            }

            // BCrypt 校验密码
            if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
                throw new GlobalExceptionHandler.BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
            }

            // 生成 JWT Token（携带租户ID）
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getTenantId());

            LoginVO vo = new LoginVO();
            vo.setToken(token);
            vo.setUsername(user.getUsername());
            vo.setRole(user.getRole());
            log.info("用户登录成功: username={}, role={}, tenantId={}", user.getUsername(), user.getRole(), user.getTenantId());
            return vo;
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public UserVO getCurrentUser(String username) {
        User user = getByUsername(username);
        if (user == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }

    @Override
    public UserVO createUser(UserCreateDTO dto, String currentRole) {
        RoleEnum operatorRole = RoleEnum.of(currentRole);
        if (operatorRole == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "当前操作者角色无效");
        }

        // RBAC 分级权限校验
        RoleEnum targetRole = RoleEnum.of(dto.getRole());
        if (targetRole == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.PARAM_ERROR, "无效的角色类型");
        }

        switch (operatorRole) {
            case SUPER_ADMIN:
                // 超管只能创建系统管理员
                if (targetRole != RoleEnum.SYSTEM_ADMIN) {
                    throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "超级管理员只能创建系统管理员");
                }
                break;
            case SYSTEM_ADMIN:
                // 系统管理员只能创建普通用户
                if (targetRole != RoleEnum.USER) {
                    throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "系统管理员只能创建普通用户");
                }
                break;
            case USER:
                // 普通用户不能创建任何用户
                throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "普通用户无权创建用户");
            default:
                throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "无权限创建用户");
        }

        // 校验用户名是否重复
        User exist = getByUsername(dto.getUsername());
        if (exist != null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.USERNAME_EXISTS);
        }

        // 构建用户实体并保存
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setRole(targetRole.getCode());
        user.setStatus(1);
        userMapper.insert(user);
        log.info("用户创建成功: username={}, role={}, 操作者角色={}", dto.getUsername(), targetRole.getCode(), currentRole);

        return convertToVO(user);
    }

    @Override
    public List<UserVO> listUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreateTime);
        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long id, Long currentUserId, String currentRole) {
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 不能删除自己
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "不能删除当前登录用户");
        }

        // 不能删除同级或更高级用户
        RoleEnum operatorRole = RoleEnum.of(currentRole);
        RoleEnum targetRole = RoleEnum.of(target.getRole());
        if (operatorRole == null || targetRole == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "角色信息异常");
        }
        if (!canManage(operatorRole, targetRole)) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "无权删除同级或更高级用户");
        }

        userMapper.deleteById(id);
        log.info("用户已删除: id={}, username={}, 操作者角色={}", id, target.getUsername(), currentRole);
    }

    @Override
    public void updateStatus(Long id, Integer status, String currentRole) {
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 不能修改同级或更高级用户状态
        RoleEnum operatorRole = RoleEnum.of(currentRole);
        RoleEnum targetRole = RoleEnum.of(target.getRole());
        if (operatorRole == null || targetRole == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "角色信息异常");
        }
        if (!canManage(operatorRole, targetRole)) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FORBIDDEN, "无权修改同级或更高级用户状态");
        }

        User update = new User();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        log.info("用户状态已更新: id={}, status={}, 操作者角色={}", id, status, currentRole);
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        wrapper.last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }

    /**
     * 判断操作者是否可管理目标用户
     * <p>
     * 角色等级：SUPER_ADMIN > SYSTEM_ADMIN > USER
     * 只能管理比自己低级的用户。
     *
     * @param operator 操作者角色
     * @param target   目标角色
     * @return true-可管理
     */
    private boolean canManage(RoleEnum operator, RoleEnum target) {
        return operator.ordinal() < target.ordinal();
    }

    /**
     * 实体转 VO（不返回密码）
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
