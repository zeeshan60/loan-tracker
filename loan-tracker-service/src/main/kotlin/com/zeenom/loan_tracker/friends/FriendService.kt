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

    suspend fun findAllByUserId(userId: String): FriendsWithAllTimeBalancesDto {
        val user = userEventHandler.findUserById(userId) ?: throw IllegalArgumentException("User $userId not found")
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

    suspend fun createFriend(userId: String, friendDto: CreateFriendDto) {
        val user = userEventHandler.findUserById(userId) ?: throw IllegalArgumentException("User $userId not found")
        validateFriendInformation(friendDto = friendDto, user = user, friendId = null)

        friendsEventHandler.addEvent(
            FriendCreated(
                userId = userId,
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber,
                friendDisplayName = friendDto.name,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                id = null,
                createdBy = userId,
            )
        )
        makeMeThisUsersFriendAsWell(friendDto.email, friendDto.phoneNumber, user)
    }

    suspend fun updateFriend(userId: String, friendDto: UpdateFriendDto) {
        val user = userEventHandler.findUserById(userId) ?: throw IllegalArgumentException("User $userId not found")
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
                userId = userId,
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber,
                friendDisplayName = friendDto.name,
                createdAt = Instant.now(),
                streamId = friend.streamId,
                version = friend.version + 1,
                createdBy = userId,
            )
        )
        makeMeThisUsersFriendAsWell(friendDto.email, friendDto.phoneNumber, user)
        val friendUser = friendFinderStrategy.findUserFriend(userId, friendDto.email, friendDto.phoneNumber)
        if (friendUser.friendUid == null) {
            return
        }
        val findSelf = friendFinderStrategy.findUserFriend(friendUser.friendUid, user.email, user.phoneNumber)
        val friendModel =
            friendsEventHandler.findByUserUidAndFriendId(friendUser.friendUid, findSelf.friendStreamId)
        friendModel?.let {
            transactionEventHandler.syncTransactions(friendModel, friend)
        }
    }

    suspend fun deleteFriend(userId: String, friendId: UUID) {
        userEventHandler.findUserById(userId) ?: throw IllegalArgumentException("User $userId not found")
        val friend = friendsEventHandler.findByUserUidAndFriendId(userId, friendId)
            ?: throw IllegalArgumentException("Friend not found")
        friendsEventHandler.addEvent(
            FriendDeleted(
                userId = userId,
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

    private suspend fun makeMeThisUsersFriendAsWell(friendEmail: String?, phoneNumber: String?, me: UserDto) {
        val friendsExistingUser =
            friendEmail?.let { userEventHandler.findUserByEmail(friendEmail) } ?: phoneNumber?.let {
                userEventHandler.findUserByPhoneNumber(phoneNumber)
            }
        friendsExistingUser?.let { usersFriend ->
            (me.email?.let {
                friendsEventHandler.findByUserUidAndFriendEmail(
                    usersFriend.uid,
                    me.email
                )
            }
                ?: me.phoneNumber?.let {
                    friendsEventHandler.findByUserUidAndFriendPhoneNumber(
                        usersFriend.uid,
                        me.phoneNumber
                    )
                }).let {
                if (it == null) {
                    friendsEventHandler.addEvent(
                        FriendCreated(
                            userId = usersFriend.uid,
                            friendEmail = me.email,
                            friendPhoneNumber = me.phoneNumber,
                            friendDisplayName = me.displayName,
                            createdAt = Instant.now(),
                            streamId = UUID.randomUUID(),
                            version = 1,
                            id = null,
                            createdBy = me.uid,
                        )
                    )
                }
            }
        }
    }

    suspend fun searchUsersImFriendOfAndAddThemAsMyFriends(uid: String) {
        val user = userEventHandler.findUserById(uid) ?: throw IllegalArgumentException("User not found")
        val emailFriends = user.email?.let { friendsEventHandler.findByFriendEmail(user.email) } ?: emptyList()
        val phoneFriends =
            user.phoneNumber?.let { friendsEventHandler.findByFriendPhoneNumber(user.phoneNumber) } ?: emptyList()

        val allFriends = emailFriends.plus(phoneFriends).distinct()
        val myFriendIds = allFriends.map { it.userUid }
        val friends =
            userEventHandler.findUsersByUids(myFriendIds)

        friendsEventHandler.saveAllUsersAsFriends(uid, friends)

        val userFriends = friendsEventHandler.findAllFriendsByUserId(uid)
        userFriends.forEach { friend ->
            val friendUser =
                allFriends.find { it.friendEmail == user.email || it.friendPhoneNumber == user.phoneNumber }
            friendUser?.let { transactionEventHandler.syncTransactions(friend, friendUser) }
        }
    }

    suspend fun findByUserIdFriendId(userId: String, friendEmail: String?, friendPhone: String?): FriendDto {
        val user = userEventHandler.findUserById(userId) ?: throw IllegalArgumentException("User $userId not found")
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