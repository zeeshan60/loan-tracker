package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.events.ReadModel
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class TransactionReadModel(
    private val transactionEventRepository: TransactionEventRepository,
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
            TransactionEventType.TRANSACTION_CREATED -> {
                existing.copy(
                    amount = next.amount,
                    currency = next.currency,
                    transactionType = next.transactionType,
                    recipientId = next.recipientId,
                    createdAt = next.createdAt,
                    version = next.version
                )
            }
        }
    }
}