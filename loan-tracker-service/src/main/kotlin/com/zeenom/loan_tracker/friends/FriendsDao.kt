package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.UserEventDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class FriendsDao(
    private val eventRepository: FriendEventRepository,
    private val userEventDao: UserEventDao,
) {
    suspend fun findAllByUserId(userId: String): FriendsDto {
        val events = eventRepository.findAllByUserUid(userId).toList()
        val phones = events.mapNotNull { it.friendPhoneNumber }
        val usersByPhones = userEventDao.findUsersByPhoneNumbers(phones).toList().associateBy { it.phoneNumber }
        val emails = events.filter { it.friendPhoneNumber !in usersByPhones.keys }.mapNotNull { it.friendEmail }
        val usersByEmails = userEventDao.findUsersByEmails(emails).toList().associateBy { it.email }
        val friends = events.map {
            val user =
                it.friendPhoneNumber?.let { usersByPhones[it] } ?: it.friendEmail?.let { usersByEmails[it] }
            FriendDto(
                email = it.friendEmail,
                phoneNumber = it.friendPhoneNumber,
                name = it.friendDisplayName,
                photoUrl = user?.photoUrl,
                loanAmount = null
            )
        }
        return FriendsDto(friends)
    }

    suspend fun saveFriend(uid: String, friendDto: CreateFriendDto) {

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
        val user = userEventDao.findUserById(uid) ?: throw IllegalArgumentException("User not found")
        val emailFriends = user.email?.let { eventRepository.findByFriendEmail(user.email) } ?: emptyFlow()
        val phoneFriends =
            user.phoneNumber?.let { eventRepository.findByFriendPhoneNumber(user.phoneNumber) } ?: emptyFlow()

        val myFriendIds = emailFriends.flatMapMerge { phoneFriends }.map { it.userUid }.toList().distinct()
        val friends =
            userEventDao.findUsersByUids(myFriendIds)

        friends.collect { friendDto ->
            eventRepository.save(
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
            )
        }
    }
}