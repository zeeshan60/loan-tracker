@file:Suppress("unused")

package com.zeenom.loan_tracker

import com.fasterxml.jackson.databind.ObjectMapper

fun Any.pretty(objectMapper: ObjectMapper): String =
    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

fun Any.prettyAndPrint(objectMapper: ObjectMapper) = this.pretty(objectMapper).also { println(it) }

val currencyRateMap = mapOf("USD" to 1.0.toBigDecimal(), "PKR" to 260.0.toBigDecimal(), "SGD" to 1.3.toBigDecimal())