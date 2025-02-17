package com.zeenom.loan_tracker.transactions

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Table("transaction_events")
data class TransactionEvent(
    @Id val id: UUID? = null,
    val userUid: String,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: TransactionType,
    val recipientId: UUID,
    val createdAt: Instant,
    val createdBy: String,
    val streamId: UUID,
    val version: Int,
    val eventType: TransactionEventType,
)

enum class TransactionType {
    CREDIT, DEBIT
}

enum class TransactionEventType {
    TRANSACTION_CREATED
}

@Repository
interface TransactionEventRepository : CoroutineCrudRepository<TransactionEvent, UUID>
