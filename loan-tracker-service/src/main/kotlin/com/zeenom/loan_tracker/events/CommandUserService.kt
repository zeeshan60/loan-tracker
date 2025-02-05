package com.zeenom.loan_tracker.events

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.users.UserDao
import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

@Service
class CommandCreateUser(
    private val userDao: UserDao,
    private val eventDao: EventDao
) : Command<UserDto> {
    override suspend fun execute(eventDto: EventDto<UserDto>): Unit = coroutineScope {
        launch { eventDao.saveEvent(eventDto) }
        userDao.createUser(userDto = eventDto.payload)
    }
}