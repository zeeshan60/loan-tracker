package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.apply
import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.common.isOwed
import com.zeenom.loan_tracker.common.reverse
import java.math.BigDecimal
import java.time.Instant
import java.util.*

interface ITransactionEvent : IEvent<TransactionModel>, TransactionChangeSummary, CrossTransactionable {
    val id: UUID?
    val userId: UUID
    val recipientId: UUID
    //i hate it. for now we are including these properties to be able to filter events by group
    //ideally events should resolve to models and models should be filtered
    val groupId: UUID?
    override fun toEntity(): TransactionEvent
    fun activityLog(current: TransactionModel): ActivityLog
}

data class TransactionCreated(
    override val id: UUID?,
    override val userId: UUID,
    override val recipientId: UUID,
    val description: String,
    val currency: String,
    val splitType: SplitType,
    val totalAmount: BigDecimal,
    val amount: AmountDto? = null, //TODO remove null
    val transactionDate: Instant,
    override val groupId: UUID?,
    override val createdAt: Instant,
    override val createdBy: UUID,
    override val streamId: UUID,
    override val version: Int,
) : ITransactionEvent {
    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            userUid = userId,
            description = description,
            currency = currency,
            splitType = splitType,
            totalAmount = totalAmount,
            recipientId = recipientId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = TransactionEventType.TRANSACTION_CREATED,
            transactionDate = transactionDate,
            groupId = groupId,
        )
    }

    override fun applyEvent(existing: TransactionModel?): TransactionModel {
        return TransactionModel(
            historyLogId = streamId,
            userId = userId,
            description = description,
            currency = currency,
            splitType = splitType,
            totalAmount = totalAmount,
            recipientId = recipientId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            firstCreatedAt = createdAt,
            updatedAt = createdAt,
            updatedBy = null,
            deleted = false,
            transactionDate = transactionDate,
            groupId = groupId
        )
    }

    override fun crossTransaction(recipientUserId: UUID, userStreamId: UUID): ITransactionEvent {
        return TransactionCreated(
            id = null,
            userId = recipientUserId,
            recipientId = userStreamId,
            description = description,
            currency = currency,
            splitType = splitType.reverse(),
            totalAmount = totalAmount,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            transactionDate = transactionDate,
            groupId = groupId
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            id = current.historyLogId ?: throw IllegalStateException("Transaction event id is required for activity log"),
            userId = userId,
            activityType = ActivityType.CREATED,
            amount = splitType.apply(totalAmount),
            currency = currency,
            isOwed = splitType.isOwed(),
            date = createdAt,
            transactionModel = current,
            activityByUid = createdBy,
            description = description
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        throw UnsupportedOperationException("Transaction change summary event cannot be applied to Created Event")
    }
}

data class TransactionDateChanged(
    override val id: UUID?,
    override val userId: UUID,
    override val recipientId: UUID,
    val transactionDate: Instant,
    override val groupId: UUID?,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: UUID,
) : ITransactionEvent {
    override fun applyEvent(existing: TransactionModel?): TransactionModel {
        requireNotNull(existing) {"TransactionModel cannot be null"}
        return existing.copy(
            historyLogId = id,
            transactionDate = transactionDate,
            version = version,
            updatedBy = createdBy,
            updatedAt = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            userUid = userId,
            description = null,
            currency = null,
            splitType = null,
            totalAmount = null,
            recipientId = recipientId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = TransactionEventType.TRANSACTION_DATE_CHANGED,
            transactionDate = transactionDate,
            groupId = groupId
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            changedBy = createdBy,
            oldValue = existing.transactionDate.toString(),
            newValue = transactionDate.toString(),
            type = TransactionChangeType.TRANSACTION_DATE,
            date = createdAt
        )
    }

    override fun crossTransaction(recipientUserId: UUID, userStreamId: UUID): ITransactionEvent {
        return TransactionDateChanged(
            id = id,
            userId = recipientUserId,
            recipientId = userStreamId,
            transactionDate = transactionDate,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            groupId = groupId
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            id = current.historyLogId
                ?: throw IllegalStateException("Transaction event id is required for activity log"),
            activityType = ActivityType.UPDATED,
            amount = current.splitType.apply(current.totalAmount),
            currency = current.currency,
            isOwed = current.splitType.isOwed(),
            date = createdAt,
            activityByUid = createdBy,
            description = current.description,
            userId = current.userId,
            transactionModel = current,
        )
    }
}

