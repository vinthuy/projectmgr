package com.workitem.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ContextManager {
    
    private final Map<String, Deque<Map<String, Object>>> sessionContexts = new ConcurrentHashMap<>();
    private static final int MAX_CONTEXT_SIZE = 10;
    
    public void addContext(String sessionId, Map<String, Object> context) {
        sessionContexts.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        Deque<Map<String, Object>> contexts = sessionContexts.get(sessionId);
        
        if (contexts.size() >= MAX_CONTEXT_SIZE) {
            contexts.pollFirst(); // 移除最旧的上下文
        }
        
        contexts.addLast(context);
        log.debug("添加上下文: sessionId={}, size={}", sessionId, contexts.size());
    }
    
    public Map<String, Object> getContext(String sessionId) {
        Deque<Map<String, Object>> contexts = sessionContexts.get(sessionId);
        if (contexts == null || contexts.isEmpty()) {
            return new HashMap<>();
        }
        
        // 合并所有上下文
        Map<String, Object> merged = new HashMap<>();
        for (Map<String, Object> ctx : contexts) {
            merged.putAll(ctx);
        }
        
        return merged;
    }
    
    public void clearContext(String sessionId) {
        sessionContexts.remove(sessionId);
        log.debug("清除上下文: sessionId={}", sessionId);
    }
}
