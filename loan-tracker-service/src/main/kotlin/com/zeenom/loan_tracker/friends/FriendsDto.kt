package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.events.CommandPayloadDto
import com.zeenom.loan_tracker.transactions.AmountDto
import com.zeenom.loan_tracker.transactions.AmountResponse
import java.util.*

data class FriendDto(
    val friendId: UUID,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val name: String,
    val mainCurrency: Currency?,
    val balances: AllTimeBalanceDto,
)

data class FriendSummaryDto(
    val friendId: UUID?,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val name: String?,
)

data class FriendsWithAllTimeBalancesDto(
    val friends: List<FriendDto>,
    val balance: AllTimeBalanceDto,
) : CommandPayloadDto

data class AllTimeBalanceDto(
    val main: AmountDto?,
    val other: List<AmountDto>,
) : CommandPayloadDto

data class BalanceResponse(
    val main: AmountResponse?,
    val other: List<AmountResponse>,
)

fun AllTimeBalanceDto.toResponse(): BalanceResponse {
    return BalanceResponse(
        main = main?.toResponse(),
        other = other.map { it.toResponse() }
    )
}

fun AmountDto.toResponse() =
    AmountResponse(
        amount = amount,
        currency = currency.currencyCode,
        isOwed = isOwed
    )