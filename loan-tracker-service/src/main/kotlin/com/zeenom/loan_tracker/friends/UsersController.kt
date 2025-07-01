package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import com.zeenom.loan_tracker.firebase.FirebaseService
import com.zeenom.loan_tracker.users.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UsersController(
    private val updateUserCommand: UpdateUserCommand,
    private val userQuery: UserQuery,
    private val userService: UserService,
    private val firebaseService: FirebaseService,
) {

    @Operation(summary = "Get user", description = "Get user details")
    @GetMapping
    suspend fun getUser(@AuthenticationPrincipal userId: UUID): UserResponse {
        return userQuery.execute(userId).let {
            UserResponse(
                uid = it.uid.toString(),
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
        @AuthenticationPrincipal userId: UUID,
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
                userId = userId,
                userFBId = null
            )
        )
        return userQuery.execute(userId).let {
            UserResponse(
                uid = it.uid.toString(),
                email = it.email,
                phoneNumber = it.phoneNumber,
                displayName = it.displayName,
                currency = it.currency,
                photoUrl = it.photoUrl,
                emailVerified = it.emailVerified
            )
        }
    }

    @Operation(summary = "Delete a user", description = "Delete user")
    @DeleteMapping
    suspend fun deleteUser(@AuthenticationPrincipal userId: UUID) {
        val deleted = userService.deleteUser(userId)
        firebaseService.deleteUserByFBId(deleted.uid)
    }
}

@Schema(description = "Request to update user details. Null values will not update the field")
data class UpdateUserRequest(
    var displayName: String?,
    @field:Pattern(
        regexp = "\\+?[0-9]+",
        message = "Invalid phone number format"
    )
    var phoneNumber: String?,
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