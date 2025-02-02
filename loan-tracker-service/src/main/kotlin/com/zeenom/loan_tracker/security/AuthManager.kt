package com.zeenom.loan_tracker.security

import com.zeenom.loan_tracker.properties.AuthProperties
import io.jsonwebtoken.Jwts
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AuthManager(val authProperties: AuthProperties) : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return mono {
            doAuthenticate(authentication)
        }
    }

    suspend fun doAuthenticate(authentication: Authentication): Authentication {
        if (authentication is InternalAuthToken) {
            val token = authentication.credentials
            val uid = validateToken(token)
            val result = authorizeUser(authentication.action, uid)
            return authentication.copy(authenticated = result)
        }
        return authentication
    }

    suspend fun authorizeUser(action: Action, userId: String): Boolean {
        return true
    }

    fun validateToken(token: String): String {
        return Jwts.parserBuilder()
            .setSigningKey(authProperties.secretKey.toByteArray())
            .build()
            .parseClaimsJws(token)
            .body.subject
    }
}