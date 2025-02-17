package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.FriendsEventHandler
import com.zeenom.loan_tracker.friends.TestPostgresConfig
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventHandler
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import java.util.*

@DataR2dbcTest
class TransactionEventHandlerTest(@Autowired private val transactionEventRepository: TransactionEventRepository) :
    TestPostgresConfig() {
    private val userEventHandler = mock<UserEventHandler>()
    private val friendEventHandler = mock<FriendsEventHandler>()

    private val transactionEventHandler = TransactionEventHandler(
        transactionEventRepository = transactionEventRepository,
        userEventHandler = userEventHandler,
        friendEventHandler = friendEventHandler
    )

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        transactionEventRepository.deleteAll()
    }

    @Test
    fun `given user and friend save transaction successfully`(): Unit = runBlocking {
        doReturn(
            UserDto(
                uid = "123",
                email = "user@gmail.com",
                phoneNumber = "+923001234567",
                displayName = "Test User",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventHandler).findUserById("123")

        val friendEventStreamId = UUID.randomUUID()
        doReturn(true).`when`(friendEventHandler).friendExistsByUserIdAndFriendId("123", friendEventStreamId)
        val transactionDto = TransactionDto(
            amount = AmountDto(
                currency = Currency.getInstance("USD"),
                amount = 100.0.toBigDecimal(),
                isOwed = true
            ),
            recipientId = friendEventStreamId,
        )

        transactionEventHandler.saveEvent(
            userUid = "123",
            transactionDto = transactionDto
        )

        val transactionEvent = transactionEventRepository.findAll().toList()

        assertThat(transactionEvent).hasSize(1)
        assertThat(transactionEvent[0].userUid).isEqualTo("123")
        assertThat(transactionEvent[0].amount).isEqualTo(transactionDto.amount.amount)
        assertThat(transactionEvent[0].currency).isEqualTo(transactionDto.amount.currency.toString())
        assertThat(transactionEvent[0].transactionType).isEqualTo(TransactionType.CREDIT)
        assertThat(transactionEvent[0].recipientId).isEqualTo(transactionDto.recipientId)
        assertThat(transactionEvent[0].createdAt).isNotNull
        assertThat(transactionEvent[0].createdBy).isEqualTo("123")
        assertThat(transactionEvent[0].streamId).isNotNull()
        assertThat(transactionEvent[0].version).isEqualTo(1)
        assertThat(transactionEvent[0].eventType).isEqualTo(TransactionEventType.TRANSACTION_CREATED)
    }

    @Test
    fun `save transaction should fail when user is not found`(): Unit = runBlocking {
        doReturn(null).`when`(userEventHandler).findUserById("1234")

        val friendEventStreamId = UUID.randomUUID()
        doReturn(true).`when`(friendEventHandler).friendExistsByUserIdAndFriendId("1234", friendEventStreamId)
        val transactionDto = TransactionDto(
            amount = AmountDto(
                currency = Currency.getInstance("USD"),
                amount = 100.0.toBigDecimal(),
                isOwed = true
            ),
            recipientId = friendEventStreamId,
        )

        assertThatThrownBy {
            runBlocking {
                transactionEventHandler.saveEvent(
                    userUid = "1234",
                    transactionDto = transactionDto
                )
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User with id 1234 does not exist")
    }

    @Test
    fun `save transaction should fail if friend is not available`(): Unit = runBlocking {
        doReturn(
            UserDto(
                uid = "123",
                email = "user@gmail.com",
                phoneNumber = "+923001234567",
                displayName = "Test User",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventHandler).findUserById("123")
        val friendEventStreamId = UUID.randomUUID()
        doReturn(false).`when`(friendEventHandler).friendExistsByUserIdAndFriendId("123", friendEventStreamId)

        val transactionDto = TransactionDto(
            amount = AmountDto(
                currency = Currency.getInstance("USD"),
                amount = 100.0.toBigDecimal(),
                isOwed = true
            ),
            recipientId = friendEventStreamId,
        )

        assertThatThrownBy { runBlocking { transactionEventHandler.saveEvent("123", transactionDto) } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User with id 123 does not have friend with id $friendEventStreamId")
    }
}