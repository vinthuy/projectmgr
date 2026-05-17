package com.workitem.agent;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AgentResponse {
    private String content;                    // AI回复内容（支持Markdown）
    private String intent;                     // 识别的意图
    private Double confidence;                 // 置信度
    private Map<String, Object> workItem;      // 相关工作项数据（单个）
    private List<Map<String, Object>> workItems; // 工作项列表
    private List<Map<String, Object>> suggestedActions; // 建议操作
    private Map<String, Object> metadata;      // 元数据
}
