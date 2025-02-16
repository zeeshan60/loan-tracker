package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventDao
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest

@DataR2dbcTest
class NewFriendEventsDaoTest(@Autowired private val eventRepository: NewFriendEventRepository) : TestPostgresConfig() {

    private val userEventDao = mock<UserEventDao>()
    private val newFriendEventsDao = NewFriendEventsDao(eventRepository = eventRepository, userEventDao = userEventDao)


    @BeforeEach
    fun setUp(): Unit = runBlocking {
        eventRepository.deleteAll()
    }

    @Test
    fun `saves friend event successfully`(): Unit = runBlocking {
        newFriendEventsDao.saveFriend(
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
        ).`when`(userEventDao).findUserById("123")

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
        ).`when`(userEventDao).findUsersByUids(listOf("124", "125"))

        newFriendEventsDao.saveFriend(
            uid = "124",
            friendDto = CreateFriendDto(
                name = "User 1",
                email = "user1@gmail.com",
                phoneNumber = "+923001234568"
            )
        )
        newFriendEventsDao.saveFriend(
            uid = "125",
            friendDto = CreateFriendDto(
                name = "User 1",
                email = "user1@gmail.com",
                phoneNumber = "+923001234568"
            )
        )

        newFriendEventsDao.makeMyOwnersMyFriends("123")

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
}
