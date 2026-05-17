package com.workitem.context;

import lombok.Data;

/**
 * 用户上下文信息
 */
@Data
public class UserContext {
    private Long userId;
    private String username;
    private Long tenantId;
    private String role;
    
    /**
     * 创建默认用户上下文（用于测试）
     */
    public static UserContext defaultContext() {
        UserContext context = new UserContext();
        context.setUserId(1L);
        context.setUsername("admin");
        context.setTenantId(1L);
        context.setRole("ADMIN");
        return context;
    }
}
