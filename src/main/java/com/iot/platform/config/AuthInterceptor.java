package com.iot.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.common.JwtUtils;
import com.iot.platform.common.Result;
import com.iot.platform.common.ResultCode;
import com.iot.platform.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 认证拦截器
 * <p>
 * 拦截除登录、文档外的所有请求，校验 JWT Token 有效性，
 * 并将解析出的用户名、角色信息存入 request attribute 供 Controller 使用。
 *
 * @author iot-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USERNAME = "currentUsername";
    public static final String CURRENT_ROLE = "currentRole";

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();

        // 放行登录与文档路径
        if (shouldExclude(requestUri)) {
            return true;
        }

        // 提取 Authorization header
        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (jwtUtils.isTokenExpired(token)) {
            writeUnauthorized(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        try {
            String username = jwtUtils.getUsernameFromToken(token);
            String role = jwtUtils.getRoleFromToken(token);
            Long tenantId = jwtUtils.getTenantIdFromToken(token);
            // 将用户信息存入 request attribute
            request.setAttribute(CURRENT_USERNAME, username);
            request.setAttribute(CURRENT_ROLE, role);
            // 注入租户上下文（MyBatis-Plus 自动追加 tenant_id 条件）
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            // 超管可跨租户操作
            if ("SUPER_ADMIN".equals(role)) {
                TenantContext.setIgnore(true);
            }
            return true;
        } catch (Exception e) {
            log.warn("Token 解析失败: {}", e.getMessage());
            writeUnauthorized(response, ResultCode.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除租户上下文，防止线程池内存泄漏
        TenantContext.clear();
    }

    /**
     * 判断请求是否需要跳过鉴权
     *
     * @param uri 请求 URI
     * @return true-跳过鉴权
     */
    private boolean shouldExclude(String uri) {
        if (uri == null) {
            return false;
        }
        // 放行登录接口与 API 文档相关资源
        return uri.startsWith("/iot/auth/")
                || uri.startsWith("/iot/doc.html")
                || uri.startsWith("/iot/swagger-ui")
                || uri.startsWith("/iot/v3/api-docs")
                || uri.startsWith("/iot/webjars/")
                || uri.startsWith("/iot/favicon.ico");
    }

    /**
     * 写入 401 未认证响应
     */
    private void writeUnauthorized(HttpServletResponse response, ResultCode resultCode) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.failed(resultCode);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
