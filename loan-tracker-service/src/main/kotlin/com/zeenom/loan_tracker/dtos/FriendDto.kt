package com.zeenom.loan_tracker.dtos

data class FriendDto(
    val photoUrl: String?,
    val name: String,
    val loanAmount: LoanAmountDto,
)