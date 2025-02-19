package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.events.CommandDao
import com.zeenom.loan_tracker.events.CommandDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

@Service
class CreateTransactionCommand(
    private val transactionEventHandler: TransactionEventHandler,
    private val commandDao: CommandDao,
) : Command<TransactionDto> {
    override suspend fun execute(commandDto: CommandDto<TransactionDto>) {
        CoroutineScope(Dispatchers.IO).launch {
            commandDao.addCommand(commandDto)
        }
        transactionEventHandler.addTransaction(commandDto.userId, commandDto.payload)
    }
}

@Service
class UpdateTransactionCommand(
    private val transactionEventHandler: TransactionEventHandler,
    private val commandDao: CommandDao,
) : Command<TransactionDto> {
    override suspend fun execute(commandDto: CommandDto<TransactionDto>) {
        CoroutineScope(Dispatchers.IO).launch {
            commandDao.addCommand(commandDto)
        }
        transactionEventHandler.updateTransaction(commandDto.userId, commandDto.payload)
    }
}