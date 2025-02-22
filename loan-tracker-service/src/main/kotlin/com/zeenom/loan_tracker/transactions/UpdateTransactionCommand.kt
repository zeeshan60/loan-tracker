package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.events.CommandDao
import com.zeenom.loan_tracker.events.CommandDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

@Service
class UpdateTransactionCommand(
    private val commandDao: CommandDao,
    private val transactionService: TransactionService,
) : Command<TransactionDto> {
    override suspend fun execute(commandDto: CommandDto<TransactionDto>) {
        CoroutineScope(Dispatchers.IO).launch {
            commandDao.addCommand(commandDto)
        }
        if (commandDto.payload.transactionStreamId == null) {
            throw IllegalArgumentException("Transaction stream id is required")
        }
        transactionService.updateTransaction(
            userUid = commandDto.userId,
            transactionDto = commandDto.payload
        )
    }
}