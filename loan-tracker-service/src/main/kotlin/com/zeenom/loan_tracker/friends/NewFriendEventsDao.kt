package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.UserEventDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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
    val createdAt: Instant,
    val streamId: UUID,
    val version: Int,
    val eventType: FriendEventType,
)

enum class FriendEventType {
    FRIEND_CREATED
}

@Repository
interface NewFriendEventRepository : CoroutineCrudRepository<NewFriendEvent, UUID> {
    suspend fun findByFriendEmail(email: String): Flow<NewFriendEvent>
    suspend fun findByFriendPhoneNumber(phoneNumber: String): Flow<NewFriendEvent>
}

@Service
class NewFriendEventsDao(
    private val eventRepository: NewFriendEventRepository,
    private val userEventDao: UserEventDao,
) : IFriendsDao {
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
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = FriendEventType.FRIEND_CREATED
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun makeMyOwnersMyFriends(uid: String) {
        val user = userEventDao.findUserById(uid) ?: throw IllegalArgumentException("User not found")
        val emailFriends = user.email?.let { eventRepository.findByFriendEmail(user.email) } ?: emptyFlow()
        val phoneFriends =
            user.phoneNumber?.let { eventRepository.findByFriendPhoneNumber(user.phoneNumber) } ?: emptyFlow()

        val myFriendIds = emailFriends.flatMapMerge { phoneFriends }.map { it.userUid }.toList().distinct()
        val friends =
            userEventDao.findUsersByUids(myFriendIds)

        friends.collect { friendDto ->
            eventRepository.save(
                NewFriendEvent(
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