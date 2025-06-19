package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.events.CommandPayloadDto
import java.util.UUID

data class UserDto(
    val uid: UUID?,
    val userFBId: String, //Id coming from Firebase
    val email: String?,
    val phoneNumber: String?,
    val displayName: String,
    val currency: String?,
    val photoUrl: String?,
    val emailVerified: Boolean
) : CommandPayloadDto
