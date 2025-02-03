package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.looseNanonSeconds
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserDao(private val userRepository: UserRepository, private val secondInstant: SecondInstant) {
    suspend fun createUser(userDto: UserDto): String {
        userRepository.save(
            UserEntity(
                uid = userDto.uid,
                email = userDto.email,
                displayName = userDto.displayName,
                photoUrl = userDto.photoUrl,
                emailVerified = userDto.emailVerified,
                createdAt = userDto.updatedAt ?: secondInstant.now(),
                updatedAt = userDto.updatedAt ?: secondInstant.now()
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
                emailVerified = it.emailVerified,
                updatedAt = it.updatedAt
            )
        }
    }

    suspend fun deleteUserById(uid: String): String {
        userRepository.deleteAllByUid(uid).awaitSingleOrNull()
        return "User $uid deleted"
    }
}