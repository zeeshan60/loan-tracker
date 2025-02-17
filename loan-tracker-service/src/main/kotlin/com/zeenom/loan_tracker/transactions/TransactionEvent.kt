package com.zeenom.loan_tracker.transactions

import kotlinx.coroutines.flow.Flow
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
    val currency: Currency,
    val transactionType: TransactionType,
    val recipientId: String,
    val createdAt: Instant,
    val streamId: UUID,
    val version: Int,
    val eventType: TransactionEventType
)

enum class TransactionType {
    CREDIT, DEBIT
}

enum class TransactionEventType {
    TRANSACTION_CREATED
}

@Repository
interface TransactionEventRepository : CoroutineCrudRepository<TransactionEvent, UUID> {
    suspend fun findAllByUserUid(userUid: String): Flow<TransactionEvent>
    suspend fun findByRecipientId(recipientId: String): Flow<TransactionEvent>
    suspend fun findByUserUidAndRecipientId(userUid: String, recipientId: String): TransactionEvent?
}