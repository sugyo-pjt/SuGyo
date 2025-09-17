package com.sugyo.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;

@Configuration
public class SwaggerConfig {

    //    @Value("${swagger.server-url}")
//    private String swaggerServerUrl;
//
//    @Bean
//    public OpenAPI openAPI() {
//        return new OpenAPI()
//                .info(apiInfo())
//                .servers(List.of(
//                        new Server().url(swaggerServerUrl).description("sugyo")
//                ))
//                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
//                .components(new Components()
//                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
//    }
//
//    private Info apiInfo() {
//        return new Info()
//                .title("Sugyo API")
//                .description("수어지교 백엔드 API 문서")
//                .version("1.0.0")
//                .contact(new Contact()
//                        .name("SSAFY A602")
//                        .email("contact@ssafy.com"))
//                .license(new License()
//                        .name("MIT License")
//                        .url("https://opensource.org/licenses/MIT"));
//    }
//
//    private SecurityScheme createAPIKeyScheme() {
//        return new SecurityScheme()
//                .type(SecurityScheme.Type.HTTP)
//                .bearerFormat("JWT")
//                .scheme("bearer");
//    }
//
//    @Bean
//    public ForwardedHeaderFilter forwardedHeaderFilter() {
//        return new ForwardedHeaderFilter();
//    }
    private final String jwtSchemaName = "bearerAuth";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .addSecurityItem(createSecurityRequirement())
                .components(createComponents());
    }

    private Info createApiInfo() {
        return new Info()
                .title("SuGyo API")
                .description("SuGyo API 문서입니다.")
                .version("1.0.0");
    }

    private SecurityRequirement createSecurityRequirement() {
        return new SecurityRequirement()
                .addList(jwtSchemaName);
    }

    private Components createComponents() {
        return new Components().addSecuritySchemes(jwtSchemaName, new SecurityScheme()
                .name(jwtSchemaName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization"));
    }
}
