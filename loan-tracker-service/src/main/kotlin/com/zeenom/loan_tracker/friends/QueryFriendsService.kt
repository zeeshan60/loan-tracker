package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.PaginationDto
import com.zeenom.loan_tracker.common.Query
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class QueryFriendsService : Query<PaginationDto, FriendsDto> {
    override suspend fun execute(input: PaginationDto): FriendsDto {
        return FriendsDto(
            friends = listOf(
                FriendDto(
                    photoUrl = "https://example.com/photo.jpg",
                    name = "John Doe",
                    loanAmount = AmountDto(
                        amount = BigDecimal("100.00"),
                        isOwed = true,
                        currency = Currency.getInstance("USD")
                    ),
                    email = "friend1@gmail.com",
                    phoneNumber = "+1234567890"
                ),
                FriendDto(
                    photoUrl = "https://example.com/photo.jpg",
                    name = "Noman Tufail",
                    loanAmount = AmountDto(
                        amount = BigDecimal("50.00"), isOwed = false,
                        currency = Currency.getInstance("USD")
                    ),
                    email = "friend2@gmail.com",
                    phoneNumber = "+1234567891"
                ),
                FriendDto(
                    photoUrl = "https://example.com/photo.jpg",
                    name = "Zeeshan Tufail",
                    loanAmount = AmountDto(
                        amount = BigDecimal("200.00"), isOwed = true,
                        currency = Currency.getInstance("USD")
                    ),
                    email = "friend3@gmail.com",
                    phoneNumber = "+1234567892"
                )
            )
        )
    }
}

