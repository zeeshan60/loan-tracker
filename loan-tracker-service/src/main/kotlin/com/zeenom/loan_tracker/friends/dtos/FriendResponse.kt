package com.zeenom.loan_tracker.friends.dtos

import com.zeenom.loan_tracker.dtos.LoanAmountResponse

data class FriendResponse(
    val photoUrl: String?,
    val name: String,
    val loanAmount: LoanAmountResponse,
)