package com.zeenom.loan_tracker.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.zeenom.loan_tracker.firebase.FirebaseService
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendModelRepository
import com.zeenom.loan_tracker.friends.FriendService
import com.zeenom.loan_tracker.transactions.SplitType
import com.zeenom.loan_tracker.transactions.TransactionCreateRequest
import com.zeenom.loan_tracker.transactions.TransactionsResponse
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserModelRepository
import com.zeenom.loan_tracker.users.UserService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Instant
import java.util.UUID

// Only for local testing, not for CI/CD
@Disabled
@SpringBootTest
@ActiveProfiles("local")
class GetFriendsTest{
    @Autowired
    private lateinit var friendService: FriendService

    @Autowired
    private lateinit var userService: UserService

    @Test
    fun `get friends by user id`(): Unit = runBlocking {
        val userId = userService.findByUserEmailOrPhoneNumber("zeeshantufail86@gmail.com", null)!!
        val friends = friendService.findAllByUserId(userId.uid!!)
        Assertions.assertThat(friends).isNotNull
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
    fun `running the documented usecase`(): Unit = runBlocking {
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
        Assertions.assertThat(johnFriendResponse.data.friends).hasSize(1)
        Assertions.assertThat(johnFriendResponse.data.friends[0].name).isEqualTo(zeeDto.displayName)
        // 6. Check that user 2 has cross transaction with user 1
        val transactions = getTransactions(token = johnToken).perMonth.first().transactions
        Assertions.assertThat(transactions).hasSize(1)
        // 7. Delete user 2 account
        deleteUser(token = johnToken)
        delay(100) // Wait millis for async deletion to complete
        // 8. Check that user 1 still has the friend and transaction
        val zeeFriendResponse = queryFriend(token = zeeToken)
        Assertions.assertThat(zeeFriendResponse.data.friends).hasSize(1)
        Assertions.assertThat(zeeFriendResponse.data.friends[0].name).isEqualTo(johnDto.displayName)
        val zeeTransactions = getTransactions(token = zeeToken).perMonth.first().transactions
        Assertions.assertThat(zeeTransactions).hasSize(1)
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
        Assertions.assertThat(reAddedJohnFriendResponse.data.friends).hasSize(1)
        Assertions.assertThat(reAddedJohnFriendResponse.data.friends[0].name).isEqualTo(zeeDto.displayName)
        val reAddedJohnTransactions = getTransactions(token = johnToken).perMonth.first().transactions
        Assertions.assertThat(reAddedJohnTransactions).hasSize(1)
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