package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.LoanAmountResponse
import java.util.UUID

data class FriendsResponse(val friends: List<FriendResponse>)
data class FriendResponse(
    val photoUrl: String?,
    val name: String,
    val friendId: UUID,
    val loanAmount: LoanAmountResponse?,
)