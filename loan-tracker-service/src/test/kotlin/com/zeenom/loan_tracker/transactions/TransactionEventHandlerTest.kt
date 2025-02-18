package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.FriendId
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
import org.mockito.kotlin.whenever
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
    fun `given user and friend save only one transaction when friend is not a user`(): Unit = runBlocking {
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
        doReturn(
            FriendId(
                email = "friend@gmail.com",
                phoneNumber = "+923001234568",
            )
        ).`when`(friendEventHandler).findFriendByUserIdAndFriendId("123", friendEventStreamId)
        val transactionDto = TransactionDto(
            amount = AmountDto(
                currency = Currency.getInstance("USD"),
                amount = 100.0.toBigDecimal(),
                isOwed = true
            ),
            recipientId = friendEventStreamId,
        )

        transactionEventHandler.addTransaction(
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
    fun `given user and friend save two transactions when friend is a user`(): Unit = runBlocking {
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
        whenever(friendEventHandler.findFriendByUserIdAndFriendId("123", friendEventStreamId)).thenReturn(
            FriendId(
                email = "friend@gmail.com",
                phoneNumber = "+923001234568",
            )
        )
        val myStreamId = UUID.randomUUID()
        doReturn(myStreamId).`when`(friendEventHandler).findFriendStreamIdByEmailOrPhoneNumber(
            "124",
            "user@gmail.com",
            "+923001234567"
        )
        doReturn(
            UserDto(
                uid = "124",
                email = "friend@gmail.com",
                phoneNumber = "+923001234568",
                displayName = "Friend",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventHandler).findUserByEmailOrPhoneNumber("friend@gmail.com", "+923001234568")
        val transactionDto = TransactionDto(
            amount = AmountDto(
                currency = Currency.getInstance("USD"),
                amount = 100.0.toBigDecimal(),
                isOwed = true
            ),
            recipientId = friendEventStreamId,
        )

        transactionEventHandler.addTransaction(
            userUid = "123",
            transactionDto = transactionDto
        )

        val transactionEvent = transactionEventRepository.findAll().toList()

        assertThat(transactionEvent).hasSize(2)
        assertThat(transactionEvent[0].userUid).isEqualTo("123")
        assertThat(transactionEvent[0].amount).isEqualTo(transactionDto.amount.amount)
        assertThat(transactionEvent[0].currency).isEqualTo(transactionDto.amount.currency.toString())
        assertThat(transactionEvent[0].transactionType).isEqualTo(TransactionType.CREDIT)
        assertThat(transactionEvent[0].recipientId).isEqualTo(friendEventStreamId)
        assertThat(transactionEvent[0].createdAt).isNotNull
        assertThat(transactionEvent[0].createdBy).isEqualTo("123")
        assertThat(transactionEvent[0].streamId).isNotNull()
        assertThat(transactionEvent[0].version).isEqualTo(1)
        assertThat(transactionEvent[0].eventType).isEqualTo(TransactionEventType.TRANSACTION_CREATED)


        assertThat(transactionEvent[1].userUid).isEqualTo("124")
        assertThat(transactionEvent[1].amount).isEqualTo(transactionDto.amount.amount)
        assertThat(transactionEvent[1].currency).isEqualTo(transactionDto.amount.currency.toString())
        assertThat(transactionEvent[1].transactionType).isEqualTo(TransactionType.DEBIT)
        assertThat(transactionEvent[1].recipientId).isEqualTo(myStreamId)
        assertThat(transactionEvent[1].createdAt).isNotNull
        assertThat(transactionEvent[1].createdBy).isEqualTo("123")
        assertThat(transactionEvent[1].streamId).isNotNull()
        assertThat(transactionEvent[1].version).isEqualTo(1)
        assertThat(transactionEvent[1].eventType).isEqualTo(TransactionEventType.TRANSACTION_CREATED)
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
                transactionEventHandler.addTransaction(
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

        assertThatThrownBy { runBlocking { transactionEventHandler.addTransaction("123", transactionDto) } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User with id 123 does not have friend with id $friendEventStreamId")
    }
}