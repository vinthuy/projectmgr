package com.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workitem.entity.AgentMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {
    
    @Select("SELECT * FROM agent_message WHERE session_id = #{sessionId} ORDER BY created_at ASC LIMIT #{pageSize} OFFSET #{offset}")
    List<AgentMessage> selectBySessionId(@Param("sessionId") String sessionId, 
                                         @Param("page") int page, 
                                         @Param("pageSize") int pageSize);
    
    @Select("SELECT COUNT(*) FROM agent_message WHERE session_id = #{sessionId}")
    long countBySessionId(@Param("sessionId") String sessionId);
    
    @Select("SELECT * FROM agent_message WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT #{limit}")
    List<AgentMessage> selectRecentMessages(@Param("sessionId") String sessionId, 
                                            @Param("limit") int limit);
    
    @Select("SELECT * FROM agent_message WHERE message_id = #{messageId}")
    AgentMessage selectByMessageId(@Param("messageId") String messageId);
}
