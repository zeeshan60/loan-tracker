package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.*
import com.zeenom.loan_tracker.users.UserCurrencyChanged
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import com.zeenom.loan_tracker.users.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val allTimeBalanceStrategy: AllTimeBalanceStrategy,
    private val currencyClient: ICurrencyClient,
) {
    suspend fun addTransaction(
        userUid: UUID,
        transactionDto: TransactionDto,
    ) {

        val existingUser = userEventHandler.findModelByUserId(userUid)
            ?: throw IllegalArgumentException("User with id $userUid does not exist")
        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            me = existingUser,
            recipientId = transactionDto.friendSummaryDto.friendId
                ?: throw IllegalArgumentException("Recipient id is required to add new transaction")
        )
        val event = TransactionCreated(
            id = null,
            userId = userUid,
            description = transactionDto.description,
            transactionDate = transactionDto.transactionDate,
            currency = transactionDto.currency.toString(),
            splitType = transactionDto.splitType,
            totalAmount = transactionDto.originalAmount,
            recipientId = transactionDto.friendSummaryDto.friendId,
            createdAt = Instant.now(),
            createdBy = userUid,
            streamId = transactionDto.transactionStreamId,
            version = 1
        )
        transactionEventHandler.addEvent(event)
        addDefaultCurrencyIfNotSet(existingUser, userUid, transactionDto.currency)
        if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
            event.crossTransaction(
                friendUser.uid ?: throw IllegalArgumentException("Friend user not found"),
                userStreamId
            )
        )
    }

    /**
     * We are using transaction to set default currency for user if user does not have it set.
     * User can still change it later using update user apis
     */
    private fun addDefaultCurrencyIfNotSet(
        existingUser: UserModel,
        userUid: UUID,
        currency: Currency
    ) {
        if (existingUser.currency == null) {
            CoroutineScope(Dispatchers.IO).launch {
                userEventHandler.addEvent(
                    UserCurrencyChanged(
                        currency = currency.toString(),
                        createdAt = Instant.now(),
                        streamId = existingUser.streamId,
                        version = existingUser.version + 1,
                        createdBy = userUid
                    )
                )
            }
        }
    }

    suspend fun updateTransaction(
        userUid: UUID,
        transactionDto: TransactionDto,
    ) {

        val existing =
            transactionEventHandler.read(userUid, transactionDto.transactionStreamId) ?: throw IllegalArgumentException(
                "Transaction with id ${transactionDto.transactionStreamId} does not exist"
            )

        if (existing.deleted) {
            throw IllegalArgumentException("Transaction with id ${transactionDto.transactionStreamId} is deleted")
        }

        val recipientId = existing.recipientId
        val existingUser = userEventHandler.findModelByUserId(userUid)
            ?: throw IllegalArgumentException("User with id $userUid does not exist")
        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            me = existingUser, recipientId = recipientId
        )
        var eventVersion = existing.version
        val createdAt = Instant.now()

        if (existing.transactionDate != transactionDto.transactionDate) {
            val event = TransactionDateChanged(
                id = null,
                transactionDate = transactionDto.transactionDate,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid ?: throw IllegalArgumentException("Friend user not found"),
                    userStreamId
                )
            )
        }

        if (existing.description != transactionDto.description) {
            val event = DescriptionChanged(
                id = null,
                description = transactionDto.description,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid ?: throw IllegalArgumentException("Friend user not found"),
                    userStreamId
                )
            )
        }

        if (existing.splitType != transactionDto.splitType) {
            val event = SplitTypeChanged(
                id = null,
                splitType = transactionDto.splitType,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid ?: throw IllegalArgumentException("Friend user not found"),
                    userStreamId
                )
            )
        }

        if (existing.totalAmount != transactionDto.originalAmount) {
            val event = TotalAmountChanged(
                id = null,
                totalAmount = transactionDto.originalAmount,
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid ?: throw IllegalArgumentException("Friend user not found"),
                    userStreamId
                )
            )
        }

        if (existing.currency != transactionDto.currency.toString()) {
            val event = CurrencyChanged(
                id = null,
                currency = transactionDto.currency.toString(),
                createdAt = createdAt,
                createdBy = userUid,
                streamId = existing.streamId,
                version = ++eventVersion,
            )
            transactionEventHandler.addEvent(event)
            if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
                event.crossTransaction(
                    friendUser.uid ?: throw IllegalArgumentException("Friend user not found"),
                    userStreamId
                )
            )
        }
    }

    suspend fun friendUserAndMyStreamId(me: UserModel, recipientId: UUID): Pair<UserDto?, UUID?> {
        val friend = friendsEventHandler.findFriendByUserIdAndFriendId(me.streamId, recipientId)
            ?: throw IllegalArgumentException("User with id ${me.uid} does not have friend with id $recipientId")
        val friendUser = userEventHandler.findUserByEmailOrPhoneNumber(friend.email, friend.phoneNumber)
        val userStreamId = friendUser?.let {
            requireNotNull(friendUser.uid) { "Friend user uid is required" }
            friendsEventHandler.findFriendStreamIdByEmailOrPhoneNumber(friendUser.uid, me.email, me.phoneNumber)
        }
        return Pair(friendUser, userStreamId)
    }

    suspend fun transactionsByFriendId(userId: UUID, friendId: UUID): TransactionsDto {
        val (user, friendUsersByUid, friendUsersByStreamId) = userAndFriendInfo(userId)
        val models = transactionEventHandler.transactionModelsByFriend(userId, friendId)

        val amounts = transactionEventHandler.balancesOfFriendsByCurrency(
            userId = userId,
            friendIds = listOf(friendId)
        )[friendId]?.values?.toList()
            ?: emptyList()
        val balance = allTimeBalanceStrategy.calculateAllTimeBalance(
            amounts = amounts,
            currencyRateMap = currencyClient.fetchCurrencies().rates,
            baseCurrency = user.currency?.let { Currency.getInstance(it).currencyCode } ?: "USD"
        )
        return models.map {
            it.transactionModel.toTransactionDto(
                friendUsersByStreamId = friendUsersByStreamId,
                friendUsersByUserId = friendUsersByUid,
                userDto = user,
                history = it.changeSummary,
                currencyRateMap = currencyClient.fetchCurrencies().rates,
                baseCurrency = user.currency?.let { Currency.getInstance(it).currencyCode } ?: "USD"
            )
        }.let {
            TransactionsDto(
                transactions = it,
                balance = balance
            )
        }
    }

    suspend fun deleteTransaction(userUid: UUID, transactionStreamId: UUID) {
        val existing = transactionEventHandler.read(userUid, transactionStreamId)
            ?: throw IllegalArgumentException("Transaction with id $transactionStreamId does not exist")

        val recipientId = existing.recipientId
        val existingUser = userEventHandler.findModelByUserId(userUid)
            ?: throw IllegalArgumentException("User with id $userUid does not exist")
        val (friendUser, userStreamId) = friendUserAndMyStreamId(
            me = existingUser, recipientId = recipientId
        )
        val event = TransactionDeleted(
            id = null,
            createdAt = Instant.now(),
            createdBy = userUid,
            streamId = transactionStreamId,
            version = existing.version + 1,
        )
        transactionEventHandler.addEvent(
            event
        )
        if (friendUser != null && userStreamId != null) transactionEventHandler.addEvent(
            event.crossTransaction(
                friendUser.uid ?: throw IllegalArgumentException("Friend user not found"),
                userStreamId
            )
        )
    }

    private suspend fun userAndFriendInfo(userId: UUID): Triple<UserDto, Map<UUID, FriendUserDto>, Map<UUID, FriendUserDto>> {
        val user = userEventHandler.findByUserId(userId)
        requireNotNull(user) { "User with id $userId does not exist" }
        val findUserFriends = friendFinderStrategy.findUserFriends(userId = userId, includeDeleted = true)
        val friendUsersByUid =
            findUserFriends.filter { it.friendUid != null }
                .associateBy {
                    requireNotNull(it.friendUid)
                    it.friendUid
                }
        val friendUsersByStreamId = findUserFriends.associateBy { it.friendStreamId }

        return Triple(user, friendUsersByUid, friendUsersByStreamId)
    }

    suspend fun transactionActivityLogs(userId: UUID): List<ActivityLogWithFriendInfo> {
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
                        history = transactionWithLogs.changeSummary,
                        currencyRateMap = currencyClient.fetchCurrencies().rates,
                        baseCurrency = user.currency?.let { Currency.getInstance(it).currencyCode } ?: "USD"
                    ),
                )
            }
        }.flatten().sortedWith(compareByDescending<ActivityLogWithFriendInfo> { it.date }.thenByDescending { it.id })
    }

    suspend fun findByUserIdTransactionId(userId: UUID, transactionId: UUID): TransactionDto {
        val (user, friendUsersByUid, friendUsersByStreamId) = userAndFriendInfo(userId)
        val transactionModel = transactionEventHandler.transactionModelByTransactionId(userId, transactionId)
        return transactionModel.transactionModel.toTransactionDto(
            friendUsersByStreamId = friendUsersByStreamId,
            friendUsersByUserId = friendUsersByUid,
            userDto = user,
            history = emptyList(),
            currencyRateMap = currencyClient.fetchCurrencies().rates,
            baseCurrency = user.currency?.let { Currency.getInstance(it).currencyCode } ?: "USD"
        )
    }

    fun TransactionModel.toTransactionDto(
        friendUsersByStreamId: Map<UUID, FriendUserDto>,
        friendUsersByUserId: Map<UUID, FriendUserDto>,
        userDto: UserDto,
        history: List<ChangeSummary>,
        currencyRateMap: Map<String, BigDecimal>,
        baseCurrency: String,
    ): TransactionDto {
        return TransactionDto(
            currency = Currency.getInstance(currency),
            transactionStreamId = streamId,
            description = description,
            originalAmount = totalAmount,
            splitType = splitType,
            friendSummaryDto = FriendSummaryDto(
                friendId = recipientId,
                email = friendUsersByStreamId[recipientId]?.email,
                phoneNumber = friendUsersByStreamId[recipientId]?.phoneNumber,
                photoUrl = friendUsersByStreamId[recipientId]?.photoUrl,
                name = friendUsersByStreamId[recipientId]?.name
            ),
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
            transactionDate = transactionDate,
            defaultCurrency = baseCurrency,
            amountInDefaultCurrency = allTimeBalanceStrategy.convertCurrency(
                amount = totalAmount,
                currentCurrency = currency,
                targetCurrency = baseCurrency,
                currencyRateMap = currencyRateMap,
            )
        )
    }
}

data class ActivityLogWithFriendInfo(
    val id: UUID,
    val userUid: UUID,
    val activityByUid: UUID,
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

