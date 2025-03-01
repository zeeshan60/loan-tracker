package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.transactions.IEventAble
import kotlinx.coroutines.flow.Flow
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

interface IFriendEvent : IEvent<FriendModel> {
    override fun toEntity(): FriendEvent
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
) : IEventAble<FriendModel> {
    override fun toEvent(): IFriendEvent {
        return when (eventType) {
            FriendEventType.FRIEND_CREATED -> FriendCreated(
                id = id,
                friendEmail = friendEmail,
                friendPhoneNumber = friendPhoneNumber,
                friendDisplayName = friendDisplayName,
                userId = userUid,
                createdAt = createdAt,
                streamId = streamId,
                version = version,
                createdBy = userUid
            )
        }
    }
}

data class FriendModel(
    val id: UUID?,
    val userUid: String,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
    val createdAt: Instant,
    val streamId: UUID,
    val version: Int,
)

data class FriendCreated(
    val id: UUID?,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
    override val userId: String,
    override val createdAt: Instant,
    override val streamId: UUID,
    override val version: Int,
    override val createdBy: String,
) : IFriendEvent {
    override fun toEntity(): FriendEvent {
        return FriendEvent(
            userUid = userId,
            friendEmail = friendEmail,
            friendPhoneNumber = friendPhoneNumber,
            friendDisplayName = friendDisplayName,
            createdAt = createdAt,
            streamId = streamId,
            version = version,
            eventType = FriendEventType.FRIEND_CREATED
        )
    }

    override fun applyEvent(existing: FriendModel?): FriendModel {
        return FriendModel(
            id = id,
            userUid = userId,
            friendEmail = friendEmail,
            friendPhoneNumber = friendPhoneNumber,
            friendDisplayName = friendDisplayName,
            createdAt = createdAt,
            streamId = streamId,
            version = version
        )
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