package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.events.CommandPayloadDto
import java.math.BigDecimal
import java.util.*

data class TransactionDto(
    val amount: AmountDto,
    val description: String,
    val originalAmount: BigDecimal,
    val splitType: SplitType,
    val recipientId: UUID,
    val transactionStreamId: UUID? = null
) : CommandPayloadDto

data class AmountDto(
    val amount: BigDecimal,
    val currency: Currency,
    val isOwed: Boolean
)
