package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.events.IEvent
import kotlinx.coroutines.flow.Flow
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Table("transaction_events")
data class TransactionEvent(
    @Id private val id: UUID? = null,
    val userUid: String,
    val description: String?,
    val amount: BigDecimal?,
    val currency: String?,
    val transactionType: TransactionType?,
    val splitType: SplitType?,
    val totalAmount: BigDecimal?,
    val recipientId: UUID?,
    val createdAt: Instant,
    val createdBy: String,
    val streamId: UUID,
    val version: Int,
    val eventType: TransactionEventType,
) {
    fun toEvent(): IEvent<TransactionModel> {
        return when (eventType) {
            TransactionEventType.TRANSACTION_CREATED -> TransactionCreated(
                userId = userUid,
                description = description ?: throw IllegalStateException("Description is required"),
                amount = amount ?: throw IllegalStateException("Amount is required"),
                currency = currency ?: throw IllegalStateException("Currency is required"),
                transactionType = transactionType ?: throw IllegalStateException("Transaction type is required"),
                splitType = splitType ?: throw IllegalStateException("Split type is required"),
                totalAmount = totalAmount ?: throw IllegalStateException("Total amount is required"),
                recipientId = recipientId ?: throw IllegalStateException("Recipient ID is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )
            TransactionEventType.DESCRIPTION_CHANGED -> DescriptionChanged(
                userId = userUid,
                description = description ?: throw IllegalStateException("Description is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )
            TransactionEventType.SPLIT_TYPE_CHANGED -> SplitTypeChanged(
                userId = userUid,
                splitType = splitType ?: throw IllegalStateException("Split type is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )
            TransactionEventType.TOTAL_AMOUNT_CHANGED -> TotalAmountChanged(
                userId = userUid,
                totalAmount = totalAmount ?: throw IllegalStateException("Total amount is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )
            TransactionEventType.CURRENCY_CHANGED -> CurrencyChanged(
                userId = userUid,
                currency = currency ?: throw IllegalStateException("Currency is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )
        }
    }
}

enum class TransactionType {
    CREDIT, DEBIT
}

enum class TransactionEventType {
    TRANSACTION_CREATED,
    DESCRIPTION_CHANGED,
    SPLIT_TYPE_CHANGED,
    TOTAL_AMOUNT_CHANGED,
    CURRENCY_CHANGED
}

@Repository
interface TransactionEventRepository : CoroutineCrudRepository<TransactionEvent, UUID> {
    suspend fun findAllByUserUidAndRecipientId(userId: String, recipientId: UUID): Flow<TransactionEvent>
    suspend fun findAllByUserUidAndRecipientIdIn(userId: String, recipientIds: List<UUID>): Flow<TransactionEvent>
    suspend fun findAllByUserUidAndStreamId(userId: String, streamId: UUID): Flow<TransactionEvent>
}
