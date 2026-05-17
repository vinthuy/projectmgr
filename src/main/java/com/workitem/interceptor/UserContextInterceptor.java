package com.workitem.interceptor;

import com.workitem.context.UserContext;
import com.workitem.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * 从请求头中提取用户信息并设置到上下文中
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头中获取用户信息
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        String tenantId = request.getHeader("X-Tenant-Id");
        String role = request.getHeader("X-User-Role");
        
        // 如果请求头中有用户信息，则设置到上下文
        if (tenantId != null) {
            UserContext userContext = new UserContext();
            userContext.setUserId(userId != null ? Long.parseLong(userId) : null);
            userContext.setUsername(username);
            userContext.setTenantId(Long.parseLong(tenantId));
            userContext.setRole(role);
            UserContextHolder.setUserContext(userContext);
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除上下文
        UserContextHolder.clear();
    }
}
