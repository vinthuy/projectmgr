package com.workitem.agent.tool;

import com.workitem.agent.client.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DeleteTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public DeleteTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行删除工具: params={}", params);
        
        String workItemId = (String) params.get("workItemId");
        if (workItemId == null) {
            throw new IllegalArgumentException("缺少必要参数: workItemId");
        }
        
        client.deleteWorkItem(workItemId);
        return Map.of("success", true, "message", "工作项已删除");
    }
}
