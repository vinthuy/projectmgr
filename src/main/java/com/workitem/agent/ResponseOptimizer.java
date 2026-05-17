package com.workitem.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ResponseOptimizer {
    
    public AgentResponse optimize(IntentResult intent, ActionResult action, 
                                  Map<String, Object> context) {
        AgentResponse response = new AgentResponse();
        response.setIntent(intent.getIntent());
        response.setConfidence(intent.getConfidence());
        
        if (!action.isSuccess()) {
            // 失败情况: 提供友好的错误提示和建议
            response.setContent("❌ 操作失败: " + action.getErrorMessage());
            response.setSuggestedActions(getErrorSuggestions());
            return response;
        }
        
        // 成功情况: 格式化输出
        switch (intent.getIntent()) {
            case "QUERY":
                return optimizeQueryResponse(action.getData());
            case "CREATE":
                return optimizeCreateResponse(action.getData());
            case "UPDATE":
                return optimizeUpdateResponse(action.getData());
            case "DELETE":
                return optimizeDeleteResponse(action.getData());
            case "ANALYZE":
                return optimizeAnalyzeResponse(action.getData());
            default:
                response.setContent("✅ 操作成功");
                return response;
        }
    }
    
    private AgentResponse optimizeQueryResponse(Object data) {
        AgentResponse response = new AgentResponse();
        response.setIntent("QUERY");
        
        if (data instanceof Map) {
            // 单个工作项
            Map<String, Object> workItem = (Map<String, Object>) data;
            response.setWorkItem(workItem);
            
            StringBuilder content = new StringBuilder();
            content.append("✅ 已找到工作项 ").append(workItem.get("id")).append("\n\n");
            content.append("📋 **").append(workItem.get("title")).append("**\n");
            content.append("- 状态: ").append(getStatusEmoji(workItem.get("status"))).append(" ")
                   .append(getStatusText(workItem.get("status"))).append("\n");
            content.append("- 优先级: ").append(getPriorityEmoji(workItem.get("priority"))).append(" ")
                   .append(getPriorityText(workItem.get("priority"))).append("\n");
            content.append("- 负责人: ").append(workItem.getOrDefault("assignee", "未分配")).append("\n");
            content.append("- 截止日期: ").append(workItem.getOrDefault("dueDate", "无"));
            
            response.setContent(content.toString());
            response.setSuggestedActions(Arrays.asList(
                Map.of("label", "查看详情", "action", "view", "params", Map.of("id", workItem.get("id"))),
                Map.of("label", "编辑", "action", "edit", "params", Map.of("id", workItem.get("id")))
            ));
        } else if (data instanceof List) {
            // 工作项列表
            List<?> items = (List<?>) data;
            response.setWorkItems((List<Map<String, Object>>) data);
            
            StringBuilder content = new StringBuilder();
            content.append(String.format("✅ 找到 %d 个工作项:\n\n", items.size()));
            
            for (int i = 0; i < Math.min(items.size(), 5); i++) {
                Map<String, Object> item = (Map<String, Object>) items.get(i);
                content.append(String.format("%d. [%s] %s (%s)\n", 
                    i + 1, item.get("id"), item.get("title"), item.get("status")));
            }
            
            if (items.size() > 5) {
                content.append(String.format("\n... 还有 %d 个", items.size() - 5));
            }
            
            response.setContent(content.toString());
        } else {
            response.setContent("未找到相关工作项");
        }
        
        return response;
    }
    
    private AgentResponse optimizeCreateResponse(Object data) {
        AgentResponse response = new AgentResponse();
        response.setIntent("CREATE");
        
        Map<String, Object> workItem = (Map<String, Object>) data;
        response.setWorkItem(workItem);
        
        String content = String.format("✅ 工作项创建成功!\n\nID: %s\n标题: %s", 
            workItem.get("id"), workItem.get("title"));
        response.setContent(content);
        
        response.setSuggestedActions(Arrays.asList(
            Map.of("label", "查看详情", "action", "view", "params", Map.of("id", workItem.get("id"))),
            Map.of("label", "继续创建", "action", "create")
        ));
        
        return response;
    }
    
    private AgentResponse optimizeUpdateResponse(Object data) {
        AgentResponse response = new AgentResponse();
        response.setIntent("UPDATE");
        response.setContent("✅ 工作项更新成功!");
        return response;
    }
    
    private AgentResponse optimizeDeleteResponse(Object data) {
        AgentResponse response = new AgentResponse();
        response.setIntent("DELETE");
        response.setContent("✅ 工作项已删除");
        return response;
    }
    
    private AgentResponse optimizeAnalyzeResponse(Object data) {
        AgentResponse response = new AgentResponse();
        response.setIntent("ANALYZE");
        
        if (data instanceof Map) {
            Map<String, Object> result = (Map<String, Object>) data;
            response.setContent((String) result.getOrDefault("report", "分析完成"));
            response.setMetadata(result);
        }
        
        return response;
    }
    
    private List<Map<String, Object>> getErrorSuggestions() {
        return Arrays.asList(
            Map.of("label", "重试操作", "action", "retry"),
            Map.of("label", "检查参数", "action", "check"),
            Map.of("label", "联系管理员", "action", "contact")
        );
    }
    
    // ========== 辅助函数 ==========
    
    private String getStatusEmoji(Object status) {
        if (status == null) return "⚪";
        String s = status.toString().toUpperCase();
        switch (s) {
            case "TODO": return "🔵";
            case "IN_PROGRESS": return "🟡";
            case "DONE": return "🟢";
            case "CLOSED": return "⚫";
            default: return "⚪";
        }
    }
    
    private String getStatusText(Object status) {
        if (status == null) return "未知";
        String s = status.toString().toUpperCase();
        switch (s) {
            case "TODO": return "待办";
            case "IN_PROGRESS": return "进行中";
            case "DONE": return "已完成";
            case "CLOSED": return "已关闭";
            default: return s;
        }
    }
    
    private String getPriorityEmoji(Object priority) {
        if (priority == null) return "⚪";
        String p = priority.toString().toUpperCase();
        switch (p) {
            case "LOW": return "🔵";
            case "MEDIUM": return "🟡";
            case "HIGH": return "🟠";
            case "CRITICAL": return "🔴";
            default: return "⚪";
        }
    }
    
    private String getPriorityText(Object priority) {
        if (priority == null) return "未知";
        String p = priority.toString().toUpperCase();
        switch (p) {
            case "LOW": return "低";
            case "MEDIUM": return "中";
            case "HIGH": return "高";
            case "CRITICAL": return "紧急";
            default: return p;
        }
    }
}
