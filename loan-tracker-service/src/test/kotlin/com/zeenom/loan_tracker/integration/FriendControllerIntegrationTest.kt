package com.zeenom.loan_tracker.integration

import com.zeenom.loan_tracker.friends.CreateFriendRequest
import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendResponse
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired

class FriendControllerAddFriendWithoutEmailFailIntegrationTest(): BaseIntegration() {

    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    private lateinit var zeeToken: String
    private var zeeDto = UserDto(
        uid = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        currency = null,
        emailVerified = true
    )

    private lateinit var johnToken: String
    private var johnDto = UserDto(
        uid = "124",
        email = "john@gmail.com",
        phoneNumber = "+923001234568",
        displayName = "John Doe",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
        currency = null,
        emailVerified = true
    )

    @BeforeAll
    fun beforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
    }


    @Test
    @Order(1)
    fun `user zee adds a friend john successfully`() {

        val friendResponse = webTestClient.post()
            .uri("/api/v1/friends/add")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                CreateFriendRequest(
                    name = johnDto.displayName,
                    email = null,
                    phoneNumber = "+923001234568",
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(FriendResponse::class.java).returnResult().responseBody!!
        assertThat(friendResponse.friendId).isNotNull()
        assertThat(friendResponse.name).isEqualTo("John Doe")
        assertThat(friendResponse.email).isNull()
        assertThat(friendResponse.phone).isEqualTo("+923001234568")
        assertThat(friendResponse.photoUrl).isNull()
        assertThat(friendResponse.mainBalance).isNull()
        assertThat(friendResponse.otherBalances).isEmpty()
    }
}

class FriendControllerIntegrationTest() : BaseIntegration() {

    @Autowired
    private lateinit var friendEventRepository: FriendEventRepository

    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    private lateinit var zeeToken: String
    private var zeeDto = UserDto(
        uid = "123",
        email = "zee@gmail.com",
        phoneNumber = "+923001234567",
        displayName = "Zeeshan Tufail",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
        currency = null,
        emailVerified = true
    )

    private lateinit var johnToken: String
    private var johnDto = UserDto(
        uid = "124",
        email = "john@gmail.com",
        phoneNumber = "+923001234568",
        displayName = "John Doe",
        photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
        currency = null,
        emailVerified = true
    )

    @BeforeAll
    fun beforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
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
}
