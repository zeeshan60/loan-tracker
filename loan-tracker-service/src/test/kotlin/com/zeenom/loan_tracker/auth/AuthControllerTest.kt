package com.zeenom.loan_tracker.auth

import com.zeenom.loan_tracker.events.CommandDao
import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import com.zeenom.loan_tracker.firebase.FirebaseService
import com.zeenom.loan_tracker.friends.AllTimeBalanceDto
import com.zeenom.loan_tracker.friends.FriendService
import com.zeenom.loan_tracker.friends.FriendsWithAllTimeBalancesDto
import com.zeenom.loan_tracker.security.LoginRequest
import com.zeenom.loan_tracker.users.UserDto
import com.zeenom.loan_tracker.users.UserService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerTest(
    @LocalServerPort private val port: Int,
    @Autowired @MockitoSpyBean private val firebaseService: FirebaseService,
    @Autowired @MockitoBean private val commandDao: CommandDao,
    @Autowired @MockitoBean private val userService: UserService,
    @Autowired @MockitoBean private val friendService: FriendService,
) {

    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @Test
    fun `given verified id token generates jwt token with expiry successfully`(): Unit = runBlocking {

        val idToken =
            "eyJhbGciOiJSUzI1NiIsImtpZCI6ImE0MzRmMzFkN2Y3NWRiN2QyZjQ0YjgxZDg1MjMwZWQxN2ZlNTk3MzciLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiWmVlc2hhbiBUdWZhaWwiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jS3hlSEhxNENsNU9RZjdDSENISHA4Ym1ObEswbmFGbzJHa282UTJPS0xDRjRkNjVHbHc9czk2LWMiLCJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vbG9hbi10cmFja2VyLTliMjVkIiwiYXVkIjoibG9hbi10cmFja2VyLTliMjVkIiwiYXV0aF90aW1lIjoxNzM4NTEyNzA3LCJ1c2VyX2lkIjoiQ01XTDB0YXBaR1NET0kzVGJ6UUVNOHZibFRsMiIsInN1YiI6IkNNV0wwdGFwWkdTRE9JM1RielFFTTh2YmxUbDIiLCJpYXQiOjE3Mzg1MTI3MDcsImV4cCI6MTczODUxNjMwNywiZW1haWwiOiJ6ZWVzaGFudHVmYWlsODZAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZ29vZ2xlLmNvbSI6WyIxMTE4MzA4MjI1MDU4Mjc5Mzk3OTQiXSwiZW1haWwiOlsiemVlc2hhbnR1ZmFpbDg2QGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifX0.T5p39Aja_tqrZ3Xlcl7AD0M9Lm9kkUY4ACNqr2a1eBfWfxp22Yu8BUkO3NiiSHAhx7-CgXRc8hs6KQRX3D5h8L_rwm3g5b7hVGkHy-YnvL0beOghhshJpp-WdYLP6xZ7gTB8ENwM8aC5U3kYuvc4VblwzdOC0jxkkvwNDDTmlhUJmVoua2VmEBjcuxEP0sILhHy0NWZjimf_DeeNDS7O6hI9uo5rnOfTPdfUaT5EagRyh0CNcP-FuxLsu6qFeMRqEXXIjYR8HpA3MnfcIen-_h-UTHWNxFv3SLIjkhkpRFP9oh7WGBIKJNfu6TExZlJ0A6aZSwF_lfxoGJdISnRLkQ"

        val uid = UUID.randomUUID()
        val userDto = UserDto(
            uid = uid,
            userFBId = "123",
            email = "sample@gmail.com",
            phoneNumber = "+923001234567",
            displayName = "Zeeshan Tufail",
            photoUrl = "https://lh3.googleusercontent.com/a/A9GpZGSDOI3TbzQEM8vblTl2",
            currency = null,
            emailVerified = true
        )
        Mockito.doReturn(
            userDto
        ).whenever(firebaseService).userByVerifyingIdToken(idToken)

        Mockito.doReturn(Unit).whenever(userService).createUser(userDto)
        Mockito.doReturn(userDto).whenever(userService).findUserByFBId(
            userDto.userFBId
        )
        Mockito.doReturn(userDto).whenever(userService).findByUserEmailOrPhoneNumber(
            email = userDto.email,
            phoneNumber = userDto.phoneNumber
        )
        Mockito.doReturn(Unit).whenever(friendService).searchUsersImFriendOfAndAddThemAsMyFriends(uid)
        whenever(friendService.findAllByUserId(uid)).thenReturn(
            FriendsWithAllTimeBalancesDto(
                friends = emptyList(),
                balance = AllTimeBalanceDto(main = null, other = emptyList())
            )
        )

        Mockito.doReturn(Unit).whenever(commandDao).addCommand(
            CommandDto(
                commandType = CommandType.LOGIN,
                payload = userDto,
                userId = uid,
                userFBId = "123"
            )
        )

        // When
        val response = webTestClient.post()
            .uri("/login")
            .header("Content-Type", "application/json")
            .bodyValue(LoginRequest("Bearer $idToken"))
            .exchange()

        // Then
        response.expectStatus().isOk
            .expectBody()
            .jsonPath("$.token").value { it: String? -> assertThat(it).isNotEmpty() }

        argumentCaptor<CommandDto<UserDto>>().apply {
            Mockito.verify(commandDao, Mockito.times(1)).addCommand(capture())
            assertThat(firstValue).isEqualTo(
                CommandDto(
                    commandType = CommandType.LOGIN,
                    payload = userDto,
                    userId = uid,
                    userFBId = "123"
                )
            )
        }

        val userDtoCaptor = argumentCaptor<UserDto>()
        Mockito.verify(userService, Mockito.times(1))
            .createUser(userDtoCaptor.capture())
        assertThat(userDtoCaptor.firstValue).isEqualTo(userDto)

        Mockito.verify(friendService, Mockito.times(1)).searchUsersImFriendOfAndAddThemAsMyFriends(uid)
    }
}

