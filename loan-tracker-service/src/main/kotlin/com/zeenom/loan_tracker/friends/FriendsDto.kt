package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.events.EventPayloadDto

data class FriendsDto(val friends: List<FriendDto>, val next: String? = null) : EventPayloadDto
data class FriendDto(
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val name: String,
    val loanAmount: AmountDto?,
)