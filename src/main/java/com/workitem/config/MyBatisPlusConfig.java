package com.workitem.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.workitem.mapper")
public class MyBatisPlusConfig {
    // MyBatis-Plus 会自动配置，无需手动创建 SqlSessionFactory
}
