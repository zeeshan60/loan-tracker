package com.zeenom.loan_tracker.friends

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Table("friend_events")
data class NewFriendEvent(
    @Id val id: UUID? = null,
    val userUid: String,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
    val friendPhotoUrl: String?,
    val createdAt: Instant,
    val version: Int,
    val eventType: FriendEventType,
)

enum class FriendEventType {
    CREATE_FRIEND
}

@Repository
interface NewFriendEventRepository : CoroutineCrudRepository<NewFriendEvent, UUID>

@Service
class NewFriendEventsDao(private val eventRepository: NewFriendEventRepository) : IFriendsDao {
    override suspend fun findAllByUserId(userId: String): FriendsDto {
        TODO("Not yet implemented")
    }

    override suspend fun saveFriend(uid: String, friendDto: CreateFriendDto) {

        eventRepository.save(
            NewFriendEvent(
                userUid = uid,
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber,
                friendDisplayName = friendDto.name,
                friendPhotoUrl = null,
                createdAt = Instant.now(),
                version = 1,
                eventType = FriendEventType.CREATE_FRIEND
            )
        )
    }

    override suspend fun makeMyOwnersMyFriends(uid: String) {
        TODO("Not yet implemented")
    }
}