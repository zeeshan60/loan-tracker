package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.friends.TestPostgresConfig
import com.zeenom.loan_tracker.friends.UserUpdateDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import java.util.*

@DataR2dbcTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest(
    @Autowired private val userEventRepository: UserEventRepository,
    @Autowired private val userModelRepository: UserModelRepository,
) : TestPostgresConfig()  {

    private val userEventHandler = UserEventHandler(
        userRepository = userEventRepository,
        userModelRepository = userModelRepository
    )

    private val userService = UserService(
        userEventHandler = userEventHandler
    )

    @BeforeEach
    fun beforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
        userModelRepository.deleteAll()
    }

    @Test
    fun `adds new user event successfully`(): Unit = runBlocking {
        val userDto = saveEvent(userDto = userDto)

        val userEvent = userEventRepository.findAll().toList()

        assertThat(userEvent).hasSize(1)
        assertThat(userEvent[0].uid).isEqualTo(userDto.userFBId)
        assertThat(userEvent[0].email).isEqualTo(userDto.email)
        assertThat(userEvent[0].phoneNumber).isEqualTo(userDto.phoneNumber)
        assertThat(userEvent[0].displayName).isEqualTo(userDto.displayName)
        assertThat(userEvent[0].photoUrl).isEqualTo(userDto.photoUrl)
        assertThat(userEvent[0].emailVerified).isEqualTo(userDto.emailVerified)
    }

    val userId = UUID.randomUUID()
    val userId2 = UUID.randomUUID()
    private val userDto: UserDto
        get() {
            val userDto = UserDto(
                uid = userId,
                userFBId = "123",
                email = "user@gmail.com",
                phoneNumber = "+923001234567",
                displayName = "Test User",
                photoUrl = "https://test.com",
                currency = null,
                emailVerified = true
            )
            return userDto
        }

    private suspend fun saveEvent(userDto: UserDto): UserDto {

        userService.createUser(userDto = userDto)
        return userDto
    }

    @Test
    fun `adds new user with same uid should fail as user already exists`(): Unit = runBlocking {
        saveEvent(userDto = userDto)

        assertThatThrownBy {
            runBlocking {
                saveEvent(
                    userDto = UserDto(
                        uid = userId,
                        userFBId = "123",
                        email = "user1@gmail.com",
                        phoneNumber = "+923001234568",
                        displayName = "Test User",
                        photoUrl = "https://test.com",
                        currency = null,
                        emailVerified = true
                    )
                )
            }
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("User with this unique identifier already exist")
    }

    @Test
    fun `adds new user with same email and phone should fail as user already exists`(): Unit = runBlocking {
        saveEvent(userDto = userDto)

        assertThatThrownBy { runBlocking { saveEvent(userDto = userDto) } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User with email ${userDto.email} or phone number ${userDto.phoneNumber} already exist")
    }

    @Test
    fun `find user by id returns user successfully`(): Unit = runBlocking {
        val userDto = saveEvent(userDto = userDto)

        val user = userEventHandler.findByUserId(userModelRepository.findAll().first().streamId)

        assertThat(user).isNotNull
        assertThat(user!!.uid).isEqualTo(userDto.uid)
        assertThat(user.email).isEqualTo(userDto.email)
        assertThat(user.phoneNumber).isEqualTo(userDto.phoneNumber)
        assertThat(user.displayName).isEqualTo(userDto.displayName)
        assertThat(user.photoUrl).isEqualTo(userDto.photoUrl)
        assertThat(user.emailVerified).isEqualTo(userDto.emailVerified)
    }

    @Test
    fun `find multiple users using uids successfully`(): Unit = runBlocking {
        saveEvent(userDto = userDto)
        val userDto2 = userDto.copy(uid = userId2, userFBId = "124", email = "user2@gmail.com", phoneNumber = "+923001234568")
        saveEvent(userDto = userDto2)

        val users =
            userEventHandler.findUsersByUids(userModelRepository.findAll().map { it.streamId }.toList()).toList()

        assertThat(users).hasSize(2)
        val user = users[0]
        assertThat(user.uid).isEqualTo(userDto.uid)
        assertThat(user.email).isEqualTo(userDto.email)
        assertThat(user.phoneNumber).isEqualTo(userDto.phoneNumber)
        assertThat(user.displayName).isEqualTo(userDto.displayName)
        assertThat(user.photoUrl).isEqualTo(userDto.photoUrl)
        assertThat(user.emailVerified).isEqualTo(userDto.emailVerified)

        val user2 = users[1]
        assertThat(user2.uid).isEqualTo(userDto2.uid)
        assertThat(user2.email).isEqualTo(userDto2.email)
        assertThat(user2.phoneNumber).isEqualTo(userDto2.phoneNumber)
        assertThat(user2.displayName).isEqualTo(userDto2.displayName)
        assertThat(user2.photoUrl).isEqualTo(userDto2.photoUrl)
        assertThat(user2.emailVerified).isEqualTo(userDto2.emailVerified)
    }

    @Test
    fun `update user with currency successfully`(): Unit = runBlocking {
        saveEvent(userDto = userDto)

        userService.updateUser(
            UserUpdateDto(
                uid = userDto.uid!!,
                currency = "PKR",
                displayName = userDto.displayName,
                phoneNumber = userDto.phoneNumber,
            )
        )

        userService.findUserById(userDto.uid!!)!!.let {
            assertThat(it.currency).isEqualTo("PKR")
        }
    }
}