package com.zeenom.loan_tracker.transactions

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Repository
interface TransactionModelRepository : CoroutineCrudRepository<TransactionModel, UUID>

@Table("transaction_model")
data class TransactionModel(
    val id: UUID?,
    @Id
    val streamId: UUID,
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
    val version: Int,
    val transactionDate: Instant
)

data class ActivityLog(
    val id: UUID,
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