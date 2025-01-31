package com.zeenom.loan_tracker.dtos

data class FriendResponse(
    val photoUrl: String?,
    val name: String,
    val loanAmount: LoanAmountResponse,
)