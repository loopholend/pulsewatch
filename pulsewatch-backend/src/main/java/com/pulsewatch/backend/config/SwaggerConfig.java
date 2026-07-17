package com.pulsewatch.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI pulseWatchOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("PulseWatch API")
                .description("API documentation for the PulseWatch monitoring and alerting system.")
                .version("v1.0.0"));
    }

    @Bean
    public OpenApiCustomizer globalResponsesCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
            ApiResponses apiResponses = operation.getResponses();
            
            // Standardize common error responses across all endpoints
            if (!apiResponses.containsKey("400")) {
                apiResponses.addApiResponse("400", new ApiResponse().description("Bad Request - Validation or logic error"));
            }
            if (!apiResponses.containsKey("401")) {
                apiResponses.addApiResponse("401", new ApiResponse().description("Unauthorized - Missing or invalid token"));
            }
            if (!apiResponses.containsKey("403")) {
                apiResponses.addApiResponse("403", new ApiResponse().description("Forbidden - Insufficient permissions"));
            }
            if (!apiResponses.containsKey("404")) {
                apiResponses.addApiResponse("404", new ApiResponse().description("Not Found - Resource does not exist"));
            }
            if (!apiResponses.containsKey("500")) {
                apiResponses.addApiResponse("500", new ApiResponse().description("Internal Server Error"));
            }
        }));
    }
}
