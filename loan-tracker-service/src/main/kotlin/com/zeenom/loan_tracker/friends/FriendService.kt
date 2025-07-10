package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto
import com.zeenom.loan_tracker.transactions.ICurrencyClient
import com.zeenom.loan_tracker.transactions.TransactionEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class FriendService(
    private val friendsEventHandler: FriendsEventHandler,
    private val userEventHandler: UserEventHandler,
    private val transactionEventHandler: TransactionEventHandler,
    private val friendFinderStrategy: FriendFinderStrategy,
    private val allTimeBalanceStrategy: AllTimeBalanceStrategy,
    private val currencyClient: ICurrencyClient,
) {

    suspend fun findAllByUserId(userId: UUID): FriendsWithAllTimeBalancesDto {
        val user = userEventHandler.findByUserId(userId) ?: throw IllegalArgumentException("User $userId not found")
        val friendDtos = friendFinderStrategy.findUserFriends(userId, false)
        val amountsPerFriend =
            transactionEventHandler.balancesOfFriendsByCurrency(userId, friendDtos.map { it.friendStreamId })
        val lastTransactionPerFriend = transactionEventHandler.lastTransactionOfFriends(
            userId = userId,
            friendIds = friendDtos.map { it.friendStreamId }
        )
        val mainCurrency = user.currency?.let { Currency.getInstance(user.currency) } ?: Currency.getInstance("USD")
        val friends = friendDtos.map {
            FriendDto(
                friendId = it.friendStreamId,
                email = it.email,
                phoneNumber = it.phoneNumber,
                name = it.name,
                photoUrl = it.photoUrl,
                mainCurrency = mainCurrency,
                balances = amountsPerFriend[it.friendStreamId]?.values?.toList()?.let {
                    allTimeBalanceStrategy.calculateAllTimeBalance(
                        it,
                        currencyClient.fetchCurrencies().rates,
                        mainCurrency.currencyCode
                    )
                } ?: AllTimeBalanceDto(
                    main = null,
                    other = emptyList()
                ),
                transactionUpdatedAt = lastTransactionPerFriend[it.friendStreamId]?.updatedAt
            )
        }
            .sortedWith(compareByDescending<FriendDto> { it.transactionUpdatedAt != null }.thenByDescending { it.transactionUpdatedAt })
        val balance =
            allTimeBalanceStrategy.calculateAllTimeBalance(
                amountsPerFriend.values.map(Map<String, AmountDto>::values)
                    .flatten(),
                currencyRateMap = currencyClient.fetchCurrencies().rates,
                mainCurrency.currencyCode
            )
        return FriendsWithAllTimeBalancesDto(
            friends = friends,
            balance = balance
        )
    }

    suspend fun createFriend(userId: UUID, friendDto: CreateFriendDto) {
        val user = userEventHandler.findByUserId(userId) ?: throw IllegalArgumentException("User $userId not found")
        validateFriendInformation(friendDto = friendDto, user = user, friendId = null)

        val friendUser = userEventHandler.findUserByEmailOrPhoneNumber(friendDto.email, friendDto.phoneNumber)

        friendsEventHandler.addEvent(
            FriendCreated(
                id = null,
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber,
                friendDisplayName = friendDto.name,
                userId = userId,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                createdBy = userId,
                friendId = friendUser?.uid,
            )
        )
        friendUser?.uid?.let {
            makeMeThisUsersFriendAsWell(
                friendEmail = friendDto.email,
                phoneNumber = friendDto.phoneNumber,
                me = user,
                friendId = it
            )
        }
    }

    suspend fun updateFriend(userId: UUID, friendDto: UpdateFriendDto) {
        val user = userEventHandler.findByUserId(userId) ?: throw IllegalArgumentException("User $userId not found")
        validateFriendInformation(friendDto = friendDto, user = user, friendId = friendDto.friendId)
        val friend = friendsEventHandler.findByUserUidAndFriendId(
            userId,
            friendDto.friendId
        ) ?: throw IllegalArgumentException("Friend not found")

        if (friend.deleted) {
            throw IllegalArgumentException("Friend is deleted")
        }
        friendsEventHandler.addEvent(
            FriendUpdated(
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber,
                friendDisplayName = friendDto.name,
                createdAt = Instant.now(),
                streamId = friend.streamId,
                version = friend.version + 1,
                createdBy = userId,
            )
        )
    }

    suspend fun deleteFriend(userId: UUID, friendId: UUID) {
        userEventHandler.findByUserId(userId) ?: throw IllegalArgumentException("User $userId not found")
        val friend = friendsEventHandler.findByUserUidAndFriendId(userId, friendId)
            ?: throw IllegalArgumentException("Friend not found")
        friendsEventHandler.addEvent(
            FriendDeleted(
                createdAt = Instant.now(),
                streamId = friend.streamId,
                version = friend.version + 1,
                createdBy = userId,
            )
        )
    }

    private suspend fun validateFriendInformation(
        friendDto: BaseFriendDto,
        user: UserDto,
        friendId: UUID?
    ) {
        if (friendDto.email == null && friendDto.phoneNumber == null) {
            throw IllegalArgumentException("Email or phone number is required")
        }

        if ((user.email != null && user.email == friendDto.email) || (user.phoneNumber != null && user.phoneNumber == friendDto.phoneNumber)) {
            throw IllegalArgumentException("Your friend can't have same email or phone as yours")
        }

        requireNotNull(user.uid) { "User UID must not be null while validating friend information" }

        if (friendDto.email != null)
            friendsEventHandler.findByUserUidAndFriendEmail(user.uid, friendDto.email!!)
                ?.let {
                    if (friendId == null || it.streamId != friendId)
                        throw IllegalArgumentException("Friend with email ${friendDto.email} already exist")
                }

        if (friendDto.phoneNumber != null)
            friendsEventHandler.findByUserUidAndFriendPhoneNumber(user.uid, friendDto.phoneNumber!!)
                ?.let {
                    if (friendId == null || it.streamId != friendId)
                        throw IllegalArgumentException("Friend with phone number ${friendDto.phoneNumber} already exist")
                }
    }

    private suspend fun makeMeThisUsersFriendAsWell(
        friendEmail: String?,
        phoneNumber: String?,
        me: UserDto,
        friendId: UUID
    ) {
        requireNotNull(me.uid) { "ME User UID must not be null while makeMeThisUsersFriendAsWell" }
        friendsEventHandler.addEvent(
            FriendCreated(
                id = null,
                friendEmail = friendEmail,
                friendPhoneNumber = phoneNumber,
                friendDisplayName = me.displayName,
                userId = friendId,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                createdBy = me.uid,
                friendId = me.uid
            )
        )
    }

    suspend fun updateMyFriendsWithMyId(me: UserDto) {
        val byEmail = me.email?.let { friendsEventHandler.findByFriendEmail(it) } ?: emptyList()
        val byPhone = me.phoneNumber?.let { friendsEventHandler.findByFriendPhoneNumber(it) } ?: emptyList()
        val allFriends = (byEmail + byPhone).distinctBy { it.userUid }.filter { it.friendId == null }
        allFriends.map { friend ->
            FriendIdAdded(
                friendId = me.uid ?: throw IllegalStateException("Friend ID must not be null"),
                createdAt = Instant.now(),
                streamId = friend.streamId,
                version = friend.version + 1,
                createdBy = me.uid
            )
        }.also {
            friendsEventHandler.saveAll(it)
        }
    }

    suspend fun searchUsersImFriendOfAndAddThemAsMyFriends(uid: UUID) {
        val imFriendOf = friendsEventHandler.findByFriendId(uid)
        val userFriends = userEventHandler.findUsersByUids(imFriendOf.map { it.userUid })
        if (userFriends.isEmpty()) {
            return
        }

        val existingFriendsByTheirId = friendsEventHandler.findAllFriendsByUserId(uid).map { it.friendId }.toSet()
        val remainingUserFriends = userFriends.filter { !existingFriendsByTheirId.contains(it.uid) }
        friendsEventHandler.saveAllUsersAsFriends(uid, remainingUserFriends)
        val myFriends = friendsEventHandler.findAllFriendsByUserId(uid)
        val imFriendOfByTheirIds = imFriendOf.associateBy { it.userUid }
        myFriends.forEach { myFriend ->
            val meAsTheirFriend = imFriendOfByTheirIds[myFriend.userUid]
            if (meAsTheirFriend != null) {
                transactionEventHandler.syncTransactions(myFriend, meAsTheirFriend)
            }
        }
    }

    suspend fun findByUserIdFriendId(userId: UUID, friendEmail: String?, friendPhone: String?): FriendDto {
        val user = userEventHandler.findByUserId(userId) ?: throw IllegalArgumentException("User $userId not found")
        val friendDto = friendFinderStrategy.findUserFriend(userId, friendEmail, friendPhone)
        val amountsPerFriend =
            transactionEventHandler.balancesOfFriendsByCurrency(userId, listOf(friendDto.friendStreamId))
        val lastTransactionPerFriend = transactionEventHandler.lastTransactionOfFriends(
            userId = userId,
            friendIds = listOf(friendDto.friendStreamId)
        )
        return FriendDto(
            friendId = friendDto.friendStreamId,
            email = friendDto.email,
            phoneNumber = friendDto.phoneNumber,
            photoUrl = friendDto.photoUrl,
            name = friendDto.name,
            mainCurrency = null,
            balances = amountsPerFriend[friendDto.friendStreamId]?.values?.toList()
                ?.let {
                    allTimeBalanceStrategy.calculateAllTimeBalance(
                        it,
                        currencyClient.fetchCurrencies().rates,
                        user.currency?.let { Currency.getInstance(it).currencyCode } ?: "USD"
                    )
                }
                ?: AllTimeBalanceDto(
                    main = null,
                    other = emptyList()
                ),
            transactionUpdatedAt = lastTransactionPerFriend[friendDto.friendStreamId]?.updatedAt
        )
    }
}