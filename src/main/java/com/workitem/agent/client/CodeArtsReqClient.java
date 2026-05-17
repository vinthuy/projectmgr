package com.workitem.agent.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("agentCodeArtsReqClient")
public class CodeArtsReqClient {
    
    @Value("${codearts.api.base-url:https://openapi.huaweicloud.com}")
    private String baseUrl;
    
    @Value("${codearts.api.token:}")
    private String apiToken;
    
    @Value("${codearts.api.project-id:}")
    private String projectId;
    
    /**
     * 查询单个工作项
     */
    public Map<String, Object> getWorkItem(String workItemId) {
        log.info("查询工作项: workItemId={}", workItemId);
        
        String url = String.format("%s/v1/projects/%s/work-items/%s", 
            baseUrl, projectId, workItemId);
        
        try {
            HttpResponse response = HttpRequest.get(url)
                .header("X-Auth-Token", apiToken)
                .timeout(30000)
                .execute();
            
            if (!response.isOk()) {
                throw new RuntimeException("查询工作项失败: " + response.body());
            }
            
            return JSONUtil.toBean(response.body(), Map.class);
        } catch (Exception e) {
            log.error("查询工作项异常: workItemId={}", workItemId, e);
            throw new RuntimeException("查询工作项失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查询工作项列表
     */
    public List<Map<String, Object>> listWorkItems(String projectKey, 
                                                    Map<String, Object> filters) {
        log.info("查询工作项列表: projectKey={}, filters={}", projectKey, filters);
        
        String url = String.format("%s/v1/projects/%s/work-items", 
            baseUrl, projectId);
        
        try {
            Map<String, Object> queryParams = new HashMap<>();
            if (filters != null) {
                queryParams.putAll(filters);
            }
            
            HttpResponse response = HttpRequest.get(url)
                .header("X-Auth-Token", apiToken)
                .form(queryParams)
                .timeout(30000)
                .execute();
            
            if (!response.isOk()) {
                throw new RuntimeException("查询工作项列表失败: " + response.body());
            }
            
            Map<String, Object> result = JSONUtil.toBean(response.body(), Map.class);
            return (List<Map<String, Object>>) result.getOrDefault("items", List.of());
        } catch (Exception e) {
            log.error("查询工作项列表异常: projectKey={}", projectKey, e);
            throw new RuntimeException("查询工作项列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建工作项
     */
    public Map<String, Object> createWorkItem(Map<String, Object> workItem) {
        log.info("创建工作项: workItem={}", workItem);
        
        String url = String.format("%s/v1/projects/%s/work-items", 
            baseUrl, projectId);
        
        try {
            HttpResponse response = HttpRequest.post(url)
                .header("X-Auth-Token", apiToken)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(workItem))
                .timeout(30000)
                .execute();
            
            if (!response.isOk()) {
                throw new RuntimeException("创建工作项失败: " + response.body());
            }
            
            return JSONUtil.toBean(response.body(), Map.class);
        } catch (Exception e) {
            log.error("创建工作项异常: workItem={}", workItem, e);
            throw new RuntimeException("创建工作项失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新工作项
     */
    public Map<String, Object> updateWorkItem(String workItemId, 
                                               Map<String, Object> updates) {
        log.info("更新工作项: workItemId={}, updates={}", workItemId, updates);
        
        String url = String.format("%s/v1/projects/%s/work-items/%s", 
            baseUrl, projectId, workItemId);
        
        try {
            HttpResponse response = HttpRequest.put(url)
                .header("X-Auth-Token", apiToken)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(updates))
                .timeout(30000)
                .execute();
            
            if (!response.isOk()) {
                throw new RuntimeException("更新工作项失败: " + response.body());
            }
            
            return JSONUtil.toBean(response.body(), Map.class);
        } catch (Exception e) {
            log.error("更新工作项异常: workItemId={}", workItemId, e);
            throw new RuntimeException("更新工作项失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除工作项
     */
    public void deleteWorkItem(String workItemId) {
        log.info("删除工作项: workItemId={}", workItemId);
        
        String url = String.format("%s/v1/projects/%s/work-items/%s", 
            baseUrl, projectId, workItemId);
        
        try {
            HttpResponse response = HttpRequest.delete(url)
                .header("X-Auth-Token", apiToken)
                .timeout(30000)
                .execute();
            
            if (!response.isOk()) {
                throw new RuntimeException("删除工作项失败: " + response.body());
            }
        } catch (Exception e) {
            log.error("删除工作项异常: workItemId={}", workItemId, e);
            throw new RuntimeException("删除工作项失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取工作项统计
     */
    public Map<String, Object> getWorkItemStats(String projectKey) {
        log.info("获取工作项统计: projectKey={}", projectKey);
        
        String url = String.format("%s/v1/projects/%s/work-items/stats", 
            baseUrl, projectId);
        
        try {
            HttpResponse response = HttpRequest.get(url)
                .header("X-Auth-Token", apiToken)
                .timeout(30000)
                .execute();
            
            if (!response.isOk()) {
                throw new RuntimeException("获取统计失败: " + response.body());
            }
            
            return JSONUtil.toBean(response.body(), Map.class);
        } catch (Exception e) {
            log.error("获取工作项统计异常: projectKey={}", projectKey, e);
            throw new RuntimeException("获取统计失败: " + e.getMessage(), e);
        }
    }
}