data class DescriptionChanged(
    override val id: UUID?,
    override val userId: UUID,
    override val recipientId: UUID,
    override val groupId: UUID?,
    val description: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: UUID,
) : ITransactionEvent {

    override fun applyEvent(existing: TransactionModel?): TransactionModel {
        requireNotNull(existing) {"TransactionModel cannot be null"}
        return existing.copy(
            historyLogId = id,
            description = description,
            version = version,
            updatedBy = createdBy,
            updatedAt = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            description = description,
            currency = null,
            splitType = null,
            totalAmount = null,
            recipientId = recipientId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = TransactionEventType.DESCRIPTION_CHANGED,
            transactionDate = null,
            id = id,
            userUid = userId,
            groupId = groupId,
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            changedBy = createdBy,
            oldValue = existing.description,
            newValue = description,
            type = TransactionChangeType.DESCRIPTION,
            date = createdAt
        )
    }

    override fun crossTransaction(recipientUserId: UUID, userStreamId: UUID): ITransactionEvent {
        return DescriptionChanged(
            id = id,
            userId = recipientUserId,
            recipientId = userStreamId,
            description = description,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            groupId = groupId
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            id = current.historyLogId ?: throw IllegalStateException("Transaction event id is required for activity log"),
            activityType = ActivityType.UPDATED,
            amount = current.splitType.apply(current.totalAmount),
            currency = current.currency,
            isOwed = current.splitType.isOwed(),
            date = createdAt,
            activityByUid = createdBy,
            description = description,
            userId = current.userId,
            transactionModel = current
        )
    }
}

data class TransactionDeleted(
    override val id: UUID?,
    override val userId: UUID,
    override val recipientId: UUID,
    override val groupId: UUID?,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: UUID,
) : ITransactionEvent {
    override fun applyEvent(existing: TransactionModel?): TransactionModel {
        requireNotNull(existing) {"TransactionModel cannot be null"}
        return existing.copy(
            historyLogId = id,
            version = version,
            deleted = true,
            updatedBy = createdBy,
            updatedAt = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            description = null,
            currency = null,
            splitType = null,
            totalAmount = null,
            recipientId = recipientId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = TransactionEventType.TRANSACTION_DELETED,
            transactionDate = null,
            id = id,
            userUid = userId,
            groupId = groupId
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            changedBy = createdBy,
            oldValue = "Not Deleted",
            newValue = "Deleted",
            type = TransactionChangeType.DELETED,
            date = createdAt
        )
    }

    override fun crossTransaction(recipientUserId: UUID, userStreamId: UUID): ITransactionEvent {
        return TransactionDeleted(
            id = id,
            userId = recipientUserId,
            recipientId = userStreamId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            groupId = groupId
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            id = current.historyLogId
                ?: throw IllegalStateException("Transaction event id is required for activity log"),
            activityType = ActivityType.DELETED,
            amount = current.splitType.apply(current.totalAmount),
            currency = current.currency,
            isOwed = current.splitType.isOwed(),
            date = createdAt,
            transactionModel = current,
            activityByUid = createdBy,
            description = current.description,
            userId = current.userId,
        )
    }
}


data class TotalAmountChanged(
    override val id: UUID?,
    override val userId: UUID,
    override val recipientId: UUID,
    override val groupId: UUID?,
    val totalAmount: BigDecimal,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: UUID,
) : ITransactionEvent {
    override fun applyEvent(existing: TransactionModel?): TransactionModel {
        requireNotNull(existing) {"TransactionModel cannot be null"}
        return existing.copy(
            historyLogId = id,
            totalAmount = totalAmount,
            version = version,
            updatedBy = createdBy,
            updatedAt = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            description = null,
            currency = null,
            splitType = null,
            totalAmount = totalAmount,
            recipientId = recipientId,
            userUid = userId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = TransactionEventType.TOTAL_AMOUNT_CHANGED,
            transactionDate = null,
            groupId = groupId
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            changedBy = createdBy,
            oldValue = existing.totalAmount.toString(),
            newValue = totalAmount.toString(),
            type = TransactionChangeType.TOTAL_AMOUNT,
            date = createdAt
        )
    }

    override fun crossTransaction(recipientUserId: UUID, userStreamId: UUID): ITransactionEvent {
        return TotalAmountChanged(
            id = id,
            userId = recipientUserId,
            recipientId = userStreamId,
            totalAmount = totalAmount,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            groupId = groupId,
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            id = current.historyLogId ?: throw IllegalStateException("Transaction event id is required for activity log"),
            userId = current.userId,
            activityType = ActivityType.UPDATED,
            currency = current.currency,
            date = createdAt,
            transactionModel = current,
            activityByUid = createdBy,
            description = current.description,
            amount = current.splitType.apply(totalAmount),
            isOwed = current.splitType.isOwed(),
        )
    }
}

