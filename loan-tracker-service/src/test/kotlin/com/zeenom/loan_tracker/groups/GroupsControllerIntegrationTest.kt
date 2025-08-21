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

    @Autowired
    private lateinit var groupModelRepository: GroupModelRepository

    @Autowired
    private lateinit var groupEventRepository: GroupEventRepository

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

    private lateinit var groupId: UUID

    @BeforeAll
    fun beforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
        friendEventRepository.deleteAll()
        userModelRepository.deleteAll()
        friendModelRepository.deleteAll()
        groupModelRepository.deleteAll()
        groupEventRepository.deleteAll()
        zeeToken = loginUser(
            userDto = zeeDto
        ).token
        println(zeeToken)
        queryUser(token = zeeToken).also { response ->
            zeeDto = zeeDto.copy(
                uid = UUID.fromString(response.uid),
            )
        }
        johnToken = loginUser(
            userDto = johnDto
        ).token
        queryUser(token = johnToken).also { response ->
            johnDto = johnDto.copy(
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
        assertThat(response.members[0].memberId).isEqualTo(zeeDto.uid)
        assertThat(response.members[0].memberName).isEqualTo(zeeDto.displayName)
        assertThat(response.members[0].userBalanceWithThisMember).isNull()
        assertThat(response.name).isEqualTo("Test Group")
        assertThat(response.description).isEqualTo("This is a test group")
        assertThat(response.balance).isNull()
        groupId = response.id
    }

    @Test
    @Order(2)
    fun `add members to group successfully`() {
        val groupResponse = webTestClient.put()
            .uri("/api/v1/groups/$groupId/addMembers")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                GroupAddMembersRequest(
                    memberIds = listOf(johnDto.uid!!)
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupResponse::class.java).returnResult().responseBody!!

        assertThat(groupResponse.members).hasSize(2)
        assertThat(groupResponse.members[1].memberId).isEqualTo(johnDto.uid)
        assertThat(groupResponse.members[1].memberName).isEqualTo(johnDto.displayName)
    }

    @Test
    @Order(3)
    fun `remove group members successfully`() {

        val groupResponse = webTestClient.put()
            .uri("/api/v1/groups/$groupId/removeMembers")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                GroupRemoveMembersRequest(
                    memberIds = listOf(johnDto.uid!!)
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupResponse::class.java).returnResult().responseBody!!

        assertThat(groupResponse.members).hasSize(1)
        assertThat(groupResponse.members[0].memberId).isEqualTo(zeeDto.uid)
        assertThat(groupResponse.members[0].memberName).isEqualTo(zeeDto.displayName)
    }

    @Test
    @Order(4)
    fun `update group successfully`() {
        val updatedGroupResponse = webTestClient.put()
            .uri("/api/v1/groups/$groupId/update")
            .header("Authorization", "Bearer $zeeToken")
            .bodyValue(
                GroupCreateRequest(
                    name = "Updated Test Group",
                    description = "This is an updated test group",
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupResponse::class.java).returnResult().responseBody!!

        assertThat(updatedGroupResponse.id).isEqualTo(groupId)
        assertThat(updatedGroupResponse.name).isEqualTo("Updated Test Group")
        assertThat(updatedGroupResponse.description).isEqualTo("This is an updated test group")
        assertThat(updatedGroupResponse.members).hasSize(1)
        assertThat(updatedGroupResponse.members[0].memberId).isEqualTo(zeeDto.uid)
        assertThat(updatedGroupResponse.members[0].memberName).isEqualTo(zeeDto.displayName)
        assertThat(updatedGroupResponse.balance).isNull()
    }

    @Test
    @Order(5)
    fun `query group by id successfully`() {
        val groupResponse = webTestClient.get()
            .uri("/api/v1/groups/$groupId")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupResponse::class.java).returnResult().responseBody!!

        assertThat(groupResponse.id).isEqualTo(groupId)
        assertThat(groupResponse.name).isEqualTo("Updated Test Group")
        assertThat(groupResponse.description).isEqualTo("This is an updated test group")
        assertThat(groupResponse.members).hasSize(1)
        assertThat(groupResponse.members[0].memberId).isEqualTo(zeeDto.uid)
        assertThat(groupResponse.members[0].memberName).isEqualTo(zeeDto.displayName)
        assertThat(groupResponse.balance).isNull()
    }

    @Test
    @Order(6)
    fun `delete group and than get groups should return 404`() {
        webTestClient.delete()
            .uri("/api/v1/groups/$groupId/delete")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isOk
        webTestClient.get()
            .uri("/api/v1/groups/$groupId")
            .header("Authorization", "Bearer $zeeToken")
            .exchange()
            .expectStatus().isNotFound
    }
}