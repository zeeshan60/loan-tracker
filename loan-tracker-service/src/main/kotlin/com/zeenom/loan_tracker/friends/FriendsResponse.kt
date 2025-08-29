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
    @Deprecated("Should not use this property anymore, use balance.main instead")
    val mainBalance: AmountResponse?,
    @Deprecated("Should not use this property anymore, use balance.other instead")
    val otherBalances: List<OtherBalanceResponse>,
    val balance: BalanceResponse,
)
