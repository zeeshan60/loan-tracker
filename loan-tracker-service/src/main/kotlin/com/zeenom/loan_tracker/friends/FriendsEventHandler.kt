package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class FriendsEventHandler(
    private val eventRepository: FriendEventRepository,
) {

    suspend fun findAllEventsByUserId(userId: String) = eventRepository.findAllByUserUid(userId)

    suspend fun findByUserUidAndFriendStreamId(userUid: String, friendId: UUID): FriendEvent? {
        return eventRepository.findByUserUidAndStreamId(userUid, friendId)
    }

    suspend fun findByUserUidAndFriendEmail(userUid: String, email: String): FriendEvent? {
        return eventRepository.findByUserUidAndFriendEmail(userUid, email)
    }

    suspend fun findByUserUidAndFriendPhoneNumber(userUid: String, phoneNumber: String): FriendEvent? {
        return eventRepository.findByUserUidAndFriendPhoneNumber(userUid, phoneNumber)
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

    suspend fun saveAllUsersAsFriends(userId: String, userDtos: List<UserDto>) {
        userDtos.map { userDto ->
            FriendEvent(
                userUid = userId,
                friendEmail = userDto.email,
                friendPhoneNumber = userDto.phoneNumber,
                friendDisplayName = userDto.displayName,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = FriendEventType.FRIEND_CREATED
            )
        }.also {
            eventRepository.saveAll(it).toList()
        }
    }

    suspend fun findByFriendEmail(email: String): Flow<FriendEvent> {
        return eventRepository.findByFriendEmail(email)
    }

    suspend fun findByFriendPhoneNumber(phoneNumber: String): Flow<FriendEvent> {
        return eventRepository.findByFriendPhoneNumber(phoneNumber)
    }

    suspend fun findFriendByUserIdAndFriendId(userUid: String, friendId: UUID): FriendId? {
        return eventRepository.findByUserUidAndStreamId(userUid, friendId)?.let {
            FriendId(it.friendEmail, it.friendPhoneNumber, it.friendDisplayName)
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

data class FriendId(val email: String?, val phoneNumber: String?, val name: String)