package com.zeenom.loan_tracker.friends

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
interface NewFriendEventRepository : CoroutineCrudRepository<NewFriendEvent, UUID> {
    suspend fun findByFriendEmail(email: String): Flow<NewFriendEvent>
    suspend fun findByFriendPhoneNumber(phoneNumber: String): Flow<NewFriendEvent>
}

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

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun makeMyOwnersMyFriends(uid: String) {
        val emailFriends = eventRepository.findByFriendEmail(uid)
        val phoneFriends = eventRepository.findByFriendPhoneNumber(uid)



    }
}