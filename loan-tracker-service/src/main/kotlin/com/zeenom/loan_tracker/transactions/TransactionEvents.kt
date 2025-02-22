package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.apply
import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.common.reverse
import com.zeenom.loan_tracker.common.transactionType
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class TransactionCreated(
    override val userId: String,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: TransactionType,
    val splitType: SplitType,
    val totalAmount: BigDecimal,
    val recipientId: UUID,
    override val createdAt: Instant,
    override val createdBy: String,
    override val streamId: UUID,
    override val version: Int,
) : IEvent<TransactionModel>, CrossTransactionable {
    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            userUid = userId,
            description = description,
            amount = splitType.apply(totalAmount),
            currency = currency,
            transactionType = transactionType,
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

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID?): IEvent<TransactionModel> {
        return TransactionCreated(
            userId = recipientUserId,
            description = description,
            amount = splitType.apply(totalAmount),
            currency = currency,
            transactionType = if (transactionType == TransactionType.CREDIT) TransactionType.DEBIT else TransactionType.CREDIT,
            splitType = splitType.reverse(),
            totalAmount = totalAmount,
            recipientId = userStreamId
                ?: throw IllegalArgumentException("Recipient ID is required for cross transaction"),
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
        )
    }
}

data class DescriptionChanged(
    val description: String,
    override val userId: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: String,
) : IEvent<TransactionModel>, TransactionChangeSummary, CrossTransactionable {

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
            amount = null,
            currency = null,
            transactionType = null,
            splitType = null,
            totalAmount = null,
            recipientId = null,
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
            type = TransactionChangeType.DESCRIPTION
        )
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID?): IEvent<TransactionModel> {
        return DescriptionChanged(
            description = description,
            userId = recipientUserId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
        )
    }
}

data class TotalAmountChanged(
    val totalAmount: BigDecimal,
    override val userId: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: String,
) : IEvent<TransactionModel>, TransactionChangeSummary, CrossTransactionable {
    override fun applyEvent(existing: TransactionModel): TransactionModel {
        return existing.copy(
            totalAmount = totalAmount,
            amount = existing.splitType.apply(totalAmount),
            version = version,
            createdBy = createdBy,
            createdAt = createdAt
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            userUid = userId,
            description = null,
            amount = null,
            currency = null,
            transactionType = null,
            splitType = null,
            totalAmount = totalAmount,
            recipientId = null,
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
            type = TransactionChangeType.TOTAL_AMOUNT
        )
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID?): IEvent<TransactionModel> {
        return TotalAmountChanged(
            totalAmount = totalAmount,
            userId = recipientUserId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
        )
    }
}

data class CurrencyChanged(
    val currency: String,
    override val userId: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: String,
) : IEvent<TransactionModel>, TransactionChangeSummary, CrossTransactionable {
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
            amount = null,
            currency = currency,
            transactionType = null,
            splitType = null,
            totalAmount = null,
            recipientId = null,
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
            type = TransactionChangeType.CURRENCY
        )
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID?): IEvent<TransactionModel> {
        return CurrencyChanged(
            currency = currency,
            userId = recipientUserId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
        )
    }
}

data class SplitTypeChanged(
    val splitType: SplitType,
    override val userId: String,
    override val streamId: UUID,
    override val version: Int,
    override val createdAt: Instant,
    override val createdBy: String,
) : IEvent<TransactionModel>, TransactionChangeSummary, CrossTransactionable {
    override fun applyEvent(existing: TransactionModel): TransactionModel {
        return existing.copy(
            amount = splitType.apply(existing.totalAmount),
            transactionType = splitType.transactionType(),
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
            type = TransactionChangeType.SPLIT_TYPE
        )
    }

    override fun toEntity(): TransactionEvent {
        return TransactionEvent(
            userUid = userId,
            description = null,
            amount = null,
            currency = null,
            transactionType = null,
            splitType = splitType,
            totalAmount = null,
            recipientId = null,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = TransactionEventType.SPLIT_TYPE_CHANGED,
        )
    }

    override fun crossTransaction(recipientUserId: String, userStreamId: UUID?): IEvent<TransactionModel> {
        return SplitTypeChanged(
            splitType = splitType.reverse(),
            userId = recipientUserId,
            streamId = streamId,
            version = version,
            createdAt = createdAt,
            createdBy = createdBy,
        )
    }
}

interface CrossTransactionable {
    fun crossTransaction(recipientUserId: String, userStreamId: UUID? = null): IEvent<TransactionModel>
}

interface TransactionChangeSummary {
    fun changeSummary(existing: TransactionModel): ChangeSummary
}

data class ChangeSummary(
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
}