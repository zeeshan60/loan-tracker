package com.zeenom.loan_tracker.transactions

import com.zeenom.loan_tracker.friends.FriendEventRepository
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
class TransactionServiceTest(@Autowired private val transactionEventRepository: TransactionEventRepository) :
    TestPostgresConfig() {
    private val userEventHandler = mock<UserEventHandler>()
    private val friendEventHandler = mock<FriendsEventHandler>()
    private val friendEventRepository = mock<FriendEventRepository>()
    private val transactionEventHandler = TransactionEventHandler(
        transactionEventRepository = transactionEventRepository,
        friendEventRepository = friendEventRepository
    )
    private val transactionService = TransactionService(
        transactionEventHandler = transactionEventHandler,
        userEventHandler = userEventHandler,
        friendsEventHandler = friendEventHandler
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
                name = "Friend"
            )
        ).`when`(friendEventHandler).findFriendByUserIdAndFriendId("123", friendEventStreamId)
        val transactionDto = TransactionDto(
            currency = Currency.getInstance("USD"),
            recipientId = friendEventStreamId,
            description = "Test Transaction",
            splitType = SplitType.TheyOweYouAll,
            originalAmount = 100.0.toBigDecimal(),
            recipientName = "Friend",
            updatedAt = null
        )

        transactionService.addTransaction(
            userUid = "123",
            transactionDto = transactionDto
        )

        val transactionEvent = transactionEventRepository.findAll().toList()

        assertThat(transactionEvent).hasSize(1)
        assertThat(transactionEvent[0].userUid).isEqualTo("123")
        assertThat(transactionEvent[0].currency).isEqualTo(transactionDto.currency.toString())
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
        val (friendEventStreamId, myStreamId) = setupFriends()
        val transactionDto = TransactionDto(
            currency = Currency.getInstance("USD"),
            recipientId = friendEventStreamId,
            description = "Test Transaction",
            splitType = SplitType.TheyOweYouAll,
            originalAmount = 100.0.toBigDecimal(),
            recipientName = "Friend",
            updatedAt = null
        )

        transactionService.addTransaction(
            userUid = "123",
            transactionDto = transactionDto
        )

        val transactionEvent = transactionEventRepository.findAll().toList()

        assertThat(transactionEvent).hasSize(2)
        assertThat(transactionEvent[0].userUid).isEqualTo("123")
        assertThat(transactionEvent[0].currency).isEqualTo(transactionDto.currency.toString())
        assertThat(transactionEvent[0].transactionType).isEqualTo(TransactionType.CREDIT)
        assertThat(transactionEvent[0].recipientId).isEqualTo(friendEventStreamId)
        assertThat(transactionEvent[0].createdAt).isNotNull
        assertThat(transactionEvent[0].createdBy).isEqualTo("123")
        assertThat(transactionEvent[0].streamId).isNotNull()
        assertThat(transactionEvent[0].version).isEqualTo(1)
        assertThat(transactionEvent[0].eventType).isEqualTo(TransactionEventType.TRANSACTION_CREATED)


        assertThat(transactionEvent[1].userUid).isEqualTo("124")
        assertThat(transactionEvent[1].currency).isEqualTo(transactionDto.currency.toString())
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
            currency = Currency.getInstance("USD"),
            recipientId = friendEventStreamId,
            description = "Test Transaction",
            splitType = SplitType.TheyOweYouAll,
            originalAmount = 100.0.toBigDecimal(),
            recipientName = "Friend",
            updatedAt = null
        )

        assertThatThrownBy {
            runBlocking {
                transactionService.addTransaction(
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
            currency = Currency.getInstance("USD"),
            recipientId = friendEventStreamId,
            description = "Test Transaction",
            splitType = SplitType.TheyOweYouAll,
            originalAmount = 100.0.toBigDecimal(),
            recipientName = "Friend",
            updatedAt = null
        )

        assertThatThrownBy { runBlocking { transactionService.addTransaction("123", transactionDto) } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User with id 123 does not have friend with id $friendEventStreamId")
    }

    @Test
    fun `given existing transaction updates transaction successfully`(): Unit = runBlocking {
        val (friendEventStreamId, myStreamId) = setupFriends()
        val transactionDto = TransactionDto(
            currency = Currency.getInstance("USD"),
            recipientId = friendEventStreamId,
            description = "Test Transaction",
            splitType = SplitType.TheyOweYouAll,
            originalAmount = 100.0.toBigDecimal(),
            recipientName = "Friend",
            updatedAt = null
        )

        transactionService.addTransaction(
            userUid = "123",
            transactionDto = transactionDto
        )

        val events = transactionEventRepository.findAll().toList()
        assertThat(events).hasSize(2)
        val transactionStreamId = events[0].streamId

        transactionService.updateTransaction(
            userUid = "123",
            transactionDto = transactionDto.copy(
                originalAmount = 200.0.toBigDecimal(),
                transactionStreamId = transactionStreamId
            )
        )

        val transactionEvent = transactionEventRepository.findAll().toList()

        assertThat(transactionEvent).hasSize(4)
        assertAllEventsAreProperlyCreated(transactionEvent, transactionDto, friendEventStreamId, myStreamId)

        val resolvedEvents = listOf(
            transactionEventHandler.read("123", transactionStreamId)!!, transactionEventHandler.read(
                "124",
                transactionStreamId
            )!!
        )
        assertThat(resolvedEvents).hasSize(2)
        assertThat(resolvedEvents[0].userUid).isEqualTo("123")
        assertThat(resolvedEvents[0].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(resolvedEvents[0].currency).isEqualTo(transactionDto.currency.toString())
        assertThat(resolvedEvents[0].splitType).isEqualTo(SplitType.TheyOweYouAll)
        assertThat(resolvedEvents[0].recipientId).isEqualTo(friendEventStreamId)
        assertThat(resolvedEvents[0].createdAt).isNotNull
        assertThat(resolvedEvents[0].createdBy).isEqualTo("123")
        assertThat(resolvedEvents[0].streamId).isNotNull()
        assertThat(resolvedEvents[0].version).isEqualTo(2)


        assertThat(resolvedEvents[1].userUid).isEqualTo("124")
        assertThat(resolvedEvents[1].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(resolvedEvents[1].currency).isEqualTo(transactionDto.currency.toString())
        assertThat(resolvedEvents[1].splitType).isEqualTo(SplitType.YouOweThemAll)
        assertThat(resolvedEvents[1].recipientId).isEqualTo(myStreamId)
        assertThat(resolvedEvents[1].createdAt).isNotNull
        assertThat(resolvedEvents[1].createdBy).isEqualTo("123")
        assertThat(resolvedEvents[1].streamId).isNotNull()
        assertThat(resolvedEvents[1].version).isEqualTo(2)
    }

    @Test
    fun `given existing transaction deletes transaction successfully`(): Unit = runBlocking {
        val (friendEventStreamId, myStreamId) = setupFriends()
        val transactionDto = TransactionDto(
            currency = Currency.getInstance("USD"),
            recipientId = friendEventStreamId,
            description = "Test Transaction",
            splitType = SplitType.TheyOweYouAll,
            originalAmount = 100.0.toBigDecimal(),
            recipientName = "Friend",
            updatedAt = null
        )

        transactionService.addTransaction(
            userUid = "123",
            transactionDto = transactionDto
        )

        val events = transactionEventRepository.findAll().toList()
        assertThat(events).hasSize(2)
        val transactionStreamId = events[0].streamId

        transactionService.updateTransaction(
            userUid = "123",
            transactionDto = transactionDto.copy(
                originalAmount = 200.0.toBigDecimal(),
                transactionStreamId = transactionStreamId
            )
        )

        transactionService.deleteTransaction(
            userUid = "123",
            transactionStreamId = transactionStreamId
        )

        val transactionEvent = transactionEventRepository.findAll().toList()

        assertThat(transactionEvent).hasSize(6)
        assertAllEventsAreProperlyCreated(transactionEvent, transactionDto, friendEventStreamId, myStreamId)

        val resolvedEvents = listOf(
            transactionEventHandler.read("123", transactionStreamId)!!, transactionEventHandler.read(
                "124",
                transactionStreamId
            )!!
        )
        assertThat(resolvedEvents).hasSize(2)
        assertThat(resolvedEvents[0].userUid).isEqualTo("123")
        assertThat(resolvedEvents[0].currency).isEqualTo(transactionDto.currency.toString())
        assertThat(resolvedEvents[0].splitType).isEqualTo(SplitType.TheyOweYouAll)
        assertThat(resolvedEvents[0].recipientId).isEqualTo(friendEventStreamId)
        assertThat(resolvedEvents[0].createdAt).isNotNull
        assertThat(resolvedEvents[0].createdBy).isEqualTo("123")
        assertThat(resolvedEvents[0].streamId).isNotNull()
        assertThat(resolvedEvents[0].version).isEqualTo(3)
        assertThat(resolvedEvents[0].deleted).isTrue()


        assertThat(resolvedEvents[1].userUid).isEqualTo("124")
        assertThat(resolvedEvents[1].currency).isEqualTo(transactionDto.currency.toString())
        assertThat(resolvedEvents[1].splitType).isEqualTo(SplitType.YouOweThemAll)
        assertThat(resolvedEvents[1].recipientId).isEqualTo(myStreamId)
        assertThat(resolvedEvents[1].createdAt).isNotNull
        assertThat(resolvedEvents[1].createdBy).isEqualTo("123")
        assertThat(resolvedEvents[1].streamId).isNotNull()
        assertThat(resolvedEvents[1].version).isEqualTo(3)
        assertThat(resolvedEvents[1].deleted).isTrue()
    }

    private suspend fun setupFriends(): Pair<UUID, UUID> {
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
                name = "Friend"
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
        return Pair(friendEventStreamId, myStreamId)
    }

    private fun assertAllEventsAreProperlyCreated(
        transactionEvent: List<TransactionEvent>,
        transactionDto: TransactionDto,
        friendEventStreamId: UUID?,
        myStreamId: UUID?,
    ) {
        assertThat(transactionEvent[0].userUid).isEqualTo("123")
        assertThat(transactionEvent[0].amount).isEqualTo(100.0.toBigDecimal())
        assertThat(transactionEvent[0].currency).isEqualTo(transactionDto.currency.toString())
        assertThat(transactionEvent[0].transactionType).isEqualTo(TransactionType.CREDIT)
        assertThat(transactionEvent[0].recipientId).isEqualTo(friendEventStreamId)
        assertThat(transactionEvent[0].createdAt).isNotNull
        assertThat(transactionEvent[0].createdBy).isEqualTo("123")
        assertThat(transactionEvent[0].streamId).isNotNull()
        assertThat(transactionEvent[0].version).isEqualTo(1)
        assertThat(transactionEvent[0].eventType).isEqualTo(TransactionEventType.TRANSACTION_CREATED)


        assertThat(transactionEvent[1].userUid).isEqualTo("124")
        assertThat(transactionEvent[1].amount).isEqualTo(100.0.toBigDecimal())
        assertThat(transactionEvent[1].currency).isEqualTo(transactionDto.currency.toString())
        assertThat(transactionEvent[1].transactionType).isEqualTo(TransactionType.DEBIT)
        assertThat(transactionEvent[1].recipientId).isEqualTo(myStreamId)
        assertThat(transactionEvent[1].createdAt).isNotNull
        assertThat(transactionEvent[1].createdBy).isEqualTo("123")
        assertThat(transactionEvent[1].streamId).isNotNull()
        assertThat(transactionEvent[1].version).isEqualTo(1)
        assertThat(transactionEvent[1].eventType).isEqualTo(TransactionEventType.TRANSACTION_CREATED)


        assertThat(transactionEvent[2].userUid).isEqualTo("123")
        assertThat(transactionEvent[2].amount).isNull()
        assertThat(transactionEvent[2].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(transactionEvent[2].currency).isNull()
        assertThat(transactionEvent[2].transactionType).isNull()
        assertThat(transactionEvent[2].recipientId).isNotNull()
        assertThat(transactionEvent[2].createdAt).isNotNull
        assertThat(transactionEvent[2].createdBy).isEqualTo("123")
        assertThat(transactionEvent[2].streamId).isNotNull()
        assertThat(transactionEvent[2].version).isEqualTo(2)


        assertThat(transactionEvent[3].userUid).isEqualTo("124")
        assertThat(transactionEvent[2].amount).isNull()
        assertThat(transactionEvent[2].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(transactionEvent[2].currency).isNull()
        assertThat(transactionEvent[2].transactionType).isNull()
        assertThat(transactionEvent[2].recipientId).isNotNull()
        assertThat(transactionEvent[2].createdAt).isNotNull
        assertThat(transactionEvent[2].createdBy).isEqualTo("123")
        assertThat(transactionEvent[2].streamId).isNotNull()
        assertThat(transactionEvent[2].version).isEqualTo(2)
    }
}