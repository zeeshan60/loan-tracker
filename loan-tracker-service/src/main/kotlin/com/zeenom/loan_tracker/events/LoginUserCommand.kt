package com.zeenom.loan_tracker.events

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.transactions.TransactionEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

@Service
class LoginUserCommand(
    private val userEventHandler: UserEventHandler,
    private val commandDao: CommandDao,
    private val friendsEventHandler: FriendsEventHandler,
    private val transactionEventHandler: TransactionEventHandler,
) : Command<UserDto> {
    override suspend fun execute(commandDto: CommandDto<UserDto>) {
        CoroutineScope(Dispatchers.IO).launch { commandDao.addCommand(commandDto) }
        userEventHandler.findUserById(commandDto.payload.uid) ?: let {
            userEventHandler.createUser(userDto = commandDto.payload)
            friendsEventHandler.makeMyOwnersMyFriends(commandDto.userId)

            transactionEventHandler.addReverseTransactions(
                commandDto.payload,
                friendsEventHandler.findAllByUserId(commandDto.userId).friends)

        }
    }
}