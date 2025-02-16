package com.zeenom.loan_tracker.friends

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest

@DataR2dbcTest
class NewFriendEventsDaoTest(@Autowired private val eventRepository: NewFriendEventRepository) : TestPostgresConfig() {

    private val newFriendEventsDao = NewFriendEventsDao(eventRepository)

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
}
