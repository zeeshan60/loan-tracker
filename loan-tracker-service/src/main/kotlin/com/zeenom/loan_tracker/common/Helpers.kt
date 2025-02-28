@file:Suppress("unused")

package com.zeenom.loan_tracker.common

import com.zeenom.loan_tracker.transactions.SplitType
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

fun String.toInstant(): Instant {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val localDate = LocalDate.parse(this, formatter)
    val sgZone = ZoneId.of("Asia/Singapore")
    val singaporeDateTime = localDate.atStartOfDay(sgZone)
    return singaporeDateTime.toInstant()
}

fun Instant.toReadableDateFormat(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").withZone(ZoneId.of("Asia/Singapore"))
    return formatter.format(this)
}

fun Instant.startOfMonth(timeZone: String): Instant {
    val zoneId = ZoneId.of(timeZone)
    val dateTime = this.atZone(zoneId)
    val startOfMonth = dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay(zoneId)
    return startOfMonth.toInstant()
}

fun Instant.singaporeStartOfDay(): Instant {
    val sgZone = ZoneId.of("Asia/Singapore")
    val singaporeDateTime = this.atZone(sgZone)
    val startOfDay = singaporeDateTime.toLocalDate().atStartOfDay(sgZone)
    return startOfDay.toInstant()
}

fun Instant.nextSingaporeStartOfDay(): Instant {
    val sgZone = ZoneId.of("Asia/Singapore")
    val singaporeDateTime = this.atZone(sgZone)
    val nextDay = singaporeDateTime.toLocalDate().plusDays(1).atStartOfDay(sgZone)
    return nextDay.toInstant()
}

fun Instant.isToday(): Boolean {
    val sgZone = ZoneId.of("Asia/Singapore")
    val singaporeDateTime = this.atZone(sgZone)
    val today = singaporeDateTime.toLocalDate()
    return today.isEqual(LocalDate.now(sgZone))
}

fun Instant.plusHours(hour: Int): Instant {
    return this.plusSeconds(hour * 3600L)
}

fun Instant.withHour(hour: Int): Instant {
    val sgZone = ZoneId.of("Asia/Singapore")
    val singaporeDateTime = this.singaporeStartOfDay().atZone(sgZone)
    val dayWithHour = singaporeDateTime.plusHours(hour.toLong())
    return dayWithHour.toInstant()
}

fun Instant.minute(): Int {
    val sgZone = ZoneId.of("Asia/Singapore")
    val singaporeDateTime = this.atZone(sgZone)
    return singaporeDateTime.minute
}

fun Instant.withMinute(minute: Int): Instant {
    val sgZone = ZoneId.of("Asia/Singapore")
    val singaporeDateTime = this.atZone(sgZone)
    val dayWithMinute = singaporeDateTime.withMinute(minute).withSecond(0).withNano(0)
    return dayWithMinute.toInstant()
}

fun Instant.plusMinutes(minutes: Int): Instant {
    return this.plusSeconds(minutes * 60L)
}

fun Instant.isInFutureDays(days: Long = 0): Boolean {
    return this.singaporeStartOfDay().isAfter(Instant.now().singaporeStartOfDay().plusSeconds(days * 86400))
}

fun String.toFullName(lastName: String?): String {
    return if (lastName != null) {
        "$this $lastName"
    } else {
        this
    }
}

fun Instant.looseNanonSeconds(): Instant {
    return this.with(ChronoField.NANO_OF_SECOND, 0)
}

fun Instant.simpleDateFormat(): String {
    return this.format("yyyy-MM-dd")
}

fun Instant.emailDateTimeFormat(): String {
    return this.format("yyyy/MM/dd hh:mm a")
}

fun String.fromEmailDateTimeFormat(): Instant {
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm a").withZone(ZoneId.of("Asia/Singapore"))
    return Instant.from(formatter.parse(this))
}

fun Instant.format(format: String): String {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.of("Asia/Singapore"))
    return formatter.format(this)
}

fun String.encodeSpaces(): String {
    return this.replace(" ", "%20")
}

fun addQueryParams(url: String, params: Map<String, String>): String {
    val uri = URI(url)
    val newQuery = params.map { (key, value) ->
        "$key=$value"
    }.joinToString("&")

    val finalQuery = if (uri.query.isNullOrEmpty()) newQuery else "${uri.query}&$newQuery"

    return URI(
        uri.scheme,
        uri.authority,
        uri.path,
        finalQuery,
        uri.fragment
    ).toString().replace(" ", "%20")
}

fun SplitType.reverse(): SplitType {
    return when (this) {
        SplitType.TheyOweYouAll -> SplitType.YouOweThemAll
        SplitType.YouOweThemAll -> SplitType.TheyOweYouAll
        SplitType.YouPaidSplitEqually -> SplitType.TheyPaidSplitEqually
        SplitType.TheyPaidSplitEqually -> SplitType.YouPaidSplitEqually
    }
}

fun SplitType.apply(amount: BigDecimal): BigDecimal {
    return when (this) {
        SplitType.TheyOweYouAll -> amount
        SplitType.YouOweThemAll -> amount
        SplitType.YouPaidSplitEqually -> splitWithScaleOf1(amount)
        SplitType.TheyPaidSplitEqually -> splitWithScaleOf1(amount)
    }
}

private fun splitWithScaleOf1(amount: BigDecimal): BigDecimal =
    amount.divide(2.toBigDecimal()).also { if (it.scale() == 0) it.setScale(1) }

fun SplitType.isOwed(): Boolean {
    return when (this) {
        SplitType.TheyOweYouAll -> true
        SplitType.YouOweThemAll -> false
        SplitType.YouPaidSplitEqually -> true
        SplitType.TheyPaidSplitEqually -> false
    }
}

fun SplitType.amountForYou(amount: BigDecimal): BigDecimal {
    return when (this) {
        SplitType.TheyOweYouAll -> amount
        SplitType.YouOweThemAll -> amount
        SplitType.YouPaidSplitEqually -> amount / 2.toBigDecimal()
        SplitType.TheyPaidSplitEqually -> amount / 2.toBigDecimal()
    }
}