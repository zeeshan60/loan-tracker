package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.common.*
import org.springframework.stereotype.Service

@Service
class TransactionQuery(
    private val transactionService: TransactionService,
) : Query<FriendTransactionQueryDto, Paginated<List<TransactionDto>>> {
    override suspend fun execute(input: FriendTransactionQueryDto): Paginated<List<TransactionDto>> {
        return Paginated(transactionService.transactionsByFriendId(input.userId, input.friendId), null)
    }
}

@Service
class ActivityLogsQuery(
    private val transactionService: TransactionService,
) : Query<String, Paginated<List<ActivityLogsResponse>>> {
    override suspend fun execute(input: String): Paginated<List<ActivityLogsResponse>> {
        return Paginated(transactionService.transactionActivityLogs(userId = input).map {
            requireNotNull(it.transactionDto.updatedAt) { "Transaction updated at is required" }
            requireNotNull(it.transactionDto.transactionStreamId) { "Transaction stream id is required" }
            requireNotNull(it.transactionDto.recipientName) { "Recipient name is required" }
            ActivityLogsResponse(
                userUid = it.userUid,
                activityByUid = it.activityByUid,
                activityByName = it.activityByName,
                activityByPhoto = it.activityByPhoto,
                description = it.description,
                activityType = it.activityType,
                amount = it.amount,
                currency = it.currency,
                isOwed = it.isOwed,
                date = it.date,
                transactionResponse = TransactionResponse(
                    date = it.transactionDto.updatedAt,
                    transactionId = it.transactionDto.transactionStreamId,
                    friendName = it.transactionDto.recipientName,
                    amountResponse = AmountResponse(
                        amount = it.transactionDto.splitType.apply(it.transactionDto.originalAmount),
                        currency = it.transactionDto.currency.currencyCode,
                        isOwed = it.transactionDto.splitType.isOwed()
                    ),
                    history = it.transactionDto.history.groupBy { Pair(it.date.looseNanonSeconds(), it.userId) }.map {
                        ChangeSummaryResponse(
                            userId = it.key.second,
                            changes = it.value.map {
                                ChangeSummaryByUserResponse(
                                    oldValue = it.oldValue,
                                    newValue = it.newValue,
                                    type = it.type
                                )
                            }

                        )
                    },
                    description = it.transactionDto.description,
                    totalAmount = it.transactionDto.originalAmount,
                    splitType = it.transactionDto.splitType
                )
            )
        }, null)
    }
}