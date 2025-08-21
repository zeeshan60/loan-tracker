package com.zeenom.loan_tracker.groups

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.friends.FriendEvent
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
import java.util.UUID

@Table("group_model")
data class GroupModel(
    @Id val id: UUID? = null,
    val name: String,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: UUID,
    val updatedBy: UUID,
    override val streamId: UUID,
    override val version: Int,
    override val deleted: Boolean
) : SyncableModel

interface GroupModelRepository : CoroutineCrudRepository<GroupModel, UUID>,  SyncableModelRepository<GroupModel>{
    @Query("select * from group_model order by insert_order desc limit 1")
    override suspend fun findFirstSortByIdDescending(): GroupModel?
    override suspend fun findByStreamId(streamId: UUID): GroupModel?
    override fun saveAll(models: List<GroupModel>): Flow<GroupModel>
}

enum class GroupEventType {
    GROUP_CREATED,
    GROUP_MEMBER_ADDED,
    GROUP_MEMBER_REMOVED,
    GROUP_DESCRIPTION_CHANGED,
}

@Table("group_events")
data class GroupEventEntity(
    @Id
    val id: UUID? = null,
    val name: String,
    val description: String?,
    val eventType: GroupEventType,
    val createdAt: Instant,
    val createdBy: UUID,
    val streamId: UUID,
    val version: Int
) : IEventAble<GroupModel> {
    override fun toEvent(): GroupEvent {
        return when (eventType) {
            GroupEventType.GROUP_CREATED -> GroupCreated(
                name = name,
                description = description,
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version
            )

            GroupEventType.GROUP_MEMBER_ADDED -> TODO()
            GroupEventType.GROUP_MEMBER_REMOVED -> TODO()
            GroupEventType.GROUP_DESCRIPTION_CHANGED -> TODO()
        }
    }
}

interface GroupEvent : IEvent<GroupModel> {
    override fun toEntity(): GroupEventEntity
}

data class GroupCreated(
    val name: String,
    val description: String?,
    override val createdAt: Instant,
    override val createdBy: UUID,
    override val streamId: UUID,
    override val version: Int
) : GroupEvent {
    override fun toEntity(): GroupEventEntity {
         return GroupEventEntity(
            name = name,
            description = description,
            eventType = GroupEventType.GROUP_CREATED,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version
         )
    }

    override fun applyEvent(existing: GroupModel?): GroupModel {
        return GroupModel(
            name = name,
            description = description,
            streamId = streamId,
            version = version,
            deleted = false,
            createdAt = createdAt,
            updatedAt = createdAt,
            createdBy = createdBy,
            updatedBy = createdBy,
        )
    }
}

@Repository
interface GroupEventRepository : CoroutineCrudRepository<GroupEventEntity, UUID>, SyncableEventRepository<GroupEventEntity> {
    @Query(
        """
        select * from group_events 
        where insert_order > (
            select insert_order from friend_events where stream_id = :streamId and version = :version
        ) order by insert_order
        """
    )
    override suspend fun findAllSinceStreamIdAndVersion(
        streamId: UUID,
        version: Int
    ): List<GroupEventEntity>
}