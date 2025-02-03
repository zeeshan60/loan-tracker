package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.common.SecondInstant
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class UserDaoTest {

    @Autowired
    private lateinit var secondInstant: SecondInstant

    @Autowired
    lateinit var userDao: UserDao

    @Test
    fun `save user and read user successfully`(): Unit = runBlocking {
        val uid = "CMWL0tapZGSDOI3TbzQEM8vblTl2"
        userDao.deleteUserById(uid)
        val userDto = UserDto(
            uid = uid,
            email = "example@gmail.com",
            displayName = "John Doe",
            photoUrl = "https://example.com/photo.jpg",
            emailVerified = true,
            updatedAt = secondInstant.now()
        )

        userDao.createUser(userDto)
        val user = userDao.findUserById(uid)
        Assertions.assertThat(user).isEqualTo(userDto)
    }
}