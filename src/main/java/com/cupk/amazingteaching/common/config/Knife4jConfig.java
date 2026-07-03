package com.cupk.amazingteaching.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j API文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("码上学 智能教学平台 API")
                        .version("1.0.0")
                        .description("码上学 智能教学平台接口文档")
                        .contact(new Contact()
                                .name("码上学 Team")
                                .email("support@amazingteaching.com")));
    }
}
