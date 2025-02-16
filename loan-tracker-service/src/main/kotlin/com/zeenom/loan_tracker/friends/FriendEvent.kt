package com.zeenom.loan_tracker.friends

import kotlinx.coroutines.flow.Flow
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Table("friend_events")
data class FriendEvent(
    @Id val id: UUID? = null,
    val userUid: String,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
    val createdAt: Instant,
    val streamId: UUID,
    val version: Int,
    val eventType: FriendEventType,
)

enum class FriendEventType {
    FRIEND_CREATED
}

@Repository
interface FriendEventRepository : CoroutineCrudRepository<FriendEvent, UUID> {
    suspend fun findAllByUserUid(userUid: String): Flow<FriendEvent>
    suspend fun findByFriendEmail(email: String): Flow<FriendEvent>
    suspend fun findByFriendPhoneNumber(phoneNumber: String): Flow<FriendEvent>
}