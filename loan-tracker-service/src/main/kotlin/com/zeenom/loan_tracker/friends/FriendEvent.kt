package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.transactions.IEventAble
import com.zeenom.loan_tracker.users.SyncableEventRepository
import com.zeenom.loan_tracker.users.SyncableModel
import com.zeenom.loan_tracker.users.SyncableModelRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
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
    val userUid: UUID?,
    val friendId: UUID?,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String?,
    val createdAt: Instant,
    val createdBy: UUID,
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
                friendDisplayName = friendDisplayName ?: throw IllegalStateException("Friend display name is required"),
                userId = userUid ?: throw IllegalStateException("User ID is required"),
                friendId = friendId,
                createdAt = createdAt,
                streamId = streamId,
                version = version,
                createdBy = userUid
            )

            FriendEventType.FRIEND_UPDATED -> FriendUpdated(
                friendEmail = friendEmail,
                friendPhoneNumber = friendPhoneNumber,
                friendDisplayName = friendDisplayName,
                createdAt = createdAt,
                streamId = streamId,
                version = version,
                createdBy = createdBy
            )

            FriendEventType.FRIEND_DELETED -> FriendDeleted(
                createdAt = createdAt,
                streamId = streamId,
                version = version,
                createdBy = createdBy
            )

            FriendEventType.FRIEND_ID_ADDED -> FriendIdAdded(
                friendId = friendId ?: throw IllegalStateException("Friend ID is required"),
                createdAt = createdAt,
                streamId = streamId,
                version = version,
                createdBy = createdBy
            )

            FriendEventType.FRIEND_ID_REMOVED -> FriendIdRemoved(
                createdAt = createdAt,
                streamId = streamId,
                version = version,
                createdBy = createdBy
            )
        }
    }
}

@Repository
interface FriendModelRepository : CoroutineCrudRepository<FriendModel, UUID>, SyncableModelRepository<FriendModel> {
    suspend fun findAllByUserUidAndDeletedIsFalse(userUid: UUID): Flow<FriendModel>
    suspend fun findByUserUidAndFriendEmailAndDeletedIsFalse(userUid: UUID, email: String): FriendModel?
    suspend fun findAllByFriendEmailAndDeletedIsFalse(email: String): Flow<FriendModel>
    suspend fun findByUserUidAndFriendPhoneNumberAndDeletedIsFalse(userUid: UUID, phoneNumber: String): FriendModel?
    suspend fun findAllByFriendPhoneNumberAndDeletedIsFalse(phoneNumber: String): Flow<FriendModel>
    suspend fun findByUserUidAndStreamIdAndDeletedIsFalse(userUid: UUID, recipientId: UUID): FriendModel?
    override suspend fun findByStreamId(streamId: UUID): FriendModel?
    suspend fun findByFriendIdAndDeletedIsFalse(userId: UUID): Flow<FriendModel>
    suspend fun findByFriendId(userId: UUID): Flow<FriendModel>
    suspend fun findByUserUidAndFriendIdAndDeletedIsFalse(userUid: UUID, friendId: UUID): FriendModel?
    suspend fun findAllByUserUid(string: UUID): Flow<FriendModel>
    @Query("select * from friend_model order by insert_order desc limit 1")
    override suspend fun findFirstSortByIdDescending(): FriendModel?
}

@Table("friend_model")
data class FriendModel(
    @Id
    val id: UUID? = null,
    override val streamId: UUID,
    val userUid: UUID,
    val friendId: UUID?,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    override val version: Int,
    override val deleted: Boolean,
) : SyncableModel

data class FriendCreated(
    val id: UUID?,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
    val userId: UUID,
    val friendId: UUID?,
    override val createdAt: Instant,
    override val streamId: UUID,
    override val version: Int,
    override val createdBy: UUID,
) : IFriendEvent {
    override fun toEntity(): FriendEvent {
        return FriendEvent(
            userUid = userId,
            friendId = friendId,
            friendEmail = friendEmail,
            friendPhoneNumber = friendPhoneNumber,
            friendDisplayName = friendDisplayName,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = FriendEventType.FRIEND_CREATED
        )
    }

    override fun applyEvent(existing: FriendModel?): FriendModel {
        return FriendModel(
            streamId = streamId,
            userUid = userId,
            friendId = friendId,
            friendEmail = friendEmail,
            friendPhoneNumber = friendPhoneNumber,
            friendDisplayName = friendDisplayName,
            createdAt = createdAt,
            updatedAt = createdAt,
            version = version,
            deleted = false,
        )
    }
}

