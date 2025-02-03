package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.common.PaginationDto
import com.zeenom.loan_tracker.common.toPaginated
import com.zeenom.loan_tracker.friends.dtos.FriendsResponse
import com.zeenom.loan_tracker.services.QueryFriendsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class FriendsController(
    val queryFriendsService: QueryFriendsService,
    val friendsResponseAdapter: FriendsResponseAdapter
) {


    @Operation(summary = "Get friends", description = "Retrieve a list of friends with pagination")
    @GetMapping("/friends")
    suspend fun getFriends(
        @Parameter(description = "Pagination token for the next set of results") @RequestParam next: String? = null
    ): Paginated<FriendsResponse> {
        return queryFriendsService.execute(PaginationDto(next = next)).let { result ->
            friendsResponseAdapter.fromDto(result)
        }
    }
}
