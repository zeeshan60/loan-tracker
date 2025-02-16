package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandPayloadDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to create a new friend. Either email or phone number must be provided.")
data class CreateFriendRequest(
    @Schema(description = "Email address of the friend", example = "friend@example.com")
    val email: String?,

    @Schema(description = "Phone number of the friend", example = "+1234567890")
    val phoneNumber: String?,

    @Schema(description = "Name of the friend", required = true, example = "John Doe")
    val name: String,
)

data class CreateFriendDto(
    val email: String?,
    val phoneNumber: String?,
    val name: String,
) : CommandPayloadDto