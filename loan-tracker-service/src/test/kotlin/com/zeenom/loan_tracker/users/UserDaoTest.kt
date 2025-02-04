package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.common.SecondInstant
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class UserDaoTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var secondInstant: SecondInstant

    @Autowired
    lateinit var userDao: UserDao

    @Test
    fun `save user and read user successfully`(): Unit = runBlocking {
        val uid = "CMWL0tapZGSDOI3TbzQEM8vblTl2"
        userRepository.deleteAllByUid(uid).awaitSingleOrNull()
        val userDto = UserDto(
            uid = uid,
            email = "example@gmail.com",
            phoneNumber = "+1234567890",
            displayName = "John Doe",
            photoUrl = "https://example.com/photo.jpg",
            emailVerified = true
        )

        userDao.createUser(userDto)
        val user = userDao.findUserById(uid)
        assertThat(user).isEqualTo(userDto)

        val entity = userRepository.findByUid(uid).awaitSingle()
        assertThat(entity.createdAt).isBeforeOrEqualTo(secondInstant.now())
        assertThat(entity.updatedAt).isBeforeOrEqualTo(secondInstant.now())
        assertThat(entity.lastLoginAt).isNull()
    }

    @Test
    fun `login user and read user successfully`(): Unit = runBlocking {
        val uid = "CMWL0tapZGSDOI3TbzQEM8vblTl2"
        userRepository.deleteAllByUid(uid).awaitSingleOrNull()
        val userDto = UserDto(
            uid = uid,
            email = "example@gmail.com",
            phoneNumber = "+1234567890",
            displayName = "John Doe",
            photoUrl = "https://example.com/photo.jpg",
            emailVerified = true
        )

        userDao.loginUser(userDto)
        val user = userDao.findUserById(uid)
        assertThat(user).isEqualTo(userDto)

        val entity = userRepository.findByUid(uid).awaitSingle()
        assertThat(entity.createdAt).isBeforeOrEqualTo(secondInstant.now())
        assertThat(entity.updatedAt).isBeforeOrEqualTo(secondInstant.now())
        assertThat(entity.lastLoginAt).isBeforeOrEqualTo(secondInstant.now())
    }
}