package com.zeenom.loan_tracker.common

import com.zeenom.loan_tracker.events.CommandPayloadDto
import java.math.BigDecimal
import java.util.*

data class TransactionDto(
    val amount: AmountDto,
    val recipientId: String
) : CommandPayloadDto

data class AmountDto(
    val amount: BigDecimal,
    val currency: Currency,
    val isOwed: Boolean
)