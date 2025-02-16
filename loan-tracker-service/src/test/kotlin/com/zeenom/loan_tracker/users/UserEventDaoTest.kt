package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.friends.TestPostgresConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest

@DataR2dbcTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserEventDaoTest(
    @Autowired private val userEventRepository: UserEventRepository,
) : TestPostgresConfig() {

    private val userEventDao = UserEventDao(
        userRepository = userEventRepository
    )

    @BeforeEach
    fun beforeAll(): Unit = runBlocking {
        userEventRepository.deleteAll()
    }

    @Test
    fun `adds new user event successfully`(): Unit = runBlocking {
        val userDto = createUser()

        val userEvent = userEventRepository.findAll().toList()

        assertThat(userEvent).hasSize(1)
        assertThat(userEvent[0].uid).isEqualTo(userDto.uid)
        assertThat(userEvent[0].email).isEqualTo(userDto.email)
        assertThat(userEvent[0].phoneNumber).isEqualTo(userDto.phoneNumber)
        assertThat(userEvent[0].displayName).isEqualTo(userDto.displayName)
        assertThat(userEvent[0].photoUrl).isEqualTo(userDto.photoUrl)
        assertThat(userEvent[0].emailVerified).isEqualTo(userDto.emailVerified)
    }

    private val userDto: UserDto
        get() {
            val userDto = UserDto(
                uid = "123",
                email = "user@gmail.com",
                phoneNumber = "+923001234567",
                displayName = "Test User",
                photoUrl = "https://test.com",
                emailVerified = true
            )
            return userDto
        }

    private suspend fun createUser(): UserDto {

        userEventDao.createUser(userDto)
        return userDto
    }

    @Test
    fun `adds new user should fail if user already exists`(): Unit = runBlocking {
        createUser()

        assertThatThrownBy { runBlocking { createUser() } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User already exist")
    }

    @Test
    fun `find user by id returns user successfully`(): Unit = runBlocking {
        val userDto = createUser()

        val user = userEventDao.findUserById(userDto.uid)

        assertThat(user).isNotNull
        assertThat(user!!.uid).isEqualTo(userDto.uid)
        assertThat(user.email).isEqualTo(userDto.email)
        assertThat(user.phoneNumber).isEqualTo(userDto.phoneNumber)
        assertThat(user.displayName).isEqualTo(userDto.displayName)
        assertThat(user.photoUrl).isEqualTo(userDto.photoUrl)
        assertThat(user.emailVerified).isEqualTo(userDto.emailVerified)
    }
}