package com.workitem.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class IntentParser {
    
    private static final Map<String, Pattern> INTENT_PATTERNS = new HashMap<>();
    
    static {
        INTENT_PATTERNS.put("QUERY", Pattern.compile("(查询|查找|搜索|查看|显示|列出).*(工作项|任务|bug|需求)"));
        INTENT_PATTERNS.put("CREATE", Pattern.compile("(创建|新建|添加|生成).*(工作项|任务|bug|需求)"));
        INTENT_PATTERNS.put("UPDATE", Pattern.compile("(更新|修改|编辑|变更).*(工作项|任务|bug|需求)"));
        INTENT_PATTERNS.put("DELETE", Pattern.compile("(删除|移除|取消).*(工作项|任务|bug|需求)"));
        INTENT_PATTERNS.put("ANALYZE", Pattern.compile("(分析|统计|汇总|报告).*(工作项|任务|进度)"));
    }
    
    public IntentResult parse(String userInput, Map<String, Object> context) {
        IntentResult result = new IntentResult();
        result.setOriginalInput(userInput);
        
        // 1. 识别意图类型
        String intent = detectIntent(userInput);
        result.setIntent(intent);
        
        // 2. 提取实体信息
        Map<String, Object> entities = extractEntities(userInput);
        result.setEntities(entities);
        
        // 3. 计算置信度
        double confidence = calculateConfidence(intent, entities);
        result.setConfidence(confidence);
        
        // 4. 结合上下文
        enrichWithContext(result, context);
        
        log.info("意图解析完成: intent={}, confidence={}, entities={}", 
                 intent, confidence, entities);
        
        return result;
    }
    
    private String detectIntent(String input) {
        for (Map.Entry<String, Pattern> entry : INTENT_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(input).find()) {
                return entry.getKey();
            }
        }
        return "UNKNOWN";
    }
    
    private Map<String, Object> extractEntities(String input) {
        Map<String, Object> entities = new HashMap<>();
        
        // 提取工作项ID (如: PROJ-123)
        Pattern idPattern = Pattern.compile("([A-Z]+-\\d+)");
        Matcher matcher = idPattern.matcher(input);
        if (matcher.find()) {
            entities.put("workItemId", matcher.group(1));
        }
        
        // 提取项目名称
        Pattern projectPattern = Pattern.compile("项目[\\s]*([A-Za-z0-9]+)");
        matcher = projectPattern.matcher(input);
        if (matcher.find()) {
            entities.put("projectKey", matcher.group(1));
        }
        
        // 提取状态
        if (input.contains("进行中")) {
            entities.put("status", "IN_PROGRESS");
        } else if (input.contains("已完成")) {
            entities.put("status", "DONE");
        } else if (input.contains("待处理")) {
            entities.put("status", "TODO");
        }
        
        // 提取优先级
        if (input.contains("高优先级") || input.contains("紧急")) {
            entities.put("priority", "HIGH");
        } else if (input.contains("低优先级")) {
            entities.put("priority", "LOW");
        }
        
        return entities;
    }
    
    private double calculateConfidence(String intent, Map<String, Object> entities) {
        double confidence = 0.5;
        
        if (!"UNKNOWN".equals(intent)) {
            confidence += 0.3;
        }
        
        if (!entities.isEmpty()) {
            confidence += 0.2;
        }
        
        return Math.min(confidence, 1.0);
    }
    
    private void enrichWithContext(IntentResult result, Map<String, Object> context) {
        if (context != null) {
            result.getEntities().putAll(context);
        }
    }
}
