package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.services.PaginationDto
import com.zeenom.loan_tracker.services.QueryFriendsService
import com.zeenom.loan_tracker.services.toPaginated
import com.zeenom.loan_tracker.services.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@CrossOrigin
@RestController
@RequestMapping("/api/v1")
class FriendsController(val queryFriendsService: QueryFriendsService) {


    @Operation(summary = "Get friends", description = "Retrieve a list of friends with pagination")
    @GetMapping("/friends")
    fun getFriends(
        @Parameter(description = "Pagination token for the next set of results") @RequestParam next: String? = null
    ): Mono<Paginated<FriendsResponse>> {
        return queryFriendsService.execute(PaginationDto(next = next)).map { result ->
            result.toResponse().toPaginated(result.next)
        }
    }
}
