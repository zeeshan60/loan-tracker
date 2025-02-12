package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.test_configs.TestSecondInstantConfig
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
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestSecondInstantConfig::class)
@ActiveProfiles("local")
class FriendsDaoTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var friendRepository: FriendRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var friendsDao: FriendsDao

    @Test
    fun `save friend adds a friend info in user friends even if friend itself dont exists`(): Unit = runBlocking {
        cleanup()
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
        val friendDto = CreateFriendDto(
            name = "John Doe",
            email = "friend@gmail.com",
            phoneNumber = null
        )

        friendsDao.saveFriend("123", friendDto)
        val friendsDto = friendsDao.findAllByUserId("123")
        assertThat(friendsDto.friends).hasSize(1)
        assertThat(friendsDto.friends[0]).isEqualTo(
            FriendDto(
                photoUrl = null,
                name = "John Doe",
                email = "friend@gmail.com",
                phoneNumber = null,
                loanAmount = null
            )
        )
    }


    @Test
    fun `save friend adds a friend info in user friends when friend exists and uses friends photo url also makes user his friend`(): Unit =
        runBlocking {
            cleanup()
            userDao.createUser(
                UserDto(
                    uid = "123",
                    email = "sample@gmail.com",
                    phoneNumber = null,
                    displayName = "Zeeshan Tufail",
                    photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                    emailVerified = true
                )
            )
            userDao.createUser(
                UserDto(
                    uid = "124",
                    email = null,
                    phoneNumber = "+923001234567",
                    displayName = "Noman Tufail",
                    photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
                    emailVerified = true
                )
            )
            val friendDto = CreateFriendDto(
                name = "Noman pola",
                email = null,
                phoneNumber = "+923001234567"
            )

            friendsDao.saveFriend("123", friendDto)
            val friendsDto = friendsDao.findAllByUserId("123")
            assertThat(friendsDto.friends).hasSize(1)
            assertThat(friendsDto.friends[0]).isEqualTo(
                FriendDto(
                    photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
                    name = "Noman pola",
                    email = null,
                    phoneNumber = "+923001234567",
                    loanAmount = null
                )
            )

            val friendsDto2 = friendsDao.findAllByUserId("124")
            assertThat(friendsDto2.friends).hasSize(1)
            assertThat(friendsDto2.friends[0]).isEqualTo(
                FriendDto(
                    photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                    name = "Zeeshan Tufail",
                    email = "sample@gmail.com",
                    phoneNumber = null,
                    loanAmount = null
                )
            )
        }

    @Test
    fun `throws error if none of the userId, email, or phoneNumber provided for friend`(): Unit = runBlocking {

        cleanup()
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
        val friendDto = CreateFriendDto(
            name = "John Doe",
            email = null,
            phoneNumber = null
        )

        assertThatThrownBy {
            runBlocking { friendsDao.saveFriend("123", friendDto) }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("At least one of userId, email or phoneNumber must be provided")
    }

    @Test
    fun `user has two friends one is existing and one is not should return both friends with photo url of existing one`(): Unit =
        runBlocking {
            cleanup()
            userDao.createUser(
                UserDto(
                    uid = "123",
                    email = "sample@gmail.com",
                    phoneNumber = null,
                    displayName = "Zeeshan Tufail",
                    photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
                    emailVerified = true
                )
            )
            userDao.createUser(
                UserDto(
                    uid = "124",
                    email = null,
                    phoneNumber = "+923001234567",
                    displayName = "Noman Tufail",
                    photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
                    emailVerified = true
                )
            )

            friendsDao.saveFriend(
                "123", CreateFriendDto(
                    name = "Noman pola",
                    email = null,
                    phoneNumber = "+923001234567"
                )
            )

            friendsDao.saveFriend(
                "123", CreateFriendDto(
                    name = "Noman 3",
                    email = null,
                    phoneNumber = "+923001234568"
                )
            )

            val friendsDto = friendsDao.findAllByUserId("123")
            assertThat(friendsDto.friends).hasSize(2)
            assertThat(friendsDto.friends[0]).isEqualTo(
                FriendDto(
                    photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
                    name = "Noman pola",
                    email = null,
                    phoneNumber = "+923001234567",
                    loanAmount = null,
                )
            )
            assertThat(friendsDto.friends[1]).isEqualTo(
                FriendDto(
                    photoUrl = null,
                    name = "Noman 3",
                    email = null,
                    phoneNumber = "+923001234568",
                    loanAmount = null,
                )
            )
        }

    private suspend fun cleanup() {
        userRepository.deleteAll()
        friendRepository.deleteAll()
    }
}