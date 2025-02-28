package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.currencyRateMap
import com.zeenom.loan_tracker.transactions.AmountDto
import com.zeenom.loan_tracker.transactions.CurrencyResponse
import com.zeenom.loan_tracker.transactions.ICurrencyClient
import com.zeenom.loan_tracker.transactions.TransactionEventHandler
import com.zeenom.loan_tracker.users.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import java.time.Instant
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DataR2dbcTest
class FriendServiceTest(
    @Autowired private val eventRepository: FriendEventRepository,
    @Autowired private val userEventRepository: UserEventRepository,
) : TestPostgresConfig() {

    private val userEventHandler = mock<UserEventHandler>()
    private val transactionEventHandler = mock<TransactionEventHandler>()
    private val friendsEventHandler =
        FriendsEventHandler(
            eventRepository = eventRepository
        )
    private val currencyClient: ICurrencyClient = mock {
        onBlocking { fetchCurrencies() } doReturn CurrencyResponse(
            rates = currencyRateMap
        )
    }
    private val friendService = FriendService(
        userEventHandler = userEventHandler,
        transactionEventHandler = transactionEventHandler,
        friendsEventHandler = friendsEventHandler,
        friendFinderStrategy = FriendFinderStrategy(friendsEventHandler, userEventHandler),
        allTimeBalanceStrategy = AllTimeBalanceStrategy(),
        currencyClient = currencyClient
    )


    @BeforeEach
    fun setUp(): Unit = runBlocking {
        eventRepository.deleteAll()
        doReturn(emptyMap<UUID, Map<String, AmountDto>>()).whenever(transactionEventHandler)
            .balancesOfFriendsByCurrency(any(), any())
    }

    @Test
    fun `saves friend event successfully`(): Unit = runBlocking {
        friendsEventHandler.saveFriend(
            uid = "123",
            friendDto = CreateFriendDto(
                name = "John Doe",
                email = "user@gmail.com",
                phoneNumber = "+923001234567"
            )
        )

        val events = eventRepository.findAll().toList()
        assertThat(events).hasSize(1)
        assertThat(events[0].userUid).isEqualTo("123")
        assertThat(events[0].friendDisplayName).isEqualTo("John Doe")
        assertThat(events[0].friendEmail).isEqualTo("user@gmail.com")
        assertThat(events[0].friendPhoneNumber).isEqualTo("+923001234567")
    }

    @Test
    fun `make my owners my friends successfully`(): Unit = runBlocking {

        doReturn(
            UserDto(
                uid = "123",
                email = "user1@gmail.com",
                phoneNumber = "+923001234568",
                displayName = "User 1",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventHandler).findUserById("123")
        doReturn(
            UserDto(
                uid = "124",
                email = "user124@gmail.com",
                phoneNumber = "+923001234124",
                displayName = "User 1",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventHandler).findUserById("124")
        doReturn(
            UserDto(
                uid = "125",
                email = "user125@gmail.com",
                phoneNumber = "+923001234125",
                displayName = "User 1",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventHandler).findUserById("125")

        doReturn(
            listOf(
                UserDto(
                    uid = "124",
                    email = "user2@gmail.com",
                    phoneNumber = "+923001234569",
                    displayName = "User 2",
                    photoUrl = "https://test.com",
                    emailVerified = true
                ),
                UserDto(
                    uid = "125",
                    email = "user3@gmail.com",
                    phoneNumber = "+923001234570",
                    displayName = "User 3",
                    photoUrl = "https://test.com",
                    emailVerified = true
                )
            ).asFlow()
        ).`when`(userEventHandler).findUsersByUids(listOf("124", "125"))

        friendsEventHandler.saveFriend(
            uid = "124",
            friendDto = CreateFriendDto(
                name = "User 1",
                email = "user1@gmail.com",
                phoneNumber = "+923001234568"
            )
        )
        friendsEventHandler.saveFriend(
            uid = "125",
            friendDto = CreateFriendDto(
                name = "User 1",
                email = "user1@gmail.com",
                phoneNumber = "+923001234568"
            )
        )

        friendService.searchUsersImFriendOfAndAddThemAsMyFriends("123")

        val events = eventRepository.findAll().toList()

        assertThat(events).hasSize(4)
        val friend1 = events[0]
        assertThat(friend1.userUid).isEqualTo("124")
        assertThat(friend1.friendDisplayName).isEqualTo("User 1")
        assertThat(friend1.friendEmail).isEqualTo("user1@gmail.com")
        assertThat(friend1.friendPhoneNumber).isEqualTo("+923001234568")
        assertThat(friend1.createdAt).isNotNull
        assertThat(friend1.streamId).isNotNull
        assertThat(friend1.version).isEqualTo(1)
        assertThat(friend1.eventType).isEqualTo(FriendEventType.FRIEND_CREATED)

        val friend2 = events[1]
        assertThat(friend2.userUid).isEqualTo("125")
        assertThat(friend2.friendDisplayName).isEqualTo("User 1")
        assertThat(friend2.friendEmail).isEqualTo("user1@gmail.com")
        assertThat(friend2.friendPhoneNumber).isEqualTo("+923001234568")
        assertThat(friend2.createdAt).isNotNull
        assertThat(friend2.streamId).isNotNull
        assertThat(friend2.version).isEqualTo(1)
        assertThat(friend2.eventType).isEqualTo(FriendEventType.FRIEND_CREATED)

        val friend3 = events[2]
        assertThat(friend3.userUid).isEqualTo("123")
        assertThat(friend3.friendDisplayName).isEqualTo("User 2")
        assertThat(friend3.friendEmail).isEqualTo("user2@gmail.com")
        assertThat(friend3.friendPhoneNumber).isEqualTo("+923001234569")
        assertThat(friend3.createdAt).isNotNull
        assertThat(friend3.streamId).isNotNull
        assertThat(friend3.version).isEqualTo(1)
        assertThat(friend3.eventType).isEqualTo(FriendEventType.FRIEND_CREATED)

        val friend4 = events[3]
        assertThat(friend4.userUid).isEqualTo("123")
        assertThat(friend4.friendDisplayName).isEqualTo("User 3")
        assertThat(friend4.friendEmail).isEqualTo("user3@gmail.com")
        assertThat(friend4.friendPhoneNumber).isEqualTo("+923001234570")
        assertThat(friend4.createdAt).isNotNull
        assertThat(friend4.streamId).isNotNull
        assertThat(friend4.version).isEqualTo(1)
        assertThat(friend4.eventType).isEqualTo(FriendEventType.FRIEND_CREATED)
    }

    @Test
    fun `find all friends when no friend has signed up return friends successfully`(): Unit = runBlocking {
        doReturn(emptyFlow<UserEvent>()).`when`(userEventHandler)
            .findUsersByPhoneNumbers(listOf("+923001234568", "+923001234569"))
        doReturn(emptyFlow<UserEvent>()).`when`(userEventHandler)
            .findUsersByEmails(listOf("user2@gmail.com", "user3@gmail.com"))
        doReturn(
            UserDto(
                uid = "123",
                email = "user1@gmail.com",
                phoneNumber = "+923001234123",
                displayName = "User 1",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventHandler).findUserById("123")
        friendsEventHandler.saveFriend(
            uid = "123",
            friendDto = CreateFriendDto(
                name = "User 2",
                email = "user2@gmail.com",
                phoneNumber = "+923001234568"
            )
        )
        friendsEventHandler.saveFriend(
            uid = "123",
            friendDto = CreateFriendDto(
                name = "User 3",
                email = "user3@gmail.com",
                phoneNumber = "+923001234569"
            )
        )
        val friendsDto = friendService.findAllByUserId("123")
        assertThat(friendsDto.friends).hasSize(2)

        assertThat(friendsDto.friends[0].email).isEqualTo("user2@gmail.com")
        assertThat(friendsDto.friends[0].phoneNumber).isEqualTo("+923001234568")
        assertThat(friendsDto.friends[0].name).isEqualTo("User 2")
        assertThat(friendsDto.friends[0].photoUrl).isNull()
        assertThat(friendsDto.friends[0].balances.other).isEmpty()
        assertThat(friendsDto.friends[0].friendId).isNotNull()

        assertThat(friendsDto.friends[1].email).isEqualTo("user3@gmail.com")
        assertThat(friendsDto.friends[1].phoneNumber).isEqualTo("+923001234569")
        assertThat(friendsDto.friends[1].name).isEqualTo("User 3")
        assertThat(friendsDto.friends[1].photoUrl).isNull()
        assertThat(friendsDto.friends[1].balances.other).isEmpty()
        assertThat(friendsDto.friends[1].friendId).isNotNull()
    }

    data class FriendTestData(
        val email: String?,
        val phone: String?,
        val friendEmail: String?,
        val friendPhone: String?,
        val photo: String? = null,
        val friendPhoto: String? = null,
    )

    data class AddFriend(
        val email: String?,
        val phone: String?,
    )

    companion object {

        @JvmStatic
        fun addSelfAsFriendData() = listOf(
            FriendTestData(
                email = "user1@gmail.com",
                phone = "+923001234568",
                friendEmail = "user1@gmail.com",
                friendPhone = "+923001234568"
            ),
            FriendTestData(
                email = "user1@gmail.com",
                phone = "+923001234569",
                friendEmail = "user1@gmail.com",
                friendPhone = "+923001234568"
            ),
            FriendTestData(
                email = "user1@gmail.com",
                phone = "+923001234568",
                friendEmail = "user2@gmail.com",
                friendPhone = "+923001234568"
            ),
            FriendTestData(
                email = null,
                phone = "+923001234568",
                friendEmail = "user2@gmail.com",
                friendPhone = "+923001234568"
            ),
            FriendTestData(
                email = null,
                phone = "+923001234568",
                friendEmail = null,
                friendPhone = "+923001234568"
            ),
            FriendTestData(
                email = "user1@gmail.com",
                phone = null,
                friendEmail = "user1@gmail.com",
                friendPhone = null
            )
        )

        @JvmStatic
        fun friendTestData() = listOf(
            Pair(
                FriendTestData(
                    email = "user1@gmail.com",
                    phone = "+923001234568",
                    friendEmail = "user1@gmail.com",
                    friendPhone = "+923001234568",
                    photo = "https://test1.com",
                    friendPhoto = "https://test1.com"
                ),
                FriendTestData(
                    email = "user2@gmail.com",
                    phone = "+923001234569",
                    friendEmail = "user2@gmail.com",
                    friendPhone = "+923001234569",
                    photo = "https://test2.com",
                    friendPhoto = "https://test2.com"
                )
            ),
            Pair(
                FriendTestData(
                    email = "user1@gmail.com",
                    phone = null,
                    friendEmail = "user1@gmail.com",
                    friendPhone = "+923001234568",
                    photo = "https://test1.com",
                    friendPhoto = "https://test1.com"
                ),
                FriendTestData(
                    email = null,
                    phone = "+923001234569",
                    friendEmail = "user2@gmail.com",
                    friendPhone = "+923001234569",
                    photo = "https://test2.com",
                    friendPhoto = "https://test2.com"
                )
            ),
            Pair(
                FriendTestData(
                    email = "user1@gmail.com",
                    phone = null,
                    friendEmail = null,
                    friendPhone = "+923001234568",
                    photo = "https://test1.com",
                    friendPhoto = null
                ),
                FriendTestData(
                    email = null,
                    phone = "+923001234569",
                    friendEmail = null,
                    friendPhone = "+923001234569",
                    photo = "https://test2.com",
                    friendPhoto = "https://test2.com"
                )
            ),
            Pair(
                FriendTestData(
                    email = null,
                    phone = null,
                    friendEmail = null,
                    friendPhone = "+923001234568",
                    photo = null,
                    friendPhoto = null
                ),
                FriendTestData(
                    email = null,
                    phone = null,
                    friendEmail = "user2@gmail.com",
                    friendPhone = null,
                    photo = null,
                    friendPhoto = null
                )
            )
        )

        @JvmStatic
        fun addFriendTestData() = listOf(
            Pair(
                AddFriend(
                    email = "user1@gmail.com",
                    phone = "+923001234568",
                ), AddFriend(
                    email = "user1@gmail.com",
                    phone = "+923001234568",
                )
            ),
            Pair(
                AddFriend(
                    email = "user1@gmail.com",
                    phone = "+923001234568",
                ), AddFriend(
                    email = "user1@gmail.com",
                    phone = "+923001234569",
                )
            ),
            Pair(
                AddFriend(
                    email = "user1@gmail.com",
                    phone = "+923001234568",
                ), AddFriend(
                    email = "user2@gmail.com",
                    phone = "+923001234568",
                )
            ),
            Pair(
                AddFriend(
                    email = "user1@gmail.com",
                    phone = null,
                ), AddFriend(
                    email = "user1@gmail.com",
                    phone = "+923001234568",
                )
            ),
            Pair(
                AddFriend(
                    email = "user1@gmail.com",
                    phone = "+923001234568",
                ), AddFriend(
                    email = null,
                    phone = "+923001234568",
                )
            )
        )
    }

    @ParameterizedTest
    @MethodSource("friendTestData")
    fun `find all friends when everyone is signed up has photos`(
        friendData: Pair<FriendTestData, FriendTestData>,
    ): Unit = runBlocking {
        userEventRepository.deleteAll()
        eventRepository.deleteAll()
        val userEventHandler = UserEventHandler(userEventRepository)
        val friendsEventHandler =
            FriendsEventHandler(
                eventRepository = eventRepository
            )

        val friendService = FriendService(
            userEventHandler = userEventHandler,
            transactionEventHandler = transactionEventHandler,
            friendsEventHandler = friendsEventHandler,
            friendFinderStrategy = FriendFinderStrategy(
                friendsEventHandler,
                userEventHandler
            ),
            allTimeBalanceStrategy = AllTimeBalanceStrategy(),
            currencyClient = currencyClient
        )
        val (user1, user2) = friendData
        userEventHandler.saveEvent(
            UserEvent(
                uid = "123",
                email = "user123@gmail.com",
                phoneNumber = "+923001234123",
                displayName = "User 1",
                photoUrl = user1.photo,
                emailVerified = true,
                createdAt = Instant.now(),
                version = 1,
                eventType = UserEventType.CREATE_USER,
            )
        )
        user1.photo?.let {
            userEventHandler.saveEvent(
                UserEvent(
                    uid = "124",
                    email = user1.email,
                    phoneNumber = user1.phone,
                    displayName = "User 2",
                    photoUrl = user1.photo,
                    emailVerified = true,
                    createdAt = Instant.now(),
                    version = 1,
                    eventType = UserEventType.CREATE_USER,
                )
            )
        }
        user2.photo?.let {
            userEventHandler.saveEvent(
                UserEvent(
                    uid = "125",
                    email = user2.email,
                    phoneNumber = user2.phone,
                    displayName = "User 3",
                    photoUrl = user2.photo,
                    emailVerified = true,
                    createdAt = Instant.now(),
                    version = 1,
                    eventType = UserEventType.CREATE_USER,
                )
            )
        }
        friendsEventHandler.saveFriend(
            uid = "123",
            friendDto = CreateFriendDto(
                name = "User 2",
                email = user1.friendEmail,
                phoneNumber = user1.friendPhone
            )
        )
        friendsEventHandler.saveFriend(
            uid = "123",
            friendDto = CreateFriendDto(
                name = "User 3",
                email = user2.friendEmail,
                phoneNumber = user2.friendPhone
            )
        )
        val friendsDto = friendService.findAllByUserId("123")
        assertThat(friendsDto.friends).hasSize(2)

        assertThat(friendsDto.friends[0].email).isEqualTo(user1.friendEmail)
        assertThat(friendsDto.friends[0].phoneNumber).isEqualTo(user1.friendPhone)
        assertThat(friendsDto.friends[0].name).isEqualTo("User 2")
        assertThat(friendsDto.friends[0].photoUrl).isEqualTo(user1.friendPhoto)
        assertThat(friendsDto.friends[0].balances.other).isEmpty()
        assertThat(friendsDto.friends[0].friendId).isNotNull()


        assertThat(friendsDto.friends[1].email).isEqualTo(user2.friendEmail)
        assertThat(friendsDto.friends[1].phoneNumber).isEqualTo(user2.friendPhone)
        assertThat(friendsDto.friends[1].name).isEqualTo("User 3")
        assertThat(friendsDto.friends[1].photoUrl).isEqualTo(user2.friendPhoto)
        assertThat(friendsDto.friends[1].balances.other).isEmpty()
        assertThat(friendsDto.friends[1].friendId).isNotNull()
    }


    @ParameterizedTest
    @MethodSource("addFriendTestData")
    fun `friends emails and phones should not overlap`(
        addFriendData: Pair<AddFriend, AddFriend>,
    ): Unit = runBlocking {
        doReturn(
            UserDto(
                uid = "123",
                email = "user@gmail.com",
                phoneNumber = "+923001234567",
                displayName = "User 1",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventHandler).findUserById("123")
        val (friend1, friend2) = addFriendData
        friendService.createFriend(
            userId = "123",
            friendDto = CreateFriendDto(
                name = "John Doe",
                email = friend1.email,
                phoneNumber = friend1.phone
            )
        )

        assertThatThrownBy {
            runBlocking {
                friendService.createFriend(
                    userId = "123",
                    friendDto = CreateFriendDto(
                        name = "John Doe 2",
                        email = friend1.email,
                        phoneNumber = friend2.phone
                    )
                )
            }
        }.isInstanceOf(IllegalArgumentException::class.java)

    }

    @Test
    fun `add friend with no email and phone throws illegal argument`() {
        assertThatThrownBy {
            runBlocking {
                friendService.createFriend(
                    userId = "123",
                    friendDto = CreateFriendDto(
                        name = "John Doe",
                        email = null,
                        phoneNumber = null
                    )
                )
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest
    @MethodSource("addSelfAsFriendData")
    fun `add friend with self email or phone throws illegal argument`(friendData: FriendTestData): Unit = runBlocking {

        doReturn(
            UserDto(
                uid = "123",
                email = friendData.email,
                phoneNumber = friendData.phone,
                displayName = "User 1",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventHandler).findUserById("123")
        assertThatThrownBy {
            runBlocking {
                friendService.createFriend(
                    userId = "123",
                    friendDto = CreateFriendDto(
                        name = "John Doe",
                        email = friendData.friendEmail,
                        phoneNumber = friendData.friendPhone
                    )
                )
            }
        }.isInstanceOf(IllegalArgumentException::class.java)

    }

    @Test
    fun `add friend when user dont exist throws illegal argument`(): Unit = runBlocking {
        doReturn(null).`when`(userEventHandler).findUserById("123")
        assertThatThrownBy {
            runBlocking {
                friendService.createFriend(
                    userId = "123",
                    friendDto = CreateFriendDto(
                        name = "John Doe",
                        email = "user1@gmail.com",
                        phoneNumber = "+923001234568"
                    )
                )
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `get friends also adds amountdto when friends have transactions`(): Unit = runBlocking {

        val friendStreamId = UUID.randomUUID()

        whenever(userEventHandler.findUserById("123")).thenReturn(
            UserDto(
                uid = "123",
                email = "user@gmail.com",
                phoneNumber = "+923001234567",
                displayName = "User 1",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        )


        whenever(transactionEventHandler.balancesOfFriendsByCurrency("123", listOf(friendStreamId))).thenReturn(
            mapOf(
                friendStreamId to mapOf(
                    "USD" to AmountDto(
                        amount = 100.0.toBigDecimal(),
                        currency = Currency.getInstance("USD"),
                        isOwed = true
                    )
                )
            )
        )

        whenever(userEventHandler.findUsersByPhoneNumbers(listOf("+923001234568"))).thenReturn(
            listOf(
                UserDto(
                    uid = "124",
                    email = "friend1@gmail.com",
                    phoneNumber = "+923001234568",
                    displayName = "Friend 1",
                    photoUrl = "https://test.com",
                    emailVerified = true
                )
            ).asFlow()
        )
        whenever(userEventHandler.findUsersByEmails(emptyList())).thenReturn(
            emptyFlow()
        )

        eventRepository.save(
            FriendEvent(
                userUid = "123",
                friendDisplayName = "Friend 1",
                friendEmail = "friend1@gmail.com",
                friendPhoneNumber = "+923001234568",
                createdAt = Instant.now(),
                streamId = friendStreamId,
                version = 1,
                eventType = FriendEventType.FRIEND_CREATED,
            )
        )
        val friends = friendService.findAllByUserId("123")

        assertThat(friends.friends).hasSize(1)
        assertThat(friends.friends[0].email).isEqualTo("friend1@gmail.com")
        assertThat(friends.friends[0].phoneNumber).isEqualTo("+923001234568")
        assertThat(friends.friends[0].name).isEqualTo("Friend 1")
        assertThat(friends.friends[0].photoUrl).isEqualTo("https://test.com")
        assertThat(friends.friends[0].balances.other.first().amount).isEqualTo(100.0.toBigDecimal())
        assertThat(friends.friends[0].balances.other.first().currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(friends.friends[0].balances.other.first().isOwed).isTrue()
        assertThat(friends.friends[0].friendId).isNotNull
    }
}
