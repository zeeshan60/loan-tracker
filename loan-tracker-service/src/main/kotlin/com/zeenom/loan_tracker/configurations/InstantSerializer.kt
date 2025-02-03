package com.zeenom.loan_tracker.configurations

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.zeenom.loan_tracker.helpers.toReadableDateFormat
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant


class InstantSerializer : JsonSerializer<Instant>() {
    override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.toReadableDateFormat())
    }
}

class NaNToNullDeserializer : JsonDeserializer<Double?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Double? {
        val value = p.doubleValue
        return if (value.isNaN()) null else value
    }
}

@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        val mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build()

        val javaTimeModule = JavaTimeModule()
        javaTimeModule.addSerializer(Instant::class.java, InstantSerializer())
        mapper.registerModule(javaTimeModule)
        mapper.registerModule(SimpleModule().addDeserializer(Double::class.java, NaNToNullDeserializer()))
        mapper.registerKotlinModule()
        return mapper
    }
}
