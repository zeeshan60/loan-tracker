package com.zeenom.loan_tracker.controllers

import com.zeenom.loan_tracker.LoanTrackerApplication
import com.zeenom.loan_tracker.services.FriendDto
import com.zeenom.loan_tracker.services.FriendsDto
import com.zeenom.loan_tracker.services.LoanAmountDto
import com.zeenom.loan_tracker.services.QueryFriendsService
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = LoanTrackerApplication)
class FriendsControllerSpec extends Specification {

    @Autowired
    private WebTestClient webTestClient

    @MockitoBean
    private QueryFriendsService queryFriendsService

    def "friends endpoint returns friends"() {
        given:
        def friendsResponse = new FriendsDto(
                [
                        new FriendDto("https://example.com/photo.jpg", "John Doe", new LoanAmountDto(100.00, true)),
                        new FriendDto("https://example.com/photo.jpg", "Noman Tufail", new LoanAmountDto(50.00, true)),
                        new FriendDto("https://example.com/photo.jpg", "Zeeshan Tufail", new LoanAmountDto(200.00, true))
                ],
                null
        )
        Mockito.doReturn(Mono.just(friendsResponse)).when(queryFriendsService).execute(Mockito.any())
        expect:
        webTestClient.get().uri("/friends")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.data.friends').isNotEmpty()
                .jsonPath('$.next').isEqualTo(null)
    }
}