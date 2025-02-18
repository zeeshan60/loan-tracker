package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.users.UserEventHandler
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class TransactionEventHandler(
    private val transactionEventRepository: TransactionEventRepository,
    private val userEventHandler: UserEventHandler,
    private val friendEventHandler: FriendsEventHandler,
) {
    suspend fun addTransaction(userUid: String, transactionDto: TransactionDto) {
        val me = userEventHandler.findUserById(userUid)
            ?: throw IllegalArgumentException("User with id $userUid does not exist")
        val friend = friendEventHandler.findFriendByUserIdAndFriendId(userUid, transactionDto.recipientId)
            ?: throw IllegalArgumentException("User with id $userUid does not have friend with id ${transactionDto.recipientId}")

        val friendUser = userEventHandler.findUserByEmailOrPhoneNumber(friend.email, friend.phoneNumber)
        val friendStreamId = friendUser?.let {
            friendEventHandler.findFriendStreamIdByEmailOrPhoneNumber(friendUser.uid, me.email, me.phoneNumber)
                ?: throw IllegalArgumentException("Friend with email ${friend.email} or phone number ${friend.phoneNumber} does not exist")
        }

        transactionEventRepository.save(
            TransactionEvent(
                userUid = userUid,
                amount = transactionDto.amount.amount,
                currency = transactionDto.amount.currency.toString(),
                transactionType = if (transactionDto.amount.isOwed) TransactionType.CREDIT else TransactionType.DEBIT,
                recipientId = transactionDto.recipientId,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = TransactionEventType.TRANSACTION_CREATED,
                createdBy = userUid
            )
        )

        if (friendStreamId != null) {
            transactionEventRepository.save(
                TransactionEvent(
                    userUid = friendUser.uid,
                    amount = transactionDto.amount.amount,
                    currency = transactionDto.amount.currency.toString(),
                    transactionType = if (transactionDto.amount.isOwed) TransactionType.DEBIT else TransactionType.CREDIT,
                    recipientId = friendStreamId,
                    createdAt = Instant.now(),
                    streamId = UUID.randomUUID(),
                    version = 1,
                    eventType = TransactionEventType.TRANSACTION_CREATED,
                    createdBy = userUid
                )
            )
        }
    }
}