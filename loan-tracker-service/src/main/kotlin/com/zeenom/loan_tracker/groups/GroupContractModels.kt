package com.zeenom.loan_tracker.groups

import com.zeenom.loan_tracker.friends.BalanceResponse
import java.util.UUID

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

data class GroupResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val members: List<GroupMemberResponse>,
    val balance: BalanceResponse?
)

data class GroupMemberResponse(
    val memberId: UUID,
    val memberName: String,
    val userBalanceWithThisMember: BalanceResponse?,
)

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
