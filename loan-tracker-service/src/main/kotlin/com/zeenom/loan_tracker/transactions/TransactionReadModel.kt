package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.events.ReadModel
import com.zeenom.loan_tracker.friends.FriendEventRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class TransactionReadModel(
    private val transactionEventRepository: TransactionEventRepository,
    private val friendEventRepository: FriendEventRepository,
) : ReadModel<TransactionEvent> {

    private val transactionEventState: TransactionEventState = TransactionEventState()
    override suspend fun apply(existing: TransactionEvent, next: TransactionEvent): TransactionEvent {
        return transactionEventState.apply(existing, next)
    }

    override suspend fun read(userId: String, streamId: UUID): TransactionEvent? {
        return transactionEventRepository
            .findAllByUserUidAndStreamId(userId, streamId).toList()
            .reduceOrNull { current, next -> transactionEventState.apply(current, next) }
    }

    suspend fun transactionsByFriendId(userId: String, friendId: UUID): List<TransactionDto> {
        val friend = friendEventRepository.findByUserUidAndStreamId(userId, friendId) ?: throw IllegalArgumentException(
            "Friend not found"
        )
        return transactionEventRepository
            .findAllByUserUidAndRecipientId(userId, friendId).toList().let { this.resolveAll(it) }.map {
                TransactionDto(
                    amount = AmountDto(
                        amount = it.amount,
                        currency = Currency.getInstance(it.currency),
                        isOwed = it.transactionType == TransactionType.CREDIT
                    ),
                    recipientId = it.recipientId,
                    transactionStreamId = it.streamId,
                    description = it.description,
                    originalAmount = it.totalAmount,
                    splitType = it.splitType,
                    recipientName = friend.friendDisplayName
                )
            }
    }

    suspend fun balancesOfFriends(userId: String, friendIds: List<UUID>): Map<UUID, AmountDto> {
        return transactionEventRepository
            .findAllByUserUidAndRecipientIdIn(userId, friendIds).toList()
            .let { this.resolveAll(it) }
            .groupBy { it.currency }.entries.firstOrNull()?.let {
                val currency = it.key
                it.value.groupBy { it.recipientId }
                    .mapValues { (_, events) ->
                        val balance =
                            events.map { if (it.transactionType == TransactionType.CREDIT) it.amount else -it.amount }
                                .reduceOrNull { current, next -> current + next } ?: BigDecimal.ZERO
                        AmountDto(
                            amount = if (balance > BigDecimal.ZERO) balance else balance * BigDecimal(-1),
                            currency = Currency.getInstance(currency),
                            isOwed = balance > BigDecimal.ZERO
                        )
                    }
            } ?: emptyMap()
    }
}

@Component
class TransactionEventState {
    fun apply(existing: TransactionEvent, next: TransactionEvent): TransactionEvent {
        return when (next.eventType) {
            TransactionEventType.TRANSACTION_UPDATED -> {
                next
            }

            else -> throw IllegalArgumentException("Transaction event type not supported")
        }
    }
}