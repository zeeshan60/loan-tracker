package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandPayloadDto
import java.util.UUID

data class UserUpdateDto(
    val uid: UUID,
    val displayName: String?,
    val phoneNumber: String?,
    val currency: String?,
) : CommandPayloadDto