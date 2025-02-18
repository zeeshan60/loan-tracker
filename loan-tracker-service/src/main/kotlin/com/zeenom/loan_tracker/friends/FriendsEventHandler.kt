package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.TransactionReadModel
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*


@Service
class FriendsEventHandler(
    private val eventRepository: FriendEventRepository,
    private val userEventHandler: UserEventHandler,
    private val transactionReadModel: TransactionReadModel,
) {

    suspend fun findAllByUserId(userId: String): FriendsDto = withContext(Dispatchers.IO) {
        val events = eventRepository.findAllByUserUid(userId).toList()
        val amountsPerFriend = async { transactionReadModel.balancesOfFriends(userId, events.map { it.streamId }) }
        val phones = events.mapNotNull { it.friendPhoneNumber }
        val usersByPhones = userEventHandler.findUsersByPhoneNumbers(phones).toList().associateBy { it.phoneNumber }
        val emails = events.filter { it.friendPhoneNumber !in usersByPhones.keys }.mapNotNull { it.friendEmail }
        val usersByEmails = userEventHandler.findUsersByEmails(emails).toList().associateBy { it.email }
        val friends = events.map {
            val user =
                it.friendPhoneNumber?.let { usersByPhones[it] } ?: it.friendEmail?.let { usersByEmails[it] }
            FriendDto(
                email = it.friendEmail,
                phoneNumber = it.friendPhoneNumber,
                name = it.friendDisplayName,
                photoUrl = user?.photoUrl,
                loanAmount = amountsPerFriend.await()[it.streamId]
            )
        }
        FriendsDto(friends)
    }

    suspend fun saveFriend(uid: String, friendDto: CreateFriendDto) {

        if (friendDto.email == null && friendDto.phoneNumber == null) {
            throw IllegalArgumentException("Email or phone number is required")
        }

        val user = userEventHandler.findUserById(uid) ?: throw IllegalArgumentException("User $uid not found")
        if (user.email == friendDto.email || user.phoneNumber == friendDto.phoneNumber) {
            throw IllegalArgumentException("Your friend can't have same email or phone as yours")
        }

        if (friendDto.email != null)
            eventRepository.findByUserUidAndFriendEmail(uid, friendDto.email)
                ?.let { throw IllegalArgumentException("Friend with email ${friendDto.email} already exist") }

        if (friendDto.phoneNumber != null)
            eventRepository.findByUserUidAndFriendPhoneNumber(uid, friendDto.phoneNumber)
                ?.let { throw IllegalArgumentException("Friend with phone number ${friendDto.phoneNumber} already exist") }

        eventRepository.save(
            FriendEvent(
                userUid = uid,
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber,
                friendDisplayName = friendDto.name,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = FriendEventType.FRIEND_CREATED
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun makeMyOwnersMyFriends(uid: String) {
        val user = userEventHandler.findUserById(uid) ?: throw IllegalArgumentException("User not found")
        val emailFriends = user.email?.let { eventRepository.findByFriendEmail(user.email) } ?: emptyFlow()
        val phoneFriends =
            user.phoneNumber?.let { eventRepository.findByFriendPhoneNumber(user.phoneNumber) } ?: emptyFlow()

        val myFriendIds = emailFriends.flatMapMerge { phoneFriends }.map { it.userUid }.toList().distinct()
        val friends =
            userEventHandler.findUsersByUids(myFriendIds)

        friends.map { friendDto ->
            FriendEvent(
                userUid = uid,
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber,
                friendDisplayName = friendDto.displayName,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = FriendEventType.FRIEND_CREATED
            )
        }.also {
            eventRepository.saveAll(it).toList()
        }
    }

    suspend fun findFriendByUserIdAndFriendId(userUid: String, friendId: UUID): FriendId? {
        return eventRepository.findByUserUidAndStreamId(userUid, friendId)?.let {
            FriendId(it.friendEmail, it.friendPhoneNumber)
        }
    }

    suspend fun friendExistsByUserIdAndFriendId(userUid: String, recipientId: UUID): Boolean {
        return eventRepository.findByUserUidAndStreamId(userUid, recipientId) != null
    }

    suspend fun findFriendStreamIdByEmailOrPhoneNumber(userUid: String, email: String?, phoneNumber: String?): UUID? {
        return (email?.let { eventRepository.findByUserUidAndFriendEmail(userUid, email) }
            ?: phoneNumber?.let { eventRepository.findByUserUidAndFriendPhoneNumber(userUid, phoneNumber) })?.streamId
    }
}

data class FriendId(val email: String?, val phoneNumber: String?)