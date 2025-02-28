package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.*
import org.springframework.stereotype.Service

@Service
class TransactionsQuery(
    private val transactionService: TransactionService,
) : Query<FriendTransactionsQueryDto, Paginated<List<TransactionDto>>> {
    override suspend fun execute(input: FriendTransactionsQueryDto): Paginated<List<TransactionDto>> {
        return Paginated(transactionService.transactionsByFriendId(input.userId, input.friendId), null)
    }
}

@Service
class TransactionQuery(private val transactionService: TransactionService) : Query<FriendTransactionQueryDto, TransactionDto> {
    override suspend fun execute(input: FriendTransactionQueryDto): TransactionDto {
        return transactionService.findByUserIdTransactionId(input.userId, input.friendId, input.transactionId)
    }
}

