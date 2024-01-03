package com.agilemall.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenAPIConfig {
    @Bean
    OpenAPI orderOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Order API")
                        .description("Order application")
                        .version("v0.0.1")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("About Application")
                        .url("https://happykubepia.github.io/agilemall-axon"));
    }
}