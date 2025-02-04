package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.users.UserDao
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.*

@SpringBootTest
@ActiveProfiles("local")
class FriendsDaoTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var friendsDao: FriendsDao

    @Test
    fun `save friend also creates a friend if none exists`(): Unit = runBlocking {
        userRepository.deleteAll().awaitSingleOrNull()
        userDao.createUser(
            UserDto(
                uid = "123",
                email = "sample@gmail.com",
                phoneNumber = "+923001234567",
                displayName = "Zeeshan Tufail",
                photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                emailVerified = true
            )
        )
        val friendDto = FriendDto(
            userId = null,
            photoUrl = null,
            name = "John Doe",
            email = "friend@gmail.com",
            phoneNumber = null,
            loanAmount = AmountDto(
                amount = 100.0.toBigDecimal(),
                currency = Currency.getInstance("USD"),
                isOwed = true
            ),
        )

        friendsDao.saveFriend("123", friendDto)
        val friendsDto = friendsDao.findAllByUserId("123")
        assertThat(friendsDto.friends).hasSize(1)
        assertThat(friendsDto.friends[0]).isEqualTo(friendDto)
    }

    @Test
    fun `throws error if none of the userId, email, or phoneNumber provided for friend`(): Unit = runBlocking {

        userRepository.deleteAll().awaitSingleOrNull()
        userDao.createUser(
            UserDto(
                uid = "123",
                email = "example@gmail.com",
                phoneNumber = "+1234567890",
                displayName = "John Doe",
                photoUrl = "https://example.com/photo.jpg",
                emailVerified = true
            )
        )
        val friendDto = FriendDto(
            userId = null,
            photoUrl = null,
            name = "John Doe",
            email = null,
            phoneNumber = null,
            loanAmount = AmountDto(
                amount = 100.0.toBigDecimal(),
                currency = Currency.getInstance("USD"),
                isOwed = true
            ),
        )

        assertThatThrownBy {
            runBlocking { friendsDao.saveFriend("123", friendDto) }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("At least one of userId, email or phoneNumber must be provided")
    }
}