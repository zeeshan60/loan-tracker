package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.common.Query
import org.springframework.stereotype.Service

@Service
class TransactionQuery(
    private val transactionReadModel: TransactionReadModel,
) : Query<FriendTransactionQueryDto, Paginated<List<TransactionDto>>> {
    override suspend fun execute(input: FriendTransactionQueryDto): Paginated<List<TransactionDto>> {
        return Paginated(transactionReadModel.transactionsByFriendId(input.userId, input.friendId), null)
    }
}