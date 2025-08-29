package com.zeenom.loan_tracker.groups

import com.zeenom.loan_tracker.common.exceptions.NotFoundException
import com.zeenom.loan_tracker.users.UserEventHandler
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class GroupsService(private val eventHandler: GroupEventHandler, val userEventHandler: UserEventHandler) {

    suspend fun createGroup(request: GroupCreateRequest, userId: UUID): UUID {
        val streamId = UUID.randomUUID()
        val event = GroupCreated(
            name = request.name,
            description = request.description,
            createdAt = Instant.now(),
            createdBy = userId,
            streamId = streamId,
            version = 1
        )

        eventHandler.saveEvent(event)
        val addMemberEvent = GroupMembersAdded(
            memberIds = listOf(userId),
            createdAt = Instant.now(),
            createdBy = userId,
            streamId = streamId,
            version = 2
        )
        eventHandler.saveEvent(addMemberEvent)
        eventHandler.synchronize()

        return streamId
    }

    suspend fun getGroupResponseById(groupId: UUID): GroupResponse {
        val model = eventHandler.getModelByStreamId(groupId)
        if (model == null) {
            throw NotFoundException("Group model not found after creation")
        }

        val members = model.memberIds?.ids?.let { userEventHandler.findUsersByUids(it) } ?: emptyList()

        return GroupResponse(
            id = model.streamId,
            name = model.name,
            description = model.description,
            members = members.map { member ->
                GroupMemberResponse(
                    memberId = member.uid ?: throw IllegalStateException("Member UID is null"),
                    memberName = member.displayName,
                    userBalanceWithThisMember = null
                )
            },
            balance = null
        )
    }

    suspend fun addMembers(groupId: UUID, request: GroupAddMembersRequest, userId: UUID) {
        val existing = eventHandler.getModelByStreamId(groupId)
            ?: throw IllegalArgumentException("Group with ID $groupId does not exist")

        val memberIds = request.memberIds
        val event = GroupMembersAdded(
            memberIds = memberIds,
            createdAt = Instant.now(),
            createdBy = userId,
            streamId = existing.streamId,
            version = existing.version + 1
        )

        validateMembers(memberIds)

        eventHandler.saveEvent(event)
        eventHandler.synchronize()
    }

    private suspend fun validateMembers(memberIds: List<UUID>) {
        val members = userEventHandler.findUsersByUids(memberIds)

        if (members.size != memberIds.size) {
            val missingIds = memberIds.filter { id -> members.none { it.uid == id } }
            throw IllegalArgumentException("Some member IDs do not correspond to existing users: $missingIds")
        }
    }

    suspend fun removeMembers(groupId: UUID, request: GroupRemoveMembersRequest, userId: UUID) {
        val existing = eventHandler.getModelByStreamId(groupId)
            ?: throw IllegalArgumentException("Group with ID $groupId does not exist")
        val event = GroupMembersRemoved(
            memberIds = request.memberIds,
            createdAt = Instant.now(),
            createdBy = userId,
            streamId = existing.streamId,
            version = existing.version + 1
        )

        eventHandler.saveEvent(event)
        eventHandler.synchronize()
    }

    suspend fun getGroupSummaries(userId: UUID): GroupSummariesResponse {
        return eventHandler.getAllModels(userId = userId).map { model ->
            GroupSummaryResponse(
                id = model.streamId,
                name = model.name,
                description = model.description,
                memberCount = model.memberIds?.ids?.size ?: 0,
                balance = null
            )
        }.let { GroupSummariesResponse(it) }
    }

    suspend fun updateGroup(groupId: UUID, request: GroupCreateRequest, userId: UUID) {
        val existing = eventHandler.getModelByStreamId(groupId)
            ?: throw IllegalArgumentException("Group with ID $groupId does not exist")

        val event = GroupUpdated(
            name = request.name,
            description = request.description,
            createdAt = Instant.now(),
            createdBy = userId,
            streamId = existing.streamId,
            version = existing.version + 1
        )
        eventHandler.saveEvent(event)
        eventHandler.synchronize()
    }

    suspend fun deleteGroup(groupId: UUID, userId: UUID) {
        val existing = eventHandler.getModelByStreamId(groupId)
            ?: throw IllegalArgumentException("Group with ID $groupId does not exist")

        val event = GroupDeleted(
            createdAt = Instant.now(),
            streamId = existing.streamId,
            version = existing.version + 1,
            createdBy = userId
        )
        eventHandler.saveEvent(event)
        eventHandler.synchronize()
    }
}