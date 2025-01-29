package com.zeenom.loan_tracker.controllers

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@RestController
class HealthCheckController {
    @GetMapping("/health")
    fun healthCheck(): Mono<MessageResponse> {
        return MessageResponse(message = "I'm alive!").toMono()
    }
}

@Configuration
class SwaggerConfiguration {
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI().info(
                Info()
                    .title("Loan Tracker API")
                    .version("1.0.0")
                    .description("API documentation for the Loan Tracker application")
            )
            .servers(
                listOf(
                    Server().url("https://b090-103-252-202-39.ngrok-free.app").description("Production server")
                )
            )
    }
}