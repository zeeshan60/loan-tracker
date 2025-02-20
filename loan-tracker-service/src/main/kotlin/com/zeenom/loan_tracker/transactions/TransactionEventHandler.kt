package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.reverse
import com.zeenom.loan_tracker.friends.FriendDto
import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class TransactionEventHandler(
    private val transactionEventRepository: TransactionEventRepository,
    private val userEventHandler: UserEventHandler,
    private val friendEventHandler: FriendsEventHandler,
    private val transactionReadModel: TransactionReadModel,
) {
    suspend fun addTransaction(userUid: String, transactionDto: TransactionDto) {
        val me = userEventHandler.findUserById(userUid)
            ?: throw IllegalArgumentException("User with id $userUid does not exist")
        val (friendUser, friendStreamId) = findFriendAndUserStreamId(
            userUid = userUid,
            userEmail = me.email,
            userPhone = me.phoneNumber,
            recipientId = transactionDto.recipientId
        )

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

        if (friendStreamId != null) {
            transactionEventRepository.save(
                event.reverse(friendUser!!.uid, friendStreamId)
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
        version = 1,
        eventType = eventType,
        createdBy = createdBy,
        description = description,
        splitType = splitType.reverse(),
        totalAmount = totalAmount,
    )

    suspend fun updateTransaction(userUid: String, transactionDto: TransactionDto) {
        val me = userEventHandler.findUserById(userUid)
            ?: throw IllegalArgumentException("User with id $userUid does not exist")
        val (friendUser, friendStreamId) = findFriendAndUserStreamId(
            userUid = userUid,
            userEmail = me.email,
            userPhone = me.phoneNumber,
            recipientId = transactionDto.recipientId
        )
        if (transactionDto.transactionStreamId == null) {
            throw IllegalArgumentException("Transaction stream id is required")
        }

        val existingTransaction = transactionReadModel.read(userUid, transactionDto.transactionStreamId)
            ?: throw IllegalArgumentException("Transaction with id ${transactionDto.transactionStreamId} does not exist")
        val existingCrossTransaction = transactionReadModel.read(friendUser!!.uid, transactionDto.transactionStreamId)
            ?: throw IllegalArgumentException("Transaction with id ${transactionDto.transactionStreamId} does not exist")

        transactionEventRepository.save(
            TransactionEvent(
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
                    streamId = transactionDto.transactionStreamId,
                    version = existingCrossTransaction.version + 1,
                    eventType = TransactionEventType.TRANSACTION_UPDATED,
                    createdBy = userUid,
                    description = transactionDto.description,
                    splitType = transactionDto.splitType,
                    totalAmount = transactionDto.originalAmount,
                )
            )
        }
    }

    private suspend fun findFriendAndUserStreamId(
        userUid: String,
        userEmail: String?,
        userPhone: String?,
        recipientId: UUID,
    ): Pair<UserDto?, UUID?> {
        val friend = friendEventHandler.findFriendByUserIdAndFriendId(userUid, recipientId)
            ?: throw IllegalArgumentException("User with id $userUid does not have friend with id $recipientId")
        val friendUser = userEventHandler.findUserByEmailOrPhoneNumber(friend.email, friend.phoneNumber)
        val friendStreamId = friendUser?.let {
            friendEventHandler.findFriendStreamIdByEmailOrPhoneNumber(friendUser.uid, userEmail, userPhone)
                ?: throw IllegalArgumentException("Friend with email ${friend.email} or phone number ${friend.phoneNumber} does not exist")
        }
        return Pair(friendUser, friendStreamId)
    }

    suspend fun addReverseTransactions(userDto: UserDto, friendDtos: List<FriendDto>) {

        friendDtos.forEach { friendDto ->
            val (friend, userStreamId) = findFriendAndUserStreamId(
                userUid = userDto.uid,
                userEmail = userDto.email,
                userPhone = userDto.phoneNumber,
                recipientId = friendDto.friendId
            )

            transactionEventRepository.findAllByUserUidAndRecipientId(friend!!.uid, userStreamId!!).toList()
                .forEach {
                    transactionEventRepository.save(
                        it.reverse(
                            userDto.uid,
                            friendDto.friendId
                        )
                    )
                }
        }
    }
}