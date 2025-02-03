package com.zeenom.loan_tracker.daos

import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.events.*
import io.swagger.v3.core.util.Json
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.*

@SpringBootTest
@ActiveProfiles("local")
class EventDaoTest {

    @Autowired
    private lateinit var secondInstant: SecondInstant

    @Autowired
    private lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var eventDao: EventDao

    @Test
    fun `successfully saves event and reads it back`(): Unit = runBlocking {
        val eventId = "some great transaction id"
        eventRepository.deleteAllByEventId(eventId).awaitSingleOrNull()
        val eventDto = EventDto(
            eventId = eventId,
            event = EventType.CREATE_TRANSACTION,
            payload = EventPayloadDto(
                amount = AmountDto(
                    currency = Currency.getInstance("USD"),
                    amount = 100.0.toBigDecimal(),
                    amountReceivable = true
                ),
                eventReceivers = EventUsersDto(listOf("123")),
            ),
            userId = "123"
        )
        eventDao.saveEvent(eventDto)

        val createdDto = eventDao.findEventByTransactionId(eventId)
        Json.prettyPrint(createdDto)
        assertThat(createdDto).isNotNull
        assertThat(createdDto).isEqualTo(eventDto)

        val entity = eventRepository.findEventEntityByEventId(eventId).awaitSingle()
        assertThat(entity.createdAt).isBeforeOrEqualTo(secondInstant.now())
    }
}
