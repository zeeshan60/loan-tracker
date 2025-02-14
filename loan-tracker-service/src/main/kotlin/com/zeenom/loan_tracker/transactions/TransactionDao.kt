package com.zeenom.loan_tracker.transactions

import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.*

enum class TransactionType {
    DEBIT,
    CREDIT
}

@Table("transactions")
data class Transaction(
    @Id val id: UUID? = null,
    val amount: BigDecimal,
    val currency: String,
    val date: Instant,
    val description: String,
    val type: TransactionType,
    val userId: UUID,
    val friendId: UUID?,
    val friendEmail: String?,
    val friendPhone: String?,
    val transactionTrail: Json,
    val deletedAt: Instant?,
)

data class TransactionTrailDto(
    val id: UUID,
    val amount: BigDecimal,
    val currency: Currency,
    val date: Instant,
    val description: String,
    val type: TransactionType,
    val userId: UUID,
    val friendId: UUID?,
    val friendEmail: String?,
    val friendPhone: String?,
)

data class TransactionTrailsDto(
    val trail: List<TransactionTrailDto>,
)

@Repository
interface TransactionRepository : CoroutineCrudRepository<Transaction, UUID>


class TransactionDao(private val transactionRepository: TransactionRepository) {

}