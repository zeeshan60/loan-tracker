package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.apply
import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.common.isOwed
import com.zeenom.loan_tracker.common.reverse
import java.math.BigDecimal
import java.time.Instant
import java.util.*

interface ITransactionEvent : IEvent<TransactionModel>, TransactionChangeSummary, CrossTransactionable {
    val recipientId: UUID
    fun activityLog(current: TransactionModel): ActivityLog
}

data class TransactionCreated(
    override val userId: String,
    val description: String,
    val currency: String,
    val splitType: SplitType,
    val totalAmount: BigDecimal,
    override val recipientId: UUID,
    override val createdAt: Instant,
    override val createdBy: String,
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
        )
    }

    override fun applyEvent(existing: TransactionModel): TransactionModel {
        throw UnsupportedOperationException("Transaction created event cannot be applied to existing model")
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID): IEvent<TransactionModel> {
        return TransactionCreated(
            userId = recipientUserId,
            description = description,
            currency = currency,
            splitType = splitType.reverse(),
            totalAmount = totalAmount,
            recipientId = userStreamId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            userUid = userId,
            activityType = ActivityType.CREATED,
            amount = splitType.apply(totalAmount),
            currency = currency,
            isOwed = splitType.isOwed(),
            date = createdAt,
            transactionModel = TransactionModel(
                id = streamId,
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
            ),
            activityByUid = createdBy,
            description = description
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        throw UnsupportedOperationException("Transaction change summary event cannot be applied to Created Event")
    }
}

data class DescriptionChanged(
    val description: String,
    override val recipientId: UUID,
    override val userId: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: String,
) : ITransactionEvent {

    override fun applyEvent(existing: TransactionModel): TransactionModel {
        return existing.copy(
            description = description,
            version = version,
            createdBy = createdBy,
            createdAt = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            userUid = userId,
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
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            userId = userId,
            oldValue = existing.description,
            newValue = description,
            type = TransactionChangeType.DESCRIPTION,
            date = createdAt
        )
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID): IEvent<TransactionModel> {
        return DescriptionChanged(
            description = description,
            userId = recipientUserId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            recipientId = userStreamId
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            userUid = userId,
            activityType = ActivityType.UPDATED,
            amount = current.splitType.apply(current.totalAmount),
            currency = current.currency,
            isOwed = current.splitType.isOwed(),
            date = createdAt,
            activityByUid = createdBy,
            description = description,
            transactionModel = current
        )
    }
}

data class TransactionDeleted(
    override val recipientId: UUID,
    override val userId: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: String,
) : ITransactionEvent {
    override fun applyEvent(existing: TransactionModel): TransactionModel {
        return existing.copy(
            version = version,
            createdBy = createdBy,
            createdAt = createdAt,
            deleted = true
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
            eventType = TransactionEventType.TRANSACTION_DELETED,
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            userId = userId,
            oldValue = "Not Deleted",
            newValue = "Deleted",
            type = TransactionChangeType.DELETED,
            date = createdAt
        )
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID): IEvent<TransactionModel> {
        return TransactionDeleted(
            userId = recipientUserId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            recipientId = userStreamId
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            userUid = userId,
            activityType = ActivityType.DELETED,
            amount = current.splitType.apply(current.totalAmount),
            currency = current.currency,
            isOwed = current.splitType.isOwed(),
            date = createdAt,
            transactionModel = current,
            activityByUid = createdBy,
            description = current.description
        )
    }
}


data class TotalAmountChanged(
    val totalAmount: BigDecimal,
    override val recipientId: UUID,
    override val userId: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: String,
) : ITransactionEvent {
    override fun applyEvent(existing: TransactionModel): TransactionModel {
        return existing.copy(
            totalAmount = totalAmount,
            version = version,
            createdBy = createdBy,
            createdAt = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            userUid = userId,
            description = null,
            currency = null,
            splitType = null,
            totalAmount = totalAmount,
            recipientId = recipientId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = TransactionEventType.TOTAL_AMOUNT_CHANGED,
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            userId = userId,
            oldValue = existing.totalAmount.toString(),
            newValue = totalAmount.toString(),
            type = TransactionChangeType.TOTAL_AMOUNT,
            date = createdAt
        )
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID): IEvent<TransactionModel> {
        return TotalAmountChanged(
            totalAmount = totalAmount,
            userId = recipientUserId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            recipientId = userStreamId
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            userUid = userId,
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
    val currency: String,
    override val recipientId: UUID,
    override val userId: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: String,
) : ITransactionEvent {
    override fun applyEvent(existing: TransactionModel): TransactionModel {
        return existing.copy(
            currency = currency,
            version = version,
            createdBy = createdBy,
            createdAt = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            userUid = userId,
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
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            userId = userId,
            oldValue = existing.currency,
            newValue = currency,
            type = TransactionChangeType.CURRENCY,
            date = createdAt
        )
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID): IEvent<TransactionModel> {
        return CurrencyChanged(
            currency = currency,
            userId = recipientUserId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            recipientId = userStreamId
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            userUid = userId,
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
    val splitType: SplitType,
    override val recipientId: UUID,
    override val userId: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: String,
) : ITransactionEvent {
    override fun applyEvent(existing: TransactionModel): TransactionModel {
        return existing.copy(
            splitType = splitType,
            version = version,
            createdBy = createdBy,
            createdAt = createdAt
        )
    }

    override fun changeSummary(existing: TransactionModel): ChangeSummary {
        return ChangeSummary(
            userId = userId,
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
        )
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID): IEvent<TransactionModel> {
        return SplitTypeChanged(
            splitType = splitType.reverse(),
            userId = recipientUserId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
            recipientId = userStreamId
        )
    }

    override fun activityLog(current: TransactionModel): ActivityLog {
        return ActivityLog(
            userUid = userId,
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
    fun crossTransaction(recipientUserId: String, userStreamId: UUID): IEvent<TransactionModel>
}

interface TransactionChangeSummary {
    fun changeSummary(existing: TransactionModel): ChangeSummary
}

data class ChangeSummary(
    val date: Instant,
    val userId: String,
    val oldValue: String,
    val newValue: String,
    val type: TransactionChangeType,
)

enum class TransactionChangeType {
    DESCRIPTION,
    TOTAL_AMOUNT,
    CURRENCY,
    SPLIT_TYPE,
    DELETED,
}