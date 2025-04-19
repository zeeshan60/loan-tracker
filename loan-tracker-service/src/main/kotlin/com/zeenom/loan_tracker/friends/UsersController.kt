package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UsersController(
    private val updateUserCommand: UpdateUserCommand,
    private val userQuery: UserQuery,
) {

    @Operation(summary = "Get user", description = "Get user details")
    @GetMapping
    suspend fun getUser(@AuthenticationPrincipal userId: String): UserResponse {
        return userQuery.execute(userId).let {
            UserResponse(
                uid = it.uid,
                email = it.email,
                phoneNumber = it.phoneNumber,
                displayName = it.displayName,
                currency = it.currency,
                photoUrl = it.photoUrl,
                emailVerified = it.emailVerified
            )
        }
    }

    @Operation(summary = "Update user", description = "Update user details")
    @PutMapping
    suspend fun updateUser(
        @RequestBody userRequest: UpdateUserRequest,
        @AuthenticationPrincipal userId: String,
    ): UserResponse {
        updateUserCommand.execute(
            CommandDto(
                commandType = CommandType.UPDATE_USER,
                payload = UserUpdateDto(
                    uid = userId,
                    displayName = userRequest.displayName,
                    phoneNumber = userRequest.phoneNumber,
                    currency = userRequest.currency,
                ),
                userId = userId
            )
        )
        return userQuery.execute(userId).let {
            UserResponse(
                uid = it.uid,
                email = it.email,
                phoneNumber = it.phoneNumber,
                displayName = it.displayName,
                currency = it.currency,
                photoUrl = it.photoUrl,
                emailVerified = it.emailVerified
            )
        }
    }
}

data class UpdateUserRequest(
    @Schema(description = "User display name. Passing null will not update the field")
    var displayName: String?,
    @Schema(description = "User phone number. Passing null will not update the field")
    @field:Pattern(
        regexp = "\\+?[0-9]+",
        message = "Invalid phone number format"
    )
    var phoneNumber: String?,
    @Schema(description = "User currency. Passing null will not update the field")
    var currency: String?,
) {
    init {
        if (phoneNumber != null && phoneNumber!!.isBlank()) {
            phoneNumber = null
        }

        if (displayName != null && displayName!!.isBlank()) {
            displayName = null
        }

        if (currency != null && currency!!.isBlank()) {
            currency = null
        }
    }
}

data class UserResponse(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String,
    val currency: String?,
    val photoUrl: String?,
    val emailVerified: Boolean,
)