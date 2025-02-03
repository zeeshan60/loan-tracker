package com.zeenom.loan_tracker.users

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Table("users")
data class UserEntity(
    @Id val id: UUID? = null,
    val uid: String,
    val email: String?,
    val displayName: String,
    val photoUrl: String,
    val emailVerified: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Repository
interface UserRepository : ReactiveCrudRepository<UserEntity, String> {
    fun findByUid(uid: String): Mono<UserEntity>
    fun deleteAllByUid(uid: String): Mono<Void>
}