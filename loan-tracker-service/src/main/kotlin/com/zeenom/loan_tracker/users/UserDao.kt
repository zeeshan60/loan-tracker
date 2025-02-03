package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.common.SecondInstant
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class UserDao(private val userRepository: UserRepository, private val secondInstant: SecondInstant) {

    suspend fun loginUser(userDto: UserDto): String {
        val existingUser = userRepository.findByUid(userDto.uid).awaitSingleOrNull()
        if (existingUser != null) {
            userRepository.save(
                existingUser.copy(
                    email = userDto.email,
                    displayName = userDto.displayName,
                    photoUrl = userDto.photoUrl,
                    emailVerified = userDto.emailVerified,
                    updatedAt = secondInstant.now(),
                    lastLoginAt = secondInstant.now()
                )
            ).awaitSingle()
            return "User updated ${userDto.uid}"
        }
        userRepository.save(
            UserEntity(
                uid = userDto.uid,
                email = userDto.email,
                displayName = userDto.displayName,
                photoUrl = userDto.photoUrl,
                emailVerified = userDto.emailVerified,
                createdAt = secondInstant.now(),
                updatedAt = secondInstant.now(),
                lastLoginAt = secondInstant.now()
            )
        ).awaitSingle()
        return "User logged in ${userDto.uid}"
    }

    suspend fun createUser(userDto: UserDto): String {
        userRepository.save(
            UserEntity(
                uid = userDto.uid,
                email = userDto.email,
                displayName = userDto.displayName,
                photoUrl = userDto.photoUrl,
                emailVerified = userDto.emailVerified,
                createdAt = secondInstant.now(),
                updatedAt = secondInstant.now(),
                lastLoginAt = null
            )
        ).awaitSingle()
        return "User created ${userDto.uid}"
    }

    suspend fun findUserById(uid: String): UserDto? {
        return userRepository.findByUid(uid).awaitSingleOrNull()?.let {
            UserDto(
                uid = it.uid,
                email = it.email,
                displayName = it.displayName,
                photoUrl = it.photoUrl,
                emailVerified = it.emailVerified
            )
        }
    }
}