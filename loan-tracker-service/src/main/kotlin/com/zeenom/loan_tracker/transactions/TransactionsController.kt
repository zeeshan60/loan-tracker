package com.zeenom.loan_tracker.transactions;

import com.zeenom.loan_tracker.common.*
import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.*

@RestController
@RequestMapping("api/v1/transactions")
class TransactionsController(
    private val createTransactionCommand: CreateTransactionCommand,
    private val updateTransactionCommand: UpdateTransactionCommand,
    private val transactionQuery: TransactionQuery,
) {

    @Operation(summary = "Add a transaction")
    @PostMapping("/add")
    suspend fun addTransaction(
        @RequestBody transactionRequest: TransactionCreateRequest,
        @AuthenticationPrincipal userId: String,
    ): MessageResponse {
        createTransactionCommand.execute(
            CommandDto(
                userId = userId,
                payload = requestToDto(transactionRequest),
                commandType = CommandType.CREATE_TRANSACTION
            )
        )
        return MessageResponse("Transaction added successfully")
    }

    @Operation(summary = "Update a transaction")
    @PutMapping("/update/transactionId/{transactionId}")
    suspend fun updateTransaction(
        @PathVariable transactionId: UUID,
        @RequestBody transactionRequest: TransactionUpdateRequest,
        @AuthenticationPrincipal userId: String,
    ): MessageResponse {
        updateTransactionCommand.execute(
            CommandDto(
                userId = userId,
                payload = requestToDto(transactionRequest).copy(transactionStreamId = transactionId),
                commandType = CommandType.UPDATE_TRANSACTION
            )
        )
        return MessageResponse("Transaction updated successfully")
    }

    @Operation(
        summary = "Get transactions list per page for friend id",
        description = "The `timeZone` parameter should be a valid IANA timezone ID. You can find the full list at: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones"
    )

    @GetMapping("/friend/byMonth")
    suspend fun getTransactions(
        @RequestParam friendId: UUID,
        @Schema(description = "Timezone", example = "Asia/Singapore")
        @RequestParam timeZone: String,
        @AuthenticationPrincipal userId: String,
    ): TransactionsResponse {
        if (timeZone !in ZoneId.getAvailableZoneIds()) {
            throw IllegalArgumentException("Invalid timezone")
        }
        return transactionQuery.execute(
            FriendTransactionQueryDto(
                userId = userId,
                friendId = friendId
            )
        ).let {
            it.data.groupBy {
                it.updatedAt?.startOfMonth(timeZone)
                    ?: throw IllegalStateException("Transaction date is required for month grouping")
            }.map {
                TransactionsPerMonth(
                    date = it.key,
                    transactions = it.value.map { transaction ->
                        TransactionResponse(
                            date = transaction.updatedAt
                                ?: throw IllegalStateException("Transaction date is required for transactions response"),
                            description = transaction.description,
                            transactionId = transaction.transactionStreamId
                                ?: throw IllegalStateException("Transaction stream id is required in transactions response"),
                            totalAmount = transaction.originalAmount,
                            splitType = transaction.splitType,
                            friendName = transaction.recipientName
                                ?: throw IllegalStateException("Friend name is required in transactions response"),
                            amountResponse = AmountResponse(
                                amount = transaction.splitType.apply(transaction.originalAmount),
                                currency = transaction.amount.currency.currencyCode,
                                isOwed = transaction.splitType.isOwed()
                            ),
                            history = transaction.history
                        )
                    }
                )
            }.let { TransactionsResponse(it) }
        }
    }

    private fun requestToDto(transactionRequest: TransactionBaseRequest) =
        TransactionDto(
            amount = AmountDto(
                amount = transactionRequest.type.amountForYou(transactionRequest.amount),
                currency = Currency.getInstance(transactionRequest.currency),
                isOwed = transactionRequest.type.isOwed()
            ),
            recipientId = if (transactionRequest is TransactionCreateRequest) transactionRequest.recipientId else null,
            description = transactionRequest.description,
            splitType = transactionRequest.type,
            originalAmount = transactionRequest.amount,
            recipientName = null,
            updatedAt = null
        )
}


interface TransactionBaseRequest {
    val amount: BigDecimal
    val currency: String
    val type: SplitType
    val description: String
}

data class TransactionCreateRequest(
    override val amount: BigDecimal,
    override val currency: String,
    override val type: SplitType,
    val recipientId: UUID?,
    override val description: String,
) : TransactionBaseRequest

data class TransactionUpdateRequest(
    override val amount: BigDecimal,
    override val currency: String,
    override val type: SplitType,
    override val description: String,
) : TransactionBaseRequest

enum class SplitType {
    YouPaidSplitEqually,
    TheyPaidSplitEqually,
    TheyOweYouAll,
    YouOweThemAll
}

data class TransactionsResponse(
    val perMonth: List<TransactionsPerMonth>,
)

data class TransactionsPerMonth(
    val date: Instant,
    val transactions: List<TransactionResponse>,
)

data class TransactionResponse(
    val date: Instant,
    val description: String,
    val transactionId: UUID,
    val totalAmount: BigDecimal,
    val splitType: SplitType,
    val friendName: String,
    val amountResponse: AmountResponse,
    val history: List<ChangeSummary> = emptyList(),
)

data class AmountResponse(
    val amount: BigDecimal,
    val currency: String,
    val isOwed: Boolean,
)