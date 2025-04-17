package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class FriendsEventHandler(
    private val eventRepository: FriendEventRepository,
) {

    suspend fun findAllFriendsByUserId(userId: String) =
        eventRepository.findAllByUserUid(userId).toList().groupBy { it.streamId }
            .mapNotNull { resolveStream(it.value.map { it.toEvent() }) }

    suspend fun findByUserUidAndFriendEmail(userUid: String, email: String): FriendModel? {
        val friends = eventRepository.findAllByUserUid(userUid).toList().groupBy { it.streamId }
            .mapNotNull { resolveStream(it.value.map { it.toEvent() }) }
        return friends.singleOrNull { it.friendEmail == email }
    }

    suspend fun findByUserUidAndFriendPhoneNumber(userUid: String, phoneNumber: String): FriendModel? {
        val friends = eventRepository.findAllByUserUid(userUid).toList().groupBy { it.streamId }
            .mapNotNull { resolveStream(it.value.map { it.toEvent() }) }
        return friends.singleOrNull { it.friendPhoneNumber == phoneNumber }
    }

    suspend fun findByUserUidAndFriendId(userUid: String, friendId: UUID): FriendModel? {
        val events = eventRepository.findByUserUidAndStreamId(userUid, friendId)
        return resolveStream(events.map { it.toEvent() }.toList())
    }

    fun resolveStream(events: List<IEvent<FriendModel>>): FriendModel? {
        val sorted = events.sortedBy { it.version }
        return sorted.fold(null as FriendModel?) { model, event ->
            event.applyEvent(model)
        }
    }

    suspend fun addEvent(event: IFriendEvent) {
        eventRepository.save(event.toEntity())
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
        return resolveStream(eventRepository.findByUserUidAndStreamId(userUid, friendId).map { it.toEvent() }
            .toList())?.let {
            FriendId(it.friendEmail, it.friendPhoneNumber, it.friendDisplayName)
        }
    }

    suspend fun friendExistsByUserIdAndFriendId(userUid: String, recipientId: UUID): Boolean {
        return resolveStream(eventRepository.findByUserUidAndStreamId(userUid, recipientId).map { it.toEvent() }
            .toList()) != null
    }

    suspend fun findFriendStreamIdByEmailOrPhoneNumber(userUid: String, email: String?, phoneNumber: String?): UUID? {
        return (email?.let { findByUserUidAndFriendEmail(userUid, it) }
            ?: phoneNumber?.let { findByUserUidAndFriendPhoneNumber(userUid, it) })?.streamId
    }
}

data class FriendId(val email: String?, val phoneNumber: String?, val name: String)