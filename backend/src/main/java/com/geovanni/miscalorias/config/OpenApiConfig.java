package com.geovanni.miscalorias.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Recomp3B is the commercial name shown in the API docs; the Java package and
 * artifactId stay as misCalorias.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI recomp3bOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Recomp3B API")
                .version("v1")
                .description("Personal body recomposition and nutrition tracking API"));
    }
}
