package com.zeenom.loan_tracker.configurations

import com.zeenom.loan_tracker.properties.SelfProperties
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun customOpenAPI(selfProperties: SelfProperties): OpenAPI {
        return OpenAPI().info(
            Info()
                .title("Loan Tracker API")
                .version("1.0.0")
                .description("API documentation for the Loan Tracker application")
        )
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .servers(
                listOf(
                    Server().url(selfProperties.apiUrl).description("Production server")
                )
            )
    }
}
