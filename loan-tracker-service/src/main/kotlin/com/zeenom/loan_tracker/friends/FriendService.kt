package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto
import com.zeenom.loan_tracker.transactions.ICurrencyClient
import com.zeenom.loan_tracker.transactions.TransactionEventHandler
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
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
        val events = friendFinderStrategy.findUserFriends(userId)
        val amountsPerFriend =
            transactionEventHandler.balancesOfFriendsByCurrency(userId, events.map { it.friendStreamId })
        val mainCurrency = user.currency?.let { Currency.getInstance(user.currency) } ?: Currency.getInstance("USD")
        val friends = events.map {
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
                )
            )
        }
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
        if (friendDto.email == null && friendDto.phoneNumber == null) {
            throw IllegalArgumentException("Email or phone number is required")
        }

        val user = userEventHandler.findUserById(userId) ?: throw IllegalArgumentException("User $userId not found")
        if (user.email == friendDto.email || user.phoneNumber == friendDto.phoneNumber) {
            throw IllegalArgumentException("Your friend can't have same email or phone as yours")
        }

        if (friendDto.email != null)
            friendsEventHandler.findByUserUidAndFriendEmail(userId, friendDto.email)
                ?.let { throw IllegalArgumentException("Friend with email ${friendDto.email} already exist") }

        if (friendDto.phoneNumber != null)
            friendsEventHandler.findByUserUidAndFriendPhoneNumber(userId, friendDto.phoneNumber)
                ?.let { throw IllegalArgumentException("Friend with phone number ${friendDto.phoneNumber} already exist") }

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
        val emailFriends = user.email?.let { friendsEventHandler.findByFriendEmail(user.email) } ?: emptyFlow()
        val phoneFriends =
            user.phoneNumber?.let { friendsEventHandler.findByFriendPhoneNumber(user.phoneNumber) } ?: emptyFlow()

        val myFriendIds = emailFriends.toList().plus(phoneFriends.toList()).map { it.userUid }.distinct()
        val friends =
            userEventHandler.findUsersByUids(myFriendIds).toList()

        friendsEventHandler.saveAllUsersAsFriends(uid, friends)
    }


    suspend fun findFriendAndUserStreamId(
        userUid: String,
        userEmail: String?,
        userPhone: String?,
        recipientId: UUID,
    ): Pair<UserDto?, UUID?> {
        val friend = friendsEventHandler.findFriendByUserIdAndFriendId(userUid, recipientId)
            ?: throw IllegalArgumentException("User with id $userUid does not have friend with id $recipientId")
        val friendUser = userEventHandler.findUserByEmailOrPhoneNumber(friend.email, friend.phoneNumber)
        val userStreamId = friendUser?.let {
            friendsEventHandler.findFriendStreamIdByEmailOrPhoneNumber(friendUser.uid, userEmail, userPhone)
                ?: throw IllegalArgumentException("Friend with email ${friend.email} or phone number ${friend.phoneNumber} does not exist")
        }
        return Pair(friendUser, userStreamId)
    }

    suspend fun findByUserIdFriendId(userId: String, friendEmail: String?, friendPhone: String?): FriendDto {
        val user = userEventHandler.findUserById(userId) ?: throw IllegalArgumentException("User $userId not found")
        val friendDto = friendFinderStrategy.findUserFriend(userId, friendEmail, friendPhone)
        val amountsPerFriend =
            transactionEventHandler.balancesOfFriendsByCurrency(userId, listOf(friendDto.friendStreamId))
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
                )
        )
    }

}