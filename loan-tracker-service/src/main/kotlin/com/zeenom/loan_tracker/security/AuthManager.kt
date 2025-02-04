package com.zeenom.loan_tracker.security

import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AuthManager : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return mono {
            doAuthenticate(authentication)
        }
    }

    suspend fun doAuthenticate(authentication: Authentication): Authentication {
        if (authentication is InternalAuthToken) {
            val result = authorizeUser(authentication.action, authentication.principal)
            return authentication.copy(authenticated = result)
        }
        return authentication
    }

    suspend fun authorizeUser(action: Action, userId: String): Boolean {
        return true
    }
}