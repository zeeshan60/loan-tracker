package com.zeenom.loan_tracker.controllers

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun webHttpSecurity(
        http: ServerHttpSecurity,
        authManager: AuthManager,
        authConverter: AuthConverter
    ): SecurityWebFilterChain {
        return http {
            authorizeExchange {

                authorize("/health", permitAll)
                authorize("/login", permitAll)
                authorize("/actuator", permitAll)
                authorize("/actuator/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/api-docs/**", permitAll)

                authorize(anyExchange, authenticated)
            }
            exceptionHandling {
                authenticationEntryPoint = ServerAuthenticationEntryPoint { exchange, ex ->
                    Mono.fromRunnable {
                        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    }
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
}

@Component
class AuthManager : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return Mono.just(authentication.apply {
            isAuthenticated = true
        })
    }
}

@Component
class AuthConverter : ServerAuthenticationConverter {
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            Mono.just(InternalAuthToken(token))
        } else {
            Mono.empty()
        }
    }
}

class InternalAuthToken(private val token: String) : AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {

    override fun getCredentials(): Any {
        return token
    }

    override fun getPrincipal(): Any {
        return token
    }
}