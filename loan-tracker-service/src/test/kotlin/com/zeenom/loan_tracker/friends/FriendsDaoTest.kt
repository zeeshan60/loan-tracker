package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.JacksonConfig
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.toJson
import com.zeenom.loan_tracker.users.UserDao
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEntity
import com.zeenom.loan_tracker.users.UserRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*

@DataR2dbcTest
@Import(JacksonConfig::class)
@ActiveProfiles("test")
class FriendsDaoTest(
    @Autowired private val friendRepository: FriendRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val objectMapper: ObjectMapper,
) : TestPostgresConfig() {

    private val secondInstant = SecondInstant()
    private val friendsDao = FriendsDao(
        friendRepository = friendRepository,
        userRepository = userRepository,
        secondInstant = secondInstant,
        objectMapper = objectMapper,
    )

    private val userDao = UserDao(
        userRepository = userRepository,
        secondInstant = secondInstant
    )

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

    fun cleanup() = runBlocking {
        userRepository.deleteAll()
        friendRepository.deleteAll()
    }
}

@SpringBootTest
@ActiveProfiles("local")
class FriendsDaoLocalIntegrationTest(
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val friendRepository: FriendRepository,
    @Autowired private val userRepository: UserRepository,
) {

    @Test
    @Disabled
    fun `add bunch of test friends`(): Unit = runBlocking {
        cleanup()
        val result = UserEntity(
            uid = "QlI1B6ili1SsB9ovzKharlWFvHr1",
            email = "nomantufail100@gmail.com",
            phoneNumber = null,
            displayName = "Noman Tufail",
            photoUrl = "https://lh3.googleusercontent.com/a/ACg8ocLl28eLnZCPrAzvJyKlz_UrvsCxYQu8yDSc-RQTP87SAQ1DCD9L=s96-c",
            emailVerified = true,
            createdAt = Instant.parse("2025-02-12T06:17:29.000Z"),
            updatedAt = Instant.parse("2025-02-12T17:58:25.000Z"),
            lastLoginAt = Instant.parse("2025-02-12T17:58:25.000Z")
        )
        val nomi = userRepository.save(result)

        val friendTemplate = UserFriendEntity(
            userId = nomi.id!!,
            friendId = null,
            friendEmail = "zeeshantufail86@gmail.com",
            friendPhoneNumber = "+923001234567",
            friendDisplayName = "Zeeshan Tufail",
            friendTotalAmountsDto = objectMapper.writeValueAsString(
                FriendTotalAmountsDto(
                    amountsPerCurrency = listOf(
                        AmountDto(
                            amount = 100.0.toBigDecimal(),
                            isOwed = true,
                            currency = Currency.getInstance("USD")
                        )
                    )
                )
            ).toJson(objectMapper),
            createdAt = Instant.parse("2025-02-13T01:04:14.000Z"),
            updatedAt = Instant.parse("2025-02-13T01:04:14.000Z")
        )

        (1..50).map {
            friendRepository.save(
                friendTemplate.copy(
                    friendEmail = "friend$it@gmail.com",
                    friendDisplayName = "Friend $it",
                    friendPhoneNumber = "+92300123456$it",
                    friendTotalAmountsDto = FriendTotalAmountsDto(
                        amountsPerCurrency = listOf(
                            AmountDto(
                                amount = (100.0 + it).toBigDecimal(),
                                currency = Currency.getInstance("USD"),
                                isOwed = it % 2 == 0,
                            )
                        )
                    ).toJson(objectMapper)
                )
            )
        }

    }

    private suspend fun cleanup() {
        userRepository.deleteAll()
        friendRepository.deleteAll()
    }
}