package com.zeenom.loan_tracker.users

import java.time.Instant

data class UserDto(
    val uid: String,
    val email: String?,
    val displayName: String,
    val photoUrl: String,
    val emailVerified: Boolean,
    val updatedAt: Instant? = null
)
