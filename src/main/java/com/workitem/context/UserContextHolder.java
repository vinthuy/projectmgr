package com.workitem.context;

/**
 * 用户上下文持有者
 * 使用ThreadLocal存储当前线程的用户信息
 */
public class UserContextHolder {
    
    private static final ThreadLocal<UserContext> userContextThreadLocal = new ThreadLocal<>();
    
    /**
     * 设置用户上下文
     */
    public static void setUserContext(UserContext userContext) {
        userContextThreadLocal.set(userContext);
    }
    
    /**
     * 获取用户上下文
     */
    public static UserContext getUserContext() {
        UserContext context = userContextThreadLocal.get();
        if (context == null) {
            // 如果没有设置上下文，返回默认上下文（用于开发和测试）
            return UserContext.defaultContext();
        }
        return context;
    }
    
    /**
     * 获取当前用户的租户ID
     */
    public static Long getCurrentTenantId() {
        UserContext context = getUserContext();
        return context != null ? context.getTenantId() : 1L;
    }
    
    /**
     * 清除用户上下文
     */
    public static void clear() {
        userContextThreadLocal.remove();
    }
}
