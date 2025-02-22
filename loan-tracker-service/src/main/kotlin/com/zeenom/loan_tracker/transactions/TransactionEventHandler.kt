package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class TransactionEventHandler(
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

        val transactions = findAllByUserIdFriendId(userId, friend.streamId)

        val byStreamId = transactions.groupBy { it.streamId }
        val models = byStreamId.map { (_, events) ->
            resolveStream(events)
        }

        val historyByStream = byStreamId.map { (streamId, events) ->

            val baseModel = baseModel(events.first())
            val history = mutableListOf<ChangeSummary>()
            events.drop(1).forEach {
                if (it is TransactionChangeSummary) {
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

    private suspend fun findAllByUserIdFriendId(
        userId: String,
        friendStreamId: UUID,
    ) = transactionEventRepository
        .findAllByUserUidAndRecipientId(userId, friendStreamId).first()
        .let { transactionEventRepository.findAllByUserUidAndStreamId(userId, it.streamId) }.toList()
        .map { it.toEvent() }

    suspend fun balancesOfFriends(userId: String, friendIds: List<UUID>): Map<UUID, AmountDto> {

        val toList = transactionEventRepository.findAllByUserUidAndRecipientIdInAndEventType(
            userId,
            friendIds,
            TransactionEventType.TRANSACTION_CREATED
        ).toList().map { transactionEventRepository.findAllByUserUidAndStreamId(userId, it.streamId).toList() }
            .flatten()
        return toList
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

        transactionEventRepository.findAllByUserUidAndRecipientId(friendUid, myStreamId).toList()
            .map { it.reverse(myUid, friendStreamid) }.let {
                transactionEventRepository.saveAll(it).toList()
            }
    }
}