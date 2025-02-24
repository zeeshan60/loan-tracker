package com.zeenom.loan_tracker.transactions

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class TransactionModel(
    val id: UUID,
    val userUid: String,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: TransactionType,
    val splitType: SplitType,
    val totalAmount: BigDecimal,
    val recipientId: UUID,
    val createdAt: Instant,
    val createdBy: String,
    val deleted: Boolean = false,
    val streamId: UUID,
    val version: Int,
)