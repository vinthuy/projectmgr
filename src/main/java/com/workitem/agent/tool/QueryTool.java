package com.workitem.agent.tool;

import com.workitem.agent.client.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class QueryTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public QueryTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行查询工具: params={}", params);
        
        String workItemId = (String) params.get("workItemId");
        String projectKey = (String) params.get("projectKey");
        String status = (String) params.get("status");
        
        if (workItemId != null) {
            // 查询单个工作项
            return client.getWorkItem(workItemId);
        } else if (projectKey != null) {
            // 查询项目下的工作项列表
            Map<String, Object> filters = new HashMap<>();
            if (status != null) {
                filters.put("status", status);
            }
            return client.listWorkItems(projectKey, filters);
        } else {
            throw new IllegalArgumentException("缺少查询参数: workItemId 或 projectKey");
        }
    }
}
