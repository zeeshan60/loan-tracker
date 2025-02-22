package com.zeenom.loan_tracker.transactions;

import com.zeenom.loan_tracker.common.MessageResponse
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.common.amountForYou
import com.zeenom.loan_tracker.common.isOwed
import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
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
        @RequestBody transactionRequest: TransactionRequest,
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
        @RequestBody transactionRequest: TransactionRequest,
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

    @Operation(summary = "Get transactions list per page for friend id")
    @GetMapping("/friend")
    suspend fun getTransactions(
        @RequestParam friendId: UUID,
        @AuthenticationPrincipal userId: String,
    ): Paginated<TransactionsResponse> {
        return transactionQuery.execute(
            FriendTransactionQueryDto(
                userId = userId,
                friendId = friendId
            )
        ).let {
            Paginated(
                TransactionsResponse(
                    transactions = it.data.map { transaction ->
                        TransactionResponse(
                            transactionId = transaction.transactionStreamId!!,
                            amountResponse = AmountResponse(
                                amount = transaction.amount.amount,
                                currency = transaction.amount.currency.toString(),
                                isOwed = transaction.amount.isOwed
                            ),
                            totalAmount = transaction.originalAmount,
                            friendName = transaction.recipientName!!,
                            description = transaction.description,
                            history = transaction.history,
                        )
                    }
                ),
                it.next
            )
        }
    }

    private fun requestToDto(transactionRequest: TransactionRequest) =
        TransactionDto(
            amount = AmountDto(
                amount = transactionRequest.type.amountForYou(transactionRequest.amount),
                currency = Currency.getInstance(transactionRequest.currency),
                isOwed = transactionRequest.type.isOwed()
            ),
            recipientId = transactionRequest.recipientId,
            description = transactionRequest.description,
            splitType = transactionRequest.type,
            originalAmount = transactionRequest.amount,
            recipientName = null
        )
}


data class TransactionRequest(
    val amount: BigDecimal,
    val currency: String,
    val type: SplitType,
    @Schema(description = "For updates recipient id must be same as the original recipient id")
    val recipientId: UUID,
    val description: String,
)

enum class SplitType {
    YouPaidSplitEqually,
    TheyPaidSplitEqually,
    TheyOweYouAll,
    YouOweThemAll
}

data class TransactionsResponse(
    val transactions: List<TransactionResponse>,
)

data class TransactionResponse(
    val description: String,
    val transactionId: UUID,
    val totalAmount: BigDecimal,
    val friendName: String,
    val amountResponse: AmountResponse,
    val history: List<ChangeSummary> = emptyList(),
)

data class AmountResponse(
    val amount: BigDecimal,
    val currency: String,
    val isOwed: Boolean,
)