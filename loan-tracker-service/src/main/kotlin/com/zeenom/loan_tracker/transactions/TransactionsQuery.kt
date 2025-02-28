package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.*
import org.springframework.stereotype.Service

@Service
class TransactionsQuery(
    private val transactionService: TransactionService,
) : Query<FriendTransactionQueryDto, Paginated<List<TransactionDto>>> {
    override suspend fun execute(input: FriendTransactionQueryDto): Paginated<List<TransactionDto>> {
        return Paginated(transactionService.transactionsByFriendId(input.userId, input.friendId), null)
    }
}

