package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.events.CommandPayloadDto

data class UserDto(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String,
    val photoUrl: String?,
    val emailVerified: Boolean
) : CommandPayloadDto
