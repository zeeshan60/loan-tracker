package com.zeenom.loan_tracker.transactions

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class TransactionModel(
    val id: UUID,
    val userUid: String,
    val description: String,
    val currency: String,
    val splitType: SplitType,
    val totalAmount: BigDecimal,
    val recipientId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val firstCreatedAt: Instant,
    val createdBy: String,
    val updatedBy: String?,
    val deleted: Boolean = false,
    val streamId: UUID,
    val version: Int,
)

data class ActivityLog(
    val userUid: String,
    val activityByUid: String,
    val description: String,
    val activityType: ActivityType,
    val amount: BigDecimal,
    val currency: String,
    val isOwed: Boolean,
    val date: Instant,
    val transactionModel: TransactionModel,
)

enum class ActivityType {
    CREATED,
    UPDATED,
    DELETED,
}