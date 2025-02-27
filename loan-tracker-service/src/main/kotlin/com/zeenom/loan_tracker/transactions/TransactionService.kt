package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.FriendFinderStrategy
import com.zeenom.loan_tracker.friends.FriendUserDto
import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Service
class TransactionService(
    private val transactionEventHandler: TransactionEventHandler,
    private val userEventHandler: UserEventHandler,
    private val friendsEventHandler: FriendsEventHandler,
    private val friendFinderStrategy: FriendFinderStrategy,
) {
    suspend fun addTransaction(
        userUid: String,
        transactionDto: TransactionDto,
    ): Unit = withContext(Dispatchers.IO) {

        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            userUid = userUid,
            recipientId = transactionDto.recipientId
                ?: throw IllegalArgumentException("Recipient id is required to add new transaction")
        )

        val streamId = UUID.randomUUID()
        val event = TransactionCreated(
            id = null,
            userId = userUid,
            description = transactionDto.description,
            transactionDate = transactionDto.transactionDate,
            currency = transactionDto.currency.toString(),
            splitType = transactionDto.splitType,
            totalAmount = transactionDto.originalAmount,
            recipientId = transactionDto.recipientId,
            createdAt = Instant.now(),
            createdBy = userUid,
            streamId = streamId,
            version = 1
        )
        transactionEventHandler.addEvent(event)
        if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
            event.crossTransaction(
                friendUser.uid,
                userStreamId
            )
        )
    }

    suspend fun updateTransaction(
        userUid: String,
        transactionDto: TransactionDto,
    ): Unit = withContext(Dispatchers.IO) {

        if (transactionDto.transactionStreamId == null) {
            throw IllegalArgumentException("Transaction stream id is required")
        }

        val existing =
            transactionEventHandler.read(userUid, transactionDto.transactionStreamId) ?: throw IllegalArgumentException(
                "Transaction with id ${transactionDto.transactionStreamId} does not exist"
            )

        if (existing.deleted) {
            throw IllegalArgumentException("Transaction with id ${transactionDto.transactionStreamId} is deleted")
        }

        val recipientId = existing.recipientId
        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            userUid = userUid, recipientId = recipientId
        )
        var eventVersion = existing.version
        val createdAt = Instant.now()

        if (existing.transactionDate != transactionDto.transactionDate) {
            val event = TransactionDateChanged(
                id = null,
                userId = userUid,
                transactionDate = transactionDto.transactionDate,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
                recipientId = recipientId
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid,
                    userStreamId
                )
            )
        }

        if (existing.description != transactionDto.description) {
            val event = DescriptionChanged(
                id = null,
                userId = userUid,
                description = transactionDto.description,
                transactionDate = existing.transactionDate,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
                recipientId = recipientId
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid,
                    userStreamId
                )
            )
        }

        if (existing.splitType != transactionDto.splitType) {
            val event = SplitTypeChanged(
                id = null,
                userId = userUid,
                splitType = transactionDto.splitType,
                transactionDate = existing.transactionDate,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
                recipientId = recipientId
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid,
                    userStreamId
                )
            )
        }

        if (existing.totalAmount != transactionDto.originalAmount) {
            val event = TotalAmountChanged(
                id = null,
                userId = userUid,
                totalAmount = transactionDto.originalAmount,
                transactionDate = existing.transactionDate,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
                recipientId = recipientId
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid,
                    userStreamId
                )
            )
        }

        if (existing.currency != transactionDto.currency.toString()) {
            val event = CurrencyChanged(
                id = null,
                userId = userUid,
                currency = transactionDto.currency.toString(),
                createdAt = createdAt,
                transactionDate = existing.transactionDate,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
                recipientId = recipientId
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid,
                    userStreamId
                )
            )
        }
    }

    suspend fun friendUserAndMyStreamId(userUid: String, recipientId: UUID): Pair<UserDto?, UUID?> {
        val me = userEventHandler.findUserById(userUid)
            ?: throw IllegalArgumentException("User with id $userUid does not exist")
        val friend = friendsEventHandler.findFriendByUserIdAndFriendId(userUid, recipientId)
            ?: throw IllegalArgumentException("User with id $userUid does not have friend with id $recipientId")
        val friendUser = userEventHandler.findUserByEmailOrPhoneNumber(friend.email, friend.phoneNumber)
        val userStreamId = friendUser?.let {
            friendsEventHandler.findFriendStreamIdByEmailOrPhoneNumber(friendUser.uid, me.email, me.phoneNumber)
        }
        return Pair(friendUser, userStreamId)
    }

    suspend fun transactionsByFriendId(userId: String, friendId: UUID): List<TransactionDto> {
        val (user, friendUsersByUid, friendUsersByStreamId) = userAndFriendInfo(userId)
        val models = transactionEventHandler.transactionModelsByFriend(userId, friendId)

        return models.map {
            it.transactionModel.toTransactionDto(
                friendUsersByStreamId = friendUsersByStreamId,
                friendUsersByUserId = friendUsersByUid,
                userDto = user,
                history = it.changeSummary
            )
        }
    }

    suspend fun deleteTransaction(userUid: String, transactionStreamId: UUID) {
        val existing = transactionEventHandler.read(userUid, transactionStreamId)
            ?: throw IllegalArgumentException("Transaction with id $transactionStreamId does not exist")

        val recipientId = existing.recipientId
        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            userUid = userUid, recipientId = recipientId
        )
        val event = TransactionDeleted(
            id = null,
            userId = userUid,
            createdAt = Instant.now(),
            transactionDate = existing.transactionDate,
            createdBy = userUid,
            streamId = transactionStreamId,
            version = existing.version + 1,
            recipientId = recipientId
        )
        transactionEventHandler.addEvent(
            event
        )
        if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
            event.crossTransaction(
                friendUser.uid,
                userStreamId
            )
        )
    }

    private suspend fun userAndFriendInfo(userId: String): Triple<UserDto, Map<String, FriendUserDto>, Map<UUID, FriendUserDto>> {
        val user = userEventHandler.findUserById(userId)
        requireNotNull(user) { "User with id $userId does not exist" }
        val findUserFriends = friendFinderStrategy.findUserFriends(userId)
        val friendUsersByUid =
            findUserFriends.filter { it.friendUid != null }
                .associateBy {
                    requireNotNull(it.friendUid)
                    it.friendUid
                }
        val friendUsersByStreamId = findUserFriends.associateBy { it.friendStreamId }

        return Triple(user, friendUsersByUid, friendUsersByStreamId)
    }

    suspend fun transactionActivityLogs(userId: String): List<ActivityLogWithFriendInfo> {
        val (user, friendUsersByUid, friendUsersByStreamId) = userAndFriendInfo(userId)
        val transactionsWithLogs = transactionEventHandler.transactionsWithActivityLogs(userId)

        return transactionsWithLogs.map { transactionWithLogs ->
            transactionWithLogs.activityLogs.groupBy { it.date }.map { logs ->
                val it = logs.value.maxByOrNull { it.id }!!
                ActivityLogWithFriendInfo(
                    id = it.id,
                    userUid = userId,
                    activityByUid = it.activityByUid,
                    activityByName = if (it.activityByUid == userId) user.displayName else friendUsersByUid[it.activityByUid]?.name,
                    activityByPhoto = if (it.activityByUid == userId) user.photoUrl else friendUsersByUid[it.activityByUid]?.photoUrl,
                    description = it.description,
                    activityType = it.activityType,
                    amount = it.amount,
                    currency = it.currency,
                    isOwed = it.isOwed,
                    date = it.date,
                    transactionDto = transactionWithLogs.transactionModel.toTransactionDto(
                        friendUsersByStreamId = friendUsersByStreamId,
                        friendUsersByUserId = friendUsersByUid,
                        userDto = user,
                        history = transactionWithLogs.changeSummary
                    ),
                )
            }
        }.flatten().sortedWith(compareByDescending<ActivityLogWithFriendInfo> { it.date }.thenByDescending { it.id })
    }

    fun TransactionModel.toTransactionDto(
        friendUsersByStreamId: Map<UUID, FriendUserDto>,
        friendUsersByUserId: Map<String, FriendUserDto>,
        userDto: UserDto,
        history: List<ChangeSummary>,
    ): TransactionDto {
        return TransactionDto(
            currency = Currency.getInstance(currency),
            recipientId = recipientId,
            transactionStreamId = streamId,
            description = description,
            originalAmount = totalAmount,
            splitType = splitType,
            recipientName = friendUsersByStreamId[recipientId]?.name,
            deleted = deleted,
            history = history.map {
                ChangeSummaryDto(
                    date = it.date,
                    changedBy = it.changedBy,
                    changedByName = if (it.changedBy == userDto.uid) "You" else friendUsersByUserId[it.changedBy]?.name
                        ?: throw IllegalStateException("Friend with id ${it.changedBy} not found"),
                    changedByPhoto = if (it.changedBy == userDto.uid) userDto.photoUrl else friendUsersByUserId[it.changedBy]?.photoUrl,
                    oldValue = it.oldValue,
                    newValue = it.newValue,
                    type = it.type
                )
            },
            createdAt = createdAt,
            createdBy = createdBy,
            createdByName = if (createdBy == userDto.uid) "You" else friendUsersByUserId[createdBy]?.name,
            updatedAt = updatedAt,
            updatedBy = updatedBy,
            updatedByName = if (updatedBy == userDto.uid) "You" else friendUsersByUserId[updatedBy]?.name,
            transactionDate = transactionDate
        )
    }
}

data class ActivityLogWithFriendInfo(
    val id: UUID,
    val userUid: String,
    val activityByUid: String,
    val activityByName: String?,
    val activityByPhoto: String?,
    val description: String,
    val activityType: ActivityType,
    val amount: BigDecimal,
    val currency: String,
    val isOwed: Boolean,
    val date: Instant,
    val transactionDto: TransactionDto,
)

