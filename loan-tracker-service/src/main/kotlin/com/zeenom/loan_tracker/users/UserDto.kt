package com.zeenom.loan_tracker.users

data class UserDto(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String,
    val emailVerified: Boolean
)