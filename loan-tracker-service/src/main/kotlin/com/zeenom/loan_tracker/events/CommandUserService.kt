package com.zeenom.loan_tracker.events

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.friends.FriendsDao
import com.zeenom.loan_tracker.users.UserEventHandler
import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

@Service
class CommandCreateUser(
    private val userEventHandler: UserEventHandler,
    private val commandDao: CommandDao,
    private val friendsDao: FriendsDao,
) : Command<UserDto> {
    override suspend fun execute(commandDto: CommandDto<UserDto>) {
        CoroutineScope(Dispatchers.IO).launch { commandDao.saveEvent(commandDto) }
        userEventHandler.createIfNotExist(userDto = commandDto.payload)
        friendsDao.makeMyOwnersMyFriends(commandDto.userId)
    }
}