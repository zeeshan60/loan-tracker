package com.zeenom.loan_tracker

import com.fasterxml.jackson.databind.ObjectMapper

fun Any.pretty(objectMapper: ObjectMapper): String =
    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

fun Any.prettyAndPrint(objectMapper: ObjectMapper) = this.pretty(objectMapper).also { println(it) }