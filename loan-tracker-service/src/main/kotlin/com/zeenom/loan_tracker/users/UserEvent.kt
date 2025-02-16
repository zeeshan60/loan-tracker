package com.zeenom.loan_tracker.users

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

@Table("user_events")
data class UserEvent(
    @Id val id: UUID? = null,
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