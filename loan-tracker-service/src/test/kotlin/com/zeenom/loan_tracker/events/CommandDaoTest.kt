package com.zeenom.loan_tracker.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.transactions.AmountDto
import com.zeenom.loan_tracker.common.JacksonConfig
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.transactions.TransactionDto
import com.zeenom.loan_tracker.common.r2dbc.toClass
import com.zeenom.loan_tracker.friends.TestPostgresConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.*

@DataR2dbcTest
@ActiveProfiles("test")
@Import(JacksonConfig::class)
class CommandDaoTest(
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val objectMapper: ObjectMapper,
) : TestPostgresConfig() {

    private val secondInstant = SecondInstant()
    private val commandDao = CommandDao(
        eventRepository = eventRepository,
        objectMapper = objectMapper,
    )

    @Test
    fun `successfully saves event and reads it back`(): Unit = runBlocking {
        eventRepository.deleteAll()
        val commandDto = CommandDto(
            commandType = CommandType.CREATE_TRANSACTION,
            payload = TransactionDto(
                amount = AmountDto(
                    currency = Currency.getInstance("USD"),
                    amount = 100.0.toBigDecimal(),
                    isOwed = true
                ),
                recipientId = UUID.randomUUID(),
            ),
            userId = "123",
        )
        commandDao.saveEvent(commandDto)


        val entity = eventRepository.findAll().first()
        assertThat(entity.commandType).isEqualTo(commandDto.commandType)
        assertThat(entity.userId).isEqualTo(commandDto.userId)
        assertThat(entity.payload?.toClass(objectMapper, CommandPayloadDto::class.java)).isEqualTo(commandDto.payload)
        assertThat(entity.createdAt).isBeforeOrEqualTo(secondInstant.now())
    }
}
