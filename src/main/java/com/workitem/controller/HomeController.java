package com.workitem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Result<Map<String, Object>> home() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Work Item Management System");
        data.put("version", "1.0.0");
        data.put("description", "工单管理系统 - 支持动态字段配置");
        data.put("status", "running");
        
        return Result.success(data);
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }
}
