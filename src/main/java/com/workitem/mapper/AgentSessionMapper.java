package com.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workitem.entity.AgentSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {
    
    @Select("SELECT * FROM agent_session WHERE tenant_id = #{tenantId} AND is_active = true ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<AgentSession> selectByTenantId(@Param("tenantId") Long tenantId, 
                                        @Param("page") int page, 
                                        @Param("pageSize") int pageSize);
    
    @Select("SELECT COUNT(*) FROM agent_session WHERE tenant_id = #{tenantId} AND is_active = true")
    long countByTenantId(@Param("tenantId") Long tenantId);
}
