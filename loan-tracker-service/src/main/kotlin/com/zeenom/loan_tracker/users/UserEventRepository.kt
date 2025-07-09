package com.zeenom.loan_tracker.users

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserEventRepository : CoroutineCrudRepository<UserEvent, UUID> {
    @Query(
        """
        select * from user_events 
        where insert_order > (
            select insert_order from user_events where stream_id = :streamId and version = :version
        ) order by insert_order
        """
    )
    suspend fun findAllSinceStreamIdAndVersion(streamId: UUID, version: Int): List<UserEvent>
}

fun userModels(eventStreams: List<UserEvent>) =
    eventStreams.groupBy { it.streamId }.map { (_, events) ->
        userModel(events) ?: throw IllegalStateException("User model not found")
    }

fun userModel(eventStream: List<UserEvent>) =
    eventStream.map { it.toEvent() }.sortedBy { it.version }
        .fold(null as UserModel?) { model, userEvent ->
            userEvent.applyEvent(model)
        }
