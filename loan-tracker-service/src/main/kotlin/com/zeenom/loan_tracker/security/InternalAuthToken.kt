package com.zeenom.loan_tracker.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import java.util.UUID

data class InternalAuthToken(
    private val token: String,
    private val principal: UUID,
    val action: Action,
    private val authenticated: Boolean = false
) : AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {

    override fun isAuthenticated(): Boolean {
        return authenticated
    }

    override fun getCredentials(): String {
        return token
    }

    override fun getPrincipal(): UUID {
        return principal
    }
}

enum class Action {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    UNKNOWN
}