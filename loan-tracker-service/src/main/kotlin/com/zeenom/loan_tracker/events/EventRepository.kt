package com.zeenom.loan_tracker.events

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EventRepository : CoroutineCrudRepository<EventEntity, String>