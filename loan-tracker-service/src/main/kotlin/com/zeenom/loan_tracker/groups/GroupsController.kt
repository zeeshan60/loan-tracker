package com.zeenom.loan_tracker.groups

import io.swagger.v3.oas.annotations.Operation
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
        return groupsService.createGroup(request, userId)
    }

}