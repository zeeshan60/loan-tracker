package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.PaginationDto
import com.zeenom.loan_tracker.common.Query
import org.springframework.stereotype.Service

@Service
class FriendsQuery(private val friendService: FriendService) :
    Query<PaginationDto<String>, FriendsWithAllTimeBalancesDto> {
    override suspend fun execute(input: PaginationDto<String>): FriendsWithAllTimeBalancesDto {
        return friendService.findAllByUserId(input.input)
    }
}

@Service
class FriendQuery(private val friendService: FriendService) : Query<FriendQueryDto, FriendDto> {
    override suspend fun execute(input: FriendQueryDto): FriendDto {
        return friendService.findByUserIdFriendId(input.userId, input.friendEmail, input.friendPhoneNumber)
    }
}

data class FriendQueryDto(val userId: String, val friendEmail: String?, val friendPhoneNumber: String?)
