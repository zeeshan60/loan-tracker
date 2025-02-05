package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.friends.CreateFriendDto
import com.zeenom.loan_tracker.friends.FriendDto
import com.zeenom.loan_tracker.friends.FriendsDao
import com.zeenom.loan_tracker.test_configs.TestSecondInstantConfig
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
@Import(TestSecondInstantConfig::class)
class UserDaoTest {

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

    @Test
    fun `make all owners of this user his friends`(): Unit = runBlocking {
        userRepository.deleteAll().awaitSingleOrNull()
        val userDto = UserDto(
            uid = "123",
            email = "sample@gmail.com",
            phoneNumber = "+923001234567",
            displayName = "Zeeshan Tufail",
            photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
            emailVerified = true
        )

        val owner1 = UserDto(
            uid = "124",
            email = "owner1@gmail.com",
            phoneNumber = "+923001234568",
            displayName = "Owner 1",
            photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl3",
            emailVerified = true
        )

        userDao.createUser(owner1)

        friendsDao.saveFriend(
            owner1.uid,
            CreateFriendDto(name = userDto.displayName, email = userDto.email, phoneNumber = userDto.phoneNumber)
        )

        val owner1FriendsDto = friendsDao.findAllByUserId(owner1.uid)
        assertThat(owner1FriendsDto.friends).hasSize(1)
        assertThat(owner1FriendsDto.friends[0]).isEqualTo(
            FriendDto(
                photoUrl = null,
                name = userDto.displayName,
                email = userDto.email,
                phoneNumber = userDto.phoneNumber,
                loanAmount = null,
            )
        )

        val owner2 = UserDto(
            uid = "125",
            email = "owner2@gmail.com",
            phoneNumber = "+923001234569",
            displayName = "Owner 2",
            photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl4",
            emailVerified = true
        )

        userDao.createUser(owner2)
        friendsDao.saveFriend(
            owner2.uid,
            CreateFriendDto(name = userDto.displayName, email = userDto.email, phoneNumber = userDto.phoneNumber)
        )

        val owner2FriendsDto = friendsDao.findAllByUserId(owner2.uid)
        assertThat(owner2FriendsDto.friends).hasSize(1)
        assertThat(owner2FriendsDto.friends[0]).isEqualTo(
            FriendDto(
                photoUrl = null,
                name = userDto.displayName,
                email = userDto.email,
                phoneNumber = userDto.phoneNumber,
                loanAmount = null,
            )
        )

        userDao.createUser(userDto)
        friendsDao.makeMyOwnersMyFriends(userDto.uid)

        val friendsDto = friendsDao.findAllByUserId(userDto.uid)

        assertThat(friendsDto.friends).hasSize(2)
        assertThat(friendsDto.friends[0]).isEqualTo(
            FriendDto(
                photoUrl = owner1.photoUrl,
                name = owner1.displayName,
                email = owner1.email,
                phoneNumber = owner1.phoneNumber,
                loanAmount = null,
            )
        )
        assertThat(friendsDto.friends[1]).isEqualTo(
            FriendDto(
                photoUrl = owner2.photoUrl,
                name = owner2.displayName,
                email = owner2.email,
                phoneNumber = owner2.phoneNumber,
                loanAmount = null,
            )
        )
    }
}