package com.workitem.agent.tool;

import com.workitem.agent.client.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UpdateTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public UpdateTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行更新工具: params={}", params);
        
        String workItemId = (String) params.get("workItemId");
        if (workItemId == null) {
            throw new IllegalArgumentException("缺少必要参数: workItemId");
        }
        
        Map<String, Object> updates = new HashMap<>();
        if (params.containsKey("title")) {
            updates.put("title", params.get("title"));
        }
        if (params.containsKey("status")) {
            updates.put("status", params.get("status"));
        }
        if (params.containsKey("priority")) {
            updates.put("priority", params.get("priority"));
        }
        if (params.containsKey("description")) {
            updates.put("description", params.get("description"));
        }
        
        return client.updateWorkItem(workItemId, updates);
    }
}
