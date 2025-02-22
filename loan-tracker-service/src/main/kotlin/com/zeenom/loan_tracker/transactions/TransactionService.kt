package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import org.springframework.stereotype.Service
import java.util.*

@Service
class TransactionService(
    private val transactionEventHandler: TransactionEventHandler,
    private val userEventHandler: UserEventHandler,
    private val friendsEventHandler: FriendsEventHandler,
) {
    suspend fun addTransaction(
        userUid: String,
        transactionDto: TransactionDto,
    ) {

        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            userUid = userUid,
            recipientId = transactionDto.recipientId
        )
        transactionEventHandler.addTransaction(
            userUid = userUid,
            friendUid = friendUser?.uid,
            userStreamId = userStreamId,
            transactionDto = transactionDto
        )
    }

    suspend fun updateTransaction(
        userUid: String,
        transactionDto: TransactionDto,
    ) {
        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            userUid = userUid,
            recipientId = transactionDto.recipientId
        )
        transactionEventHandler.updateTransaction(
            userStreamId = userStreamId,
            transactionDto = transactionDto,
            userUid = userUid,
            friendUid = friendUser?.uid,
        )
    }

    suspend fun transactionsByFriendId(userUid: String, friendId: UUID): List<TransactionDto> {
        return transactionEventHandler.transactionsByFriendId(userUid, friendId)
    }

    suspend fun friendUserAndMyStreamId(userUid:String, recipientId: UUID): Pair<UserDto?, UUID?> {
        val me = userEventHandler.findUserById(userUid) ?: throw IllegalArgumentException("User with id $userUid does not exist")
        val friend = friendsEventHandler.findFriendByUserIdAndFriendId(userUid, recipientId)
            ?: throw IllegalArgumentException("User with id $userUid does not have friend with id $recipientId")
        val friendUser = userEventHandler.findUserByEmailOrPhoneNumber(friend.email, friend.phoneNumber)
        val userStreamId = friendUser?.let {
            friendsEventHandler.findFriendStreamIdByEmailOrPhoneNumber(friendUser.uid, me.email, me.phoneNumber)
                ?: throw IllegalArgumentException("Friend with email ${friend.email} or phone number ${friend.phoneNumber} does not exist")
        }
        return Pair(friendUser, userStreamId)
    }
}