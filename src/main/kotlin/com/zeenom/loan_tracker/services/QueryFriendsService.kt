package com.zeenom.loan_tracker.services

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal

interface Query<IN, OUT> {
    fun execute(input: IN): OUT
}

data class FriendsDto(val friends: List<FriendDto>, val next: String? = null)

data class FriendDto(
    val photoUrl: String?,
    val name: String,
    val loanAmount: LoanAmountDto,
)

data class LoanAmountDto(val amount: BigDecimal, val isOwed: Boolean)

data class PaginationDto(val next: String?)

@Service
class QueryFriendsService : Query<PaginationDto, Mono<FriendsDto>> {
    override fun execute(input: PaginationDto): Mono<FriendsDto> {
        return FriendsDto(
            friends = listOf(
                FriendDto(
                    photoUrl = "https://example.com/photo.jpg",
                    name = "John Doe",
                    loanAmount = LoanAmountDto(amount = BigDecimal("100.00"), isOwed = true)
                ),
                FriendDto(
                    photoUrl = "https://example.com/photo.jpg",
                    name = "Noman Tufail",
                    loanAmount = LoanAmountDto(amount = BigDecimal("50.00"), isOwed = true)
                ),
                FriendDto(
                    photoUrl = "https://example.com/photo.jpg",
                    name = "Zeeshan Tufail",
                    loanAmount = LoanAmountDto(amount = BigDecimal("200.00"), isOwed = true)
                )
            )
        ).toMono()
    }
}

