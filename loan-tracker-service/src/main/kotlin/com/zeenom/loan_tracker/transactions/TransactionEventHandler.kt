package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.reverse
import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.flow.toList
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

@Service
class TransactionEventHandler(
    private val transactionEventRepository: TransactionEventRepository,
    private val transactionReadModel: TransactionReadModel,
) {
    suspend fun addTransaction(
        userUid: String,
        friendUid: String?,
        userStreamId: UUID?,
        transactionDto: TransactionDto,
    ) {
        val transactionStreamId = UUID.randomUUID()
        val event = TransactionEvent(
            userUid = userUid,
            amount = transactionDto.amount.amount,
            currency = transactionDto.amount.currency.toString(),
            transactionType = if (transactionDto.amount.isOwed) TransactionType.CREDIT else TransactionType.DEBIT,
            recipientId = transactionDto.recipientId,
            createdAt = Instant.now(),
            streamId = transactionStreamId,
            version = 1,
            eventType = TransactionEventType.TRANSACTION_CREATED,
            createdBy = userUid,
            description = transactionDto.description,
            splitType = transactionDto.splitType,
            totalAmount = transactionDto.originalAmount,
        )
        transactionEventRepository.save(
            event
        )

        if (userStreamId != null && friendUid != null) {
            transactionEventRepository.save(
                event.reverse(friendUid, userStreamId)
            )
        }
    }

    private fun TransactionEvent.reverse(
        friendUserId: String,
        friendStreamId: UUID,
    ) = TransactionEvent(
        userUid = friendUserId,
        amount = amount,
        currency = currency,
        transactionType = if (transactionType == TransactionType.CREDIT) TransactionType.DEBIT else TransactionType.CREDIT,
        recipientId = friendStreamId,
        createdAt = createdAt,
        streamId = streamId,
        version = this.version,
        eventType = eventType,
        createdBy = createdBy,
        description = description,
        splitType = splitType.reverse(),
        totalAmount = totalAmount,
    )

    suspend fun updateTransaction(
        userUid: String,
        friendUid: String?,
        userStreamId: UUID?,
        transactionDto: TransactionDto,
    ) {
        if (transactionDto.transactionStreamId == null) {
            throw IllegalArgumentException("Transaction stream id is required")
        }

        val existingTransaction = transactionReadModel.read(userUid, transactionDto.transactionStreamId)
            ?: throw IllegalArgumentException("Transaction with id ${transactionDto.transactionStreamId} does not exist")

        val event = TransactionEvent(
            userUid = userUid,
            amount = transactionDto.amount.amount,
            currency = transactionDto.amount.currency.toString(),
            transactionType = if (transactionDto.amount.isOwed) TransactionType.CREDIT else TransactionType.DEBIT,
            recipientId = transactionDto.recipientId,
            createdAt = Instant.now(),
            streamId = transactionDto.transactionStreamId,
            version = existingTransaction.version + 1,
            eventType = TransactionEventType.TRANSACTION_UPDATED,
            createdBy = userUid,
            description = transactionDto.description,
            splitType = transactionDto.splitType,
            totalAmount = transactionDto.originalAmount,
        )
        transactionEventRepository.save(
            event
        )

        if (userStreamId != null && friendUid != null) {
            transactionEventRepository.save(
                event.reverse(friendUid, userStreamId)
            )
        }
    }

    suspend fun addReverseEventsForUserAndFriend(
        myUid: String,
        myStreamId: UUID,
        friendUid: String,
        friendStreamid: UUID
    ) {
        transactionEventRepository.findAllByUserUidAndRecipientId(friendUid, myStreamId).toList()
            .forEach {
                transactionEventRepository.save(
                    it.reverse(
                        myUid,
                        friendStreamid
                    )
                )
            }
    }
}