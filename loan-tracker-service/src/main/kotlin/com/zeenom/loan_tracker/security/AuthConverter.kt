package com.zeenom.loan_tracker.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*

@Component
class AuthConverter(private val authProperties: AuthProperties) : ServerAuthenticationConverter {
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val action = when (exchange.request.method) {
            HttpMethod.GET -> Action.READ
            HttpMethod.POST -> Action.CREATE
            HttpMethod.PUT -> Action.UPDATE
            HttpMethod.DELETE -> Action.DELETE
            else -> Action.UNKNOWN
        }
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            mono {
                InternalAuthToken(
                    token = token,
                    principal = validateToken(token),
                    action = action
                )
            }
        } else {
            Mono.empty()
        }
    }

    fun validateToken(token: String): UUID {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(authProperties.secretKey.toByteArray())
                .build()
                .parseClaimsJws(token)
                .body.subject.let { UUID.fromString(it) }
        } catch (ex: ExpiredJwtException) {
            throw BadCredentialsException("JWT expired", ex) // Wrap as AuthenticationException
        } catch (ex: Exception) {
            throw BadCredentialsException("Invalid JWT", ex)
        }
    }
}