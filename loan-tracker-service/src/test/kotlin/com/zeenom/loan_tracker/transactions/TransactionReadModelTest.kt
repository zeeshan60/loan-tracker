package com.zeenom.loan_tracker.transactions

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.*

class TransactionReadModelTest {

    @Test
    fun `given a friend and some transactions return balance successfully`(): Unit = runBlocking {
        val friendStreamId = UUID.randomUUID()
        val transactionReadModel = TransactionReadModel(mock {
            on {
                runBlocking {
                    findAllByUserUidAndRecipientIdIn(
                        "123",
                        listOf(friendStreamId)
                    )
                }
            } doReturn sampleTransactions(
                friendStreamId
            ).asFlow()
        })

        val balances = transactionReadModel.balancesOfFriends("123", listOf(friendStreamId))
        assertThat(balances).hasSize(1)
        assertThat(balances[friendStreamId]?.amount).isEqualTo(150.0.toBigDecimal())
        assertThat(balances[friendStreamId]?.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balances[friendStreamId]?.isOwed).isFalse()
    }

    @Test
    fun `given multiple friends return their respective balances`(): Unit = runBlocking {
        val friendStreamId1 = UUID.randomUUID()
        val friendStreamId2 = UUID.randomUUID()
        val transactionReadModel = TransactionReadModel(mock {
            on {
                runBlocking {
                    findAllByUserUidAndRecipientIdIn(
                        "123",
                        listOf(friendStreamId1, friendStreamId2)
                    )
                }
            } doReturn sampleTransactions(
                friendStreamId1
            ).plus(sampleTransactions(friendStreamId2)).asFlow()
        })

        val balances = transactionReadModel.balancesOfFriends("123", listOf(friendStreamId1, friendStreamId2))
        assertThat(balances).hasSize(2)
        assertThat(balances[friendStreamId1]?.amount).isEqualTo(150.0.toBigDecimal())
        assertThat(balances[friendStreamId1]?.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balances[friendStreamId1]?.isOwed).isFalse()

        assertThat(balances[friendStreamId2]?.amount).isEqualTo(150.0.toBigDecimal())
        assertThat(balances[friendStreamId2]?.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balances[friendStreamId2]?.isOwed).isFalse()
    }

    @Test
    fun `read single transaction stream successfully`(): Unit = runBlocking {
        val transactionStreamId = UUID.randomUUID()
        val transactionReadModel = TransactionReadModel(mock {
            val friendStreamId = UUID.randomUUID()
            on {
                runBlocking {
                    findAllByUserUidAndStreamId(
                        "123",
                        transactionStreamId
                    )
                }
            } doReturn listOf(
                TransactionEvent(
                    userUid = "123",
                    amount = 200.0.toBigDecimal(),
                    currency = "USD",
                    transactionType = TransactionType.CREDIT,
                    recipientId = friendStreamId,
                    createdAt = Date().toInstant(),
                    createdBy = "123",
                    streamId = transactionStreamId,
                    version = 1,
                    eventType = TransactionEventType.TRANSACTION_CREATED
                ),
                TransactionEvent(
                    userUid = "123",
                    amount = 100.0.toBigDecimal(),
                    currency = "USD",
                    transactionType = TransactionType.DEBIT,
                    recipientId = friendStreamId,
                    createdAt = Date().toInstant(),
                    createdBy = "123",
                    streamId = transactionStreamId,
                    version = 2,
                    eventType = TransactionEventType.TRANSACTION_UPDATED
                )
            ).asFlow()
        })

        val transaction = transactionReadModel.read("123", transactionStreamId)
        assertThat(transaction).isNotNull
        assertThat(transaction?.amount).isEqualTo(100.0.toBigDecimal())
        assertThat(transaction?.currency).isEqualTo("USD")
        assertThat(transaction?.transactionType).isEqualTo(TransactionType.DEBIT)
    }

    private fun sampleTransactions(friendStreamId: UUID): List<TransactionEvent> {
        val transactionStreamId = UUID.randomUUID()
        return listOf(
            TransactionEvent(
                userUid = "123",
                amount = 100.0.toBigDecimal(),
                currency = "USD",
                transactionType = TransactionType.CREDIT,
                recipientId = friendStreamId,
                createdAt = Date().toInstant(),
                createdBy = "123",
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = TransactionEventType.TRANSACTION_CREATED
            ),
            TransactionEvent(
                userUid = "123",
                amount = 200.0.toBigDecimal(),
                currency = "USD",
                transactionType = TransactionType.CREDIT,
                recipientId = friendStreamId,
                createdAt = Date().toInstant(),
                createdBy = "123",
                streamId = transactionStreamId,
                version = 1,
                eventType = TransactionEventType.TRANSACTION_CREATED
            ),
            TransactionEvent(
                userUid = "123",
                amount = 100.0.toBigDecimal(),
                currency = "USD",
                transactionType = TransactionType.DEBIT,
                recipientId = friendStreamId,
                createdAt = Date().toInstant(),
                createdBy = "123",
                streamId = transactionStreamId,
                version = 2,
                eventType = TransactionEventType.TRANSACTION_UPDATED
            ),
            TransactionEvent(
                userUid = "123",
                amount = 150.0.toBigDecimal(),
                currency = "USD",
                transactionType = TransactionType.DEBIT,
                recipientId = friendStreamId,
                createdAt = Date().toInstant(),
                createdBy = "123",
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = TransactionEventType.TRANSACTION_CREATED
            )
        )
    }
}