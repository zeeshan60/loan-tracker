package com.zeenom.loan_tracker.groups

import com.zeenom.loan_tracker.friends.FriendEventRepository
import com.zeenom.loan_tracker.friends.FriendModelRepository
import com.zeenom.loan_tracker.integration.BaseIntegration
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserModelRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.Test

class GroupsControllerIntegrationTest : BaseIntegration() {

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
        println(zeeToken)
        queryUser(token = zeeToken).also { response ->
            zeeDto = zeeDto.copy(
                uid = UUID.fromString(response.uid),
            )
        }
    }

    @Test
    @Order(1)
    fun `create group successfully`() {


        val response = webTestClient.post()
            .uri("/api/v1/groups/create")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                GroupCreateRequest(
                    name = "Test Group",
                    description = "This is a test group",
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupResponse::class.java).returnResult().responseBody!!

        assertThat(response.id).isNotNull
        assertThat(response.members).hasSize(1)
        assertThat(response.members[0].memberId).isEqualTo(zeeDto.uid.toString())
        assertThat(response.members[0].memberName).isEqualTo(zeeDto.displayName)
        assertThat(response.members[0].userBalanceWithThisMember).isNull()
        assertThat(response.name).isEqualTo("Test Group")
        assertThat(response.description).isEqualTo("This is a test group")
        assertThat(response.balance).isNull()
    }
}