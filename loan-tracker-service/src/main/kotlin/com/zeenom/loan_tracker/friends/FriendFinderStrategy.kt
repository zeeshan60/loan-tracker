package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class FriendFinderStrategy(
    private val friendsEventHandler: FriendsEventHandler,
    private val userEventHandler: UserEventHandler,
) {
    suspend fun findUserFriends(userId: String): List<FriendUserDto> {
        val friends = friendsEventHandler.findAllEventsByUserId(userId).toList()
        val usersByPhones =
            userEventHandler.findUsersByPhoneNumbers(friends.mapNotNull { it.friendPhoneNumber }).toList()
                .associateBy { it.phoneNumber }
        val usersByEmails =
            userEventHandler.findUsersByEmails(friends.filter { it.friendPhoneNumber !in usersByPhones.keys }
                .mapNotNull { it.friendEmail }).toList().associateBy { it.email }
        return friends.map {
            val user =
                it.friendPhoneNumber?.let { usersByPhones[it] } ?: it.friendEmail?.let { usersByEmails[it] }
            FriendUserDto(
                friendUid = user?.uid,
                friendStreamId = it.streamId,
                email = it.friendEmail,
                phoneNumber = it.friendPhoneNumber,
                name = it.friendDisplayName,
                photoUrl = user?.photoUrl,
            )
        }
    }
}