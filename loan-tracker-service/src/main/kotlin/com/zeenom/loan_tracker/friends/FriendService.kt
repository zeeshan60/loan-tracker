package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto
import com.zeenom.loan_tracker.transactions.TransactionEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class FriendService(
    private val friendsEventHandler: FriendsEventHandler,
    private val userEventHandler: UserEventHandler,
    private val transactionEventHandler: TransactionEventHandler,
    private val friendFinderStrategy: FriendFinderStrategy,
) {

    suspend fun findAllByUserId(userId: String): FriendsWithAllTimeBalancesDto = withContext(Dispatchers.IO) {
        val events = friendFinderStrategy.findUserFriends(userId)
        val amountsPerFriend =
            async { transactionEventHandler.balancesOfFriendsByCurrency(userId, events.map { it.friendStreamId }) }
        val friends = events.map {
            FriendDto(
                friendId = it.friendStreamId,
                email = it.email,
                phoneNumber = it.phoneNumber,
                name = it.name,
                photoUrl = it.photoUrl,
                mainCurrency = null, //TODO implement main currency
                balances = amountsPerFriend.await()[it.friendStreamId]?.values?.toList() ?: emptyList()
            )
        }
        val sortedByDescendingEntries =
            amountsPerFriend.await().values.map { amountsPerCurrency -> amountsPerCurrency.values }.flatten()
                .groupBy { it.currency }.entries.sortedByDescending { it.value.size }
        val main =
            sortedByDescendingEntries.firstOrNull()
        val other = sortedByDescendingEntries.drop(1).associate { it.key to it.value }
        val balance = AllTimeBalanceDto(
            main = main?.value?.sumOf {
                if (it.isOwed) it.amount else
                    -it.amount
            }?.let { AmountDto(it, main.key, it >= 0.toBigDecimal()) },
            other = other.map {
                val total = it.value.sumOf { amount -> amount.amount }
                AmountDto(total, it.key, total >= 0.toBigDecimal())
            }
        )
        FriendsWithAllTimeBalancesDto(
            friends = friends,
            balance = balance
        )
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


    suspend fun findFriendAndUserStreamId(
        userUid: String,
        userEmail: String?,
        userPhone: String?,
        recipientId: UUID,
    ): Pair<UserDto?, UUID?> {
        val friend = friendsEventHandler.findFriendByUserIdAndFriendId(userUid, recipientId)
            ?: throw IllegalArgumentException("User with id $userUid does not have friend with id $recipientId")
        val friendUser = userEventHandler.findUserByEmailOrPhoneNumber(friend.email, friend.phoneNumber)
        val userStreamId = friendUser?.let {
            friendsEventHandler.findFriendStreamIdByEmailOrPhoneNumber(friendUser.uid, userEmail, userPhone)
                ?: throw IllegalArgumentException("Friend with email ${friend.email} or phone number ${friend.phoneNumber} does not exist")
        }
        return Pair(friendUser, userStreamId)
    }

}