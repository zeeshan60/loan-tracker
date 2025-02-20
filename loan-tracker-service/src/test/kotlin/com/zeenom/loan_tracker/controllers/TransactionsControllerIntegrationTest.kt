package com.zeenom.loan_tracker.controllers

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendsResponse
import com.zeenom.loan_tracker.transactions.SplitType
import com.zeenom.loan_tracker.transactions.TransactionEventRepository
import com.zeenom.loan_tracker.transactions.TransactionRequest
import com.zeenom.loan_tracker.transactions.TransactionsResponse
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
    private var zeeDto = UserDto(
        uid = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        emailVerified = true
    )
    private lateinit var johnFriendId: UUID

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
    }
}