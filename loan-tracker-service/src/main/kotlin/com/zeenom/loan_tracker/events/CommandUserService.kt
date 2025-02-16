package com.zeenom.loan_tracker.events

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.friends.FriendsDao
import com.zeenom.loan_tracker.users.UserEventDao
import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

@Service
class CommandCreateUser(
    private val userDao: UserEventDao,
    private val eventDao: EventDao,
    private val friendsDao: FriendsDao,
) : Command<UserDto> {
    override suspend fun execute(eventDto: EventDto<UserDto>) {
        CoroutineScope(Dispatchers.IO).launch { eventDao.saveEvent(eventDto) }
        userDao.loginUser(userDto = eventDto.payload)
        friendsDao.makeMyOwnersMyFriends(eventDto.userId)
    }
}