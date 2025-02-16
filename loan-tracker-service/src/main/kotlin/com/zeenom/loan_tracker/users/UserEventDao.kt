package com.zeenom.loan_tracker.users

import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Repository
interface UserEventRepository : CoroutineCrudRepository<UserEvent, UUID> {
    suspend fun findByUid(uid: String): UserEvent?
    suspend fun findByEmail(email: String): UserEvent?
    suspend fun findByPhoneNumber(phoneNumber: String): UserEvent?
}

@Service
class UserEventDao(private val userRepository: UserEventRepository) {
    suspend fun createIfNotExist(userDto: UserDto) {
        findUserById(userDto.uid)?.let { createUser(userDto) }
    }

    suspend fun createUser(userDto: UserDto) {
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