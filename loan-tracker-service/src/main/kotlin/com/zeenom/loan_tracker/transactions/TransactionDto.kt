package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.events.CommandPayloadDto
import com.zeenom.loan_tracker.friends.AllTimeBalanceDto
import com.zeenom.loan_tracker.friends.FriendSummaryDto
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class TransactionsDto(
    val transactions: List<TransactionDto>,
    val balance: AllTimeBalanceDto
)

data class TransactionDto(
    val defaultCurrency: String?,
    val amountInDefaultCurrency: BigDecimal?,
    val currency: Currency,
    val description: String,
    val originalAmount: BigDecimal,
    val splitType: SplitType,
    val friendSummaryDto: FriendSummaryDto,
    val transactionStreamId: UUID,
    val transactionDate: Instant,
    val updatedAt: Instant?,
    val createdAt: Instant?,
    val createdBy: UUID?,
    val createdByName: String?,
    val updatedBy: UUID?,
    val updatedByName: String?,
    val deleted: Boolean = false,
    val history: List<ChangeSummaryDto> = emptyList()
) : CommandPayloadDto

data class AmountDto(
    val amount: BigDecimal,
    val currency: Currency,
    val isOwed: Boolean
)

data class TimedAmountDto(
    val amountDto: AmountDto,
    val lastUpdated: Instant,
)

data class ChangeSummaryDto(
    val date: Instant,
    val changedBy: UUID,
    val changedByName: String,
    val changedByPhoto: String?,
    val oldValue: String,
    val newValue: String,
    val type: TransactionChangeType,
)