data class FriendIdAdded(
    val friendId: UUID,
    override val createdAt: Instant,
    override val streamId: UUID,
    override val version: Int,
    override val createdBy: UUID,
) : IFriendEvent {
    override fun toEntity(): FriendEvent {
        return FriendEvent(
            userUid = null,
            friendId = friendId,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = FriendEventType.FRIEND_ID_ADDED,
            id = null,
            friendEmail = null,
            friendPhoneNumber = null,
            friendDisplayName = null,
        )
    }

    override fun applyEvent(existing: FriendModel?): FriendModel {
        return existing?.copy(
            friendId = friendId,
            updatedAt = createdAt,
            streamId = streamId,
            version = version
        ) ?: throw IllegalStateException("Friend not found while trying to resolve friend ID addition")
    }
}

data class FriendIdRemoved(
    override val createdAt: Instant,
    override val streamId: UUID,
    override val version: Int,
    override val createdBy: UUID,
) : IFriendEvent {
    override fun toEntity(): FriendEvent {
        return FriendEvent(
            userUid = null,
            friendId = null,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = FriendEventType.FRIEND_ID_REMOVED,
            id = null,
            friendEmail = null,
            friendPhoneNumber = null,
            friendDisplayName = null,
        )
    }

    override fun applyEvent(existing: FriendModel?): FriendModel {
        return existing?.copy(
            friendId = null, // Removing the friend ID
            updatedAt = createdAt,
            streamId = streamId,
            version = version
        ) ?: throw IllegalStateException("Friend not found while trying to resolve friend ID removal")
    }
}

data class FriendUpdated(
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String?,
    override val createdAt: Instant,
    override val streamId: UUID,
    override val version: Int,
    override val createdBy: UUID,
) : IFriendEvent {
    override fun toEntity(): FriendEvent {
        return FriendEvent(
            userUid = null,
            friendId = null,
            friendEmail = friendEmail,
            friendPhoneNumber = friendPhoneNumber,
            friendDisplayName = friendDisplayName,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = FriendEventType.FRIEND_UPDATED
        )
    }

    override fun applyEvent(existing: FriendModel?): FriendModel {
        return existing?.copy(
            friendEmail = friendEmail ?: existing.friendEmail,
            friendPhoneNumber = friendPhoneNumber ?: existing.friendPhoneNumber,
            friendDisplayName = friendDisplayName ?: existing.friendDisplayName,
            updatedAt = createdAt,
            streamId = streamId,
            version = version
        ) ?: throw IllegalStateException("Friend not found")
    }
}

data class FriendDeleted(
    override val createdAt: Instant,
    override val streamId: UUID,
    override val version: Int,
    override val createdBy: UUID,
) : IFriendEvent {
    override fun toEntity(): FriendEvent {
        return FriendEvent(
            userUid = null,
            friendId = null,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            eventType = FriendEventType.FRIEND_DELETED,
            id = null,
            friendEmail = null,
            friendPhoneNumber = null,
            friendDisplayName = null,
        )
    }

    override fun applyEvent(existing: FriendModel?): FriendModel {
        return existing?.copy(
            updatedAt = createdAt,
            streamId = streamId,
            version = version,
            deleted = true
        ) ?: throw IllegalStateException("Friend not found while trying to resolve friend deletion")
    }
}

enum class FriendEventType {
    FRIEND_CREATED,
    FRIEND_ID_ADDED,
    FRIEND_ID_REMOVED,
    FRIEND_UPDATED,
    FRIEND_DELETED
}

@Repository
interface FriendEventRepository : CoroutineCrudRepository<FriendEvent, UUID>, SyncableEventRepository<FriendEvent> {
    @Query(
        """
        select * from friend_events 
        where insert_order > (
            select insert_order from friend_events where stream_id = :streamId and version = :version
        ) order by insert_order
        """
    )
    override suspend fun findAllSinceStreamIdAndVersion(
        streamId: UUID,
        version: Int
    ): List<FriendEvent>
}