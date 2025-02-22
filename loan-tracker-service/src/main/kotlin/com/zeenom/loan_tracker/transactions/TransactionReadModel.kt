package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.apply
import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.common.reverse
import com.zeenom.loan_tracker.common.transactionType
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendModel
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class TransactionModel(
    val id: UUID,
    val userUid: String,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: TransactionType,
    val splitType: SplitType,
    val totalAmount: BigDecimal,
    val recipientId: UUID,
    val createdAt: Instant,
    val createdBy: String,
    val streamId: UUID,
    val version: Int,
)

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
            amount = amount,
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
            amount = amount,
            currency = currency,
            transactionType = if (transactionType == TransactionType.CREDIT) TransactionType.DEBIT else TransactionType.CREDIT,
            splitType = splitType,
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
) : IEvent<TransactionModel>, ITransactionChangeSummary, CrossTransactionable {

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
            id = UUID.randomUUID(),
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
) : IEvent<TransactionModel>, ITransactionChangeSummary, CrossTransactionable {
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
) : IEvent<TransactionModel>, ITransactionChangeSummary, CrossTransactionable {
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
            id = UUID.randomUUID(),
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
) : IEvent<TransactionModel>, ITransactionChangeSummary, CrossTransactionable {
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
            id = UUID.randomUUID(),
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

interface ITransactionChangeSummary {
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


@Service
class TransactionReadModel(
    private val transactionEventRepository: TransactionEventRepository,
    private val friendEventRepository: FriendEventRepository,
) {

    suspend fun read(userId: String, streamId: UUID): TransactionModel? {
        return transactionEventRepository
            .findAllByUserUidAndStreamId(userId, streamId).toList().map { it.toEvent() }.let {
                resolveStream(it)
            }
    }

    suspend fun transactionsByFriendId(userId: String, friendId: UUID): List<TransactionDto> {
        val friend = friendEventRepository.findByUserUidAndStreamId(userId, friendId) ?: throw IllegalArgumentException(
            "Friend not found"
        )
        return transactionsByFriend(userId, friend.let {
            FriendModel(
                userId = it.userUid,
                streamId = it.streamId,
                friendEmail = it.friendEmail,
                friendPhoneNumber = it.friendPhoneNumber,
                friendDisplayName = it.friendDisplayName
            )
        })
    }

    fun resolveStream(events: List<IEvent<TransactionModel>>): TransactionModel {
        val firstEvent = events.first()
        var model: TransactionModel = baseModel(firstEvent)
        events.drop(1).forEach {
            model = it.applyEvent(model)
        }
        return model
    }

    private fun baseModel(firstEvent: IEvent<TransactionModel>): TransactionModel {
        return if (firstEvent is TransactionCreated) firstEvent.let {
            TransactionModel(
                id = it.streamId,
                userUid = it.userId,
                description = it.description,
                amount = it.amount,
                currency = it.currency,
                transactionType = it.transactionType,
                splitType = it.splitType,
                totalAmount = it.totalAmount,
                recipientId = it.recipientId,
                createdAt = it.createdAt,
                createdBy = it.createdBy,
                streamId = it.streamId,
                version = it.version,
            )
        } else throw IllegalArgumentException("First event must be a transaction created event")
    }

    suspend fun transactionsByFriend(userId: String, friend: FriendModel): List<TransactionDto> {

        val transactions = transactionEventRepository
            .findAllByUserUidAndRecipientId(userId, friend.streamId).toList().map { it.toEvent() }

        val byStreamId = transactions.groupBy { it.streamId }
        val models = byStreamId.map { (_, events) ->
            resolveStream(events)
        }

        val historyByStream = byStreamId.map { (streamId, events) ->

            val baseModel = baseModel(events.first())
            val history = mutableListOf<ChangeSummary>()
            events.drop(1).forEach {
                if (it is ITransactionChangeSummary) {
                    history.add(it.changeSummary(baseModel))
                }
            }
            streamId to history
        }.toMap()


        return models.map {
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
                recipientName = friend.friendDisplayName,
                history = historyByStream[it.streamId] ?: emptyList()
            )
        }
    }

    suspend fun balancesOfFriends(userId: String, friendIds: List<UUID>): Map<UUID, AmountDto> {

        return transactionEventRepository.findAllByUserUidAndRecipientIdIn(userId, friendIds).toList()
            .map { it.toEvent() }
            .groupBy { it.streamId }
            .map { (_, events) ->
                resolveStream(events)
            }.groupBy { it.recipientId }.mapValues { (_, events) ->
                val balance = events.map { if (it.transactionType == TransactionType.CREDIT) it.amount else -it.amount }
                    .reduceOrNull { current, next -> current + next } ?: BigDecimal.ZERO
                AmountDto(
                    amount = if (balance > BigDecimal.ZERO) balance else balance * BigDecimal(-1),
                    currency = Currency.getInstance(events.first().currency),
                    isOwed = balance > BigDecimal.ZERO
                )
            }
    }
}