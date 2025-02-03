package com.zeenom.loan_tracker.friends.dtos

import com.zeenom.loan_tracker.common.LoanAmountDto

data class FriendDto(
    val photoUrl: String?,
    val name: String,
    val loanAmount: LoanAmountDto,
)