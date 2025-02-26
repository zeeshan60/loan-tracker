package com.zeenom.loan_tracker.controllers

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendsResponse
import com.zeenom.loan_tracker.prettyAndPrint
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
import java.util.*

class TransactionsControllerIntegrationTest(@LocalServerPort private val port: Int) :
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
                TransactionCreateRequest(
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
            .uri("/api/v1/transactions/friend/byMonth?friendId=$johnFriendId&timeZone=Asia/Singapore")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<TransactionsResponse>() {})
            }

        assertThat(result.perMonth).hasSize(1)
        assertThat(result.perMonth[0].transactions[0].friendName).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[0].amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amountResponse.currency).isEqualTo("USD")
        assertThat(result.perMonth[0].transactions[0].amountResponse.isOwed).isTrue()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction")
        transactionId = result.perMonth[0].transactions[0].transactionId
    }

    @Order(3)
    @Test
    fun `login with friend as user`() {
        johnToken = loginUser(johnDto).token
        val queryFriend = queryFriend(johnToken)
        zeeFriendId = queryFriend.data.friends.first().friendId
        assertThat(zeeFriendId).isNotNull()
        val loanAmount = queryFriend.data.friends.first().mainBalance
        assertThat(loanAmount).isNotNull
        assertThat(loanAmount!!.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(loanAmount.isOwed).isFalse()
    }

    @Order(4)
    @Test
    fun `get all transactions as john`() {
        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend/byMonth?friendId=$zeeFriendId&timeZone=Asia/Singapore")
            .header("Authorization", "Bearer $johnToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<TransactionsResponse>() {})
            }

        assertThat(result.perMonth).hasSize(1)
        assertThat(result.perMonth[0].transactions[0].friendName).isEqualTo("Zeeshan Tufail")
        assertThat(result.perMonth[0].transactions[0].amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amountResponse.currency).isEqualTo("USD")
        assertThat(result.perMonth[0].transactions[0].amountResponse.isOwed).isFalse()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.TheyPaidSplitEqually)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction")
        assertThat(result.perMonth[0].transactions[0].history).isEmpty()
    }

    @Order(5)
    @Test
    fun `update transaction as zee`() {
        webTestClient.put()
            .uri("/api/v1/transactions/update/transactionId/$transactionId")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                TransactionUpdateRequest(
                    amount = 200.0.toBigDecimal(),
                    currency = "SGD",
                    type = SplitType.TheyOweYouAll,
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
            .uri("/api/v1/transactions/friend/byMonth?friendId=$johnFriendId&timeZone=Asia/Singapore")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<TransactionsResponse>() {})
            }

        assertThat(result.perMonth).hasSize(1)
        assertThat(result.perMonth[0].transactions[0].friendName).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[0].amountResponse.amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amountResponse.currency).isEqualTo("SGD")
        assertThat(result.perMonth[0].transactions[0].amountResponse.isOwed).isTrue()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.TheyOweYouAll)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction edited")
        assertThat(result.perMonth[0].transactions[0].history[0].changes).hasSize(4)
        val history1 = result.perMonth[0].transactions[0].history[0]
        val history1Changes = result.perMonth[0].transactions[0].history[0].changes[0]
        assertThat(history1Changes.type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(history1.userId).isEqualTo(zeeDto.uid)
        assertThat(history1Changes.oldValue).isEqualTo("Sample transaction")
        assertThat(history1Changes.newValue).isEqualTo("Sample transaction edited")
        val history2 = result.perMonth[0].transactions[0].history[0]
        val history2Changes = result.perMonth[0].transactions[0].history[0].changes[1]
        assertThat(history2Changes.type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(history2.userId).isEqualTo(zeeDto.uid)
        assertThat(history2Changes.oldValue).isEqualTo("YouPaidSplitEqually")
        assertThat(history2Changes.newValue).isEqualTo("TheyOweYouAll")
        val history3 = result.perMonth[0].transactions[0].history[0]
        val history3Changes = result.perMonth[0].transactions[0].history[0].changes[2]
        assertThat(history3Changes.type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)
        assertThat(history3.userId).isEqualTo(zeeDto.uid)
        assertThat(history3Changes.oldValue).isEqualTo("100.0")
        assertThat(history3Changes.newValue).isEqualTo("200.0")
        val history4 = result.perMonth[0].transactions[0].history[0]
        val history4Changes = result.perMonth[0].transactions[0].history[0].changes[3]
        assertThat(history4Changes.type).isEqualTo(TransactionChangeType.CURRENCY)
        assertThat(history4.userId).isEqualTo(zeeDto.uid)
        assertThat(history4Changes.oldValue).isEqualTo("USD")
        assertThat(history4Changes.newValue).isEqualTo("SGD")


    }

    @Order(7)
    @Test
    fun `get all transactions as john has history now`() {
        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend/byMonth?friendId=$zeeFriendId&timeZone=Asia/Singapore")
            .header("Authorization", "Bearer $johnToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<TransactionsResponse>() {})
            }

        assertThat(result.perMonth).hasSize(1)
        assertThat(result.perMonth[0].transactions[0].friendName).isEqualTo("Zeeshan Tufail")
        assertThat(result.perMonth[0].transactions[0].amountResponse.amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amountResponse.currency).isEqualTo("SGD")
        assertThat(result.perMonth[0].transactions[0].amountResponse.isOwed).isFalse()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.YouOweThemAll)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction edited")
        assertThat(result.perMonth[0].transactions[0].history[0].changes).hasSize(4)
        val history1 = result.perMonth[0].transactions[0].history[0]
        val history1Changes = result.perMonth[0].transactions[0].history[0].changes[0]
        assertThat(history1Changes.type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(history1.userId).isEqualTo(johnDto.uid)
        assertThat(history1Changes.oldValue).isEqualTo("Sample transaction")
        assertThat(history1Changes.newValue).isEqualTo("Sample transaction edited")
        val history2 = result.perMonth[0].transactions[0].history[0]
        val history2Changes = result.perMonth[0].transactions[0].history[0].changes[1]
        assertThat(history2Changes.type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(history2.userId).isEqualTo(johnDto.uid)
        assertThat(history2Changes.oldValue).isEqualTo("TheyPaidSplitEqually")
        assertThat(history2Changes.newValue).isEqualTo("YouOweThemAll")
        val history3 = result.perMonth[0].transactions[0].history[0]
        val history3Changes = result.perMonth[0].transactions[0].history[0].changes[2]
        assertThat(history3Changes.type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)
        assertThat(history3.userId).isEqualTo(johnDto.uid)
        assertThat(history3Changes.oldValue).isEqualTo("100.0")
        assertThat(history3Changes.newValue).isEqualTo("200.0")
        val history4 = result.perMonth[0].transactions[0].history[0]
        val history4Changes = result.perMonth[0].transactions[0].history[0].changes[3]
        assertThat(history4Changes.type).isEqualTo(TransactionChangeType.CURRENCY)
        assertThat(history4.userId).isEqualTo(johnDto.uid)
        assertThat(history4Changes.oldValue).isEqualTo("USD")
        assertThat(history4Changes.newValue).isEqualTo("SGD")
    }

    @Order(8)
    @Test
    fun `get friends as zee now has updated balance amount`() {
        val result = webTestClient.get()
            .uri("/api/v1/friends")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<Paginated<FriendsResponse>>() {})
            }

        assertThat(result.data.friends).hasSize(1)
        assertThat(result.data.friends[0].name).isEqualTo("john")
        assertThat(result.data.friends[0].mainBalance).isNotNull
        assertThat(result.data.friends[0].mainBalance!!.amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.data.friends[0].mainBalance!!.isOwed).isTrue()
        assertThat(result.data.friends[0].photoUrl).isEqualTo(johnDto.photoUrl)
    }

    @Order(9)
    @Test
    fun `get friends as john now has updated balance amount`() {
        val result = webTestClient.get()
            .uri("/api/v1/friends")
            .header("Authorization", "Bearer $johnToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<Paginated<FriendsResponse>>() {})
            }

        assertThat(result.data.friends).hasSize(1)
        assertThat(result.data.friends[0].name).isEqualTo("Zeeshan Tufail")
        assertThat(result.data.friends[0].mainBalance).isNotNull
        assertThat(result.data.friends[0].mainBalance!!.amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.data.friends[0].mainBalance!!.isOwed).isFalse()
        assertThat(result.data.friends[0].photoUrl).isEqualTo(zeeDto.photoUrl)
    }

    @Order(10)
    @Test
    fun `add new transaction as zee`() {
        webTestClient.post()
            .uri("/api/v1/transactions/add")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                TransactionCreateRequest(
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

    @Order(11)
    @Test
    fun `get all transactions should return two transactions`() {
        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend/byMonth?friendId=$johnFriendId&timeZone=Asia/Singapore")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<TransactionsResponse>() {})
            }

        assertThat(result.perMonth[0].transactions).hasSize(2)
        assertThat(result.perMonth[0].transactions[1].friendName).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[1].amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[1].amountResponse.currency).isEqualTo("USD")
        assertThat(result.perMonth[0].transactions[1].amountResponse.isOwed).isTrue()
        assertThat(result.perMonth[0].transactions[1].totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[1].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[1].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        assertThat(result.perMonth[0].transactions[1].description).isEqualTo("Sample transaction")
    }

    @Order(12)
    @Test
    fun `update transaction as john`() {
        webTestClient.put()
            .uri("/api/v1/transactions/update/transactionId/$transactionId")
            .header("Authorization", "Bearer $johnToken")
            .bodyValue(
                TransactionUpdateRequest(
                    amount = 300.0.toBigDecimal(),
                    currency = "SGD",
                    type = SplitType.TheyOweYouAll,
                    description = "Sample transaction edited by john"
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.message").isEqualTo("Transaction updated successfully")
    }


    @Order(13)
    @Test
    fun `delete transaction as zee`() {
        webTestClient.delete()
            .uri("/api/v1/transactions/delete/transactionId/$transactionId")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.message").isEqualTo("Transaction deleted successfully")
    }

    @Order(14)
    @Test
    fun `get all transactions should return one transaction`() {
        val result = webTestClient.get()
            .uri("/api/v1/transactions/friend/byMonth?friendId=$johnFriendId&timeZone=Asia/Singapore")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<TransactionsResponse>() {})
            }

        assertThat(result.perMonth[0].transactions).hasSize(1)
        assertThat(result.perMonth[0].transactions[0].friendName).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[0].amountResponse.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amountResponse.currency).isEqualTo("USD")
        assertThat(result.perMonth[0].transactions[0].amountResponse.isOwed).isTrue()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction")
    }


    @Order(15)
    @Test
    fun `get activity logs for approval`() {

        val result = webTestClient.get()
            .uri("/api/v1/transactions/activityLogs")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.let {
                objectMapper.readValue(
                    it,
                    object : TypeReference<Paginated<List<ActivityLogResponse>>>() {})
            }

        result.prettyAndPrint(objectMapper)

        assertThat(result.data).hasSize(5)

        // First Activity Log (DELETED)
        assertThat(result.data[0].activityType).isEqualTo(ActivityType.DELETED)
        assertThat(result.data[0].amount).isEqualTo(300.0.toBigDecimal())
        assertThat(result.data[0].currency).isEqualTo("SGD")
        assertThat(result.data[0].description).isEqualTo("Sample transaction edited by john")
        assertThat(result.data[0].activityByName).isEqualTo(zeeDto.displayName)
        assertThat(result.data[0].activityByPhoto).isEqualTo(zeeDto.photoUrl)

        // Second Activity Log (UPDATED)
        assertThat(result.data[1].activityType).isEqualTo(ActivityType.UPDATED)
        assertThat(result.data[1].amount).isEqualTo(300.0.toBigDecimal())
        assertThat(result.data[1].currency).isEqualTo("SGD")
        assertThat(result.data[1].description).isEqualTo("Sample transaction edited by john")
        assertThat(result.data[1].activityByName).isEqualTo("john")
        assertThat(result.data[1].activityByPhoto).isEqualTo(johnDto.photoUrl)

        // Third Activity Log (UPDATED with USD currency)
        assertThat(result.data[2].activityType).isEqualTo(ActivityType.UPDATED)
        assertThat(result.data[2].amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.data[2].currency).isEqualTo("SGD")
        assertThat(result.data[2].description).isEqualTo("Sample transaction edited")
        assertThat(result.data[2].activityByName).isEqualTo(zeeDto.displayName)
        assertThat(result.data[2].activityByPhoto).isEqualTo(zeeDto.photoUrl)

        // Fourth Activity Log (UPDATED with amount 100.0)
        assertThat(result.data[3].activityType).isEqualTo(ActivityType.CREATED)
        assertThat(result.data[3].amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.data[3].currency).isEqualTo("USD")
        assertThat(result.data[3].description).isEqualTo("Sample transaction")
        assertThat(result.data[3].activityByName).isEqualTo(zeeDto.displayName)
        assertThat(result.data[3].activityByPhoto).isEqualTo(zeeDto.photoUrl)

        // Fifth Activity Log (UPDATED with amount 50.0)
        assertThat(result.data[4].activityType).isEqualTo(ActivityType.CREATED)
        assertThat(result.data[4].amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.data[4].currency).isEqualTo("USD")
        assertThat(result.data[4].description).isEqualTo("Sample transaction")
        assertThat(result.data[4].activityByName).isEqualTo(zeeDto.displayName)
        assertThat(result.data[4].activityByPhoto).isEqualTo(zeeDto.photoUrl)

        // Verify transaction response of the first activity log
        val transactionResponse = result.data[0].transactionResponse
        assertThat(transactionResponse.transactionId).isEqualTo(transactionId)
        assertThat(transactionResponse.totalAmount).isEqualTo(300.0.toBigDecimal())
        assertThat(transactionResponse.splitType.name).isEqualTo("YouOweThemAll")
        assertThat(transactionResponse.friendName).isEqualTo("john")

        // Verify history changes of the first activity log
        val history = transactionResponse.history
        assertThat(history).isNotEmpty
    }
}