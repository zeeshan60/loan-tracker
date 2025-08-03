package com.zeenom.loan_tracker.transactions

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class TransactionModel(
    val streamId: UUID,
    val userId: UUID,
    val description: String,
    val currency: String,
    val splitType: SplitType,
    val totalAmount: BigDecimal,
    val recipientId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val firstCreatedAt: Instant,
    val createdBy: UUID,
    val updatedBy: UUID?,
    val deleted: Boolean = false,
    val version: Int,
    val transactionDate: Instant,
    //i hate it
    val historyLogId: UUID?,
    val groupId: UUID?,
)

data class ActivityLog(
    val id: UUID,
    val userId: UUID,
    val activityByUid: UUID,
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