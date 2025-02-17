package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto

data class FriendTotalAmountsDto(
    val amountsPerCurrency: List<AmountDto>
)

data class UserFriendDto(
    val userId: String,
    val friendId: String,
    val friendTotalAmountsDto: FriendTotalAmountsDto?
)
