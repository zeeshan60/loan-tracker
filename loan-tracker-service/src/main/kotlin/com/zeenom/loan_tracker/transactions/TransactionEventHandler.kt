package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.reverse
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

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