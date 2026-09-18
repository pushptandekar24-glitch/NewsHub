package com.apihub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at http://localhost:8080/swagger-ui.html
 *
 * The bearerAuth scheme adds an "Authorize" button, so protected endpoints can
 * be tested from the browser by pasting a JWT from /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI newsHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NewsHub API")
                        .version("0.1.0")
                        .description("Global news platform + API explorer. "
                                + "Article text belongs to the original publishers; "
                                + "this API serves metadata and links only.")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
