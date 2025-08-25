package com.zeenom.loan_tracker.groups

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

@Table("group_model")
data class GroupModel(
    @Id val id: UUID? = null,
    val name: String,
    val description: String?,
    val memberIds: GroupMemberIds?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: UUID,
    val updatedBy: UUID,
    override val streamId: UUID,
    override val version: Int,
    override val deleted: Boolean
) : SyncableModel

interface GroupModelRepository : CoroutineCrudRepository<GroupModel, UUID>, SyncableModelRepository<GroupModel> {
    @Query("select * from group_model order by insert_order desc limit 1")
    override suspend fun findFirstSortByIdDescending(): GroupModel?
    override suspend fun findByStreamId(streamId: UUID): GroupModel?
    suspend fun findByStreamIdAndDeletedIsFalse(streamId: UUID): GroupModel?
    override fun saveAll(models: List<GroupModel>): Flow<GroupModel>
}

enum class GroupEventType {
    GROUP_CREATED,
    GROUP_MEMBERS_ADDED,
    GROUP_MEMBERS_REMOVED,
    GROUP_UPDATED,
    GROUP_DELETED,
}

data class GroupMemberIds(
    val ids: List<UUID>
)

@Table("group_events")
data class GroupEventEntity(
    @Id
    val id: UUID? = null,
    val name: String?,
    val description: String?,
    val memberIds: GroupMemberIds?,
    val eventType: GroupEventType,
    val createdAt: Instant,
    val createdBy: UUID,
    val streamId: UUID,
    val version: Int
) : IEventAble<GroupModel> {
    override fun toEvent(): GroupEvent {
        return when (eventType) {
            GroupEventType.GROUP_CREATED -> GroupCreated(
                name = name ?: throw IllegalStateException("Group name is required"),
                description = description,
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version
            )

            GroupEventType.GROUP_MEMBERS_ADDED -> GroupMembersAdded(
                memberIds = memberIds?.ids ?: throw IllegalStateException("Member IDs are required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version
            )

            GroupEventType.GROUP_MEMBERS_REMOVED -> GroupMembersRemoved(
                memberIds = memberIds?.ids ?: throw IllegalStateException("Member IDs are required"),
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version
            )

            GroupEventType.GROUP_UPDATED -> GroupUpdated(
                name = name ?: throw IllegalStateException("Group name is required"),
                description = description,
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version
            )

            GroupEventType.GROUP_DELETED -> GroupDeleted(
                createdAt = createdAt,
                createdBy = createdBy,
                streamId = streamId,
                version = version
            )
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
            version = version,
            memberIds = null
        )
    }

    override fun applyEvent(existing: GroupModel?): GroupModel {
        return GroupModel(
            name = name,
            description = description,
            memberIds = null,
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

data class GroupUpdated(
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
            eventType = GroupEventType.GROUP_UPDATED,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            memberIds = null
        )
    }

    override fun applyEvent(existing: GroupModel?): GroupModel {
        requireNotNull(existing) { "Existing group model cannot be null" }
        return existing.copy(
            name = name,
            description = description,
            version = version,
            updatedAt = createdAt,
            updatedBy = createdBy
        )
    }
}

data class GroupDeleted(
    override val createdAt: Instant,
    override val createdBy: UUID,
    override val streamId: UUID,
    override val version: Int
) : GroupEvent {
    override fun toEntity(): GroupEventEntity {
        return GroupEventEntity(
            name = null,
            description = null,
            eventType = GroupEventType.GROUP_DELETED,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            memberIds = null
        )
    }

    override fun applyEvent(existing: GroupModel?): GroupModel {
        requireNotNull(existing) { "Existing group model cannot be null" }
        return existing.copy(
            deleted = true,
            version = version,
            updatedAt = createdAt,
            updatedBy = createdBy
        )
    }
}

data class GroupMembersAdded(
    val memberIds: List<UUID>,
    override val createdAt: Instant,
    override val createdBy: UUID,
    override val streamId: UUID,
    override val version: Int
) : GroupEvent {
    override fun toEntity(): GroupEventEntity {
        return GroupEventEntity(
            name = null,
            description = null,
            memberIds = GroupMemberIds(memberIds),
            eventType = GroupEventType.GROUP_MEMBERS_ADDED,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
        )
    }

    override fun applyEvent(existing: GroupModel?): GroupModel {
        requireNotNull(existing) { "Existing group model cannot be null" }
        val updatedMemberIds = existing.memberIds?.ids?.toMutableList() ?: mutableListOf()
        updatedMemberIds.addAll(memberIds)
        return existing.copy(
            version = version,
            memberIds = GroupMemberIds(updatedMemberIds.distinct()),
            updatedAt = createdAt,
            updatedBy = createdBy
        )
    }
}

data class GroupMembersRemoved(
    val memberIds: List<UUID>,
    override val createdAt: Instant,
    override val createdBy: UUID,
    override val streamId: UUID,
    override val version: Int
) : GroupEvent {
    override fun toEntity(): GroupEventEntity {
        return GroupEventEntity(
            name = null,
            description = null,
            eventType = GroupEventType.GROUP_MEMBERS_REMOVED,
            createdAt = createdAt,
            createdBy = createdBy,
            streamId = streamId,
            version = version,
            memberIds = GroupMemberIds(memberIds)
        )
    }

    override fun applyEvent(existing: GroupModel?): GroupModel {
        requireNotNull(existing) { "Existing group model cannot be null" }
        val updatedMemberIds = existing.memberIds?.ids?.toMutableList() ?: mutableListOf()
        updatedMemberIds.removeAll(memberIds)
        return existing.copy(
            version = version,
            memberIds = GroupMemberIds(updatedMemberIds),
            updatedAt = createdAt,
            updatedBy = createdBy
        )
    }
}

@Repository
interface GroupEventRepository : CoroutineCrudRepository<GroupEventEntity, UUID>,
    SyncableEventRepository<GroupEventEntity> {
    @Query(
        """
        select * from group_events 
        where insert_order > (
            select insert_order from group_events where stream_id = :streamId and version = :version
        ) order by insert_order
        """
    )
    override suspend fun findAllSinceStreamIdAndVersion(
        streamId: UUID,
        version: Int
    ): List<GroupEventEntity>
}