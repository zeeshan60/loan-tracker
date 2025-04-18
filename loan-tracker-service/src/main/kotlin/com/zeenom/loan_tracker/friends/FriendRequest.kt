package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandPayloadDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import java.util.*

@Schema(description = "Request to create a new friend. Either email or phone number must be provided.")
data class FriendRequest(
    @field:Schema(description = "Email address of the friend", example = "friend@example.com")
    @field:Email(message = "Invalid email format")
    var email: String? = null,

    @field:Schema(description = "Phone number of the friend", example = "+1234567890")
    @field:Pattern(
        regexp = "\\+?[0-9]+",
        message = "Invalid phone number format"
    )
    var phoneNumber: String? = null,

    @field:Schema(description = "Name of the friend", required = true, example = "John Doe")
    var name: String
) {
    init {
        if (email != null && email!!.isBlank()) {
            email = null
        }
        if (phoneNumber != null && phoneNumber!!.isBlank()) {
            phoneNumber = null
        }
        require(email != null || phoneNumber != null) {
            "Either email or phone number must be provided"
        }
    }
}

data class UpdateFriendRequest(
    @field:Email(message = "Invalid email format")
    var email: String?,
    @field:Pattern(
        regexp = "\\+?[0-9]+",
        message = "Invalid phone number format"
    )
    var phoneNumber: String?,
    var name: String?,
) {
    init {
        if (email != null && email!!.isBlank()) {
            email = null
        }
        if (phoneNumber != null && phoneNumber!!.isBlank()) {
            phoneNumber = null
        }
    }
}

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