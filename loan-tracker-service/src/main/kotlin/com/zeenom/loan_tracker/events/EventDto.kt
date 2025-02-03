package com.zeenom.loan_tracker.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zeenom.loan_tracker.common.TransactionDto

data class EventDto(
    val eventId: String,
    val event: EventType,
    val payload: EventPayloadDto?,
    val userId: String,
    val source: EventSource
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TransactionDto::class, name = "transaction")
)
interface EventPayloadDto

enum class EventType {
    LOGIN, CREATE_TRANSACTION, UPDATE_TRANSACTION, DELETE_TRANSACTION, ADD_NOTES, ADD_FRIEND
}