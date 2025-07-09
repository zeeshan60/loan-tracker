package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class FriendsEventHandler(
    private val eventRepository: FriendEventRepository,
    private val friendModelRepository: FriendModelRepository
) {

    suspend fun addEvent(event: IFriendEvent) {
        eventRepository.save(event.toEntity())
        val existing = friendModelRepository.findByStreamIdAndDeletedIsFalse(event.streamId)
        friendModelRepository.save(event.applyEvent(existing))
    }

    suspend fun findAllFriendsByUserId(userId: UUID, includeDeleted: Boolean = false): List<FriendModel> {
        return if (includeDeleted) {
            friendModelRepository.findAllByUserUid(userId).toList()
        } else {
            friendModelRepository.findAllByUserUidAndDeletedIsFalse(userId).toList()
        }
    }

    suspend fun findByUserUidAndFriendEmail(userUid: UUID, email: String): FriendModel? {
        return friendModelRepository.findByUserUidAndFriendEmailAndDeletedIsFalse(userUid, email)
    }

    suspend fun findByUserUidAndFriendPhoneNumber(userUid: UUID, phoneNumber: String): FriendModel? {
        return friendModelRepository.findByUserUidAndFriendPhoneNumberAndDeletedIsFalse(userUid, phoneNumber)
    }

    suspend fun findByUserUidAndFriendId(userUid: UUID, friendId: UUID): FriendModel? {
        return friendModelRepository.findByUserUidAndStreamIdAndDeletedIsFalse(userUid, friendId)
    }

    suspend fun saveAllUsersAsFriends(userId: UUID, userDtos: List<UserDto>) {
        userDtos.map { userDto ->
            FriendEvent(
                userUid = userId,
                friendEmail = userDto.email,
                friendPhoneNumber = userDto.phoneNumber,
                friendDisplayName = userDto.displayName,
                createdAt = Instant.now(),
                createdBy = userId,
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = FriendEventType.FRIEND_CREATED
            )
        }.also {
            eventRepository.saveAll(it).toList()
            friendModelRepository.saveAll(
                it.map { event ->
                    FriendModel(
                        streamId = event.streamId,
                        userUid = userId,
                        friendEmail = event.friendEmail,
                        friendPhoneNumber = event.friendPhoneNumber,
                        friendDisplayName = event.friendDisplayName
                            ?: throw IllegalStateException("Friend display name is required"),
                        createdAt = event.createdAt,
                        updatedAt = event.createdAt,
                        version = event.version,
                        deleted = false
                    )
                }
            ).toList()
        }
    }

    suspend fun findByFriendEmail(email: String): List<FriendModel> {
        return friendModelRepository.findAllByFriendEmailAndDeletedIsFalse(email).toList()
    }

    suspend fun findByFriendPhoneNumber(phoneNumber: String): List<FriendModel> {
        return friendModelRepository.findAllByFriendPhoneNumberAndDeletedIsFalse(phoneNumber).toList()
    }

    suspend fun findFriendByUserIdAndFriendId(userUid: UUID, friendId: UUID): FriendId? {
        return friendModelRepository.findByUserUidAndStreamIdAndDeletedIsFalse(userUid, friendId)?.let {
            FriendId(it.friendEmail, it.friendPhoneNumber, it.friendDisplayName)
        }
    }

    suspend fun friendExistsByUserIdAndFriendId(userUid: UUID, recipientId: UUID): Boolean {
        return friendModelRepository.findByUserUidAndStreamIdAndDeletedIsFalse(userUid, recipientId) != null
    }

    suspend fun findFriendStreamIdByEmailOrPhoneNumber(userUid: UUID, email: String?, phoneNumber: String?): UUID? {
        return (email?.let { findByUserUidAndFriendEmail(userUid, it) }
            ?: phoneNumber?.let { findByUserUidAndFriendPhoneNumber(userUid, it) })?.streamId
    }
}

data class FriendId(val email: String?, val phoneNumber: String?, val name: String)