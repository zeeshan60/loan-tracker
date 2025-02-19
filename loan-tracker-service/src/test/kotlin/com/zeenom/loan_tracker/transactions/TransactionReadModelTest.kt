package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.FriendEvent
import com.zeenom.loan_tracker.friends.FriendEventType
import com.zeenom.loan_tracker.friends.FriendId
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Instant
import java.util.*

class TransactionReadModelTest {

    @Test
    fun `given a friend and some transactions return balance successfully`(): Unit = runBlocking {
        val friendStreamId = UUID.randomUUID()
        val transactionReadModel = TransactionReadModel(
            mock {
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
            },
            friendEventRepository = mock {
                on {
                    runBlocking {
                        findByUserUidAndStreamId("123", friendStreamId)
                    }
                } doReturn FriendEvent(
                    friendDisplayName = "John Doe",
                    userUid = "124",
                    friendEmail = "John@gmail.com",
                    friendPhoneNumber = "+923001234567",
                    createdAt = Instant.now(),
                    streamId = friendStreamId,
                    version = 1,
                    eventType = FriendEventType.FRIEND_CREATED,
                )
            }
        )

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
        },
            friendEventRepository = mock {
                on {
                    runBlocking {
                        findByUserUidAndStreamId("123", friendStreamId1)
                    }
                } doReturn FriendEvent(
                    friendDisplayName = "John Doe",
                    userUid = "123",
                    friendEmail = "John@gmail.com",
                    friendPhoneNumber = "+923001234567",
                    createdAt = Instant.now(),
                    streamId = friendStreamId1,
                    version = 1,
                    eventType = FriendEventType.FRIEND_CREATED,
                )
                on {
                    runBlocking {
                        findByUserUidAndStreamId("123", friendStreamId2)
                    }
                } doReturn FriendEvent(
                    friendDisplayName = "John Doe 2",
                    userUid = "123",
                    friendEmail = "John2@gmail.com",
                    friendPhoneNumber = "+923001234568",
                    createdAt = Instant.now(),
                    streamId = friendStreamId2,
                    version = 1,
                    eventType = FriendEventType.FRIEND_CREATED,
                )
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
        val friendStreamId = UUID.randomUUID()
        val transactionReadModel = TransactionReadModel(
            mock {
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
                        eventType = TransactionEventType.TRANSACTION_CREATED,
                        description = "some description",
                        splitType = SplitType.TheyOweYouAll,
                        totalAmount = 200.0.toBigDecimal()
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
                        eventType = TransactionEventType.TRANSACTION_UPDATED,
                        description = "some description",
                        splitType = SplitType.YouOweThemAll,
                        totalAmount = 200.0.toBigDecimal()
                    )
                ).asFlow()
            },
            friendEventRepository = mock {
                on {
                    runBlocking {
                        findByUserUidAndStreamId("123", friendStreamId)
                    }
                } doReturn FriendEvent(
                    friendDisplayName = "John Doe",
                    userUid = "124",
                    friendEmail = "John@gmail.com",
                    friendPhoneNumber = "+923001234567",
                    createdAt = Instant.now(),
                    streamId = friendStreamId,
                    version = 1,
                    eventType = FriendEventType.FRIEND_CREATED,
                )
            }
        )

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
                eventType = TransactionEventType.TRANSACTION_CREATED,
                description = "some description",
                splitType = SplitType.TheyOweYouAll,
                totalAmount = 100.0.toBigDecimal()
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
                eventType = TransactionEventType.TRANSACTION_CREATED,
                description = "some description",
                splitType = SplitType.TheyOweYouAll,
                totalAmount = 200.0.toBigDecimal()
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
                eventType = TransactionEventType.TRANSACTION_UPDATED,
                description = "some description",
                splitType = SplitType.YouOweThemAll,
                totalAmount = 200.0.toBigDecimal()
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
                eventType = TransactionEventType.TRANSACTION_CREATED,
                description = "some description",
                splitType = SplitType.YouOweThemAll,
                totalAmount = 150.0.toBigDecimal()
            )
        )
    }
}