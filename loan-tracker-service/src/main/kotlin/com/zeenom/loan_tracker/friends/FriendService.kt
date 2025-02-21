package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.TransactionReadModel
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class FriendService(
    private val friendsEventHandler: FriendsEventHandler,
    private val userEventHandler: UserEventHandler,
    private val transactionReadModel: TransactionReadModel,
) {
    suspend fun findAllByUserId(userId: String): FriendsDto = withContext(Dispatchers.IO) {
        val events = friendsEventHandler.findAllEventsByUserId(userId).toList()
        val amountsPerFriend = async { transactionReadModel.balancesOfFriends(userId, events.map { it.streamId }) }
        val usersByPhones =
            userEventHandler.findUsersByPhoneNumbers(events.mapNotNull { it.friendPhoneNumber }).toList()
                .associateBy { it.phoneNumber }
        val usersByEmails =
            userEventHandler.findUsersByEmails(events.filter { it.friendPhoneNumber !in usersByPhones.keys }
                .mapNotNull { it.friendEmail }).toList().associateBy { it.email }
        val friends = events.map {
            val user =
                it.friendPhoneNumber?.let { usersByPhones[it] } ?: it.friendEmail?.let { usersByEmails[it] }
            FriendDto(
                friendId = it.streamId,
                email = it.friendEmail,
                phoneNumber = it.friendPhoneNumber,
                name = it.friendDisplayName,
                photoUrl = user?.photoUrl,
                loanAmount = amountsPerFriend.await()[it.streamId]
            )
        }
        FriendsDto(friends)
    }

    suspend fun createFriend(userId: String, friendDto: CreateFriendDto) {
        if (friendDto.email == null && friendDto.phoneNumber == null) {
            throw IllegalArgumentException("Email or phone number is required")
        }

        val user = userEventHandler.findUserById(userId) ?: throw IllegalArgumentException("User $userId not found")
        if (user.email == friendDto.email || user.phoneNumber == friendDto.phoneNumber) {
            throw IllegalArgumentException("Your friend can't have same email or phone as yours")
        }

        if (friendDto.email != null)
            friendsEventHandler.findByUserUidAndFriendEmail(userId, friendDto.email)
                ?.let { throw IllegalArgumentException("Friend with email ${friendDto.email} already exist") }

        if (friendDto.phoneNumber != null)
            friendsEventHandler.findByUserUidAndFriendPhoneNumber(userId, friendDto.phoneNumber)
                ?.let { throw IllegalArgumentException("Friend with phone number ${friendDto.phoneNumber} already exist") }

        friendsEventHandler.saveFriend(userId, friendDto)
    }

    suspend fun searchUsersImFriendOfAndAddThemAsMyFriends(uid: String) {
        val user = userEventHandler.findUserById(uid) ?: throw IllegalArgumentException("User not found")
        val emailFriends = user.email?.let { friendsEventHandler.findByFriendEmail(user.email) } ?: emptyFlow()
        val phoneFriends =
            user.phoneNumber?.let { friendsEventHandler.findByFriendPhoneNumber(user.phoneNumber) } ?: emptyFlow()

        val myFriendIds = emailFriends.toList().plus(phoneFriends.toList()).map { it.userUid }.distinct()
        val friends =
            userEventHandler.findUsersByUids(myFriendIds).toList()

        friendsEventHandler.saveAllUsersAsFriends(uid, friends)
    }

}