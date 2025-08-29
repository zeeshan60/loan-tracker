package com.zeenom.loan_tracker.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.friends.*
import com.zeenom.loan_tracker.prettyAndPrint
import com.zeenom.loan_tracker.transactions.*
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserModelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AutoCloseableSoftAssertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.*

class TransactionsControllerIntegrationTest :
    BaseIntegration() {

    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    @Autowired
    private lateinit var transactionEventRepository: TransactionEventRepository

    @Autowired
    private lateinit var userModelRepository: UserModelRepository

    @Autowired
    private lateinit var friendModelRepository: FriendModelRepository

    private lateinit var zeeToken: String
    private lateinit var johnToken: String
    private lateinit var jasonToken: String
    private var zeeDto = UserDto(
        uid = null,
        userFBId = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        currency = null,
        emailVerified = true
    )
    private var johnDto = UserDto(
        uid = null,
        userFBId = "124",
        email = "john@gmail.com",
        phoneNumber = "+923001234568",
        displayName = "John Doe",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
        currency = null,
        emailVerified = true
    )
    private var jasonDto = UserDto(
        uid = null,
        userFBId = "125",
        email = "jason@gmail.com",
        phoneNumber = "+923001234569",
        displayName = "Jason Doe",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl4",
        currency = null,
        emailVerified = true
    )
    private lateinit var johnFriendId: UUID

    private lateinit var zeeFriendId: UUID

    @BeforeAll
    fun setupBeforeAll(): Unit = runBlocking {
        userModelRepository.deleteAll()
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        transactionEventRepository.deleteAll()
        friendModelRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        zeeDto = userModelRepository.findByUidAndDeletedIsFalse(zeeDto.userFBId)!!.let {
            UserDto(
                uid = it.streamId,
                userFBId = it.uid,
                email = it.email,
                phoneNumber = it.phoneNumber,
                displayName = it.displayName,
                photoUrl = it.photoUrl,
                currency = it.currency?.toString(),
                emailVerified = it.emailVerified
            )
        }
        addFriend(zeeToken, "john")
        johnFriendId = queryFriend(
            token = zeeToken
        ).data.friends.first().friendId
    }

    @Order(1)
    @Test
    fun `add a transaction adds transaction and also sets the currency successfully when user didnt have any currency`(): Unit =
        runBlocking {

            webTestClient.post()
                .uri("/api/v1/transactions/add")
                .header("Authorization", "Bearer $zeeToken")
                .bodyValue(
                    TransactionCreateRequest(
                        amount = 100.0.toBigDecimal(),
                        currency = "USD",
                        type = SplitType.YouPaidSplitEqually,
                        recipientId = johnFriendId,
                        description = "Sample transaction",
                        transactionDate = Instant.parse("2025-02-26T00:00:00Z"),
                        groupId = null,
                        groupAmountSplit = null,
                    )
                )
                .exchange()
                .expectStatus().isOk
                .expectBody().jsonPath("$.description").isEqualTo("Sample transaction")

            delay(100)
            val existing = userModelRepository.findByStreamIdAndDeletedIsFalse(zeeDto.uid!!)
            assertThat(existing!!.currency.toString()).isEqualTo("USD")
        }

    private lateinit var transaction: TransactionResponse

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
        assertThat(result.perMonth[0].transactions[0].friend.name).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[0].amount.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amount.currency).isEqualTo("USD")
        assertThat(result.perMonth[0].transactions[0].amount.isOwed).isTrue()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction")
        assertThat(result.perMonth[0].transactions[0].defaultCurrencyAmount).isNotNull
        assertThat(result.perMonth[0].transactions[0].defaultCurrencyAmount!!.amount).isEqualTo(50.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].defaultCurrencyAmount!!.currency).isEqualTo("USD")
        transaction = result.perMonth[0].transactions[0]
    }

    @Order(3)
    @Test
    fun `login with friend as user`(): Unit = runBlocking {
        johnToken = loginUser(johnDto).token
        johnDto = userModelRepository.findByUidAndDeletedIsFalse(johnDto.userFBId)!!.let {
            UserDto(
                uid = it.streamId,
                userFBId = it.uid,
                email = it.email,
                phoneNumber = it.phoneNumber,
                displayName = it.displayName,
                photoUrl = it.photoUrl,
                currency = it.currency?.toString(),
                emailVerified = it.emailVerified
            )
        }
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
        assertThat(result.perMonth[0].transactions[0].friend.friendId).isEqualTo(zeeFriendId)
        assertThat(result.perMonth[0].transactions[0].friend.name).isEqualTo(zeeDto.displayName)
        assertThat(result.perMonth[0].transactions[0].friend.email).isEqualTo(zeeDto.email)
        assertThat(result.perMonth[0].transactions[0].friend.phoneNumber).isEqualTo(zeeDto.phoneNumber)
        assertThat(result.perMonth[0].transactions[0].friend.photoUrl).isEqualTo(zeeDto.photoUrl)
        assertThat(result.perMonth[0].transactions[0].amount.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amount.currency).isEqualTo("USD")
        assertThat(result.perMonth[0].transactions[0].amount.isOwed).isFalse()
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
            .uri("/api/v1/transactions/update/transactionId/${transaction.transactionId}")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                TransactionUpdateRequest(
                    amount = 200.0.toBigDecimal(),
                    currency = "SGD",
                    type = SplitType.TheyOweYouAll,

                    description = "Sample transaction edited",
                    transactionDate = Instant.parse("2025-02-25T00:00:00Z"),
                    groupAmountSplit = null
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.description").isEqualTo("Sample transaction edited")
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
        assertThat(result.perMonth[0].transactions[0].friend.name).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[0].date).isEqualTo(Instant.parse("2025-02-25T00:00:00Z"))
        assertThat(result.perMonth[0].transactions[0].amount.amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amount.currency).isEqualTo("SGD")
        assertThat(result.perMonth[0].transactions[0].amount.isOwed).isTrue()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.TheyOweYouAll)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction edited")
        assertThat(result.perMonth[0].transactions[0].history[0].changes).hasSize(5)
        val history1 = result.perMonth[0].transactions[0].history[0]
        val history0Changes = result.perMonth[0].transactions[0].history[0].changes[0]
        assertThat(history0Changes.type).isEqualTo(TransactionChangeType.TRANSACTION_DATE)
        assertThat(history1.changedBy).isEqualTo(zeeDto.uid)
        assertThat(history0Changes.oldValue).isEqualTo("2025-02-26T00:00:00Z")
        assertThat(history0Changes.newValue).isEqualTo("2025-02-25T00:00:00Z")
        val history1Changes = result.perMonth[0].transactions[0].history[0].changes[1]
        assertThat(history1Changes.type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(history1.changedBy).isEqualTo(zeeDto.uid)
        assertThat(history1Changes.oldValue).isEqualTo("Sample transaction")
        assertThat(history1Changes.newValue).isEqualTo("Sample transaction edited")
        val history2 = result.perMonth[0].transactions[0].history[0]
        val history2Changes = result.perMonth[0].transactions[0].history[0].changes[2]
        assertThat(history2Changes.type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(history2.changedBy).isEqualTo(zeeDto.uid)
        assertThat(history2Changes.oldValue).isEqualTo("YouPaidSplitEqually")
        assertThat(history2Changes.newValue).isEqualTo("TheyOweYouAll")
        val history3 = result.perMonth[0].transactions[0].history[0]
        val history3Changes = result.perMonth[0].transactions[0].history[0].changes[3]
        assertThat(history3Changes.type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)
        assertThat(history3.changedBy).isEqualTo(zeeDto.uid)
        assertThat(history3Changes.oldValue).isEqualTo("100.0")
        assertThat(history3Changes.newValue).isEqualTo("200.0")
        val history4 = result.perMonth[0].transactions[0].history[0]
        val history4Changes = result.perMonth[0].transactions[0].history[0].changes[4]
        assertThat(history4Changes.type).isEqualTo(TransactionChangeType.CURRENCY)
        assertThat(history4.changedBy).isEqualTo(zeeDto.uid)
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
        assertThat(result.perMonth[0].transactions[0].friend.name).isEqualTo("Zeeshan Tufail")
        assertThat(result.perMonth[0].transactions[0].amount.amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amount.currency).isEqualTo("SGD")
        assertThat(result.perMonth[0].transactions[0].amount.isOwed).isFalse()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.YouOweThemAll)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction edited")
        assertThat(result.perMonth[0].transactions[0].history[0].changes).hasSize(5)
        val history0 = result.perMonth[0].transactions[0].history[0]
        val history0Changes = result.perMonth[0].transactions[0].history[0].changes[0]
        assertThat(history0Changes.type).isEqualTo(TransactionChangeType.TRANSACTION_DATE)
        assertThat(history0.changedBy).isEqualTo(zeeDto.uid)
        assertThat(history0Changes.oldValue).isEqualTo("2025-02-26T00:00:00Z")
        assertThat(history0Changes.newValue).isEqualTo("2025-02-25T00:00:00Z")
        val history1 = result.perMonth[0].transactions[0].history[0]
        val history1Changes = result.perMonth[0].transactions[0].history[0].changes[1]
        assertThat(history1Changes.type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(history1.changedBy).isEqualTo(zeeDto.uid)
        assertThat(history1Changes.oldValue).isEqualTo("Sample transaction")
        assertThat(history1Changes.newValue).isEqualTo("Sample transaction edited")
        val history2 = result.perMonth[0].transactions[0].history[0]
        val history2Changes = result.perMonth[0].transactions[0].history[0].changes[2]
        assertThat(history2Changes.type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(history2.changedBy).isEqualTo(zeeDto.uid)
        assertThat(history2Changes.oldValue).isEqualTo("TheyPaidSplitEqually")
        assertThat(history2Changes.newValue).isEqualTo("YouOweThemAll")
        val history3 = result.perMonth[0].transactions[0].history[0]
        val history3Changes = result.perMonth[0].transactions[0].history[0].changes[3]
        assertThat(history3Changes.type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)
        assertThat(history3.changedBy).isEqualTo(zeeDto.uid)
        assertThat(history3Changes.oldValue).isEqualTo("100.0")
        assertThat(history3Changes.newValue).isEqualTo("200.0")
        val history4 = result.perMonth[0].transactions[0].history[0]
        val history4Changes = result.perMonth[0].transactions[0].history[0].changes[4]
        assertThat(history4Changes.type).isEqualTo(TransactionChangeType.CURRENCY)
        assertThat(history4.changedBy).isEqualTo(zeeDto.uid)
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
        assertThat(result.data.friends[0].mainBalance!!.amount).isEqualTo(153.85.toBigDecimal())
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
        assertThat(result.data.friends[0].mainBalance!!.amount).isEqualTo(153.85.toBigDecimal())
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
                    description = "Sample transaction 2",
                    transactionDate = Instant.parse("2025-02-27T00:00:00Z"),
                    groupId = null,
                    groupAmountSplit = null
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.description").isEqualTo("Sample transaction 2")
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
        assertThat(result.perMonth[0].transactions[0].friend.name).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[0].amount.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amount.currency).isEqualTo("USD")
        assertThat(result.perMonth[0].transactions[0].amount.isOwed).isTrue()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction 2")
    }

    @Order(12)
    @Test
    fun `update transaction as john`() {
        webTestClient.put()
            .uri("/api/v1/transactions/update/transactionId/${transaction.transactionId}")
            .header("Authorization", "Bearer $johnToken")
            .bodyValue(
                TransactionUpdateRequest(
                    amount = 300.0.toBigDecimal(),
                    currency = "SGD",
                    type = SplitType.TheyOweYouAll,
                    description = "Sample transaction edited by john",
                    transactionDate = Instant.parse("2025-02-25T00:00:00Z"),
                    groupAmountSplit = null
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.description").isEqualTo("Sample transaction edited by john")
    }

    lateinit var transaction2: TransactionResponse

    @Order(13)
    @Test
    fun `get all transactions as zee has history for john as well`() {
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
        transaction2 = result.perMonth[0].transactions[1]
        assertThat(result.perMonth[0].transactions[1].friend.name).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[1].amount.amount).isEqualTo(300.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[1].amount.currency).isEqualTo("SGD")
        assertThat(result.perMonth[0].transactions[1].amount.isOwed).isFalse()
        assertThat(result.perMonth[0].transactions[1].totalAmount).isEqualTo(300.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[1].transactionId).isEqualTo(transaction.transactionId)
        assertThat(result.perMonth[0].transactions[1].splitType).isEqualTo(SplitType.YouOweThemAll)
        assertThat(result.perMonth[0].transactions[1].description).isEqualTo("Sample transaction edited by john")
        assertThat(result.perMonth[0].transactions[1].createdBy.name).isEqualTo("You")
        assertThat(result.perMonth[0].transactions[1].updatedBy!!.name).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[1].createdBy.id).isEqualTo(zeeDto.uid)
        assertThat(result.perMonth[0].transactions[1].updatedBy!!.id).isEqualTo(johnDto.uid)
        assertThat(result.perMonth[0].transactions[1].createdAt).isNotNull()
        assertThat(result.perMonth[0].transactions[1].updatedAt).isNotNull()
        assertThat(result.perMonth[0].transactions[1].history).hasSize(2)
        assertThat(result.perMonth[0].transactions[1].history[0].changes).hasSize(5)
        val history0Changes = result.perMonth[0].transactions[1].history[0].changes[0]
        assertThat(history0Changes.type).isEqualTo(TransactionChangeType.TRANSACTION_DATE)
        assertThat(result.perMonth[0].transactions[1].history[0].changedBy).isEqualTo(zeeDto.uid)
        assertThat(history0Changes.oldValue).isEqualTo("2025-02-26T00:00:00Z")
        assertThat(history0Changes.newValue).isEqualTo("2025-02-25T00:00:00Z")
        val history1Changes = result.perMonth[0].transactions[1].history[0].changes[1]
        assertThat(history1Changes.type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(result.perMonth[0].transactions[1].history[0].changedBy).isEqualTo(zeeDto.uid)
        assertThat(history1Changes.oldValue).isEqualTo("Sample transaction")
        assertThat(history1Changes.newValue).isEqualTo("Sample transaction edited")
        val history2Changes = result.perMonth[0].transactions[1].history[0].changes[2]
        assertThat(history2Changes.type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(result.perMonth[0].transactions[1].history[0].changedBy).isEqualTo(zeeDto.uid)
        assertThat(history2Changes.oldValue).isEqualTo("YouPaidSplitEqually")
        assertThat(history2Changes.newValue).isEqualTo("TheyOweYouAll")
        val history3Changes = result.perMonth[0].transactions[1].history[0].changes[3]
        assertThat(history3Changes.type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)
        assertThat(result.perMonth[0].transactions[1].history[0].changedBy).isEqualTo(zeeDto.uid)
        assertThat(history3Changes.oldValue).isEqualTo("100.0")
        assertThat(history3Changes.newValue).isEqualTo("200.0")
        val history4Changes = result.perMonth[0].transactions[1].history[0].changes[4]
        assertThat(history4Changes.type).isEqualTo(TransactionChangeType.CURRENCY)
        assertThat(result.perMonth[0].transactions[1].history[0].changedBy).isEqualTo(zeeDto.uid)
        assertThat(history4Changes.oldValue).isEqualTo("USD")
        assertThat(history4Changes.newValue).isEqualTo("SGD")
        assertThat(result.perMonth[0].transactions[1].history[1].changedBy).isEqualTo(johnDto.uid)
        assertThat(result.perMonth[0].transactions[1].history[1].changes).hasSize(3)
        val history1Changes2 = result.perMonth[0].transactions[1].history[1].changes[0]
        assertThat(history1Changes2.type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(result.perMonth[0].transactions[1].history[1].changedBy).isEqualTo(johnDto.uid)
        assertThat(history1Changes2.oldValue).isEqualTo("Sample transaction edited")
        assertThat(history1Changes2.newValue).isEqualTo("Sample transaction edited by john")
        val history2Changes2 = result.perMonth[0].transactions[1].history[1].changes[1]
        assertThat(history2Changes2.type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(result.perMonth[0].transactions[1].history[1].changedBy).isEqualTo(johnDto.uid)
        assertThat(history2Changes2.oldValue).isEqualTo("TheyOweYouAll")
        assertThat(history2Changes2.newValue).isEqualTo("YouOweThemAll")
        val history3Changes2 = result.perMonth[0].transactions[1].history[1].changes[2]
        assertThat(history3Changes2.type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)
        assertThat(result.perMonth[0].transactions[1].history[1].changedBy).isEqualTo(johnDto.uid)
        assertThat(history3Changes2.oldValue).isEqualTo("200.0")
        assertThat(history3Changes2.newValue).isEqualTo("300.0")
    }

    @Order(14)
    @Test
    fun `delete transaction as zee`() {
        webTestClient.delete()
            .uri("/api/v1/transactions/delete/transactionId/${transaction.transactionId}")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.deleted").isEqualTo(true)
    }

    @Order(15)
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
        assertThat(result.perMonth[0].transactions[0].friend.name).isEqualTo("john")
        assertThat(result.perMonth[0].transactions[0].amount.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].amount.currency).isEqualTo("USD")
        assertThat(result.perMonth[0].transactions[0].amount.isOwed).isTrue()
        assertThat(result.perMonth[0].transactions[0].totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(result.perMonth[0].transactions[0].transactionId).isNotNull()
        assertThat(result.perMonth[0].transactions[0].splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        assertThat(result.perMonth[0].transactions[0].description).isEqualTo("Sample transaction 2")
        assertThat(result.balance.main!!.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.balance.main!!.isOwed).isTrue()
        assertThat(result.balance.main!!.currency).isEqualTo("USD")
        assertThat(result.balance.other).hasSize(1)
        assertThat(result.balance.other[0].amount.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.balance.other[0].amount.isOwed).isTrue()
        assertThat(result.balance.other[0].amount.currency).isEqualTo("USD")
    }

    @Order(16)
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

        assertThat(result.data).hasSize(5)

        result.prettyAndPrint(objectMapper)

        assertThat(result.data[0].id).isNotNull()
        assertThat(result.data[0].activityType).isEqualTo(ActivityType.DELETED)
        assertThat(result.data[0].amount).isEqualTo(300.0.toBigDecimal())
        assertThat(result.data[0].currency).isEqualTo("SGD")
        assertThat(result.data[0].description).isEqualTo("Sample transaction edited by john")
        assertThat(result.data[0].activityByName).isEqualTo(zeeDto.displayName)
        assertThat(result.data[0].activityByPhoto).isEqualTo(zeeDto.photoUrl)
        assertTransactionDataCorrectness(result.data[0].transactionResponse)

        assertThat(result.data[1].activityType).isEqualTo(ActivityType.UPDATED)
        assertThat(result.data[1].amount).isEqualTo(300.0.toBigDecimal())
        assertThat(result.data[1].currency).isEqualTo("SGD")
        assertThat(result.data[1].description).isEqualTo("Sample transaction edited by john")
        assertThat(result.data[1].transactionResponse.transactionId).isEqualTo(transaction.transactionId)
        assertThat(result.data[1].activityByName).isEqualTo("john")
        assertThat(result.data[1].activityByPhoto).isEqualTo(johnDto.photoUrl)
        assertTransactionDataCorrectness(result.data[1].transactionResponse)

        assertThat(result.data[2].activityType).isEqualTo(ActivityType.CREATED)
        assertThat(result.data[2].amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.data[2].currency).isEqualTo("USD")
        assertThat(result.data[2].description).isEqualTo("Sample transaction 2")
        assertThat(result.data[2].transactionResponse.transactionId).isNotEqualTo(transaction.transactionId)
        assertThat(result.data[2].activityByName).isEqualTo(zeeDto.displayName)
        assertThat(result.data[2].activityByPhoto).isEqualTo(zeeDto.photoUrl)
        assertTransaction2DataCorrectness(result.data[2].transactionResponse)

        assertThat(result.data[3].activityType).isEqualTo(ActivityType.UPDATED)
        assertThat(result.data[3].amount).isEqualTo(200.0.toBigDecimal())
        assertThat(result.data[3].currency).isEqualTo("SGD")
        assertThat(result.data[3].description).isEqualTo("Sample transaction edited")
        assertThat(result.data[3].activityByName).isEqualTo(zeeDto.displayName)
        assertThat(result.data[3].activityByPhoto).isEqualTo(zeeDto.photoUrl)
        assertTransactionDataCorrectness(result.data[3].transactionResponse)

        assertThat(result.data[4].activityType).isEqualTo(ActivityType.CREATED)
        assertThat(result.data[4].amount).isEqualTo(50.0.toBigDecimal())
        assertThat(result.data[4].currency).isEqualTo("USD")
        assertThat(result.data[4].description).isEqualTo("Sample transaction")
        assertThat(result.data[4].activityByName).isEqualTo(zeeDto.displayName)
        assertThat(result.data[4].activityByPhoto).isEqualTo(zeeDto.photoUrl)
        assertTransactionDataCorrectness(result.data[4].transactionResponse)
    }

    @Order(17)
    @Test
    fun `login as jason add john as friend and see if john also has jason as friend and jason has john as friend`() {
        jasonToken = loginUser(jasonDto).token
        addFriend(jasonToken, "johni")
        val johnFriends = queryFriend(johnToken).data.friends
        assertThat(johnFriends).hasSize(2)
        assertThat(johnFriends[1].name).isEqualTo("Jason Doe")

        val jasonFriends = queryFriend(jasonToken).data.friends
        assertThat(jasonFriends).hasSize(1)
        assertThat(jasonFriends[0].name).isEqualTo("johni")
    }

    @Order(18)
    @Test
    fun `update zee main currency and get friends returns main balance in updated currency`() {
        val userRequest = UpdateUserRequest(
            displayName = zeeDto.displayName,
            phoneNumber = zeeDto.phoneNumber,
            currency = "PKR"
        )
        webTestClient.put()
            .uri("/api/v1/users")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(userRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult().responseBody!!

        val friends = queryFriend(zeeToken).data.friends

        assertThat(friends).hasSize(1)
        AutoCloseableSoftAssertions().use { soft ->
            soft.assertThat(friends[0].mainBalance!!.amount).isEqualTo(13000.toBigDecimal())
            soft.assertThat(friends[0].mainBalance!!.isOwed).isTrue()
            soft.assertThat(friends[0].mainBalance!!.currency).isEqualTo("PKR")
            soft.assertThat(friends[0].otherBalances).hasSize(1)
            soft.assertThat(friends[0].otherBalances[0].amount.amount).isEqualTo(50.0.toBigDecimal())
            soft.assertThat(friends[0].otherBalances[0].amount.isOwed).isTrue()
            soft.assertThat(friends[0].otherBalances[0].amount.currency).isEqualTo("USD")
        }
    }

    private fun assertTransactionDataCorrectness(transactionResponse: TransactionResponse) {
        assertThat(transactionResponse.date).isEqualTo("2025-02-25T00:00:00Z")
        assertThat(transactionResponse.transactionId).isEqualTo(transaction.transactionId)
        assertThat(transactionResponse.description).isEqualTo("Sample transaction edited by john")
        assertThat(transactionResponse.totalAmount).isEqualTo(300.0.toBigDecimal())
        assertThat(transactionResponse.splitType).isEqualTo(SplitType.YouOweThemAll)
        assertThat(transactionResponse.friend.name).isEqualTo("john")
        assertThat(transactionResponse.amount.amount).isEqualTo(300.0.toBigDecimal())
        assertThat(transactionResponse.amount.currency).isEqualTo("SGD")
        assertThat(transactionResponse.amount.isOwed).isFalse()

        assertThat(transactionResponse.history[0].changes).hasSize(5)
        assertThat(transactionResponse.history[0].changedBy).isEqualTo(zeeDto.uid)
        assertThat(transactionResponse.history[0].changes[0].oldValue).isEqualTo("2025-02-26T00:00:00Z")
        assertThat(transactionResponse.history[0].changes[0].newValue).isEqualTo("2025-02-25T00:00:00Z")
        assertThat(transactionResponse.history[0].changes[0].type).isEqualTo(TransactionChangeType.TRANSACTION_DATE)
        assertThat(transactionResponse.history[0].changes[1].oldValue).isEqualTo("Sample transaction")
        assertThat(transactionResponse.history[0].changes[1].newValue).isEqualTo("Sample transaction edited")
        assertThat(transactionResponse.history[0].changes[1].type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(transactionResponse.history[0].changes[2].oldValue).isEqualTo("YouPaidSplitEqually")
        assertThat(transactionResponse.history[0].changes[2].newValue).isEqualTo("TheyOweYouAll")
        assertThat(transactionResponse.history[0].changes[2].type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(transactionResponse.history[0].changes[3].oldValue).isEqualTo("100.0")
        assertThat(transactionResponse.history[0].changes[3].newValue).isEqualTo("200.0")
        assertThat(transactionResponse.history[0].changes[3].type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)
        assertThat(transactionResponse.history[0].changes[4].oldValue).isEqualTo("USD")
        assertThat(transactionResponse.history[0].changes[4].newValue).isEqualTo("SGD")
        assertThat(transactionResponse.history[0].changes[4].type).isEqualTo(TransactionChangeType.CURRENCY)

        assertThat(transactionResponse.history[1].changes).hasSize(3)
        assertThat(transactionResponse.history[1].changedBy).isEqualTo(johnDto.uid)
        assertThat(transactionResponse.history[1].changes[0].oldValue).isEqualTo("Sample transaction edited")
        assertThat(transactionResponse.history[1].changes[0].newValue).isEqualTo("Sample transaction edited by john")
        assertThat(transactionResponse.history[1].changes[0].type).isEqualTo(TransactionChangeType.DESCRIPTION)
        assertThat(transactionResponse.history[1].changes[1].oldValue).isEqualTo("TheyOweYouAll")
        assertThat(transactionResponse.history[1].changes[1].newValue).isEqualTo("YouOweThemAll")
        assertThat(transactionResponse.history[1].changes[1].type).isEqualTo(TransactionChangeType.SPLIT_TYPE)
        assertThat(transactionResponse.history[1].changes[2].oldValue).isEqualTo("200.0")
        assertThat(transactionResponse.history[1].changes[2].newValue).isEqualTo("300.0")
        assertThat(transactionResponse.history[1].changes[2].type).isEqualTo(TransactionChangeType.TOTAL_AMOUNT)

        assertThat(transactionResponse.history[2].changes).hasSize(1)
        assertThat(transactionResponse.history[2].changedBy).isEqualTo(zeeDto.uid)
        assertThat(transactionResponse.history[2].changes[0].oldValue).isEqualTo("Not Deleted")
        assertThat(transactionResponse.history[2].changes[0].newValue).isEqualTo("Deleted")
        assertThat(transactionResponse.history[2].changes[0].type).isEqualTo(TransactionChangeType.DELETED)

        assertThat(transactionResponse.createdAt).isNotNull()
        assertThat(transactionResponse.updatedAt).isNotNull()
        assertThat(transactionResponse.createdBy.id).isEqualTo(zeeDto.uid)
        assertThat(transactionResponse.createdBy.name).isEqualTo("You")
        assertThat(transactionResponse.updatedBy!!.id).isEqualTo(zeeDto.uid)
        assertThat(transactionResponse.updatedBy!!.name).isEqualTo("You")
    }

    private fun assertTransaction2DataCorrectness(transactionResponse: TransactionResponse) {
        assertThat(transactionResponse.date).isEqualTo("2025-02-27T00:00:00Z")
        assertThat(transactionResponse.transactionId).isNotEqualTo(transaction2.transactionId)
        assertThat(transactionResponse.description).isEqualTo("Sample transaction 2")
        assertThat(transactionResponse.totalAmount).isEqualTo(100.0.toBigDecimal())
        assertThat(transactionResponse.splitType).isEqualTo(SplitType.YouPaidSplitEqually)
        assertThat(transactionResponse.friend.name).isEqualTo("john")
        assertThat(transactionResponse.amount.amount).isEqualTo(50.0.toBigDecimal())
        assertThat(transactionResponse.amount.currency).isEqualTo("USD")
        assertThat(transactionResponse.amount.isOwed).isTrue()

        assertThat(transactionResponse.createdAt).isNotNull()
        assertThat(transactionResponse.updatedAt).isNotNull()
        assertThat(transactionResponse.createdBy.id).isEqualTo(zeeDto.uid)
        assertThat(transactionResponse.createdBy.name).isEqualTo("You")
        assertThat(transactionResponse.updatedBy).isNull()
    }
}