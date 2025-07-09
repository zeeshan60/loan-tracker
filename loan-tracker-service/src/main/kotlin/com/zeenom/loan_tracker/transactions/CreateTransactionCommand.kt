package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.events.CommandDao
import com.zeenom.loan_tracker.events.CommandDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.util.*

@Service
class CreateTransactionCommand(
    private val commandDao: CommandDao,
    private val transactionService: TransactionService
) : Command<TransactionDto> {
    override suspend fun execute(commandDto: CommandDto<TransactionDto>) {
        CoroutineScope(Dispatchers.IO).launch {
            commandDao.addCommand(commandDto)
        }

        requireNotNull(commandDto.userId) { "User ID must not be null for CreateTransaction" }
        transactionService.addTransaction(
            userUid = commandDto.userId,
            transactionDto = commandDto.payload
        )
    }
}

data class FriendTransactionsQueryDto(
    val userId: UUID,
    val friendId: UUID,
)

data class FriendTransactionQueryDto(
    val userId: UUID,
    val transactionId: UUID,
)