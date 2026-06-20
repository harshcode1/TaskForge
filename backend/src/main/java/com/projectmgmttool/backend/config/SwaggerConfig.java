package com.projectmgmttool.backend.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.*;

@Configuration
public class SwaggerConfig {

    @Primary
    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Project Management Tool API")
                        .version("1.0")
                        .description("Backend APIs for TaskForge (Jira-like PM Tool)")
                        .contact(new Contact()
                                .name("Your Name")
                                .email("your@email.com")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-apis")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskForge API Documentation")
                        .version("1.0.0")
                        .description("Complete API documentation for TaskForge - A comprehensive project management tool similar to Jira")
                        .contact(new Contact()
                                .name("TaskForge Development Team")
                                .email("support@taskforge.com")
                                .url("https://taskforge.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authentication")));
    }
}
