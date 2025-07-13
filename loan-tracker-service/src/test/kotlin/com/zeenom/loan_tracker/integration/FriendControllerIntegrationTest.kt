package com.zeenom.loan_tracker.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.firebase.FirebaseService
import com.zeenom.loan_tracker.friends.*
import com.zeenom.loan_tracker.prettyAndPrint
import com.zeenom.loan_tracker.transactions.SplitType
import com.zeenom.loan_tracker.transactions.TransactionCreateRequest
import com.zeenom.loan_tracker.transactions.TransactionsResponse
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserModelRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Instant
import java.util.*

class CreateFriendWithInvalidRequestTest() : BaseIntegration() {
    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    @Autowired
    private lateinit var userModelRepository: UserModelRepository

    @Autowired
    private lateinit var friendModelRepository: FriendModelRepository

    private lateinit var zeeToken: String
    private var zeeDto = UserDto(
        uid = UUID.randomUUID(),
        userFBId = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        currency = null,
        emailVerified = true
    )

    private lateinit var johnFriendId: UUID

    @BeforeAll
    fun beforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        userModelRepository.deleteAll()
        friendModelRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
    }

    @Test
    @Order(1)
    fun `user zee adds a friend john successfully`() {

        webTestClient.post()
            .uri("/api/v1/friends/add")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                FriendRequest(
                    name = "John Doe",
                    email = "invalid email",
                    phoneNumber = "+923001234567",
                )
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("$.error.message")
            .value(containsString("Invalid email format"))
    }

    @Test
    @Order(2)
    fun `user zee adds a friend john with empty email twice successfully`() {

        webTestClient.post()
            .uri("/api/v1/friends/add")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                FriendRequest(
                    name = "John Doe",
                    email = "",
                    phoneNumber = "+923001234568",
                )
            )
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/v1/friends/add")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                FriendRequest(
                    name = "John Doe 2",
                    email = "",
                    phoneNumber = "+923001234569",
                )
            )
            .exchange()
            .expectStatus().isOk
    }
}

/**
 * To reproduce issue:
 * 1. Signup with user 1
 * 2. Add a friend with email and null phone
 * 3. Add a test transaction with user 1 and friend
 * 4. Signup with user 2
 * 5. Check that user 2 has user 1 as friend
 * 6. Check that user 2 has cross transaction with user 1
 * 7. Delete user 2 account
 * 8. Check that user 1 still has the friend and transaction
 * 9. Sign up user 2 again with same email and phone
 * 10. User 2 should have user 1 as friend and cross transaction (But it doesn't which is the issue)
 */
class FriendDeleteIssueTest() : BaseIntegration() {
    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    @Autowired
    private lateinit var userModelRepository: UserModelRepository

    @Autowired
    private lateinit var friendModelRepository: FriendModelRepository

    @MockitoSpyBean
    private lateinit var firebaseService: FirebaseService

