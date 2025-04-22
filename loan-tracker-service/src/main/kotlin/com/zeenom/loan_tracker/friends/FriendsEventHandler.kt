package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.users.UserDto
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
        models().filter { it.userUid == userId }

    suspend fun findByUserUidAndFriendEmail(userUid: String, email: String): FriendModel? {
        return models().find { it.userUid == userUid && it.friendEmail == email }
    }

    suspend fun findByUserUidAndFriendPhoneNumber(userUid: String, phoneNumber: String): FriendModel? {
        return models().find { it.userUid == userUid && it.friendPhoneNumber == phoneNumber }
    }

    suspend fun findByUserUidAndFriendId(userUid: String, friendId: UUID): FriendModel? {
        return models().find { it.userUid == userUid && it.streamId == friendId }
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

    suspend fun findByFriendEmail(email: String): List<FriendModel> {
        return models().filter { !it.deleted && it.friendEmail == email }
    }

    suspend fun findByFriendPhoneNumber(phoneNumber: String): List<FriendModel> {
        return models().filter { !it.deleted && it.friendPhoneNumber == phoneNumber }
    }

    private suspend fun models(): List<FriendModel> = eventRepository.findAll().toList().groupBy { it.streamId }
        .mapNotNull { resolveStream(it.value.map { it.toEvent() }) }

    suspend fun findFriendByUserIdAndFriendId(userUid: String, friendId: UUID): FriendId? {
        return models().find { it.userUid == userUid && it.streamId == friendId }?.let {
            if (it.deleted) null else FriendId(it.friendEmail, it.friendPhoneNumber, it.friendDisplayName)
        }
    }

    suspend fun friendExistsByUserIdAndFriendId(userUid: String, recipientId: UUID): Boolean {
        return models().find { it.userUid == userUid && it.streamId == recipientId }
            ?.let { if (it.deleted) null else it } != null
    }

    suspend fun findFriendStreamIdByEmailOrPhoneNumber(userUid: String, email: String?, phoneNumber: String?): UUID? {
        val models = models()
        return (email?.let { models.find { it.userUid == userUid && it.friendEmail == email } }
            ?: phoneNumber?.let { models.find { it.userUid == userUid && it.friendPhoneNumber == phoneNumber } })?.streamId
    }
}

data class FriendId(val email: String?, val phoneNumber: String?, val name: String)