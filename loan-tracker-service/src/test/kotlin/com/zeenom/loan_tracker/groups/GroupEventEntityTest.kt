package com.zeenom.loan_tracker.groups

import com.zeenom.loan_tracker.integration.BaseIntegration
import com.zeenom.loan_tracker.prettyAndPrint
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.*

class GroupEventEntityTest(@Autowired val groupEventRepository: GroupEventRepository) : BaseIntegration() {

    @Test
    fun `create group successfully`(): Unit = runBlocking {
        val memberAdded = GroupMembersAdded(
            streamId = UUID.randomUUID(),
            version = 1,
            createdAt = Instant.now(),
            createdBy = UUID.randomUUID(),
            memberIds = listOf(UUID.randomUUID(), UUID.randomUUID()),
        ).toEntity()

        groupEventRepository.save(memberAdded)

        val events = groupEventRepository.findAll().toList()
        events.prettyAndPrint(objectMapper = objectMapper)
        assertThat(events).hasSize(1)
        assertThat(events.first().eventType).isEqualTo(GroupEventType.GROUP_MEMBERS_ADDED)
        assertThat(events.first().memberIds).isNotNull
        assertThat(events.first().memberIds!!.ids).hasSize(2)
    }
}