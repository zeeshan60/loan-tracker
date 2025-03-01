package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.transactions.IEventAble
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

interface IUserEvent : IEvent<UserModel> {
    override fun toEntity(): UserEvent
}

@Table("user_events")
data class UserEvent(
    @Id val id: UUID? = null,
    val uid: String,
    val streamId: UUID,
    val displayName: String?,
    val phoneNumber: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean?,
    val currency: String?,
    val createdAt: Instant,
    val version: Int,
    val eventType: UserEventType,
) : IEventAble<UserModel> {
    override fun toEvent(): IEvent<UserModel> {
        return when (eventType) {
            UserEventType.USER_CREATED -> UserCreated(
                userId = uid,
                displayName = displayName ?: throw IllegalStateException("Display name is required"),
                phoneNumber = phoneNumber,
                email = email,
                photoUrl = photoUrl,
                emailVerified = emailVerified ?: throw IllegalStateException("Email verified is required"),
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = uid
            )

            UserEventType.CURRENCY_CHANGED -> UserCurrencyChanged(
                userId = uid,
                currency = currency ?: throw IllegalStateException("Currency is required"),
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = uid
            )
        }
    }
}

data class UserModel(
    val streamId: UUID,
    val uid: String,
    val displayName: String,
    val phoneNumber: String?,
    val currency: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean?,
    val createdAt: Instant,
    val version: Int,
)

data class UserCreated(
    val displayName: String,
    val phoneNumber: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean,
    override val userId: String,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: String,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = userId,
            streamId = streamId,
            displayName = displayName,
            phoneNumber = phoneNumber,
            email = email,
            photoUrl = photoUrl,
            emailVerified = emailVerified,
            currency = null,
            createdAt = createdAt,
            version = version,
            eventType = UserEventType.USER_CREATED
        )
    }

    override fun applyEvent(existing: UserModel?): UserModel {
        return UserModel(
            uid = userId,
            streamId = streamId,
            displayName = displayName,
            phoneNumber = phoneNumber,
            email = email,
            photoUrl = photoUrl,
            currency = null,
            emailVerified = emailVerified,
            createdAt = createdAt,
            version = version
        )
    }
}

data class UserCurrencyChanged(
    val currency: String?,
    override val userId: String,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: String,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = userId,
            streamId = streamId,
            displayName = null,
            phoneNumber = null,
            email = null,
            photoUrl = null,
            emailVerified = null,
            currency = currency,
            createdAt = createdAt,
            version = version,
            eventType = UserEventType.CURRENCY_CHANGED
        )
    }

    override fun applyEvent(existing: UserModel?): UserModel {
        requireNotNull(existing) { "User must exist" }
        return UserModel(
            uid = existing.uid,
            streamId = existing.streamId,
            displayName = existing.displayName,
            phoneNumber = existing.phoneNumber,
            email = existing.email,
            photoUrl = existing.photoUrl,
            currency = currency,
            emailVerified = existing.emailVerified,
            createdAt = createdAt,
            version = version
        )
    }
}

enum class UserEventType {
    USER_CREATED,
    CURRENCY_CHANGED,
}