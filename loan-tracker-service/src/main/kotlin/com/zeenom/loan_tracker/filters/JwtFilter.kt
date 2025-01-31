package com.zeenom.loan_tracker.filters

import io.jsonwebtoken.Jwts
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtFilter : WebFilter {

    private val secretKey = "your-secret-key"

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request: ServerHttpRequest = exchange.request
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            validateToken(token)
                .flatMap { uid ->
                    exchange.attributes["userId"] = uid
                    chain.filter(exchange)
                }
                .onErrorResume {
                    exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    exchange.response.setComplete()
                }
        } else {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            exchange.response.setComplete()
        }
    }

    private fun validateToken(token: String): Mono<String> {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey.toByteArray())
                .build()
                .parseClaimsJws(token)
                .body

            Mono.just(claims.subject)
        } catch (e: Exception) {
            Mono.error(e)
        }
    }
}