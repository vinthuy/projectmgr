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
        // 查询相关
        INTENT_PATTERNS.put("QUERY", Pattern.compile("(查询|查找|搜索|查看|显示|列出|获取).*(工作项|任务|bug|需求|史诗|故事)"));
        INTENT_PATTERNS.put("QUERY", Pattern.compile(".*(工作项|任务|bug|需求|史诗|故事).*(查询|查找|搜索|查看|显示|列出|获取)"));
        
        // 创建相关
        INTENT_PATTERNS.put("CREATE", Pattern.compile("(创建|新建|添加|生成|建立).*(工作项|任务|bug|需求|史诗|故事)"));
        INTENT_PATTERNS.put("CREATE", Pattern.compile(".*(工作项|任务|bug|需求|史诗|故事).*(创建|新建|添加|生成|建立)"));
        
        // 更新相关
        INTENT_PATTERNS.put("UPDATE", Pattern.compile("(更新|修改|编辑|变更|改).*(工作项|任务|bug|需求|状态|优先级|负责人)"));
        INTENT_PATTERNS.put("UPDATE", Pattern.compile(".*(工作项|任务|bug|需求).*(更新|修改|编辑|变更|改为|改成)"));
        
        // 删除相关
        INTENT_PATTERNS.put("DELETE", Pattern.compile("(删除|移除|取消|废弃).*(工作项|任务|bug|需求)"));
        
        // 统计相关
        INTENT_PATTERNS.put("ANALYZE", Pattern.compile("(分析|统计|汇总|报告|概览).*(工作项|任务|进度|项目)"));
        INTENT_PATTERNS.put("ANALYZE", Pattern.compile(".*(工作项|任务|项目).*(统计|汇总|报告|概览)"));
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
        // 首先尝试精确匹配
        for (Map.Entry<String, Pattern> entry : INTENT_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(input).find()) {
                return entry.getKey();
            }
        }
        
        // 如果没有匹配，根据关键词智能判断默认意图
        String lowerInput = input.toLowerCase();
        
        // 包含问句特征，默认为查询
        if (input.contains("?") || input.contains("？") || 
            input.contains("什么") || input.contains("哪些") || 
            input.contains("多少") || input.contains("怎么")) {
            return "QUERY";
        }
        
        // 包含工作项相关词汇，默认为查询
        if (input.contains("工作项") || input.contains("任务") || 
            input.contains("bug") || input.contains("需求")) {
            return "QUERY";
        }
        
        return "UNKNOWN";
    }
    
    private Map<String, Object> extractEntities(String input) {
        Map<String, Object> entities = new HashMap<>();
        
        // 提取工作项ID (如: PROJ-123, BUG-001)
        Pattern idPattern = Pattern.compile("([A-Z]+-\\d+)");
        Matcher matcher = idPattern.matcher(input);
        if (matcher.find()) {
            entities.put("issueKey", matcher.group(1));
        }
        
        // 提取项目名称
        Pattern projectPattern = Pattern.compile("(PROJ|PROJECT|项目)[\\s]*([A-Za-z0-9-]+)");
        matcher = projectPattern.matcher(input);
        if (matcher.find()) {
            entities.put("projectKey", matcher.group(2));
        } else {
            // 尝试简单匹配
            projectPattern = Pattern.compile("项目[\\s]*([A-Za-z0-9]+)");
            matcher = projectPattern.matcher(input);
            if (matcher.find()) {
                entities.put("projectKey", matcher.group(1));
            }
        }
        
        // 提取工作项类型
        if (input.contains("bug") || input.contains("缺陷") || input.contains("问题")) {
            entities.put("type", "BUG");
        } else if (input.contains("任务") || input.contains("task")) {
            entities.put("type", "TASK");
        } else if (input.contains("故事") || input.contains("story") || input.contains("用户故事")) {
            entities.put("type", "STORY");
        } else if (input.contains("史诗") || input.contains("epic")) {
            entities.put("type", "EPIC");
        }
        
        // 提取状态
        if (input.contains("进行中") || input.contains("IN_PROGRESS")) {
            entities.put("status", "IN_PROGRESS");
        } else if (input.contains("已完成") || input.contains("CLOSED") || input.contains("关闭")) {
            entities.put("status", "CLOSED");
        } else if (input.contains("待处理") || input.contains("OPEN") || input.contains("打开")) {
            entities.put("status", "OPEN");
        } else if (input.contains("已解决") || input.contains("RESOLVED")) {
            entities.put("status", "RESOLVED");
        }
        
        // 提取优先级
        if (input.contains("高优先级") || input.contains("紧急") || input.contains("HIGH")) {
            entities.put("priority", "HIGH");
        } else if (input.contains("中优先级") || input.contains("MEDIUM")) {
            entities.put("priority", "MEDIUM");
        } else if (input.contains("低优先级") || input.contains("LOW")) {
            entities.put("priority", "LOW");
        }
        
        // 提取标题/描述（简化版）
        Pattern titlePattern = Pattern.compile("标题[是:：]+([^，。,!]+)");
        matcher = titlePattern.matcher(input);
        if (matcher.find()) {
            entities.put("title", matcher.group(1).trim());
        }
        
        // 提取负责人
        Pattern assigneePattern = Pattern.compile("负责人[是:：]+([^，。,!]+)");
        matcher = assigneePattern.matcher(input);
        if (matcher.find()) {
            entities.put("assignee", matcher.group(1).trim());
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
