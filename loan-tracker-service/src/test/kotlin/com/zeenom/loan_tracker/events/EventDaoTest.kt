package com.zeenom.loan_tracker.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.TransactionDto
import com.zeenom.loan_tracker.common.r2dbc.toClass
import com.zeenom.loan_tracker.test_configs.TestSecondInstantConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.*

@SpringBootTest
@ActiveProfiles("local")
@Import(TestSecondInstantConfig::class)
class EventDaoTest {

    @Autowired
    private lateinit var secondInstant: SecondInstant

    @Autowired
    private lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var eventDao: EventDao

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `successfully saves event and reads it back`(): Unit = runBlocking {
        eventRepository.deleteAll()
        val eventDto = EventDto(
            event = EventType.CREATE_TRANSACTION,
            payload = TransactionDto(
                amount = AmountDto(
                    currency = Currency.getInstance("USD"),
                    amount = 100.0.toBigDecimal(),
                    isOwed = true
                ),
                recipientId = "123",
            ),
            userId = "123",
        )
        eventDao.saveEvent(eventDto)


        val entity = eventRepository.findAll().first()
        assertThat(entity.event).isEqualTo(eventDto.event)
        assertThat(entity.userId).isEqualTo(eventDto.userId)
        assertThat(entity.payload?.toClass(objectMapper, EventPayloadDto::class.java)).isEqualTo(eventDto.payload)
        assertThat(entity.createdAt).isBeforeOrEqualTo(secondInstant.now())
    }
}
