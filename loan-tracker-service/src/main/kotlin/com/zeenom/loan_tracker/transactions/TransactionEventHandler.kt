package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.apply
import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.common.isOwed
import io.swagger.v3.core.util.Json
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class TransactionEventHandler(
    private val transactionEventRepository: TransactionEventRepository,
) {

    suspend fun read(userId: String, streamId: UUID): TransactionModel? {
        return transactionEventRepository
            .findAllByUserUidAndStreamId(userId, streamId).toList().map { it.toEvent() }.let {
                resolveStream(it)
            }
    }

    fun resolveStream(events: List<IEvent<TransactionModel>>): TransactionModel {
        val sorted = events.sortedBy { it.version }
        val firstEvent = sorted.first()
        var model: TransactionModel = baseModel(firstEvent)
        sorted.drop(1).forEach {
            model = it.applyEvent(model)
        }
        return model
    }

    fun resolveStreamAndGenerateLogs(events: List<ITransactionEvent>): Pair<TransactionModel, List<ActivityLog>> {
        val sorted = events.sortedBy { it.version }
        val firstEvent = sorted.first()
        var model: TransactionModel = baseModel(firstEvent)
        val logs = mutableListOf(firstEvent.activityLog(model))
        sorted.drop(1).forEach {
            model = it.applyEvent(model)
            logs.add(it.activityLog(model))
        }
        return model to logs
    }

    private fun baseModel(firstEvent: IEvent<TransactionModel>): TransactionModel {
        return if (firstEvent is TransactionCreated) firstEvent.let {
            TransactionModel(
                id = it.streamId,
                userUid = it.userId,
                description = it.description,
                currency = it.currency,
                splitType = it.splitType,
                totalAmount = it.totalAmount,
                recipientId = it.recipientId,
                createdAt = it.createdAt,
                createdBy = it.createdBy,
                streamId = it.streamId,
                version = it.version,
                firstCreatedAt = it.createdAt,
                updatedAt = null,
                updatedBy = null,
                deleted = false,
                transactionDate = it.transactionDate
            )
        } else throw IllegalArgumentException("First event must be a transaction created event")
    }

    suspend fun transactionModelsByFriend(
        userId: String,
        friendStreamId: UUID,
    ): List<TransactionModelWithChangeSummary> {

        val transactions = findAllByUserIdFriendId(userId, friendStreamId)

        val byStreamId = transactions.groupBy { it.streamId }
        val models = byStreamId.map { (_, events) ->
            resolveStream(events)
        }
        val historyByStream = changeSummaryByTransactionId(transactions)

        return models.map {
            TransactionModelWithChangeSummary(
                transactionModel = it,
                changeSummary = historyByStream[it.streamId] ?: emptyList()
            )
        }
            .sortedWith(compareByDescending<TransactionModelWithChangeSummary> { it.transactionModel.transactionDate }.thenByDescending { it.transactionModel.id })
    }

    private fun changeSummaryByTransactionId(transactionEvents: List<ITransactionEvent>): Map<UUID, List<ChangeSummary>> {

        val byStreamId = transactionEvents.groupBy { it.streamId }
        return byStreamId.map { (streamId, events) ->

            val sortedEvents = events.sortedBy { it.version }
            val baseModel = baseModel(sortedEvents.first())
            val history = mutableListOf<ChangeSummary>()
            sortedEvents.drop(1).forEach {
                history.add(it.changeSummary(baseModel))
            }
            streamId to history
        }.toMap()
    }

    suspend fun transactionsWithActivityLogs(userId: String): List<TransactionModelWithActivityLogs> {

        val transactions = transactionEventRepository.findAllByUserUid(userId).toList()
            .map { it.toEvent() as ITransactionEvent }
        val changeSummaryByTransactionId = changeSummaryByTransactionId(transactions)
        val byStreamId = transactions.groupBy { Pair(it.streamId, it.recipientId) }
        return byStreamId.map { (_, events) ->
            val (model, logs) = resolveStreamAndGenerateLogs(events)
            TransactionModelWithActivityLogs(model, logs, changeSummaryByTransactionId[model.streamId] ?: emptyList())
        }
    }

    private suspend fun findAllByUserIdFriendId(
        userId: String,
        friendStreamId: UUID,
    ) = findAllEventsByUserIdFriendId(userId, friendStreamId)
        .map { it.toEvent() as ITransactionEvent }

    private suspend fun findAllEventsByUserIdFriendId(
        userId: String,
        friendStreamId: UUID,
    ): List<TransactionEvent> {
        return transactionEventRepository
            .findAllByUserUidAndRecipientId(
                userId,
                friendStreamId
            ).toList()
    }

    suspend fun balancesOfFriendsByCurrency(userId: String, friendIds: List<UUID>): Map<UUID, Map<String, AmountDto>> {
        val transactions = transactionEventRepository.findAllByUserUidAndRecipientIdIn(userId, friendIds).toList()
            .map { it.toEvent() }

        return transactions
            .groupBy { it.streamId }
            .values
            .map { resolveStream(it) }
            .groupBy { it.recipientId }
            .mapValues { (_, transactionsByFriend) ->
                transactionsByFriend
                    .groupBy { it.currency }
                    .mapValues { (_, transactionsByCurrency) ->
                        val balance = transactionsByCurrency
                            .sumOf {
                                val amount = it.splitType.apply(it.totalAmount)
                                if (it.splitType.isOwed()) amount else -amount
                            }
                        AmountDto(
                            amount = balance.abs(),
                            currency = Currency.getInstance(transactionsByCurrency.first().currency),
                            isOwed = balance > BigDecimal.ZERO
                        )
                    }
            }
    }


    suspend fun addEvent(event: IEvent<TransactionModel>) {
        val entity = event.toEntity()
        if (entity is TransactionEvent)
            transactionEventRepository.save(entity)
        else throw IllegalArgumentException("Invalid event type ${entity.javaClass}")
    }

    private fun TransactionEvent.reverse(
        friendUserId: String,
        myStreamId: UUID,
    ): TransactionEvent = this.toEvent().let {
        if (it is CrossTransactionable) it.crossTransaction(friendUserId, myStreamId)
            .toEntity() as TransactionEvent
        else throw IllegalArgumentException("Invalid event type ${it.javaClass}")
    }

    suspend fun addReverseEventsForUserAndFriend(
        myUid: String,
        myStreamId: UUID,
        friendUid: String,
        friendStreamid: UUID,
    ) {

        findAllEventsByUserIdFriendId(friendUid, myStreamId)
            .map { it.reverse(myUid, friendStreamid) }.let {
                Json.prettyPrint(it)
                transactionEventRepository.saveAll(it).toList()
            }
    }
}

data class TransactionModelWithActivityLogs(
    val transactionModel: TransactionModel,
    val activityLogs: List<ActivityLog>,
    val changeSummary: List<ChangeSummary>,
)


data class TransactionModelWithChangeSummary(
    val transactionModel: TransactionModel,
    val changeSummary: List<ChangeSummary>,
)