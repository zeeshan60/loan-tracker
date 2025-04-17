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
    private val friendService: FriendService,
    private val commandDao: CommandDao,
) : Command<CreateFriendDto> {
    override suspend fun execute(commandDto: CommandDto<CreateFriendDto>) {
        CoroutineScope(Dispatchers.IO).launch { commandDao.addCommand(commandDto) }
        friendService.createFriend(commandDto.userId, commandDto.payload)
    }
}

@Service
class UpdateFriendCommand(
    private val friendService: FriendService,
    private val commandDao: CommandDao,
) : Command<UpdateFriendDto> {
    override suspend fun execute(commandDto: CommandDto<UpdateFriendDto>) {
        CoroutineScope(Dispatchers.IO).launch { commandDao.addCommand(commandDto) }
        friendService.updateFriend(commandDto.userId, commandDto.payload)
    }
}