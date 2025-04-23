package com.zeenom.loan_tracker.integration

import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendModelRepository
import com.zeenom.loan_tracker.friends.UpdateUserRequest
import com.zeenom.loan_tracker.friends.UserResponse
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserModelRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.http.HttpHeaders

class UsersControllerIntegrationTest(@LocalServerPort private val port: Int) : BaseIntegration() {

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var transactionEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userModelRepository: UserModelRepository

    @Autowired
    private lateinit var friendModelRepository: FriendModelRepository

    private var zeeDto = UserDto(
        uid = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        currency = null,
        emailVerified = true
    )
    private lateinit var zeeToken: String

    @BeforeAll
    fun setupBeforeAll(): Unit = runBlocking {
        userModelRepository.deleteAll()
        friendModelRepository.deleteAll()
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        transactionEventRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        addFriend(zeeToken, "john")
    }

    @Order(1)
    @Test
    fun `update user successfully`(): Unit = runBlocking {
        val userRequest = UpdateUserRequest(
            displayName = null,
            phoneNumber = "+923001234568",
            currency = "USD"
        )
        webTestClient.put()
            .uri("/api/v1/users")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(userRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult().responseBody!!

        val userResponse = webTestClient.get()
            .uri("/api/v1/users")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult().responseBody!!

        assertThat(userResponse.displayName).isEqualTo(zeeDto.displayName)
        assertThat(userResponse.phoneNumber).isEqualTo(userRequest.phoneNumber)
        assertThat(userResponse.currency).isEqualTo(userRequest.currency)
    }

}