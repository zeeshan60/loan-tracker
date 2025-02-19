package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.common.Query
import org.springframework.stereotype.Service
import java.util.*

@Service
class TransactionQuery(
    private val transactionReadModel: TransactionReadModel,
) : Query<FriendTransactionQueryDto, Paginated<List<TransactionDto>>> {
    override suspend fun execute(input: FriendTransactionQueryDto): Paginated<List<TransactionDto>> {
        return transactionReadModel.transactionsByFriendId(input.userId, input.friendId).map {
            TransactionDto(
                amount = AmountDto(
                    amount = it.amount,
                    currency = Currency.getInstance(it.currency),
                    isOwed = it.transactionType == TransactionType.CREDIT
                ),
                recipientId = it.recipientId,
                transactionStreamId = it.streamId,
                description = it.description,
                originalAmount = it.totalAmount,
                splitType = it.splitType
            )
        }.let { Paginated(it, null) }
    }
}