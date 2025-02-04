package com.zeenom.loan_tracker.common.r2dbc

import com.fasterxml.jackson.databind.ObjectMapper
import io.r2dbc.postgresql.codec.Json


fun Any.toJson(objectMapper: ObjectMapper): Json {
    return Json.of(objectMapper.writeValueAsString(this))
}

fun <T> Json.toClass(objectMapper: ObjectMapper, clazz: Class<T>): T {
    return objectMapper.readValue(this.asString(), clazz)
}