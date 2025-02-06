package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.PaginationDto
import com.zeenom.loan_tracker.common.Query
import org.springframework.stereotype.Service

@Service
class QueryFriendsService(private val friendsDao: FriendsDao) : Query<PaginationDto<String>, FriendsDto> {
    override suspend fun execute(input: PaginationDto<String>): FriendsDto {
        return friendsDao.findAllByUserId(input.input)
    }
}

