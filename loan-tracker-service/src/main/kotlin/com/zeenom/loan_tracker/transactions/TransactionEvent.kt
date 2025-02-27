package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.events.IEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
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
    val transactionDate: Instant,
    val description: String?,
    val currency: String?,
    val splitType: SplitType?,
    val totalAmount: BigDecimal?,
    val recipientId: UUID,
    val createdAt: Instant,
    val createdBy: String,
    val streamId: UUID,
    val version: Int,
    val eventType: TransactionEventType,
) {
    fun toEvent(): IEvent<TransactionModel> {
        return when (eventType) {
            TransactionEventType.TRANSACTION_CREATED -> TransactionCreated(
                id = id,
                userId = userUid,
                description = description ?: throw IllegalStateException("Description is required"),
                currency = currency ?: throw IllegalStateException("Currency is required"),
                splitType = splitType ?: throw IllegalStateException("Split type is required"),
                totalAmount = totalAmount ?: throw IllegalStateException("Total amount is required"),
                recipientId = recipientId,
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
                transactionDate = transactionDate
            )

            TransactionEventType.DESCRIPTION_CHANGED -> DescriptionChanged(
                id = id,
                userId = userUid,
                description = description ?: throw IllegalStateException("Description is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
                transactionDate = transactionDate,
                recipientId = recipientId,
            )

            TransactionEventType.SPLIT_TYPE_CHANGED -> SplitTypeChanged(
                id = id,
                userId = userUid,
                splitType = splitType ?: throw IllegalStateException("Split type is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
                transactionDate = transactionDate,
                recipientId = recipientId
            )

            TransactionEventType.TOTAL_AMOUNT_CHANGED -> TotalAmountChanged(
                id = id,
                userId = userUid,
                totalAmount = totalAmount ?: throw IllegalStateException("Total amount is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
                transactionDate = transactionDate,
                recipientId = recipientId,
            )

            TransactionEventType.CURRENCY_CHANGED -> CurrencyChanged(
                id = id,
                userId = userUid,
                currency = currency ?: throw IllegalStateException("Currency is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
                transactionDate = transactionDate,
                recipientId = recipientId
            )

            TransactionEventType.TRANSACTION_DELETED -> TransactionDeleted(
                id = id,
                userId = userUid,
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
                transactionDate = transactionDate,
                recipientId = recipientId
            )
        }
    }
}

enum class TransactionEventType {
    TRANSACTION_CREATED,
    DESCRIPTION_CHANGED,
    SPLIT_TYPE_CHANGED,
    TOTAL_AMOUNT_CHANGED,
    CURRENCY_CHANGED,
    TRANSACTION_DELETED
}

@Repository
interface TransactionEventRepository : CoroutineCrudRepository<TransactionEvent, UUID> {
    @Query("SELECT * FROM transaction_events WHERE user_uid = :userId AND recipient_id = :recipientId order by stream_id desc, version")
    suspend fun findAllByUserUidAndRecipientId(userId: String, recipientId: UUID): Flow<TransactionEvent>
    @Query("SELECT * FROM transaction_events WHERE user_uid = :userId order by stream_id desc, version desc")
    suspend fun findAllByUserUid(userId: String): Flow<TransactionEvent>
    suspend fun findAllByUserUidAndRecipientIdIn(
        userId: String,
        recipientIds: List<UUID>
    ): Flow<TransactionEvent> {
        if (recipientIds.isEmpty()) {
            return emptyFlow()
        }
        return findAllByUserUidAndRecipientIdInInternal(userId, recipientIds)
    }

    @Query("SELECT * FROM transaction_events WHERE user_uid = :userId AND recipient_id IN (:recipientIds) order by stream_id desc, version")
    suspend fun findAllByUserUidAndRecipientIdInInternal(
        userId: String,
        recipientIds: List<UUID>
    ): Flow<TransactionEvent>

    @Query("SELECT * FROM transaction_events WHERE user_uid = :userId AND stream_id = :streamId order by version")
    suspend fun findAllByUserUidAndStreamId(userId: String, streamId: UUID): Flow<TransactionEvent>
}
