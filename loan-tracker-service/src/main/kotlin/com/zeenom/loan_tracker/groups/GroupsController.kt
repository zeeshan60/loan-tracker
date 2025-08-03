package com.zeenom.loan_tracker.groups

import com.zeenom.loan_tracker.friends.BalanceResponse
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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



@RestController
@RequestMapping("/api/v1/groups")
class GroupsController {

}