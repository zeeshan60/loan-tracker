package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.friends.TestPostgresConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*

@DataR2dbcTest
@ActiveProfiles("test")
class UserEventHandlerTest : TestPostgresConfig() {
    @Autowired
    private lateinit var userEventRepository: UserEventRepository

    private lateinit var userEventHandler: UserEventHandler

    @Autowired
    private lateinit var userModelRepository: UserModelRepository

    @BeforeEach
    fun setUp() {
        userEventHandler = UserEventHandler(
            userRepository = userEventRepository,
            userModelRepository = userModelRepository
        )
        runBlocking {
            userModelRepository.deleteAll()
            userEventRepository.deleteAll()
        }
    }

    @Test
    fun `test synchronize logic`(): Unit = runBlocking {

        /**
         * Lets add some user events that are already synchronized than add some new events and test the synchronization
         */

        val user1 = UUID.randomUUID()
        userEventHandler.addEvent(
            UserCreated(
                displayName = "",
                phoneNumber = "",
                email = "",
                photoUrl = "",
                emailVerified = true,
                userId = "test-user-id",
                createdAt = Instant.now().minusSeconds(5),
                version = 0,
                streamId = user1,
                createdBy = UUID.randomUUID(),
            )
        )

        userEventHandler.addEvent(
            UserCreated(
                displayName = "",
                phoneNumber = "",
                email = "",
                photoUrl = "",
                emailVerified = true,
                userId = "test-user-id-2",
                createdAt = Instant.now().minusSeconds(5),
                version = 0,
                streamId = UUID.randomUUID(),
                createdBy = UUID.randomUUID(),
            )
        )

        userEventHandler.addEvent(
            UserDisplayNameChanged(
                displayName = "changed",
                createdAt = Instant.now().minusSeconds(4),
                version = 1,
                streamId = user1,
                createdBy = UUID.randomUUID(),
            )
        )

        userEventHandler.synchronize()

        val users = userModelRepository.findAll().toList().sortedBy { it.insertOrder }
        assertThat(users).hasSize(2)
        assertThat(users[0].streamId).isEqualTo(user1)
        assertThat(users[0].displayName).isEqualTo("changed")
        assertThat(users[0].version).isEqualTo(1)
        assertThat(users[1].displayName).isEqualTo("")
        assertThat(users[1].version).isEqualTo(0)

        //Now lets add more events and test the synchronization again
        userEventHandler.addEvent(
            UserCreated(
                displayName = "",
                phoneNumber = "",
                email = "",
                photoUrl = "",
                emailVerified = true,
                userId = "test-user-id-3",
                createdAt = Instant.now().minusSeconds(3),
                version = 0,
                streamId = UUID.randomUUID(),
                createdBy = UUID.randomUUID(),
            )
        )
        userEventHandler.addEvent(
            UserDisplayNameChanged(
                displayName = "changed again",
                createdAt = Instant.now().minusSeconds(2),
                version = 2,
                streamId = user1,
                createdBy = UUID.randomUUID(),
            )
        )

        userEventHandler.synchronize()
        val newUsers = userModelRepository.findAll().toList().sortedBy { it.insertOrder }
        assertThat(newUsers).hasSize(3)
        assertThat(newUsers[0].streamId).isEqualTo(user1)
        assertThat(newUsers[0].displayName).isEqualTo("changed again")
        assertThat(newUsers[0].version).isEqualTo(2)
        assertThat(newUsers[1].displayName).isEqualTo("")
        assertThat(newUsers[1].version).isEqualTo(0)
        assertThat(newUsers[2].displayName).isEqualTo("")
        assertThat(newUsers[2].version).isEqualTo(0)
    }
}