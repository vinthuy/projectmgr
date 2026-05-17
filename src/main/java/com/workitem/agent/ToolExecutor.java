package com.workitem.agent;

import com.workitem.agent.client.CodeArtsReqClient;
import com.workitem.agent.tool.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ToolExecutor {
    
    private final CodeArtsReqClient codeArtsClient;
    private final Map<String, Tool> tools = new HashMap<>();
    
    public ToolExecutor(CodeArtsReqClient codeArtsClient) {
        this.codeArtsClient = codeArtsClient;
        registerTools();
    }
    
    private void registerTools() {
        tools.put("QUERY", new QueryTool(codeArtsClient));
        tools.put("CREATE", new CreateTool(codeArtsClient));
        tools.put("UPDATE", new UpdateTool(codeArtsClient));
        tools.put("DELETE", new DeleteTool(codeArtsClient));
        tools.put("ANALYZE", new AnalyzeTool(codeArtsClient));
    }
    
    public ActionResult execute(IntentResult intent) {
        String intentType = intent.getIntent();
        Tool tool = tools.get(intentType);
        
        if (tool == null) {
            return ActionResult.error("不支持的操作类型: " + intentType);
        }
        
        try {
            Object result = tool.execute(intent.getEntities());
            return ActionResult.success(result);
        } catch (Exception e) {
            log.error("工具执行失败: intentType={}", intentType, e);
            return ActionResult.error("执行失败: " + e.getMessage());
        }
    }
}
