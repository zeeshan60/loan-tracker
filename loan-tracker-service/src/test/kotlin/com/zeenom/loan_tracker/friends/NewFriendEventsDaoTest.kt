package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserEventDao
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

    private val newFriendEventsDao = NewFriendEventsDao(eventRepository)

    private val userEventDao = mock<UserEventDao>()

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
                phoneNumber = "+923001234567",
                displayName = "User 1",
                photoUrl = "https://test.com",
                emailVerified = true
            )
        ).`when`(userEventDao).findUserById("123")

        newFriendEventsDao.saveFriend(
            uid = "124",
            friendDto = CreateFriendDto(
                name = "User 2",
                email = "user2@gmail.com",
                phoneNumber = "+923001234568"
            )
        )

        newFriendEventsDao.makeMyOwnersMyFriends("124")
    }
}
