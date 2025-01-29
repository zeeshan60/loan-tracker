package com.zeenom.loan_tracker.configurations

import com.zeenom.loan_tracker.properties.SelfProperties
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfiguration {
    @Bean
    fun customOpenAPI(selfProperties: SelfProperties): OpenAPI {
        return OpenAPI().info(
            Info()
                .title("Loan Tracker API")
                .version("1.0.0")
                .description("API documentation for the Loan Tracker application")
        )
            .servers(
                listOf(
                    Server().url(selfProperties.apiUrl).description("Production server")
                )
            )
    }
}

