package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.apply
import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.common.exceptions.NotFoundException
import com.zeenom.loan_tracker.common.isOwed
import com.zeenom.loan_tracker.friends.FriendModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class TransactionEventHandler(
    private val transactionEventRepository: TransactionEventRepository,
) {

    suspend fun read(userId: UUID, streamId: UUID): TransactionModel? {
        return transactionEventRepository
            .findAllByUserUidAndStreamId(userId, streamId).toList().map { it.toEvent() }.let {
                resolveStream(it)
            }
    }

    fun resolveStream(events: List<IEvent<TransactionModel>>): TransactionModel? {
        val sorted = events.sortedBy { it.version }
        return sorted.fold(null as TransactionModel?) { model, event ->
            event.applyEvent(model)
        }
    }

    fun resolveStreamAndGenerateLogs(events: List<ITransactionEvent>): Pair<TransactionModel, List<ActivityLog>>? {
        val sorted = events.sortedBy { it.version }

        return sorted.fold(null as TransactionModel? to mutableListOf<ActivityLog>()) { (model, logs), event ->
            val updatedModel = event.applyEvent(model)
            logs.add(event.activityLog(updatedModel))
            updatedModel to logs
        }.let { (model, logs) -> model?.let { it to logs } }
    }

    suspend fun transactionModelsByFriend(
        userId: UUID,
        friendStreamId: UUID,
    ): List<TransactionModelWithChangeSummary> {

        val transactions = findAllByUserIdFriendId(userId, friendStreamId)

        val byStreamId = transactions.groupBy { it.streamId }
        val models = byStreamId.mapNotNull { (_, events) ->
            resolveStream(events)
        }.filter { !it.deleted }
        val historyByStream = changeSummaryByTransactionId(transactions)

        return models.map {
            TransactionModelWithChangeSummary(
                transactionModel = it,
                changeSummary = historyByStream[it.streamId] ?: emptyList()
            )
        }
            .sortedWith(compareByDescending<TransactionModelWithChangeSummary> { it.transactionModel.transactionDate }.thenByDescending { it.transactionModel.streamId })
    }

    suspend fun transactionModelByTransactionId(
        userId: UUID,
        transactionId: UUID,
    ): TransactionModelWithChangeSummary {
        val transactions = transactionEventRepository.findAllByUserUidAndStreamId(userId, transactionId).toList()
            .map { it.toEvent() as ITransactionEvent }
        val model =
            resolveStream(transactions) ?: throw NotFoundException("Transaction not found for id: $transactionId")
        val history = changeSummaryByTransactionId(transactions)
        return TransactionModelWithChangeSummary(model, history[model.streamId] ?: emptyList())
    }

    private fun changeSummaryByTransactionId(transactionEvents: List<ITransactionEvent>): Map<UUID, List<ChangeSummary>> {

        val byStreamId = transactionEvents.groupBy { it.streamId }
        return byStreamId.map { (streamId, events) ->

            val sortedEvents = events.sortedBy { it.version }
            sortedEvents.fold(null as TransactionModel? to mutableListOf<ChangeSummary>()) { (model, history), event ->
                model?.let { history.add(event.changeSummary(model)) }
                event.applyEvent(model) to history
            }.let { streamId to it.second }
        }.toMap()
    }

    val logger = org.slf4j.LoggerFactory.getLogger(TransactionEventHandler::class.java)
    suspend fun transactionsWithActivityLogs(userId: UUID): List<TransactionModelWithActivityLogs> {

        //These transactions are supposed to be ordered by createdAt descending
        //We only picking first few events. 200 at the time of this comment.
        //Meaning some events for the same streamId could be missing.
        val transactions = transactionEventRepository.findAllByUserUid(userId).toList()
            .map { it.toEvent() as ITransactionEvent }
        var completeStreamTransactionEvents = transactions.toMutableList()
        val version1SteamIds = transactions
            .filter { it.version == 1 }
            .map { it.streamId }
            .toSet()
        val otherVersion1SteamIds = transactions
            .filter { it.version != 1 }
            .map { it.streamId }
            .toSet()
        val inCompleteStreamIds = otherVersion1SteamIds - version1SteamIds
        if (inCompleteStreamIds.isNotEmpty()) {
            completeStreamTransactionEvents.removeIf { it.streamId in inCompleteStreamIds }

            logger.info("Refetching full events for incomplete streamId: $inCompleteStreamIds for user: $userId")
            completeStreamTransactionEvents.addAll(
                transactionEventRepository.findAllByUserUidAndStreamIdIn(userId, inCompleteStreamIds)
                    .map { it.toEvent() as ITransactionEvent }.toList()
            )
        }
        val modelByStreamId = completeStreamTransactionEvents.groupBy { it.streamId }
            .mapNotNull { (_, events) ->
                resolveStream(events)
            }
            .associateBy { it.streamId }
        val changeSummaryByTransactionId = changeSummaryByTransactionId(completeStreamTransactionEvents)
        val byStreamId =
            completeStreamTransactionEvents.groupBy { Pair(it.streamId, modelByStreamId[it.streamId]!!.recipientId) }
        return byStreamId.map { (_, events) ->
            val (model, logs) = resolveStreamAndGenerateLogs(events)
                ?: throw IllegalStateException("Events cant be empty at this stage")
            TransactionModelWithActivityLogs(model, logs, changeSummaryByTransactionId[model.streamId] ?: emptyList())
        }
    }

    private suspend fun findAllByUserIdFriendId(
        userId: UUID,
        friendStreamId: UUID,
    ) = findAllEventsByUserIdFriendId(userId, friendStreamId)
        .map { it.toEvent() as ITransactionEvent }

    private suspend fun findAllEventsByUserIdFriendId(
        userId: UUID,
        friendStreamId: UUID,
    ): List<TransactionEvent> {
        return transactionEventRepository
            .findAllByUserUidAndRecipientId(
                userId,
                friendStreamId
            ).toList()
    }

    suspend fun balancesOfFriendsByCurrency(userId: UUID, friendIds: List<UUID>): Map<UUID, Map<String, AmountDto>> {
        val transactions = transactionEventRepository.findAllByUserUidAndRecipientIdIn(userId, friendIds).toList()
            .map { it.toEvent() }

        return transactions
            .groupBy { it.streamId }
            .values
            .map { resolveStream(it)!! }.filter { !it.deleted }
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

    suspend fun lastTransactionOfFriends(
        userId: UUID,
        friendIds: List<UUID>
    ): Map<UUID, TransactionModel?> {
        val transactions = transactionEventRepository.findAllByUserUidAndRecipientIdIn(userId, friendIds).toList()
            .map { it.toEvent() as ITransactionEvent }

        return transactions
            .groupBy { it.streamId }
            .values
            .map { resolveStream(it)!! }.filter { !it.deleted }
            .groupBy { it.recipientId }
            .mapValues { (_, transactionsByFriend) ->
                transactionsByFriend.maxByOrNull { it.createdAt }
            }
    }

    suspend fun addEvent(event: ITransactionEvent) {
        transactionEventRepository.save(event.toEntity())
    }

    /**
     * Only synchronize transactions if one user has some transactions with other and other dont have any
     * This could happen if one user is added as a friend and other user has transactions with him
     * Or if user updated friend phone or email and other user has transactions with him
     */
    suspend fun syncTransactions(friend1: FriendModel, friend2: FriendModel) {
        val transactions1 = findAllByUserIdFriendId(friend1.userUid, friend1.streamId)
        val transactions2 = findAllByUserIdFriendId(friend2.userUid, friend2.streamId)

        if (transactions1.isEmpty() && transactions2.isNotEmpty()) {
            transactionEventRepository.saveAll(transactions2.map { transaction ->
                transaction.crossTransaction(
                    recipientUserId = friend1.userUid,
                    userStreamId = friend1.streamId
                ).toEntity()
            }.toList()).toList()
        } else if (transactions2.isEmpty() && transactions1.isNotEmpty()) {
            transactionEventRepository.saveAll(transactions1.map { transaction ->
                transaction.crossTransaction(
                    recipientUserId = friend2.userUid,
                    userStreamId = friend2.streamId
                ).toEntity()
            }.toList()).toList()
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