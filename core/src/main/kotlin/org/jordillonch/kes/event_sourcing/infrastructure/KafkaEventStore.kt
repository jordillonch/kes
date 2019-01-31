package org.jordillonch.kes.event_sourcing.infrastructure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.event_sourcing.domain.Aggregate
import org.jordillonch.kes.event_sourcing.domain.AggregateId
import org.jordillonch.kes.event_sourcing.domain.EventStore

class KafkaEventStore : EventStore {

    private val publisher = kafkaProducer()
    private val objectMapper = jacksonObjectMapper()

    override fun <A : Aggregate> load(factory: () -> A, id: AggregateId): A {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun append(aggregate: Aggregate, eventSequence: Int, event: Event) {
        ProducerRecord("source-of-truth", aggregate.id().id(), event.toBaseEvent(eventSequence).asJson())
                .let(publisher::send)
                .get()
    }

    private fun kafkaProducer() =
            KafkaProducer<String, String>(mapOf(
                    "bootstrap.servers" to "127.0.0.1:9092",
                    "topic" to "source-of-truth",
                    "key.serializer" to StringSerializer::class.java.canonicalName,
                    "value.serializer" to StringSerializer::class.java.canonicalName))

    private data class BaseEvent(val sequenceNumber: Int, val type: String, val event: Event)

    private fun Event.toBaseEvent(sequenceNumber: Int) =
            BaseEvent(sequenceNumber, this.javaClass.kotlin.simpleName!!, this)

    private fun BaseEvent.asJson() = objectMapper.writeValueAsString(this)
}

