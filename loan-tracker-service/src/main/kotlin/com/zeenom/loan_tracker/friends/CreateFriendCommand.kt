package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.events.CommandDao
import com.zeenom.loan_tracker.events.CommandDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

@Service
class CreateFriendCommand(
    private val friendsEventHandler: FriendsEventHandler,
    private val commandDao: CommandDao,
) : Command<CreateFriendDto> {
    override suspend fun execute(commandDto: CommandDto<CreateFriendDto>) {
        CoroutineScope(Dispatchers.IO).launch { commandDao.saveEvent(commandDto) }
        friendsEventHandler.saveFriend(commandDto.userId, commandDto.payload)
    }
}
