package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandPayloadDto
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a new friend. Either email or phone number must be provided.")
data class FriendRequest(
    @Schema(description = "Email address of the friend", example = "friend@example.com")
    val email: String?,

    @Schema(description = "Phone number of the friend", example = "+1234567890")
    val phoneNumber: String?,

    @Schema(description = "Name of the friend", required = true, example = "John Doe")
    val name: String,
)

data class UpdateFriendRequest(
    val email: String?,
    val phoneNumber: String?,
    val name: String?,
)

interface BaseFriendDto {
    val name: String?
    val email: String?
    val phoneNumber: String?
}

data class CreateFriendDto(
    override val email: String?,
    override val phoneNumber: String?,
    override val name: String,
) : CommandPayloadDto, BaseFriendDto

data class UpdateFriendDto(
    override val email: String?,
    override val phoneNumber: String?,
    override val name: String?,
    val friendId: UUID,
) : CommandPayloadDto, BaseFriendDto

data class DeleteFriendDto(
    val friendId: UUID,
) : CommandPayloadDto