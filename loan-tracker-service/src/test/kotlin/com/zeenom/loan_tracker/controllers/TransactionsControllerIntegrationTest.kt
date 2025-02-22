package com.zeenom.loan_tracker.controllers

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.transactions.*
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.UUID

/**
 * Minimal Test case script
 * 1. Login as a new user during setup
 * 2. Add a friend
 * 3. Add a transaction
 * 4. Get all transactions
 * 5. Create friend as a user
 * 6. Get all transactions by friend user
 */
class TransactionsControllerIntegrationTest_MinimalScript(@LocalServerPort private val port: Int) :
    BaseIntegration(port) {

    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    @Autowired
    private lateinit var transactionEventRepository: TransactionEventRepository

    private lateinit var zeeToken: String
    private lateinit var johnToken: String
    private var zeeDto = UserDto(
        uid = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        emailVerified = true
    )
    private var johnDto = UserDto(
        uid = "124",
        email = "john@gmail.com",
        phoneNumber = "+923001234568",
        displayName = "John Doe",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
        emailVerified = true
    )
    private lateinit var johnFriendId: UUID

    private lateinit var zeeFriendId: UUID

    @BeforeAll
    fun setupBeforeAll(): Unit = runBlocking {

        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        transactionEventRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        addFriend(zeeToken, "john")
        johnFriendId = queryFriend(
            token = zeeToken
        ).data.friends.first().friendId
    }

    @Order(1)
    @Test
    fun `add a transaction`() {

        webTestClient.post()
            .uri("/api/v1/transactions/add")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                TransactionRequest(
                    amount = 100.0.toBigDecimal(),
                    currency = "USD",
                    type = SplitType.YouPaidSplitEqually,
                    recipientId = johnFriendId,
                    description = "Sample transaction"
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.message").isEqualTo("Transaction added successfully")
    }

    private lateinit var transactionId: UUID

    @Order(2)
    @Test
    fun `get all transactions`() {
        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend?friendId=$johnFriendId")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<Paginated<TransactionsResponse>>() {})
            }

        assertThat(result.data.transactions).hasSize(1)
        assertThat(result.data.transactions[0].friendName).isEqualTo("john")
        assertThat(result.data.transactions[0].amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.data.transactions[0].amountResponse.currency).isEqualTo("USD")
        assertThat(result.data.transactions[0].amountResponse.isOwed).isTrue()
        assertThat(result.data.transactions[0].totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.data.transactions[0].transactionId).isNotNull()
        assertThat(result.data.transactions[0].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        assertThat(result.data.transactions[0].description).isEqualTo("Sample transaction")
        transactionId = result.data.transactions[0].transactionId
    }

    @Order(3)
    @Test
    fun `login with friend as user`() {
        johnToken = loginUser(johnDto).token
        val queryFriend = queryFriend(johnToken)
        zeeFriendId = queryFriend.data.friends.first().friendId
        assertThat(zeeFriendId).isNotNull()
        val loanAmount = queryFriend.data.friends.first().loanAmount
        assertThat(loanAmount).isNotNull
        assertThat(loanAmount!!.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(loanAmount.isOwed).isFalse()
    }

    @Order(4)
    @Test
    fun `get all transactions as john`() {
        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend?friendId=$zeeFriendId")
            .header("Authorization", "Bearer $johnToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<Paginated<TransactionsResponse>>() {})
            }

        assertThat(result.data.transactions).hasSize(1)
        assertThat(result.data.transactions[0].friendName).isEqualTo("Zeeshan Tufail")
        assertThat(result.data.transactions[0].amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.data.transactions[0].amountResponse.currency).isEqualTo("USD")
        assertThat(result.data.transactions[0].amountResponse.isOwed).isFalse()
        assertThat(result.data.transactions[0].totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.data.transactions[0].transactionId).isNotNull()
        assertThat(result.data.transactions[0].splitType).isEqualTo(SplitType.TheyPaidSplitEqually)
        assertThat(result.data.transactions[0].description).isEqualTo("Sample transaction")
        assertThat(result.data.transactions[0].history).isEmpty()
    }

    @Order(5)
    @Test
    fun `update transaction as zee`() {
        webTestClient.put()
            .uri("/api/v1/transactions/update/transactionId/$transactionId")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                TransactionRequest(
                    amount = 200.0.toBigDecimal(),
                    currency = "SGD",
                    type = SplitType.TheyOweYouAll,
                    recipientId = johnFriendId,
                    description = "Sample transaction edited"
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.message").isEqualTo("Transaction updated successfully")
    }

    @Order(6)
    @Test
    fun `get all transactions as zee has history now`() {
        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend?friendId=$johnFriendId")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<Paginated<TransactionsResponse>>() {})
            }

        assertThat(result.data.transactions).hasSize(1)
        assertThat(result.data.transactions[0].friendName).isEqualTo("john")
        assertThat(result.data.transactions[0].amountResponse.amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.data.transactions[0].amountResponse.currency).isEqualTo("SGD")
        assertThat(result.data.transactions[0].amountResponse.isOwed).isTrue()
        assertThat(result.data.transactions[0].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.data.transactions[0].transactionId).isNotNull()
        assertThat(result.data.transactions[0].splitType).isEqualTo(SplitType.TheyOweYouAll)
        assertThat(result.data.transactions[0].description).isEqualTo("Sample transaction edited")
        assertThat(result.data.transactions[0].history).hasSize(4)
        val history1 = result.data.transactions[0].history[0]
        assertThat(history1.type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(history1.userId).isEqualTo(zeeDto.uid)
        assertThat(history1.oldValue).isEqualTo("Sample transaction")
        assertThat(history1.newValue).isEqualTo("Sample transaction edited")
        val history2 = result.data.transactions[0].history[1]
        assertThat(history2.type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(history2.userId).isEqualTo(zeeDto.uid)
        assertThat(history2.oldValue).isEqualTo("YouPaidSplitEqually")
        assertThat(history2.newValue).isEqualTo("TheyOweYouAll")
        val history3 = result.data.transactions[0].history[2]
        assertThat(history3.type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)
        assertThat(history3.userId).isEqualTo(zeeDto.uid)
        assertThat(history3.oldValue).isEqualTo("100.0")
        assertThat(history3.newValue).isEqualTo("200.0")
        val history4 = result.data.transactions[0].history[3]
        assertThat(history4.type).isEqualTo(TransactionChangeType.CURRENCY)
        assertThat(history4.userId).isEqualTo(zeeDto.uid)
        assertThat(history4.oldValue).isEqualTo("USD")
        assertThat(history4.newValue).isEqualTo("SGD")


    }

    @Order(7)
    @Test
    fun `get all transactions as john has history now`() {
        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend?friendId=$zeeFriendId")
            .header("Authorization", "Bearer $johnToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<Paginated<TransactionsResponse>>() {})
            }

        assertThat(result.data.transactions).hasSize(1)
        assertThat(result.data.transactions[0].friendName).isEqualTo("Zeeshan Tufail")
        assertThat(result.data.transactions[0].amountResponse.amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.data.transactions[0].amountResponse.currency).isEqualTo("SGD")
        assertThat(result.data.transactions[0].amountResponse.isOwed).isFalse()
        assertThat(result.data.transactions[0].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.data.transactions[0].transactionId).isNotNull()
        assertThat(result.data.transactions[0].splitType).isEqualTo(SplitType.YouOweThemAll)
        assertThat(result.data.transactions[0].description).isEqualTo("Sample transaction edited")
        assertThat(result.data.transactions[0].history).hasSize(4)
        val history1 = result.data.transactions[0].history[0]
        assertThat(history1.type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(history1.userId).isEqualTo(johnDto.uid)
        assertThat(history1.oldValue).isEqualTo("Sample transaction")
        assertThat(history1.newValue).isEqualTo("Sample transaction edited")
        val history2 = result.data.transactions[0].history[1]
        assertThat(history2.type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(history2.userId).isEqualTo(johnDto.uid)
        assertThat(history2.oldValue).isEqualTo("TheyPaidSplitEqually")
        assertThat(history2.newValue).isEqualTo("YouOweThemAll")
        val history3 = result.data.transactions[0].history[2]
        assertThat(history3.type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)
        assertThat(history3.userId).isEqualTo(johnDto.uid)
        assertThat(history3.oldValue).isEqualTo("100.0")
        assertThat(history3.newValue).isEqualTo("200.0")
        val history4 = result.data.transactions[0].history[3]
        assertThat(history4.type).isEqualTo(TransactionChangeType.CURRENCY)
        assertThat(history4.userId).isEqualTo(johnDto.uid)
        assertThat(history4.oldValue).isEqualTo("USD")
        assertThat(history4.newValue).isEqualTo("SGD")
    }
}