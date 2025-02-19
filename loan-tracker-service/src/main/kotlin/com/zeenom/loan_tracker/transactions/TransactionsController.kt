package com.zeenom.loan_tracker.transactions;

import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("api/v1/transactions")
class TransactionsController(private val createTransactionCommand: CreateTransactionCommand) {

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
        createTransactionCommand.execute(
            CommandDto(
                userId = userId,
                payload = requestToDto(transactionRequest),
                commandType = CommandType.UPDATE_TRANSACTION
            )
        )
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
                currency = transactionRequest.currency,
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
    val currency: Currency,
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


