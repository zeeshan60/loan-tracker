package com.zeenom.loan_tracker.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils

data class InternalAuthToken(
    private val token: String,
    val action: Action,
    private val authenticated: Boolean = false
) : AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {

    override fun isAuthenticated(): Boolean {
        return authenticated
    }

    override fun getCredentials(): String {
        return token
    }

    override fun getPrincipal(): String {
        return token
    }
}

enum class Action {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    UNKNOWN
}