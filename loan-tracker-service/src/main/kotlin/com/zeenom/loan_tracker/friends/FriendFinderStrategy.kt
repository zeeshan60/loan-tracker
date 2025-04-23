package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.UserEventHandler
import org.springframework.stereotype.Service

@Service
class FriendFinderStrategy(
    private val friendsEventHandler: FriendsEventHandler,
    private val userEventHandler: UserEventHandler,
) {
    suspend fun findUserFriends(userId: String, includeDeleted: Boolean = false): List<FriendUserDto> {
        val friends = friendsEventHandler.findAllFriendsByUserId(userId = userId, includeDeleted = includeDeleted)
        val usersByPhones =
            userEventHandler.findUsersByPhoneNumbers(friends.mapNotNull { it.friendPhoneNumber })
                .associateBy { it.phoneNumber }
        val usersByEmails =
            userEventHandler.findUsersByEmails(friends.filter { it.friendPhoneNumber !in usersByPhones.keys }
                .mapNotNull { it.friendEmail }).associateBy { it.email }
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
                deleted = it.deleted,
            )
        }
    }

    suspend fun findUserFriend(userId: String, friendEmail: String?, friendPhone: String?): FriendUserDto {
        val friend = findFriendModel(userId = userId, friendEmail = friendEmail, friendPhone = friendPhone)
            ?: throw IllegalStateException("Friend not found with email or phone")
        val user = friend.friendPhoneNumber?.let { userEventHandler.findUserByPhoneNumber(friend.friendPhoneNumber) }
            ?: friend.friendEmail?.let { userEventHandler.findUserByEmail(friend.friendEmail) }
        return FriendUserDto(
            friendUid = user?.uid,
            friendStreamId = friend.streamId,
            email = friend.friendEmail,
            phoneNumber = friend.friendPhoneNumber,
            name = friend.friendDisplayName,
            photoUrl = user?.photoUrl,
            deleted = friend.deleted,
        )
    }

    suspend fun findFriendModel(
        userId: String,
        friendEmail: String?,
        friendPhone: String?
    ): FriendModel? {
        val friend = friendEmail?.let { friendsEventHandler.findByUserUidAndFriendEmail(userId, friendEmail) }
            ?: friendPhone?.let { friendsEventHandler.findByUserUidAndFriendPhoneNumber(userId, friendPhone) }
        return friend
    }
}