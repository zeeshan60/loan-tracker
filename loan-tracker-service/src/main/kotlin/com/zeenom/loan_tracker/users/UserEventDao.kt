package com.zeenom.loan_tracker.users

import org.springframework.dao.DuplicateKeyException
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Table("user_events")
data class UserEvent(
    @Id val id: UUID? = null,
    val userId: UUID,
    val uid: String,
    val displayName: String,
    val phoneNumber: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean?,
    val createdAt: Instant,
    val version: Int,
    val eventType: UserEventType,
)

enum class UserEventType {
    CREATE_USER
}

@Service
class NewEventsDao {
    suspend fun addEvent(event: NewEvent) {

    }
}

@Repository
interface NewUserRepository : CoroutineCrudRepository<UserEvent, UUID> {
    suspend fun findByUid(uid: String): UserEvent?
    suspend fun findByEmail(email: String): UserEvent?
    suspend fun findByPhoneNumber(phoneNumber: String): UserEvent?
}

@Service
class UserEventDao(private val userRepository: NewUserRepository) {
    suspend fun loginUser(userDto: UserDto): String {
        val existing = findUserById(userDto.uid)
        if (existing != null) {
            // update user
            return "User updated"
        }

        createUser(userDto)
        return "User logged in successfully"
    }

    suspend fun createUser(userDto: UserDto): String {
        try {
            userRepository.save(
                UserEvent(
                    userId = UUID.randomUUID(),
                    uid = userDto.uid,
                    displayName = userDto.displayName,
                    phoneNumber = userDto.phoneNumber,
                    email = userDto.email,
                    emailVerified = userDto.emailVerified,
                    photoUrl = userDto.photoUrl,
                    createdAt = Instant.now(),
                    version = 1,
                    eventType = UserEventType.CREATE_USER
                )
            )
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException("User already exist")
        }

        return "User created successfully"
    }

    suspend fun findUserById(uid: String): UserDto? {
        return userRepository.findByUid(uid)?.let {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                email = it.email,
                emailVerified = it.emailVerified ?: false, //todo make the dto optional
                photoUrl = it.photoUrl
            )
        }
    }
}

interface NewEvent {
    val type: NewEventType
}

enum class NewEventType {
    CREATE_USER
}