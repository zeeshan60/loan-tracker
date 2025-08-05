package com.zeenom.loan_tracker.groups

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.friends.BalanceResponse
import com.zeenom.loan_tracker.transactions.IEventAble
import com.zeenom.loan_tracker.users.SyncableEventHandler
import com.zeenom.loan_tracker.users.SyncableEventRepository
import com.zeenom.loan_tracker.users.SyncableModel
import com.zeenom.loan_tracker.users.SyncableModelRepository
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*

data class GroupSummariesResponse(
    val groups: List<GroupSummaryResponse>,
)

data class GroupSummaryResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val memberCount: Int,
    val balance: BalanceResponse
)

data class GroupResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val members: List<GroupMemberResponse>,
    val balance: BalanceResponse
)

data class GroupMemberResponse(
    val memberId: UUID,
    val memberName: String,
    val memberGroupBalance: BalanceResponse,
    val userBalanceWithThisMember: BalanceResponse,
)

data class GroupCreateRequest(
    val name: String,
    val description: String?,
)

data class GroupRemoveMembersRequest(
    val memberIds: List<UUID>,
)

data class GroupAddMembersRequest(
    val memberIds: List<UUID>,
)

data class GroupModel(
    val id: UUID,
    val name: String,
    val description: String?,
    override val streamId: UUID,
    override val version: Int,
    override val deleted: Boolean
) : SyncableModel

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
        TODO("Not yet implemented")
    }

    override fun applyEvent(existing: GroupModel?): GroupModel {
        TODO("Not yet implemented")
    }
}

@Repository
interface GroupEventRepository : CoroutineCrudRepository<GroupEventEntity, UUID> {
}

@Service
class GroupEventHandler(val groupEventRepository: GroupEventRepository) :
    SyncableEventHandler<GroupModel, GroupEventEntity> {

    suspend fun saveEvent(event: GroupEvent) {
        val entity = event.toEntity()
        groupEventRepository.save(entity)
    }

    override fun modelRepository(): SyncableModelRepository<GroupModel> {
        TODO("Not yet implemented")
    }

    override fun eventRepository(): SyncableEventRepository<GroupEventEntity> {
        TODO("Not yet implemented")
    }

    fun getModelByStreamId(streamId: UUID): GroupModel? {
        // Logic to fetch the model by stream ID
        TODO("Implement fetching model by stream ID")
    }
}

@Service
class GroupsService(private val eventHandler: GroupEventHandler) {

    suspend fun createGroup(request: GroupCreateRequest, userId: UUID): GroupSummaryResponse {
        val event = GroupCreated(
            name = request.name,
            description = request.description,
            createdAt = Instant.now(),
            createdBy = userId,
            streamId = UUID.randomUUID(),
            version = 1
        )

        eventHandler.saveEvent(event)
        eventHandler.synchronize()

        val model = eventHandler.getModelByStreamId(event.streamId)
        if (model == null) {
            throw IllegalStateException("Group model not found after creation")
        }

        return GroupSummaryResponse(
            id = model.streamId,
            name = model.name,
            description = model.description,
            memberCount = 0, // Initially no members
            balance = BalanceResponse(
                main = null,
                other = emptyList()
            )
        )
    }

    suspend fun removeMembers(groupId: UUID, request: GroupRemoveMembersRequest): GroupSummaryResponse {
        // Logic to remove members from a group and return its updated summary
        TODO("Implement member removal logic")
    }

    suspend fun addMembers(groupId: UUID, request: GroupAddMembersRequest): GroupSummaryResponse {
        // Logic to add members to a group and return its updated summary
        TODO("Implement member addition logic")
    }

    suspend fun getGroupSummaries(): GroupSummariesResponse {
        // Logic to fetch all group summaries
        TODO("Implement fetching group summaries logic")
    }
}

@RestController
@RequestMapping("/api/v1/groups")
class GroupsController {

}