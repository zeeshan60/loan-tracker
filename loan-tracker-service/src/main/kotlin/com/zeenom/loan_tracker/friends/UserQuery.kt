package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserService
import org.springframework.stereotype.Service

@Service
class UserQuery(
    private val userService: UserService,
) {
    suspend fun execute(userId: String): UserDto {
        return userService.findUserById(userId) ?: throw IllegalStateException("User not found")
    }
}