package com.zeenom.loan_tracker.users

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.toJson
import com.zeenom.loan_tracker.friends.*
import com.zeenom.loan_tracker.test_configs.TestSecondInstantConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.*

@SpringBootTest
@ActiveProfiles("local")
@Import(TestSecondInstantConfig::class)
class UserDaoTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var friendRepository: FriendRepository

    @Autowired
    private lateinit var friendsDao: FriendsDao

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

    @Nested
    inner class `Making owners friends` {

        @Test
        fun `make all owners of this user his friends`(): Unit = runBlocking {
            userRepository.deleteAll().awaitSingleOrNull()
            val owner1 = getOwner1()
            userDao.createUser(owner1)
            val owner2 = getOwner2()
            userDao.createUser(owner2)

            val userDto = userDto()
            addUserAsFriendToOwner(owner1, userDto)
            addUserAsFriendToOwner(owner2, userDto)

            //Force add some amount to check if it shows up in friends account
            addSomeLoanAmountToThisFriend(userDto.phoneNumber!!)

            assertOwner1HasAFriend(owner1, userDto)
            assertOwner2HasAFriend(owner2, userDto)

            userDao.createUser(userDto)
            assertUserHasNoFriendsYet(userDto)
            friendsDao.makeMyOwnersMyFriends(userDto.uid)

            val friendsDto = friendsDao.findAllByUserId(userDto.uid)

            assertThat(friendsDto.friends).hasSize(2)
            assertThat(friendsDto.friends[0]).isEqualTo(
                FriendDto(
                    photoUrl = owner1.photoUrl,
                    name = owner1.displayName,
                    email = owner1.email,
                    phoneNumber = owner1.phoneNumber,
                    loanAmount = AmountDto(
                        100.0.toBigDecimal(),
                        Currency.getInstance("USD"),
                        false
                    )
                )
            )
            assertThat(friendsDto.friends[1]).isEqualTo(
                FriendDto(
                    photoUrl = owner2.photoUrl,
                    name = owner2.displayName,
                    email = owner2.email,
                    phoneNumber = owner2.phoneNumber,
                    loanAmount = AmountDto(
                        100.0.toBigDecimal(),
                        Currency.getInstance("USD"),
                        false
                    ),
                )
            )
        }

        private suspend fun assertUserHasNoFriendsYet(userDto: UserDto) {
            friendsDao.findAllByUserId(userDto.uid).let {
                assertThat(it.friends).isEmpty()
            }
        }

        private suspend fun addUserAsFriendToOwner(
            owner1: UserDto,
            userDto: UserDto
        ) {
            friendsDao.saveFriend(
                owner1.uid,
                CreateFriendDto(name = userDto.displayName, email = userDto.email, phoneNumber = userDto.phoneNumber)
            )
        }

        private suspend fun assertOwner2HasAFriend(
            owner2: UserDto,
            userDto: UserDto
        ) {
            val owner2FriendsDto = friendsDao.findAllByUserId(owner2.uid)
            assertThat(owner2FriendsDto.friends).hasSize(1)
            assertThat(owner2FriendsDto.friends[0]).isEqualTo(
                FriendDto(
                    photoUrl = null,
                    name = userDto.displayName,
                    email = userDto.email,
                    phoneNumber = userDto.phoneNumber,
                    loanAmount = AmountDto(
                        100.0.toBigDecimal(),
                        Currency.getInstance("USD"),
                        true
                    )
                ),
            )
        }

        private fun getOwner2() = UserDto(
            uid = "125",
            email = "owner2@gmail.com",
            phoneNumber = "+923001234569",
            displayName = "Owner 2",
            photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl4",
            emailVerified = true
        )

        private suspend fun assertOwner1HasAFriend(
            owner1: UserDto,
            userDto: UserDto
        ) {
            val owner1FriendsDto = friendsDao.findAllByUserId(owner1.uid)
            assertThat(owner1FriendsDto.friends).hasSize(1)
            assertThat(owner1FriendsDto.friends[0]).isEqualTo(
                FriendDto(
                    photoUrl = null,
                    name = userDto.displayName,
                    email = userDto.email,
                    phoneNumber = userDto.phoneNumber,
                    loanAmount = AmountDto(
                        100.0.toBigDecimal(),
                        Currency.getInstance("USD"),
                        true
                    )
                )
            )
        }

        private suspend fun addSomeLoanAmountToThisFriend(phoneNumber: String) {
            val entities =
                friendRepository.findAll().toList().filter { it.friendPhoneNumber == phoneNumber }.toList()
            entities.forEach { friendEntity ->
                friendRepository.save(
                    friendEntity.copy(
                        friendTotalAmountsDto = FriendTotalAmountsDto(
                            amountsPerCurrency = listOf(
                                AmountDto(
                                    100.0.toBigDecimal(),
                                    Currency.getInstance("USD"),
                                    true
                                )
                            )
                        ).toJson(objectMapper = objectMapper)
                    )
                )
            }
        }

        private fun getOwner1() = UserDto(
            uid = "124",
            email = "owner1@gmail.com",
            phoneNumber = "+923001234568",
            displayName = "Owner 1",
            photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
            emailVerified = true
        )

        private fun userDto() = UserDto(
            uid = "123",
            email = "sample@gmail.com",
            phoneNumber = "+923001234567",
            displayName = "Zeeshan Tufail",
            photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
            emailVerified = true
        )
    }

}