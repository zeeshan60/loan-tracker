package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.events.CommandPayloadDto
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class TransactionDto(
    val currency: Currency,
    val description: String,
    val originalAmount: BigDecimal,
    val splitType: SplitType,
    val recipientId: UUID?,
    val recipientName: String?,
    val transactionStreamId: UUID? = null,
    val transactionDate: Instant,
    val updatedAt: Instant?,
    val createdAt: Instant?,
    val createdBy: String?,
    val createdByName: String?,
    val updatedBy: String?,
    val updatedByName: String?,
    val deleted: Boolean = false,
    val history: List<ChangeSummary> = emptyList()
) : CommandPayloadDto

data class AmountDto(
    val amount: BigDecimal,
    val currency: Currency,
    val isOwed: Boolean
)
