package com.zeenom.loan_tracker.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zeenom.loan_tracker.common.TransactionDto

data class EventDto(
    val event: EventType,
    val payload: EventPayloadDto?,
    val userId: String
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TransactionDto::class, name = "transaction")
)
interface EventPayloadDto

enum class EventType {
    LOGIN, CREATE_TRANSACTION
}