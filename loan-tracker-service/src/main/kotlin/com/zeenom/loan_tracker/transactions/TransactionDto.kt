package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.events.CommandPayloadDto
import com.zeenom.loan_tracker.friends.FriendSummaryDto
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class TransactionDto(
    val currency: Currency,
    val description: String,
    val originalAmount: BigDecimal,
    val splitType: SplitType,
    val friendSummaryDto: FriendSummaryDto,
    val transactionStreamId: UUID,
    val transactionDate: Instant,
    val updatedAt: Instant?,
    val createdAt: Instant?,
    val createdBy: String?,
    val createdByName: String?,
    val updatedBy: String?,
    val updatedByName: String?,
    val deleted: Boolean = false,
    val history: List<ChangeSummaryDto> = emptyList()
) : CommandPayloadDto

data class AmountDto(
    val amount: BigDecimal,
    val currency: Currency,
    val isOwed: Boolean
)

data class ChangeSummaryDto(
    val date: Instant,
    val changedBy: String,
    val changedByName: String,
    val changedByPhoto: String?,
    val oldValue: String,
    val newValue: String,
    val type: TransactionChangeType,
)