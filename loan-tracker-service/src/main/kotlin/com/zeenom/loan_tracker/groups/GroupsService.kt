package com.zeenom.loan_tracker.groups

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class GroupsService(private val eventHandler: GroupEventHandler) {

    suspend fun createGroup(request: GroupCreateRequest, userId: UUID): GroupResponse {
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

        return GroupResponse(
            id = model.streamId,
            name = model.name,
            description = model.description,
            members = emptyList(),
            balance = null
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