package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.transactions.AmountDto
import com.zeenom.loan_tracker.events.CommandPayloadDto
import java.util.Currency
import java.util.UUID

data class FriendsDto(val friends: List<FriendDto>) : CommandPayloadDto
data class FriendDto(
    val friendId: UUID,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val name: String,
    val mainCurrency: Currency?,
    val balances: List<AmountDto>
)

data class FriendsWithAllTimeBalancesDto(
    val friends: List<FriendDto>,
    val balance: AllTimeBalanceDto
) : CommandPayloadDto

data class AllTimeBalanceDto(
    val main: AmountDto?,
    val other: List<AmountDto>
) : CommandPayloadDto