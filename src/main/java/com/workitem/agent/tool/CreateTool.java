package com.workitem.agent.tool;

import com.workitem.agent.client.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CreateTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public CreateTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行创建工具: params={}", params);
        
        String projectKey = (String) params.get("projectKey");
        String title = (String) params.get("title");
        String description = (String) params.get("description");
        String type = (String) params.getOrDefault("type", "TASK");
        String priority = (String) params.getOrDefault("priority", "MEDIUM");
        
        if (projectKey == null || title == null) {
            throw new IllegalArgumentException("缺少必要参数: projectKey 和 title");
        }
        
        Map<String, Object> workItem = new HashMap<>();
        workItem.put("projectKey", projectKey);
        workItem.put("title", title);
        workItem.put("description", description);
        workItem.put("type", type);
        workItem.put("priority", priority);
        
        return client.createWorkItem(workItem);
    }
}
