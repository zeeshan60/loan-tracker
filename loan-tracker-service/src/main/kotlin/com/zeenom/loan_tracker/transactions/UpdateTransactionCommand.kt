package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.events.CommandDao
import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandPayloadDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.util.*

@Service
class UpdateTransactionCommand(
    private val commandDao: CommandDao,
    private val transactionService: TransactionService,
) : Command<TransactionDto> {
    override suspend fun execute(commandDto: CommandDto<TransactionDto>) {
        CoroutineScope(Dispatchers.IO).launch {
            commandDao.addCommand(commandDto)
        }
        requireNotNull(commandDto.userId) { "User ID must not be null for UpdateTransaction" }
        transactionService.updateTransaction(
            userUid = commandDto.userId,
            transactionDto = commandDto.payload
        )
    }
}

@Service
class DeleteTransactionCommand(
    private val commandDao: CommandDao,
    private val transactionService: TransactionService,
) : Command<TransactionId> {
    override suspend fun execute(commandDto: CommandDto<TransactionId>) {
        CoroutineScope(Dispatchers.IO).launch {
            commandDao.addCommand(commandDto)
        }
        requireNotNull(commandDto.userId) { "User ID must not be null for DeleteTransaction" }
        transactionService.deleteTransaction(
            userUid = commandDto.userId,
            transactionStreamId = commandDto.payload.transactionStreamId
        )
    }
}

data class TransactionId(val transactionStreamId: UUID): CommandPayloadDto