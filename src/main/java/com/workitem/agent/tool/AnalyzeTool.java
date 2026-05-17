package com.workitem.agent.tool;

import com.workitem.agent.client.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class AnalyzeTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public AnalyzeTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行分析工具: params={}", params);
        
        String projectKey = (String) params.get("projectKey");
        if (projectKey == null) {
            throw new IllegalArgumentException("缺少必要参数: projectKey");
        }
        
        // 获取统计数据
        Map<String, Object> stats = client.getWorkItemStats(projectKey);
        
        // 生成分析报告
        String report = generateReport(stats);
        
        return Map.of(
            "stats", stats,
            "report", report
        );
    }
    
    private String generateReport(Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 工作项分析报告\n\n");
        sb.append("总工作项数: ").append(stats.getOrDefault("total", 0)).append("\n");
        sb.append("已完成: ").append(stats.getOrDefault("completed", 0)).append("\n");
        sb.append("进行中: ").append(stats.getOrDefault("inProgress", 0)).append("\n");
        sb.append("完成率: ").append(stats.getOrDefault("completionRate", 0)).append("%\n");
        return sb.toString();
    }
}
