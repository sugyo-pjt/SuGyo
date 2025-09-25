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
