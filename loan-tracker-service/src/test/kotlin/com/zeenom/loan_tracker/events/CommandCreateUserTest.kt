package com.zeenom.loan_tracker.events

import com.zeenom.loan_tracker.users.UserDao
import com.zeenom.loan_tracker.users.UserDto
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class CommandCreateUserTest {

    @Test
    fun `failure in fire and forget event should not impact login of the user`(): Unit = runBlocking {

        val payload = UserDto(
            uid = "123",
            email = "user@gmail.com",
            phoneNumber = "+923001234567",
            displayName = "Zeeshan Tufail",
            photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
            emailVerified = true
        )
        val userDao: UserDao = mock {
            onBlocking { loginUser(userDto = payload) }.thenReturn("User logged in 123")
        }
        val commandCreateUser = CommandCreateUser(
            userDao = userDao,
            eventDao = mock {
                doReturn(Unit).`when`(this.mock).saveEvent<EventPayloadDto>(any())
            },
            friendsDao = mock {
                onBlocking { makeMyOwnersMyFriends("123") }.thenThrow(RuntimeException("making friends failed"))
            }
        )

        commandCreateUser.execute(
            EventDto(
                userId = "123",
                payload = payload,
                event = EventType.LOGIN
            )
        )

        verify(userDao).loginUser(userDto = payload)
    }
}