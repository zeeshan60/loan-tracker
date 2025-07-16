package com.zeenom.loan_tracker.integration

import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendModelRepository
import com.zeenom.loan_tracker.friends.FriendRequest
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserModelRepository
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

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
            .value(CoreMatchers.containsString("Invalid email format"))
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