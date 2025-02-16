package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.events.EventDao
import com.zeenom.loan_tracker.events.CommandDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

@Service
class CommandCreateFriends(
    private val friendsDao: FriendsDao,
    private val eventDao: EventDao,
) : Command<CreateFriendDto> {
    override suspend fun execute(commandDto: CommandDto<CreateFriendDto>) {
        CoroutineScope(Dispatchers.IO).launch { eventDao.saveEvent(commandDto) }
        friendsDao.saveFriend(commandDto.userId, commandDto.payload)
    }
}
