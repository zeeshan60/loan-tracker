package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.users.UserService
import org.springframework.stereotype.Service

@Service
class UpdateUserCommand(
    private val userService: UserService,
) {
    suspend fun execute(commandDto: CommandDto<UserUpdateDto>) {
        userService.updateUser(commandDto.payload)
    }
}