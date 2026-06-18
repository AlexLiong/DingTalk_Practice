package com.example.dingtalk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.dingtalk.mapper")
public class DingtalkAdminApplication {
    public static void main(String[] args) {
        var ctx = SpringApplication.run(DingtalkAdminApplication.class, args);
        String port = ctx.getEnvironment().getProperty("server.port", "8080");
        System.out.println("\n>>> 仿钉钉后端启动成功: http://localhost:" + port + "  (H2 控制台: /h2-console)\n");
    }
}
