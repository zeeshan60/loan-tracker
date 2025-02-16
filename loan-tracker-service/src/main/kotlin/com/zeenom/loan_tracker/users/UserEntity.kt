package com.zeenom.loan_tracker.users

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Table("users")
data class UserEntity(
    @Id val id: UUID? = null,
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String,
    val photoUrl: String?,
    val emailVerified: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastLoginAt: Instant?,
)

@Repository
interface UserRepository : CoroutineCrudRepository<UserEntity, String> {
    suspend fun findByUid(uid: String): UserEntity?
    suspend fun findByEmail(email: String): UserEntity?
    suspend fun findByPhoneNumber(phoneNumber: String): UserEntity?
}
