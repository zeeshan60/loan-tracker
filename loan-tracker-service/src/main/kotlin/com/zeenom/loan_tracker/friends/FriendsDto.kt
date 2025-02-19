package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto
import com.zeenom.loan_tracker.events.CommandPayloadDto
import java.util.UUID

data class FriendsDto(val friends: List<FriendDto>, val next: String? = null) : CommandPayloadDto
data class FriendDto(
    val friendId: UUID,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val name: String,
    val loanAmount: AmountDto?
)