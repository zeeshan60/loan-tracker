package com.zeenom.loan_tracker.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = QueryFriendsService)
class QueryFriendsServiceTest extends Specification {

    @Autowired
    QueryFriendsService queryFriendsService

    def "given pagination dto when execute query returns friends dto successfully"() {
        given:
        def paginationDto = new PaginationDto(null)

        when:
        def friendsDto = queryFriendsService.execute(paginationDto).block()

        then:
        friendsDto != null
        !friendsDto.friends.isEmpty()
    }
}