data class CurrencyChanged(
    override val id: UUID?,
    override val userId: UUID,
    override val recipientId: UUID,
    override val groupId: UUID?,
    val currency: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: UUID,
) : ITransactionEvent {
    override fun applyEvent(existing: TransactionModel?): TransactionModel {
        requireNotNull(existing) {"TransactionModel cannot be null"}
        return existing.copy(
            historyLogId = id,
            currency = currency,
            version = version,
            updatedBy = createdBy,
            updatedAt = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            description = null,
            currency = currency,
            splitType = null,
            totalAmount = null,
            recipientId = recipientId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = TransactionEventType.CURRENCY_CHANGED,
            transactionDate = null,
            userUid = userId,
            groupId = groupId,
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            changedBy = createdBy,
            oldValue = existing.currency,
            newValue = currency,
            type = TransactionChangeType.CURRENCY,
            date = createdAt
        )
    }

    override fun crossTransaction(recipientUserId: UUID, userStreamId: UUID): ITransactionEvent {
        return CurrencyChanged(
            id = id,
            userId = recipientUserId,
            recipientId = userStreamId,
            currency = currency,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            groupId = groupId,
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            id = current.historyLogId ?: throw IllegalStateException("Transaction event id is required for activity log"),
            userId = current.userId,
            activityType = ActivityType.UPDATED,
            currency = currency,
            date = createdAt,
            transactionModel = current,
            activityByUid = createdBy,
            description = current.description,
            amount = current.splitType.apply(current.totalAmount),
            isOwed = current.splitType.isOwed(),
        )
    }
}

data class SplitTypeChanged(
    override val id: UUID?,
    override val userId: UUID,
    override val recipientId: UUID,
    override val groupId: UUID?,
    val splitType: SplitType,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: UUID,
) : ITransactionEvent {
    override fun applyEvent(existing: TransactionModel?): TransactionModel {
        requireNotNull(existing) {"TransactionModel cannot be null"}
        return existing.copy(
            historyLogId = id,
            splitType = splitType,
            version = version,
            updatedBy = createdBy,
            updatedAt = createdAt
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            changedBy = createdBy,
            oldValue = existing.splitType.toString(),
            newValue = splitType.toString(),
            type = TransactionChangeType.SPLIT_TYPE,
            date = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            userUid = userId,
            description = null,
            currency = null,
            splitType = splitType,
            totalAmount = null,
            recipientId = recipientId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = TransactionEventType.SPLIT_TYPE_CHANGED,
            transactionDate = null,
            groupId = groupId,
        )
    }

    override fun crossTransaction(recipientUserId: UUID, userStreamId: UUID): ITransactionEvent {
        return SplitTypeChanged(
            id = id,
            userId = recipientUserId,
            recipientId = userStreamId,
            splitType = splitType.reverse(),
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            groupId = groupId,
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            id = current.historyLogId ?: throw IllegalStateException("Transaction event id is required for activity log"),
            userId = current.userId,
            activityType = ActivityType.UPDATED,
            currency = current.currency,
            date = createdAt,
            transactionModel = current,
            activityByUid = createdBy,
            description = current.description,
            amount = current.splitType.apply(current.totalAmount),
            isOwed = current.splitType.isOwed(),
        )
    }
}

interface CrossTransactionable {
    fun crossTransaction(recipientUserId: UUID, userStreamId: UUID): ITransactionEvent
}

interface TransactionChangeSummary {
    fun changeSummary(existing: TransactionModel): ChangeSummary
}

data class ChangeSummary(
    val date: Instant,
    val changedBy: UUID,
    val oldValue: String,
    val newValue: String,
    val type: TransactionChangeType,
)

enum class TransactionChangeType {
    TRANSACTION_DATE,
    DESCRIPTION,
    TOTAL_AMOUNT,
    CURRENCY,
    SPLIT_TYPE,
    DELETED,
}