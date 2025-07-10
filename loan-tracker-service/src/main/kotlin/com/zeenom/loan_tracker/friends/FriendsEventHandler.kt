package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.SyncableEventHandler
import com.zeenom.loan_tracker.users.SyncableEventRepository
import com.zeenom.loan_tracker.users.SyncableModelRepository
import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class FriendsEventHandler(
    private val eventRepository: FriendEventRepository,
    private val friendModelRepository: FriendModelRepository
): SyncableEventHandler<FriendModel, FriendEvent> {

    override fun eventRepository(): SyncableEventRepository<FriendEvent> {
        return eventRepository
    }

    override fun modelRepository(): SyncableModelRepository<FriendModel> {
        return friendModelRepository
    }

    suspend fun addEvent(event: IFriendEvent) {
        eventRepository.save(event.toEntity())
    }

    suspend fun findAllFriendsByUserId(userId: UUID, includeDeleted: Boolean = false): List<FriendModel> {
        synchronize()
        return if (includeDeleted) {
            friendModelRepository.findAllByUserUid(userId).toList()
        } else {
            friendModelRepository.findAllByUserUidAndDeletedIsFalse(userId).toList()
        }
    }

    suspend fun findByUserUidAndFriendEmail(userUid: UUID, email: String): FriendModel? {
        synchronize()
        return friendModelRepository.findByUserUidAndFriendEmailAndDeletedIsFalse(userUid, email)
    }

    suspend fun findByUserUidAndFriendPhoneNumber(userUid: UUID, phoneNumber: String): FriendModel? {
        synchronize()
        return friendModelRepository.findByUserUidAndFriendPhoneNumberAndDeletedIsFalse(userUid, phoneNumber)
    }

    suspend fun findByUserUidAndFriendId(userUid: UUID, friendId: UUID): FriendModel? {
        synchronize()
        return friendModelRepository.findByUserUidAndStreamIdAndDeletedIsFalse(userUid, friendId)
    }

    suspend fun saveAllUsersAsFriends(userId: UUID, userDtos: List<UserDto>) {
        userDtos.map { userDto ->
            FriendCreated(
                id = null,
                friendEmail = userDto.email,
                friendPhoneNumber = userDto.phoneNumber,
                friendDisplayName = userDto.displayName,
                userId = userId,
                friendId = userDto.uid,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                createdBy = userId
            ).toEntity()
        }.also {
            eventRepository.saveAll(it).toList()
        }
    }

    suspend fun findByFriendEmail(email: String): List<FriendModel> {
        synchronize()
        return friendModelRepository.findAllByFriendEmailAndDeletedIsFalse(email).toList()
    }

    suspend fun findByFriendId(friendId: UUID): List<FriendModel> {
        synchronize()
        return friendModelRepository.findByFriendIdAndDeletedIsFalse(friendId).toList()
    }

    suspend fun findByFriendPhoneNumber(phoneNumber: String): List<FriendModel> {
        synchronize()
        return friendModelRepository.findAllByFriendPhoneNumberAndDeletedIsFalse(phoneNumber).toList()
    }

    suspend fun findFriendByUserIdAndFriendId(userUid: UUID, friendId: UUID): FriendId? {
        synchronize()
        return friendModelRepository.findByUserUidAndStreamIdAndDeletedIsFalse(userUid, friendId)?.let {
            FriendId(it.friendEmail, it.friendPhoneNumber, it.friendDisplayName)
        }
    }

    suspend fun findFriendStreamIdByEmailOrPhoneNumber(userUid: UUID, email: String?, phoneNumber: String?): UUID? {
        return (email?.let { findByUserUidAndFriendEmail(userUid, it) }
            ?: phoneNumber?.let { findByUserUidAndFriendPhoneNumber(userUid, it) })?.streamId
    }
}

data class FriendId(val email: String?, val phoneNumber: String?, val name: String)