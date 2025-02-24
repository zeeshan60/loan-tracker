package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.Instant
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
    ): Unit = withContext(Dispatchers.IO) {

        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            userUid = userUid,
            recipientId = transactionDto.recipientId
                ?: throw IllegalArgumentException("Recipient id is required to add new transaction")
        )

        val streamId = UUID.randomUUID()
        val event = TransactionCreated(
            userId = userUid,
            description = transactionDto.description,
            currency = transactionDto.currency.toString(),
            splitType = transactionDto.splitType,
            totalAmount = transactionDto.originalAmount,
            recipientId = transactionDto.recipientId,
            createdAt = Instant.now(),
            createdBy = userUid,
            streamId = streamId,
            version = 1
        )
        transactionEventHandler.addEvent(event)
        if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
            event.crossTransaction(
                friendUser.uid,
                userStreamId
            )
        )
    }

    suspend fun updateTransaction(
        userUid: String,
        transactionDto: TransactionDto,
    ): Unit = withContext(Dispatchers.IO) {

        if (transactionDto.transactionStreamId == null) {
            throw IllegalArgumentException("Transaction stream id is required")
        }

        val existing =
            transactionEventHandler.read(userUid, transactionDto.transactionStreamId) ?: throw IllegalArgumentException(
                "Transaction with id ${transactionDto.transactionStreamId} does not exist"
            )

        val recipientId = existing.recipientId
        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            userUid = userUid, recipientId = recipientId
        )
        var eventVersion = existing.version + 1
        val createdAt = Instant.now()
        if (existing.description != transactionDto.description) {
            val event = DescriptionChanged(
                userId = userUid,
                description = transactionDto.description,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = eventVersion++,
                recipientId = recipientId
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid,
                    userStreamId
                )
            )
        }

        if (existing.splitType != transactionDto.splitType) {
            val event = SplitTypeChanged(
                userId = userUid,
                splitType = transactionDto.splitType,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = eventVersion++,
                recipientId = recipientId
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid,
                    userStreamId
                )
            )
        }

        if (existing.totalAmount != transactionDto.originalAmount) {
            val event = TotalAmountChanged(
                userId = userUid,
                totalAmount = transactionDto.originalAmount,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = eventVersion++,
                recipientId = recipientId
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid,
                    userStreamId
                )
            )
        }

        if (existing.currency != transactionDto.currency.toString()) {
            val event = CurrencyChanged(
                userId = userUid,
                currency = transactionDto.currency.toString(),
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = eventVersion,
                recipientId = recipientId
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid,
                    userStreamId
                )
            )
        }
    }

    suspend fun friendUserAndMyStreamId(userUid: String, recipientId: UUID): Pair<UserDto?, UUID?> {
        val me = userEventHandler.findUserById(userUid)
            ?: throw IllegalArgumentException("User with id $userUid does not exist")
        val friend = friendsEventHandler.findFriendByUserIdAndFriendId(userUid, recipientId)
            ?: throw IllegalArgumentException("User with id $userUid does not have friend with id $recipientId")
        val friendUser = userEventHandler.findUserByEmailOrPhoneNumber(friend.email, friend.phoneNumber)
        val userStreamId = friendUser?.let {
            friendsEventHandler.findFriendStreamIdByEmailOrPhoneNumber(friendUser.uid, me.email, me.phoneNumber)
                ?: throw IllegalArgumentException("Friend with email ${friend.email} or phone number ${friend.phoneNumber} does not exist")
        }
        return Pair(friendUser, userStreamId)
    }

    suspend fun transactionsByFriendId(userId: String, friendId: UUID): List<TransactionDto> {
        return transactionEventHandler.transactionsByFriendId(userId, friendId)
    }

    suspend fun deleteTransaction(userUid: String, transactionStreamId: UUID) {
        val existing = transactionEventHandler.read(userUid, transactionStreamId)
            ?: throw IllegalArgumentException("Transaction with id $transactionStreamId does not exist")

        val recipientId = existing.recipientId
        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            userUid = userUid, recipientId = recipientId
        )
        val event = TransactionDeleted(
            userId = userUid,
            createdAt = Instant.now(),
            createdBy = userUid,
            streamId = transactionStreamId,
            version = existing.version + 1,
            recipientId = recipientId
        )
        transactionEventHandler.addEvent(
            event
        )
        if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
            event.crossTransaction(
                friendUser.uid,
                userStreamId
            )
        )
    }
}

