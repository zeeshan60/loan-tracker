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
) : Query<String, Paginated<List<ActivityLogResponse>>> {
    override suspend fun execute(input: String): Paginated<List<ActivityLogResponse>> {
        return Paginated(transactionService.transactionActivityLogs(userId = input).map { log ->
            requireNotNull(log.transactionDto.transactionStreamId) { "Transaction stream id is required" }
            requireNotNull(log.transactionDto.friendSummaryDto.friendId) { "Recipient name is required" }
            requireNotNull(log.transactionDto.createdBy) { "Created by is required" }
            requireNotNull(log.transactionDto.createdByName) { "Created by name is required" }
            requireNotNull(log.transactionDto.createdAt) { "Created at is required" }
            ActivityLogResponse(
                id = log.id,
                userUid = log.userUid,
                activityByName = log.activityByName,
                activityByPhoto = log.activityByPhoto,
                description = log.description,
                activityType = log.activityType,
                amount = log.amount,
                currency = log.currency,
                isOwed = log.isOwed,
                date = log.date,
                transactionResponse = TransactionResponse(
                    date = log.transactionDto.transactionDate,
                    transactionId = log.transactionDto.transactionStreamId,
                    friend = log.transactionDto.friendSummaryDto,
                    amountResponse = AmountResponse(
                        amount = log.transactionDto.splitType.apply(log.transactionDto.originalAmount),
                        currency = log.transactionDto.currency.currencyCode,
                        isOwed = log.transactionDto.splitType.isOwed()
                    ),
                    history = log.transactionDto.history.groupBy { Pair(it.date, it.changedBy) }
                        .map {
                            ChangeSummaryResponse(
                                changedBy = it.key.second,
                                changes = it.value,
                                changedByName = it.value.first().changedByName,
                                changedByPhoto = it.value.first().changedByPhoto
                            )
                        },
                    description = log.transactionDto.description,
                    totalAmount = log.transactionDto.originalAmount,
                    splitType = log.transactionDto.splitType,
                    createdAt = log.transactionDto.createdAt,
                    updatedAt = log.transactionDto.updatedAt,
                    createdBy = TransactionUserResponse(
                        id = log.transactionDto.createdBy,
                        name = log.transactionDto.createdByName
                    ),
                    updatedBy = log.transactionDto.updatedBy?.let {
                        TransactionUserResponse(
                            id = it,
                            name = log.transactionDto.updatedByName
                                ?: throw IllegalStateException("Updated by name is required")
                        )
                    },
                    deleted = log.transactionDto.deleted
                )
            )
        }, null)
    }
}