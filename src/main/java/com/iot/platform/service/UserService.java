package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.dto.LoginDTO;
import com.iot.platform.model.dto.UserCreateDTO;
import com.iot.platform.model.entity.User;
import com.iot.platform.model.vo.LoginVO;
import com.iot.platform.model.vo.UserVO;

import java.util.List;

/**
 * 用户服务接口
 * <p>
 * 提供用户登录、创建、查询、删除及状态更新等能力，内含 RBAC 分级权限校验。
 *
 * @author iot-platform
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     *
     * @param dto 登录信息
     * @return 登录视图（含 token）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前登录用户信息
     *
     * @param username 当前用户名
     * @return 用户视图
     */
    UserVO getCurrentUser(String username);

    /**
     * 创建用户（RBAC 分级权限校验）
     * <ul>
     *     <li>SUPER_ADMIN 只能创建 SYSTEM_ADMIN</li>
     *     <li>SYSTEM_ADMIN 只能创建 USER</li>
     *     <li>USER 不能创建任何用户</li>
     * </ul>
     *
     * @param dto          用户创建信息
     * @param currentRole  当前操作者角色
     * @return 创建后的用户视图
     */
    UserVO createUser(UserCreateDTO dto, String currentRole);

    /**
     * 查询所有用户列表（不返回密码）
     *
     * @return 用户视图列表
     */
    List<UserVO> listUsers();

    /**
     * 删除用户
     * <p>
     * 不能删除自己，不能删除同级或更高级用户。
     *
     * @param id            被删除用户ID
     * @param currentUserId 当前操作者用户ID
     * @param currentRole   当前操作者角色
     */
    void deleteUser(Long id, Long currentUserId, String currentRole);

    /**
     * 启用/禁用用户
     *
     * @param id           用户ID
     * @param status       目标状态：0-禁用 1-启用
     * @param currentRole  当前操作者角色
     */
    void updateStatus(Long id, Integer status, String currentRole);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    User getByUsername(String username);
}
