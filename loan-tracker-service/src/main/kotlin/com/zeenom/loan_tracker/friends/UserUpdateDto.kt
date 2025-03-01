package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandPayloadDto

data class UserUpdateDto(
    val uid: String,
    val displayName: String,
    val phoneNumber: String?,
    val currency: String?,
) : CommandPayloadDto