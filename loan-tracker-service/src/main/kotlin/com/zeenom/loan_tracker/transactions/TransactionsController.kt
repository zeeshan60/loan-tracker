package com.zeenom.loan_tracker.transactions;

import com.zeenom.loan_tracker.common.Paginated
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
    ) {
        createTransactionCommand.execute(
            CommandDto(
                userId = userId,
                payload = requestToDto(transactionRequest),
                commandType = CommandType.CREATE_TRANSACTION
            )
        )
    }

    @Operation(summary = "Update a transaction")
    @PutMapping("/update")
    suspend fun updateTransaction(
        @RequestBody transactionRequest: TransactionRequest,
        @AuthenticationPrincipal userId: String,
    ) {
        updateTransactionCommand.execute(
            CommandDto(
                userId = userId,
                payload = requestToDto(transactionRequest),
                commandType = CommandType.UPDATE_TRANSACTION
            )
        )
    }

    @Operation(summary = "Get transactions list per page for friend id")
    @GetMapping("/friend")
    suspend fun getTransactions(
        @AuthenticationPrincipal userId: String,
        @RequestBody friendTransactionQueryDto: FriendTransactionQueryDto,
    ): Paginated<TransactionsResponse> {
        return transactionQuery.execute(
            FriendTransactionQueryDto(
                userId = userId,
                friendId = friendTransactionQueryDto.friendId
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
                            )
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
                amount = when (transactionRequest.type) {
                    SplitType.YouPaidSplitEqually -> transactionRequest.amount / 2.toBigDecimal()
                    SplitType.TheyPaidSplitEqually -> transactionRequest.amount / 2.toBigDecimal()
                    SplitType.TheyOweYouAll -> transactionRequest.amount
                    SplitType.YouOweThemAll -> transactionRequest.amount
                },
                currency = Currency.getInstance(transactionRequest.currency),
                isOwed = when (transactionRequest.type) {
                    SplitType.YouPaidSplitEqually -> true
                    SplitType.TheyPaidSplitEqually -> false
                    SplitType.TheyOweYouAll -> true
                    SplitType.YouOweThemAll -> false
                }
            ),
            recipientId = transactionRequest.recipientId
        )
}

data class TransactionRequest(
    val amount: BigDecimal,
    val currency: String,
    val type: SplitType,
    @Schema(description = "For updates recipient id must be same as the original recipient id")
    val recipientId: UUID,
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
    val transactionId: UUID,
    val amountResponse: AmountResponse,
)

data class AmountResponse(
    val amount: BigDecimal,
    val currency: String,
    val isOwed: Boolean,
)