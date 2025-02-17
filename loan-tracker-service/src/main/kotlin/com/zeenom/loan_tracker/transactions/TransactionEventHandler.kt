package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.users.UserEventHandler
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class TransactionEventHandler(
    private val transactionEventRepository: TransactionEventRepository,
    private val userEventHandler: UserEventHandler,
    private val friendEventHandler: FriendsEventHandler,
) {
    suspend fun saveEvent(userUid: String, transactionDto: TransactionDto) {
        userEventHandler.findUserById(userUid)
            ?: throw IllegalArgumentException("User with id $userUid does not exist")
        if (!friendEventHandler.friendExistsByUserIdAndFriendId(userUid, transactionDto.recipientId))
            throw IllegalArgumentException("User with id $userUid does not have friend with id ${transactionDto.recipientId}")
        transactionEventRepository.save(
            TransactionEvent(
                userUid = userUid,
                amount = transactionDto.amount.amount,
                currency = transactionDto.amount.currency.toString(),
                transactionType = if (transactionDto.amount.isOwed) TransactionType.CREDIT else TransactionType.DEBIT,
                recipientId = transactionDto.recipientId,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = TransactionEventType.TRANSACTION_CREATED,
                createdBy = userUid
            )
        )
    }
}