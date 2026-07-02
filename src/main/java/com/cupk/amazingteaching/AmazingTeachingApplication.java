package com.cupk.amazingteaching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AmazingTeachingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmazingTeachingApplication.class, args);
        System.out.println("========================================");
        System.out.println("  AmazingTeaching 智能教学平台启动成功！");
        System.out.println("  API文档: http://localhost:8080/api/doc.html");
        System.out.println("========================================");
    }
}
