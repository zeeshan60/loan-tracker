package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.LoanAmountResponse

data class FriendsResponse(val friends: List<FriendResponse>)
data class FriendResponse(
    val photoUrl: String?,
    val name: String,
    val loanAmount: LoanAmountResponse,
)