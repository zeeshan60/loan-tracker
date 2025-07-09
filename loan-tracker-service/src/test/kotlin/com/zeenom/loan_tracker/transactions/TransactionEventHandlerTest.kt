package com.zeenom.loan_tracker.transactions

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Instant
import java.util.*

class TransactionEventHandlerTest {

    val userId = UUID.randomUUID()

    @Test
    fun `given a friend and some transactions return balance successfully`(): Unit = runBlocking {
        val friendStreamId = UUID.randomUUID()
        val transactionEventHandler = TransactionEventHandler(
            transactionEventRepository = mock {
                on {
                    runBlocking {
                        findAllByUserUidAndRecipientIdIn(
                            userId,
                            listOf(friendStreamId)
                        )
                    }
                } doReturn sampleTransactions(
                    friendStreamId
                ).asFlow()
            }
        )

        val balances = transactionEventHandler.balancesOfFriendsByCurrency(userId, listOf(friendStreamId))
        assertThat(balances).hasSize(1)
        assertThat(balances[friendStreamId]?.get("USD")?.amount).isEqualTo(250.0.toBigDecimal())
        assertThat(balances[friendStreamId]?.get("USD")?.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balances[friendStreamId]?.get("USD")?.isOwed).isFalse()
    }

    @Test
    fun `given multiple friends return their respective balances`(): Unit = runBlocking {
        val friendStreamId1 = UUID.randomUUID()
        val friendStreamId2 = UUID.randomUUID()
        val transactionEventHandler = TransactionEventHandler(
            transactionEventRepository = mock {
                on {
                    runBlocking {
                        findAllByUserUidAndRecipientIdIn(
                            userId,
                            listOf(friendStreamId1, friendStreamId2)
                        )
                    }
                } doReturn sampleTransactions(
                    friendStreamId1
                ).plus(sampleTransactions(friendStreamId2)).asFlow()
            }
        )

        val balances =
            transactionEventHandler.balancesOfFriendsByCurrency(userId, listOf(friendStreamId1, friendStreamId2))
        assertThat(balances).hasSize(2)
        assertThat(balances[friendStreamId1]?.get("USD")?.amount).isEqualTo(250.0.toBigDecimal())
        assertThat(balances[friendStreamId1]?.get("USD")?.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balances[friendStreamId1]?.get("USD")?.isOwed).isFalse()

        assertThat(balances[friendStreamId2]?.get("USD")?.amount).isEqualTo(250.0.toBigDecimal())
        assertThat(balances[friendStreamId2]?.get("USD")?.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(balances[friendStreamId2]?.get("USD")?.isOwed).isFalse()
    }

    @Test
    fun `read single transaction stream successfully`(): Unit = runBlocking {
        val transactionStreamId = UUID.randomUUID()
        val friendStreamId = UUID.randomUUID()
        val transactionEventHandler = TransactionEventHandler(
            mock {
                on {
                    runBlocking {
                        findAllByUserUidAndStreamId(
                            userId,
                            transactionStreamId
                        )
                    }
                } doReturn listOf(
                    TransactionEvent(
                        userUid = userId,
                        currency = "USD",
                        recipientId = friendStreamId,
                        createdAt = Instant.now(),
                        transactionDate = Instant.now(),
                        createdBy = userId,
                        streamId = transactionStreamId,
                        version = 1,
                        eventType = TransactionEventType.TRANSACTION_CREATED,
                        description = "some description",
                        splitType = SplitType.TheyOweYouAll,
                        totalAmount = 200.0.toBigDecimal()
                    ),
                    TransactionEvent(
                        userUid = userId,
                        currency = null,
                        recipientId = friendStreamId,
                        createdAt = Instant.now(),
                        transactionDate = Instant.now(),
                        createdBy = userId,
                        streamId = transactionStreamId,
                        version = 2,
                        eventType = TransactionEventType.SPLIT_TYPE_CHANGED,
                        description = null,
                        splitType = SplitType.YouOweThemAll,
                        totalAmount = null
                    ),
                    TransactionEvent(
                        userUid = userId,
                        currency = null,
                        recipientId = friendStreamId,
                        createdAt = Instant.now(),
                        transactionDate = Instant.now(),
                        createdBy = userId,
                        streamId = transactionStreamId,
                        version = 3,
                        eventType = TransactionEventType.TOTAL_AMOUNT_CHANGED,
                        description = null,
                        splitType = null,
                        totalAmount = 100.0.toBigDecimal()
                    )
                ).asFlow()
            }
        )

        val transaction = transactionEventHandler.read(userId, transactionStreamId)
        assertThat(transaction).isNotNull
        assertThat(transaction?.totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(transaction?.currency).isEqualTo("USD")
        assertThat(transaction?.splitType).isEqualTo(SplitType.YouOweThemAll)
    }

    private fun sampleTransactions(friendStreamId: UUID): List<TransactionEvent> {
        val transactionStreamId = UUID.randomUUID()
        return listOf(
            TransactionEvent(
                userUid = userId,
                currency = "USD",
                recipientId = friendStreamId,
                createdAt = Instant.now(),
                transactionDate = Instant.now(),
                createdBy = userId,
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = TransactionEventType.TRANSACTION_CREATED,
                description = "some description",
                splitType = SplitType.TheyOweYouAll,
                totalAmount = 100.0.toBigDecimal()
            ),
            TransactionEvent(
                userUid = userId,
                currency = "USD",
                recipientId = friendStreamId,
                createdAt = Instant.now(),
                transactionDate = Instant.now(),
                createdBy = userId,
                streamId = transactionStreamId,
                version = 1,
                eventType = TransactionEventType.TRANSACTION_CREATED,
                description = "some description",
                splitType = SplitType.TheyOweYouAll,
                totalAmount = 200.0.toBigDecimal()
            ),
            TransactionEvent(
                userUid = userId,
                currency = null,
                recipientId = friendStreamId,
                createdAt = Instant.now(),
                transactionDate = Instant.now(),
                createdBy = userId,
                streamId = transactionStreamId,
                version = 2,
                eventType = TransactionEventType.SPLIT_TYPE_CHANGED,
                description = null,
                splitType = SplitType.YouOweThemAll,
                totalAmount = null
            ),
            TransactionEvent(
                userUid = userId,
                currency = "USD",
                recipientId = friendStreamId,
                createdAt = Instant.now(),
                transactionDate = Instant.now(),
                createdBy = userId,
                streamId = UUID.randomUUID(),
                version = 1,
                eventType = TransactionEventType.TRANSACTION_CREATED,
                description = "some description",
                splitType = SplitType.YouOweThemAll,
                totalAmount = 150.0.toBigDecimal()
            )
        )
    }
}