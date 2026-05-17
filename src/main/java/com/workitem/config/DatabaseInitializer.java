package com.workitem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据库初始化验证器
 * 在应用启动时检查表是否创建成功
 */
@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("开始验证数据库表结构...");
        log.info("========================================");

        try {
            // 检查所有必需的表
            String[] requiredTables = {"work_item_type", "workflow_status", "field_definition", "work_item"};
            
            for (String tableName : requiredTables) {
                boolean exists = checkTableExists(tableName);
                if (exists) {
                    int count = getTableRowCount(tableName);
                    log.info("✓ 表 [{}] 存在，记录数: {}", tableName, count);
                } else {
                    log.error("✗ 表 [{}] 不存在！", tableName);
                }
            }

            // 显示统计信息
            log.info("========================================");
            log.info("数据库初始化验证完成！");
            log.info("========================================");
            
            printStatistics();

        } catch (Exception e) {
            log.error("数据库验证失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean checkTableExists(String tableName) {
        try {
            String sql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableName);
            return exists != null && exists;
        } catch (Exception e) {
            log.warn("检查表 {} 时出错: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 获取表记录数
     */
    private int getTableRowCount(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM " + tableName;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 打印统计信息
     */
    private void printStatistics() {
        try {
            // 工作项类型统计
            Integer typeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM work_item_type WHERE deleted = false", 
                Integer.class
            );
            log.info("工作项类型: {} 种", typeCount);

            // 工作流状态统计
            Integer statusCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workflow_status WHERE deleted = false", 
                Integer.class
            );
            log.info("工作流状态: {} 个", statusCount);

            // 字段定义统计
            Integer fieldCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM field_definition WHERE deleted = false", 
                Integer.class
            );
            log.info("字段定义: {} 个", fieldCount);

            // 工作项统计
            Integer workItemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM work_item WHERE deleted = false", 
                Integer.class
            );
            log.info("工作项: {} 个", workItemCount);

        } catch (Exception e) {
            log.warn("统计数据查询失败: {}", e.getMessage());
        }
    }
}
