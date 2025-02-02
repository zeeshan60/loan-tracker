package com.zeenom.loan_tracker.security

import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class AuthConverter : ServerAuthenticationConverter {
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val action = when(exchange.request.method) {
            HttpMethod.GET -> Action.READ
            HttpMethod.POST -> Action.CREATE
            HttpMethod.PUT -> Action.UPDATE
            HttpMethod.DELETE -> Action.DELETE
            else -> Action.UNKNOWN
        }
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            mono {
                InternalAuthToken(token, action)
            }
        } else {
            Mono.empty()
        }
    }
}