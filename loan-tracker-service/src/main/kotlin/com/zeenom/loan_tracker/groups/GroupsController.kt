package com.zeenom.loan_tracker.groups

import com.zeenom.loan_tracker.common.MessageResponse
import com.zeenom.loan_tracker.common.Paginated
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/groups")
class GroupsController(val groupsService: GroupsService) {

    private val logger = LoggerFactory.getLogger(GroupsController::class.java)

    @Operation(summary = "Create a new group", description = "Creates a new group with user as its member")
    @PostMapping("/create")
    suspend fun createGroup(
        @RequestBody request: GroupCreateRequest,
        @AuthenticationPrincipal userId: UUID
    ): GroupResponse {
        val groupId = groupsService.createGroup(request, userId)
        return groupsService.getGroupResponseById(groupId)
    }

    @Operation(summary = "Get group by ID", description = "Returns a group by its ID")
    @GetMapping("/{groupId}")
    suspend fun getGroupById(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal userId: UUID
    ): GroupResponse {
        return groupsService.getGroupResponseById(groupId)
    }

    @Operation(summary = "Get all groups")
    @GetMapping
    suspend fun getGroupsByUserId(
        @Parameter(description = "Pagination token for the next set of results")
        @RequestParam next: String? = null,
        @AuthenticationPrincipal userId: UUID
    ): Paginated<GroupSummariesResponse> {
        return Paginated(
            data = groupsService.getGroupSummaries(userId),
            next = null
        )
    }

    @Operation(summary = "Update a existing group", description = "Updates a existing group")
    @PutMapping("/{groupId}/update")
    suspend fun updateGroup(
        @PathVariable groupId: UUID,
        @RequestBody request: GroupCreateRequest,
        @AuthenticationPrincipal userId: UUID
    ): GroupResponse {
        groupsService.updateGroup(groupId = groupId, request = request, userId = userId)
        return groupsService.getGroupResponseById(groupId)
    }

    @Operation(summary = "Add members to a group", description = "Adds members to an existing group")
    @PutMapping("/{groupId}/addMembers")
    suspend fun addMembers(
        @PathVariable groupId: UUID,
        @RequestBody request: GroupAddMembersRequest,
        @AuthenticationPrincipal userId: UUID
    ): GroupResponse {
        groupsService.addMembers(groupId = groupId, request = request, userId = userId)
        return groupsService.getGroupResponseById(groupId)
    }

    @Operation(summary = "Remove members from a group", description = "Removes members from an existing group")
    @PutMapping("/{groupId}/removeMembers")
    suspend fun removeMembers(
        @PathVariable groupId: UUID,
        @RequestBody request: GroupRemoveMembersRequest,
        @AuthenticationPrincipal userId: UUID
    ): GroupResponse {
        groupsService.removeMembers(groupId = groupId, request = request, userId = userId)
        return groupsService.getGroupResponseById(groupId)
    }

    @Operation(summary = "Delete a group", description = "Deletes an existing group")
    @DeleteMapping("/{groupId}/delete")
    suspend fun deleteGroup(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal userId: UUID
    ): MessageResponse {
        groupsService.deleteGroup(groupId = groupId, userId = userId)
        return MessageResponse("Group with ID $groupId deleted successfully")
    }
}