package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.exceptions.NotFoundException
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserQuery(
    private val userService: UserService,
) {
    suspend fun execute(userId: UUID): UserDto {
        return userService.findUserById(userId) ?: throw NotFoundException("User not found")
    }
}