package com.zeenom.loan_tracker.daos

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class EventDto(
    val eventId: String,
    val event: EventType,
    val payload: EventPayloadDto?,
    val createdAt: Instant,
    val userId: String,
)

data class EventPayloadDto(
    val amount: AmountDto,
    val eventReceivers: EventUsersDto,
)

data class EventUsersDto(
    val userId: List<String>
)

data class AmountDto(
    val amount: BigDecimal,
    val currency: Currency,
    val amountReceivable: Boolean
)

enum class EventType {
    CREATE_TRANSACTION, UPDATE_TRANSACTION, DELETE_TRANSACTION, ADD_NOTES, ADD_FRIEND
}