    private lateinit var zeeToken: String
    private lateinit var johnToken: String
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
        phoneNumber = null,
        displayName = "John Doe",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
        currency = null,
        emailVerified = true
    )

    private lateinit var johnFriendId: UUID

    @BeforeAll
    fun beforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        userModelRepository.deleteAll()
        friendModelRepository.deleteAll()

        Mockito.doReturn(
            Unit
        ).whenever(firebaseService).deleteUserByFBId(any())
    }

    @Test
    fun `running the documented usecase`() {
        // 1. Signup with user 1 (Zee)
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        queryUser(token = zeeToken).also { response ->
            zeeDto = zeeDto.copy(
                uid = UUID.fromString(response.uid),
            )
        }
        // 2. Add a friend with email and null phone
        addFriend(
            token = zeeToken,
            name = johnDto.displayName,
            email = johnDto.email,
            phone = johnDto.phoneNumber
        ).also { response ->
            johnFriendId = response.friendId
        }
        // 3. Add a test transaction with user 1 and friend
        addTransaction()
        // 4. Signup with user 2 (John)
        johnToken = loginUser(
            userDto = johnDto
        ).token
        queryUser(token = johnToken).also { response ->
            johnDto = johnDto.copy(
                uid = UUID.fromString(response.uid),
            )
        }
        // 5. Check that user 2 has user 1 as friend
        val johnFriendResponse = queryFriend(token = johnToken)
        assertThat(johnFriendResponse.data.friends).hasSize(1)
        assertThat(johnFriendResponse.data.friends[0].name).isEqualTo(zeeDto.displayName)
        // 6. Check that user 2 has cross transaction with user 1
        val transactions = getTransactions(token = johnToken).perMonth.first().transactions
        assertThat(transactions).hasSize(1)
        // 7. Delete user 2 account
        deleteUser(token = johnToken)
        // 8. Check that user 1 still has the friend and transaction
        val zeeFriendResponse = queryFriend(token = zeeToken)
        assertThat(zeeFriendResponse.data.friends).hasSize(1)
        assertThat(zeeFriendResponse.data.friends[0].name).isEqualTo(johnDto.displayName)
        val zeeTransactions = getTransactions(token = zeeToken).perMonth.first().transactions
        assertThat(zeeTransactions).hasSize(1)
        // 9. Sign up user 2 again with same email and phone
        johnToken = loginUser(
            userDto = johnDto.copy(
                uid = null // Reset UID to simulate new signup
            )
        ).token
        queryUser(token = johnToken).also { response ->
            johnDto = johnDto.copy(
                uid = UUID.fromString(response.uid),
            )
        }
        // 10. User 2 should have user 1 as friend and cross transaction
        val reAddedJohnFriendResponse = queryFriend(token = johnToken)
        assertThat(reAddedJohnFriendResponse.data.friends).hasSize(1)
        assertThat(reAddedJohnFriendResponse.data.friends[0].name).isEqualTo(zeeDto.displayName)
        val reAddedJohnTransactions = getTransactions(token = johnToken).perMonth.first().transactions
        assertThat(reAddedJohnTransactions).hasSize(1)
    }

    private fun deleteUser(token: String) {
        webTestClient.delete()
            .uri("/api/v1/users")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }

    fun addTransaction(token: String = zeeToken, recipientId: UUID = johnFriendId) {
        webTestClient.post()
            .uri("/api/v1/transactions/add")
            .header("Authorization", "Bearer $token")
            .bodyValue(
                TransactionCreateRequest(
                    amount = 100.0.toBigDecimal(),
                    currency = "USD",
                    type = SplitType.YouPaidSplitEqually,
                    recipientId = recipientId,
                    description = "Sample transaction",
                    transactionDate = Instant.parse("2025-02-26T00:00:00Z")
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.description").isEqualTo("Sample transaction")
    }

    fun getTransactions(token: String): TransactionsResponse {
        return webTestClient.get()
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
    }
}

class FriendControllerIntegrationTest() : BaseIntegration() {

    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    @Autowired
    private lateinit var friendModelRepository: FriendModelRepository

    @Autowired
    private lateinit var userModelRepository: UserModelRepository

    private lateinit var zeeToken: String
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

    private lateinit var johnToken: String
    private var johnDto = UserDto(
        uid = UUID.randomUUID(),
        userFBId = "124",
        email = "john@gmail.com",
        phoneNumber = "+923001234568",
        displayName = "John Doe",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
        currency = null,
        emailVerified = true
    )

    private lateinit var johnFriendId: UUID

    @BeforeAll
    fun beforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        userModelRepository.deleteAll()
        friendModelRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        queryUser(token = zeeToken).also { response ->
            zeeDto = zeeDto.copy(
                uid = UUID.fromString(response.uid),
            )
        }
    }

    @Test
    @Order(1)
    fun `user zee adds a friend john successfully`() {

        val friendResponse = addFriend(token = zeeToken)
        assertThat(friendResponse.friendId).isNotNull()
        assertThat(friendResponse.name).isEqualTo("John Doe")
        assertThat(friendResponse.photoUrl).isNull()
        assertThat(friendResponse.mainBalance).isNull()
        assertThat(friendResponse.otherBalances).isEmpty()
        johnFriendId = friendResponse.friendId
    }

    @Test
    @Order(2)
    fun `user zee query friends receives john as friend successfully`() {
        val friendResponse = queryFriend(
            token = zeeToken
        )

        assertThat(friendResponse.data.friends).hasSize(1)
        assertThat(friendResponse.data.friends[0].name).isEqualTo("John Doe")
        assertThat(friendResponse.data.friends[0].photoUrl).isNull()
        assertThat(friendResponse.data.friends[0].mainBalance).isNull()
        assertThat(friendResponse.data.friends[0].otherBalances).isEmpty()
        assertThat(friendResponse.data.friends[0].friendId).isNotNull()
        assertThat(friendResponse.data.balance.main).isNull()
        assertThat(friendResponse.data.balance.other).isEmpty()
    }

    @Order(4)
    @Test
    fun `john logs in successfully`() {
        johnToken = loginUser(userDto = johnDto).token
    }

    @Order(5)
    @Test
    fun `john find zee as his friend`() {
        val response = queryFriend(token = johnToken)

        assertThat(response.data.friends).hasSize(1)
        assertThat(response.data.friends[0].name).isEqualTo("Zeeshan Tufail")
        assertThat(response.data.friends[0].photoUrl).isNotNull()
        assertThat(response.data.friends[0].mainBalance).isNull()
        assertThat(response.data.friends[0].otherBalances).isEmpty()
        assertThat(response.data.friends[0].friendId).isNotNull()
        assertThat(response.data.balance.main).isNull()
        assertThat(response.data.balance.other).isEmpty()
    }

    @Order(6)
    @Test
    fun `zee query friends again and john  is linked successfully`() {
        val response = queryFriend(zeeToken)
        //Check that photoUrl is now available for friend
        assertThat(response.data.friends).hasSize(1)
        assertThat(response.data.friends[0].name).isEqualTo("John Doe")
        assertThat(response.data.friends[0].photoUrl).isNotNull()
        assertThat(response.data.friends[0].mainBalance).isNull()
        assertThat(response.data.friends[0].otherBalances).isEmpty()
        assertThat(response.data.friends[0].friendId).isNotNull()
        assertThat(response.data.balance.main).isNull()
        assertThat(response.data.balance.other).isEmpty()
    }

    @Order(7)
    @Test
    fun `update john friend information successfully`() {
        val existing = queryFriend(zeeToken)
        val friendRequest = UpdateFriendRequest(
            email = "johnupdated@gmail.com",
            phoneNumber = null,
            name = "John Doe Updated",
        )

        webTestClient.put()
            .uri("/api/v1/friends/${existing.data.friends[0].friendId}")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                friendRequest
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(FriendResponse::class.java)
            .returnResult().responseBody!!.also { it.prettyAndPrint(objectMapper) }

        val response = queryFriend(zeeToken)

        assertThat(response.data.friends).hasSize(1)
        assertThat(response.data.friends[0].name).isEqualTo("John Doe Updated")
        assertThat(response.data.friends[0].email).isEqualTo("johnupdated@gmail.com")
        assertThat(response.data.friends[0].phone).isEqualTo(johnDto.phoneNumber)
        assertThat(response.data.friends[0].photoUrl).isNotNull
        assertThat(response.data.friends[0].mainBalance).isNull()
        assertThat(response.data.friends[0].otherBalances).isEmpty()
        assertThat(response.data.friends[0].friendId).isEqualTo(existing.data.friends[0].friendId)
        assertThat(response.data.balance.main).isNull()
        assertThat(response.data.balance.other).isEmpty()
    }

    @Order(8)
    @Test
    fun `delete john friend successfully`() {
        webTestClient.delete()
            .uri("/api/v1/friends/$johnFriendId")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!.also { it.prettyAndPrint(objectMapper) }

        val response = queryFriend(zeeToken)

        assertThat(response.data.friends).isEmpty()
        assertThat(response.data.balance.main).isNull()
        assertThat(response.data.balance.other).isEmpty()
    }
}
