package com.zeenom.loan_tracker.groups

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.r2dbc.toClass
import com.zeenom.loan_tracker.common.r2dbc.toJson
import io.r2dbc.postgresql.codec.Json
import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.DialectResolver

@ReadingConverter
class GroupMemberIdsReadingConverter(
    private val objectMapper: ObjectMapper
) : Converter<Json, GroupMemberIds> {
    override fun convert(source: Json): GroupMemberIds {
        return source.toClass(objectMapper, GroupMemberIds::class.java)
    }
}

@WritingConverter
class GroupMemberIdsWritingConverter(
    private val objectMapper: ObjectMapper
) : Converter<GroupMemberIds, Json> {
    override fun convert(source: GroupMemberIds): Json {
        return source.toJson(objectMapper)
    }
}

@Configuration
class R2dbcJacksonConfig(
    private val objectMapper: ObjectMapper,
    private val connectionFactory: ConnectionFactory
) {

    @Bean
    fun r2dbcCustomConversions(): R2dbcCustomConversions {
        val dialect = DialectResolver.getDialect(connectionFactory)
        return R2dbcCustomConversions.of(
            dialect,
            listOf(
                GroupMemberIdsReadingConverter(objectMapper),
                GroupMemberIdsWritingConverter(objectMapper)
            )
        )
    }
}