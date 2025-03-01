package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.controllers.BaseIntegration
import com.zeenom.loan_tracker.friends.UpdateUserRequest
import com.zeenom.loan_tracker.friends.UserResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.body

class UsersControllerIntegrationTest(@LocalServerPort private val port: Int):BaseIntegration(port) {

    @Autowired
    private lateinit var userEventRepository: UserEventRepository
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
        userEventRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        addFriend(zeeToken, "john")
    }

    @Order(1)
    @Test
    fun `update user successfully`(): Unit = runBlocking {
        val userRequest = UpdateUserRequest(
            displayName = "Zeeshan Tufail",
            phoneNumber = "+923001234567",
            currency = "USD"
        )
        val userResponse = webTestClient.put()
            .uri("/api/v1/users")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(userRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult().responseBody!!

        assert(userResponse.displayName == userRequest.displayName)
        assert(userResponse.phoneNumber == userRequest.phoneNumber)
        assert(userResponse.currency == userRequest.currency)
    }

}