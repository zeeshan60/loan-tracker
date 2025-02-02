package com.zeenom.loan_tracker.dynamo

import io.swagger.v3.core.util.Json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class DynamoDbDaoTest {
    @Autowired
    private lateinit var dynamoDbDao: DynamoDbDao

    @Test
    fun `list tables successfully`(): Unit = runBlocking {
        dynamoDbDao.deleteTable().also {
            Json.prettyPrint(it)
        }
        dynamoDbDao.createTable().also {
            Json.prettyPrint(it)
        }
        delay(1000)
        dynamoDbDao.listTables().also {
            Json.prettyPrint(it)
        }
    }
}