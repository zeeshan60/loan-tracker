package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.events.IEvent
import kotlinx.coroutines.flow.Flow
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

data class FriendModel(
    val userId: String,
    val streamId: UUID,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
)

data class FriendCreated(
    override val userId: String,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
    override val createdAt: Instant,
    override val streamId: UUID,
    override val version: Int,
) : IEvent<FriendModel> {
    override val createdBy: String = userId

    override fun toEntity(): FriendEvent {
        return FriendEvent(
            userUid = userId,
            friendEmail = friendEmail,
            friendPhoneNumber = friendPhoneNumber,
            friendDisplayName = friendDisplayName,
            createdAt = createdAt,
            streamId = streamId,
            version = version,
            eventType = FriendEventType.FRIEND_CREATED,
        )
    }

    override fun applyEvent(existing: FriendModel): FriendModel {
        throw UnsupportedOperationException("Friend created event is not supported")
    }
}

@Table("friend_events")
data class FriendEvent(
    @Id val id: UUID? = null,
    val userUid: String,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
    val createdAt: Instant,
    val streamId: UUID,
    val version: Int,
    val eventType: FriendEventType,
) {
    fun toEvent(): IEvent<FriendModel> {
        return when (eventType) {
            FriendEventType.FRIEND_CREATED -> FriendCreated(
                userId = userUid,
                friendEmail = friendEmail,
                friendPhoneNumber = friendPhoneNumber,
                friendDisplayName = friendDisplayName,
                createdAt = createdAt,
                streamId = streamId,
                version = version,
            )
        }
    }
}

enum class FriendEventType {
    FRIEND_CREATED
}

@Repository
interface FriendEventRepository : CoroutineCrudRepository<FriendEvent, UUID> {
    suspend fun findAllByUserUid(userUid: String): Flow<FriendEvent>
    suspend fun findByFriendEmail(email: String): Flow<FriendEvent>
    suspend fun findByUserUidAndFriendEmail(userUid: String, email: String): FriendEvent?
    suspend fun findByUserUidAndFriendPhoneNumber(userUid: String, phoneNumber: String): FriendEvent?
    suspend fun findByFriendPhoneNumber(phoneNumber: String): Flow<FriendEvent>
    suspend fun findByUserUidAndStreamId(userUid: String, recipientId: UUID): FriendEvent?
}