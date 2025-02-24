package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountResponse
import java.util.*

data class FriendsResponse(val friends: List<FriendResponse>)
data class FriendResponse(
    val photoUrl: String?,
    val name: String,
    val friendId: UUID,
    val mainBalance: AmountResponse?,
    val otherBalances: List<AmountResponse>,
)