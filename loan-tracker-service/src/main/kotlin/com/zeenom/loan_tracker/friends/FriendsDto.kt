package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.AmountDto

data class FriendsDto(val friends: List<FriendDto>, val next: String? = null)
data class FriendDto(
    val userId: String?,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val name: String,
    val loanAmount: AmountDto?,
)