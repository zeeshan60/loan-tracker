package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.PaginationDto
import com.zeenom.loan_tracker.common.Query
import org.springframework.stereotype.Service

@Service
class FriendsQuery(private val friendService: FriendService) : Query<PaginationDto<String>, FriendsWithAllTimeBalancesDto> {
    override suspend fun execute(input: PaginationDto<String>): FriendsWithAllTimeBalancesDto {
        return friendService.findAllByUserId(input.input)
    }
}
