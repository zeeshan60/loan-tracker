package com.zeenom.loan_tracker.events

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.friends.FriendService
import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.transactions.TransactionEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import com.zeenom.loan_tracker.users.UserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.util.*

@Service
class LoginUserCommand(
    private val userService: UserService,
    private val commandDao: CommandDao,
    private val friendService: FriendService,
    private val transactionEventHandler: TransactionEventHandler,
) : Command<UserDto> {
    override suspend fun execute(commandDto: CommandDto<UserDto>) {
        CoroutineScope(Dispatchers.IO).launch { commandDao.addCommand(commandDto) }
        userService.findUserById(commandDto.payload.uid) ?: let {
            userService.createUser(userDto = commandDto.payload)
            friendService.searchUsersImFriendOfAndAddThemAsMyFriends(commandDto.userId)

            friendService.findAllByUserId(commandDto.userId).friends.forEach { friend ->
                val (friendEvent, userStreamId) = friendService.findFriendAndUserStreamId(
                    userUid = commandDto.payload.uid,
                    userEmail = commandDto.payload.email,
                    userPhone = commandDto.payload.phoneNumber,
                    recipientId = friend.friendId
                )

                transactionEventHandler.addReverseEventsForUserAndFriend(
                    myUid = commandDto.payload.uid,
                    myStreamId = userStreamId!!,
                    friendUid = friendEvent!!.uid,
                    friendStreamid = friend.friendId
                )
            }
        }
    }
}