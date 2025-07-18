package com.zeenom.loan_tracker.integration

import com.zeenom.loan_tracker.friends.*
import com.zeenom.loan_tracker.prettyAndPrint
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserModelRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

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
