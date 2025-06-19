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
import javax.imageio.ImageIO

interface IEventAble<T> {
    fun toEvent(): IEvent<T>
}

@Table("transaction_events")
data class TransactionEvent(
    @Id private val id: UUID? = null,
    val userId: UUID?, //TODO zeeshan make userid nullable
    val transactionDate: Instant?, //TODO zeeshan make transactionDate nullable
    val description: String?,
    val currency: String?,
    val splitType: SplitType?,
    val totalAmount: BigDecimal?,
    val recipientId: UUID?, //TODO zeeshan make recipientId nullable
    val createdAt: Instant,
    val createdBy: UUID,
    val streamId: UUID,
    val version: Int,
    val eventType: TransactionEventType,
) : IEventAble<TransactionModel> {
    override fun toEvent(): IEvent<TransactionModel> {
        return when (eventType) {
            TransactionEventType.TRANSACTION_CREATED -> TransactionCreated(
                id = id,
                userId = userId ?: throw IllegalStateException("User ID is required"),
                description = description ?: throw IllegalStateException("Description is required"),
                currency = currency ?: throw IllegalStateException("Currency is required"),
                splitType = splitType ?: throw IllegalStateException("Split type is required"),
                totalAmount = totalAmount ?: throw IllegalStateException("Total amount is required"),
                recipientId = recipientId ?: throw IllegalStateException("Recipient ID is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
                transactionDate = transactionDate ?: throw IllegalStateException("Transaction date is required"),
            )

            TransactionEventType.DESCRIPTION_CHANGED -> DescriptionChanged(
                id = id,
                description = description ?: throw IllegalStateException("Description is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )

            TransactionEventType.SPLIT_TYPE_CHANGED -> SplitTypeChanged(
                id = id,
                splitType = splitType ?: throw IllegalStateException("Split type is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )

            TransactionEventType.TOTAL_AMOUNT_CHANGED -> TotalAmountChanged(
                id = id,
                totalAmount = totalAmount ?: throw IllegalStateException("Total amount is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )

            TransactionEventType.CURRENCY_CHANGED -> CurrencyChanged(
                id = id,
                currency = currency ?: throw IllegalStateException("Currency is required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )

            TransactionEventType.TRANSACTION_DELETED -> TransactionDeleted(
                id = id,
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
            )

            TransactionEventType.TRANSACTION_DATE_CHANGED -> TransactionDateChanged(
                id = id,
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version,
                transactionDate = transactionDate ?: throw IllegalStateException("Transaction date is required")
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
    TRANSACTION_DELETED,
    TRANSACTION_DATE_CHANGED
}

@Repository
interface TransactionEventRepository : CoroutineCrudRepository<TransactionEvent, UUID> {
    @Query("SELECT * FROM transaction_events WHERE user_uid = :userId AND recipient_id = :recipientId order by stream_id desc, version")
    suspend fun findAllByUserUidAndRecipientId(userId: UUID, recipientId: UUID): Flow<TransactionEvent>

    @Query("SELECT * FROM transaction_events WHERE user_uid = :userId order by stream_id desc, version desc")
    suspend fun findAllByUserUid(userId: UUID): Flow<TransactionEvent>
    suspend fun findAllByUserUidAndRecipientIdIn(
        userId: UUID,
        recipientIds: List<UUID>
    ): Flow<TransactionEvent> {
        if (recipientIds.isEmpty()) {
            return emptyFlow()
        }
        return findAllByUserUidAndRecipientIdInInternal(userId, recipientIds)
    }

    @Query("SELECT * FROM transaction_events WHERE user_uid = :userId AND recipient_id IN (:recipientIds) order by stream_id desc, version")
    suspend fun findAllByUserUidAndRecipientIdInInternal(
        userId: UUID,
        recipientIds: List<UUID>
    ): Flow<TransactionEvent>

    @Query("SELECT * FROM transaction_events WHERE user_uid = :userId AND stream_id = :streamId order by version")
    suspend fun findAllByUserUidAndStreamId(userId: UUID, streamId: UUID): Flow<TransactionEvent>
}
