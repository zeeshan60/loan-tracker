package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UsersController(
    private val updateUserCommand: UpdateUserCommand,
    private val userQuery: UserQuery,
) {

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
    val displayName: String,
    val phoneNumber: String?,
    val currency: String?,
)

data class UserResponse(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String,
    val currency: String?,
    val photoUrl: String?,
    val emailVerified: Boolean,
)