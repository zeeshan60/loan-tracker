package com.zeenom.loan_tracker.controllers

import java.math.BigDecimal

data class FriendsResponse(val friends: List<FriendResponse>)
data class FriendResponse(
    val photoUrl: String?,
    val name: String,
    val loanAmount: LoanAmountResponse,
)

data class LoanAmountResponse(val amount: BigDecimal, val isOwed: Boolean)