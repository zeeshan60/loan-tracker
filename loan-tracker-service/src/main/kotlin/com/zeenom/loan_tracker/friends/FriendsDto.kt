package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.LoanAmountDto

data class FriendsDto(val friends: List<FriendDto>, val next: String? = null)
data class FriendDto(
    val photoUrl: String?,
    val name: String,
    val loanAmount: LoanAmountDto,
)