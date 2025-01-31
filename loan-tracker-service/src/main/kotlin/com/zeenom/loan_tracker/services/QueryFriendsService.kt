package com.zeenom.loan_tracker.services

import com.zeenom.loan_tracker.dtos.FriendDto
import com.zeenom.loan_tracker.dtos.FriendsDto
import com.zeenom.loan_tracker.dtos.LoanAmountDto
import com.zeenom.loan_tracker.dtos.PaginationDto
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal

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
                    loanAmount = LoanAmountDto(amount = BigDecimal("50.00"), isOwed = false)
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

