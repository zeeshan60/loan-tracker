package com.zeenom.loan_tracker.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.AuthorizeExchangeDsl
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets


@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun webHttpSecurity(
        http: ServerHttpSecurity,
        authManager: AuthManager,
        authConverter: AuthConverter
    ): SecurityWebFilterChain {
        return http {
            authorizeExchange {
                authorizePublicApis()
                authorize(anyExchange, authenticated)
            }
            exceptionHandling {
                authenticationEntryPoint = ServerAuthenticationEntryPoint { exchange, ex ->
                    logger.error("Error authenticating request", ex)
                    exchange.response.unauthorized(ex.message)
                }
            }
            addFilterAt(AuthenticationWebFilter(authManager).apply {
                setServerAuthenticationConverter(authConverter)
            }, SecurityWebFiltersOrder.AUTHENTICATION)
            httpBasic { disable() }
            formLogin { disable() }
            csrf { disable() }
        }
    }

    private fun AuthorizeExchangeDsl.authorizePublicApis() {
        authorize("/health", permitAll)
        authorize("/login", permitAll)
        authorize("/actuator", permitAll)
        authorize("/actuator/**", permitAll)
        authorize("/swagger-ui.html", permitAll)
        authorize("/swagger-ui/**", permitAll)
        authorize("/api-docs/**", permitAll)
    }

    fun ServerHttpResponse.unauthorized(message: String?): Mono<Void> {
        statusCode = HttpStatus.UNAUTHORIZED
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        headers.contentType = MediaType.APPLICATION_JSON

        val errorMessage =
            mapOf("error" to "Unauthorized", "message" to (message ?: "Unauthorized request"))
        val json = ObjectMapper().writeValueAsString(errorMessage)
        val buffer = bufferFactory().wrap(json.toByteArray(StandardCharsets.UTF_8))
        return writeWith(Mono.just(buffer))
    }
}

