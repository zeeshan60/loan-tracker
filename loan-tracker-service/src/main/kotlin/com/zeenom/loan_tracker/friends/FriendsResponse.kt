package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountResponse
import com.zeenom.loan_tracker.transactions.OtherBalanceResponse
import java.util.*

data class FriendsResponse(
    val friends: List<FriendResponse>,
    val balance: BalanceResponse,
)

data class FriendResponse(
    val photoUrl: String?,
    val name: String,
    val friendId: UUID,
    val settled: Boolean = false,
    val email: String?,
    val phone: String?,
    val mainBalance: AmountResponse?,
    val otherBalances: List<OtherBalanceResponse>,
)