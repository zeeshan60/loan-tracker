package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.common.apply
import com.zeenom.loan_tracker.common.isOwed
import com.zeenom.loan_tracker.common.startOfMonth
import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import com.zeenom.loan_tracker.friends.BalanceResponse
import com.zeenom.loan_tracker.friends.FriendSummaryDto
import com.zeenom.loan_tracker.friends.toResponse
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
    private val transactionsQuery: TransactionsQuery,
    private val deleteTransactionCommand: DeleteTransactionCommand,
    private val activityLogsQuery: ActivityLogsQuery,
    private val transactionQuery: TransactionQuery,
) {

    @Operation(summary = "Add a transaction")
    @PostMapping("/add")
    suspend fun addTransaction(
        @RequestBody transactionRequest: TransactionCreateRequest,
        @AuthenticationPrincipal userId: String,
    ): TransactionResponse {
        val transactionId = UUID.randomUUID()
        createTransactionCommand.execute(
            CommandDto(
                userId = userId,
                payload = requestToDto(transactionRequest, transactionId),
                commandType = CommandType.CREATE_TRANSACTION
            )
        )
        return transactionQuery.execute(
            FriendTransactionQueryDto(
                userId = userId,
                transactionId = transactionId
            )
        ).toResponse()
    }

    private fun TransactionDto.toResponse() = TransactionResponse(
        date = transactionDate,
        description = description,
        transactionId = transactionStreamId,
        totalAmount = originalAmount,
        splitType = splitType,
        amountResponse = AmountResponse(
            amount = splitType.apply(originalAmount),
            currency = currency.currencyCode,
            isOwed = splitType.isOwed()
        ),
        history = history.groupBy { Pair(it.date, it.changedBy) }.map {
            ChangeSummaryResponse(
                changedBy = it.value.first().changedBy,
                changedByName = it.value.first().changedByName,
                changedByPhoto = it.value.first().changedByPhoto,
                changes = it.value
            )
        },
        createdAt = createdAt ?: throw IllegalStateException("Created at is required"),
        updatedAt = updatedAt,
        createdBy = createdBy?.let {
            TransactionUserResponse(
                id = it,
                name = createdByName ?: throw IllegalStateException("Created by name is required")
            )
        } ?: throw IllegalStateException("Created by is required"),
        updatedBy = updatedBy?.let {
            TransactionUserResponse(
                id = it,
                name = updatedByName ?: throw IllegalStateException("Updated by name is required")
            )
        },
        friend = friendSummaryDto,
        deleted = deleted
    )

    @Operation(summary = "Update a transaction")
    @PutMapping("/update/transactionId/{transactionId}")
    suspend fun updateTransaction(
        @PathVariable transactionId: UUID,
        @RequestBody transactionRequest: TransactionUpdateRequest,
        @AuthenticationPrincipal userId: String,
    ): TransactionResponse {
        updateTransactionCommand.execute(
            CommandDto(
                userId = userId,
                payload = requestToDto(transactionRequest, transactionId),
                commandType = CommandType.UPDATE_TRANSACTION
            )
        )

        return transactionQuery.execute(
            FriendTransactionQueryDto(
                userId = userId,
                transactionId = transactionId
            )
        ).toResponse()
    }

    @Operation(summary = "Delete a transaction")
    @DeleteMapping("/delete/transactionId/{transactionId}")
    suspend fun deleteTransaction(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal userId: String,
    ): TransactionResponse {
        deleteTransactionCommand.execute(
            CommandDto(
                userId = userId,
                payload = TransactionId(transactionStreamId = transactionId),
                commandType = CommandType.DELETE_TRANSACTION
            )
        )
        return transactionQuery.execute(
            FriendTransactionQueryDto(
                userId = userId,
                transactionId = transactionId
            )
        ).toResponse()
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
        return transactionsQuery.execute(
            FriendTransactionsQueryDto(
                userId = userId,
                friendId = friendId
            )
        ).let { transactionsDtoPaginated ->
            transactionsDtoPaginated.data.transactions.sortedByDescending { it.transactionDate }.groupBy {
                it.transactionDate.startOfMonth(timeZone)
            }.map {
                TransactionsPerMonth(
                    date = it.key,
                    transactions = it.value.map { it.toResponse() }
                )
            }.let {
                TransactionsResponse(
                    perMonth = it,
                    balance = transactionsDtoPaginated.data.balance.toResponse()
                )
            }
        }
    }

    @GetMapping("/activityLogs")
    suspend fun getTransactionActivityLogs(
        @AuthenticationPrincipal userId: String,
    ): Paginated<List<ActivityLogResponse>> {
        return activityLogsQuery.execute(userId)
    }

    private fun requestToDto(transactionRequest: TransactionBaseRequest, transactionId: UUID) =
        TransactionDto(
            description = transactionRequest.description,
            splitType = transactionRequest.type,
            originalAmount = transactionRequest.amount,
            currency = Currency.getInstance(transactionRequest.currency),
            updatedAt = null,
            createdAt = null,
            createdBy = null,
            updatedBy = null,
            updatedByName = null,
            transactionStreamId = transactionId,
            createdByName = null,
            deleted = false,
            history = emptyList(),
            transactionDate = transactionRequest.transactionDate,
            friendSummaryDto = FriendSummaryDto(
                friendId = if (transactionRequest is TransactionCreateRequest) transactionRequest.recipientId else null,
                email = null,
                phoneNumber = null,
                photoUrl = null,
                name = null
            )
        )
}


interface TransactionBaseRequest {
    val transactionDate: Instant
    val amount: BigDecimal
    val currency: String
    val type: SplitType
    val description: String
}

data class TransactionCreateRequest(
    override val transactionDate: Instant,
    override val amount: BigDecimal,
    override val currency: String,
    override val type: SplitType,
    val recipientId: UUID?,
    override val description: String,
) : TransactionBaseRequest

data class TransactionUpdateRequest(
    override val transactionDate: Instant,
    override val amount: BigDecimal,
    override val currency: String,
    override val type: SplitType,
    override val description: String,
) : TransactionBaseRequest

enum class SplitType {
    YouPaidSplitEqually,
    TheyPaidSplitEqually,
    TheyOweYouAll,
    YouOweThemAll,
    TheyPaidToSettle,
    YouPaidToSettle
}

data class TransactionsResponse(
    val balance: BalanceResponse,
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
    val friend: FriendSummaryDto,
    val amountResponse: AmountResponse,
    val history: List<ChangeSummaryResponse>,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val createdBy: TransactionUserResponse,
    val updatedBy: TransactionUserResponse?,
    val deleted: Boolean,
)

data class TransactionUserResponse(
    val id: String,
    val name: String,
)

data class ChangeSummaryResponse(
    val changedBy: String,
    val changedByName: String,
    val changedByPhoto: String?,
    val changes: List<ChangeSummaryDto>,
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