package com.zeenom.loan_tracker.transactions

import java.math.BigDecimal
import java.time.Instant

data class ActivityLogsResponse(
    val userUid: String,
    val activityByUid: String,
    val activityByName: String?,
    val activityByPhoto: String?,
    val description: String,
    val activityType: ActivityType,
    val amount: BigDecimal,
    val currency: String,
    val isOwed: Boolean,
    val date: Instant,
    val transactionResponse: TransactionResponse,
)