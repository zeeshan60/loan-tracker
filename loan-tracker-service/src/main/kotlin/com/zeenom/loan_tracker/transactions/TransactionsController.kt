package com.zeenom.loan_tracker.transactions

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
    private val deleteTransactionCommand: DeleteTransactionCommand,
    private val activityLogsQuery: ActivityLogsQuery,
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

    @Operation(summary = "Delete a transaction")
    @DeleteMapping("/delete/transactionId/{transactionId}")
    suspend fun deleteTransaction(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal userId: String,
    ): MessageResponse {
        deleteTransactionCommand.execute(
            CommandDto(
                userId = userId,
                payload = TransactionId(transactionStreamId = transactionId),
                commandType = CommandType.DELETE_TRANSACTION
            )
        )
        return MessageResponse("Transaction deleted successfully")
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
            it.data.filter { !it.deleted }.groupBy {
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
                                currency = transaction.currency.currencyCode,
                                isOwed = transaction.splitType.isOwed()
                            ),
                            history = transaction.history.groupBy { Pair(it.date, it.changedBy) }.map {
                                ChangeSummaryResponse(
                                    changedBy = it.key.second,
                                    changes = it.value.map {
                                        ChangeSummaryByUserResponse(
                                            oldValue = it.oldValue,
                                            newValue = it.newValue,
                                            type = it.type
                                        )
                                    }
                                )
                            },
                            createdAt = transaction.createdAt ?: throw IllegalStateException("Transaction created at is required"),
                            updatedAt = transaction.updatedAt,
                            createdBy = transaction.createdBy?.let {
                                TransactionUserResponse(
                                    id = it,
                                    name = transaction.createdByName ?: throw IllegalStateException("Created by name is required")
                                )
                            } ?: throw IllegalStateException("Created by is required"),
                            updatedBy = transaction.updatedBy?.let {
                                TransactionUserResponse(
                                    id = it,
                                    name = transaction.updatedByName ?: throw IllegalStateException("Updated by name is required")
                                )
                            },
                        )
                    }
                )
            }.let { TransactionsResponse(it) }
        }
    }

    @GetMapping("/activityLogs")
    suspend fun getTransactionActivityLogs(
        @AuthenticationPrincipal userId: String,
    ): Paginated<List<ActivityLogResponse>> {
        return activityLogsQuery.execute(userId)
    }

    private fun requestToDto(transactionRequest: TransactionBaseRequest) =
        TransactionDto(
            recipientId = if (transactionRequest is TransactionCreateRequest) transactionRequest.recipientId else null,
            description = transactionRequest.description,
            splitType = transactionRequest.type,
            originalAmount = transactionRequest.amount,
            currency = Currency.getInstance(transactionRequest.currency),
            recipientName = null,
            updatedAt = null,
            createdAt = null,
            createdBy = null,
            updatedBy = null,
            updatedByName = null,
            transactionStreamId = null,
            createdByName = null,
            deleted = false,
            history = emptyList(),
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
    @Schema(
        description = "This will be first day at 00:00 of the month in passed timezone",
        example = "2021-01-01T00:00:00Z"
    )
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
    val history: List<ChangeSummaryResponse>,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val createdBy: TransactionUserResponse,
    val updatedBy: TransactionUserResponse?,
)

data class TransactionUserResponse(
    val id: String,
    val name: String
)

data class ChangeSummaryResponse(
    val changedBy: String,
    val changes: List<ChangeSummaryByUserResponse>,
)

data class ChangeSummaryByUserResponse(
    val oldValue: String,
    val newValue: String,
    val type: TransactionChangeType,
)

data class AmountResponse(
    val amount: BigDecimal,
    val currency: String,
    val isOwed: Boolean,
)