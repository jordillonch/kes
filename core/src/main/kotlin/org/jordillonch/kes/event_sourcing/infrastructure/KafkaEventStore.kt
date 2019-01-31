package org.jordillonch.kes.event_sourcing.infrastructure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.owlike.genson.GensonBuilder
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.event_sourcing.domain.Aggregate
import org.jordillonch.kes.event_sourcing.domain.AggregateId
import org.jordillonch.kes.event_sourcing.domain.EventStore
import java.util.*

class KafkaEventStore : EventStore {

    private val topic = "source-of-truth"
    private val publisher = kafkaProducer()
    private val objectMapper = jacksonObjectMapper()
    private val genson = GensonBuilder()
            .useConstructorWithArguments(true)
            .useClassMetadata(true)
            .useRuntimeType(true)
            .create()

    override fun <A : Aggregate> load(factory: () -> A, id: AggregateId): A {
//        Thread.sleep(1000)
//        val consumer = KafkaConsumer<String, String>(kafkaProperties().apply { this["group.id"] = "qwertyui" }.toMap())
//        consumer.subscribe(listOf("source-of-truth"))
//        consumer.seekToBeginning(emptyList())
//        Thread.sleep(1000)
//        val b = consumer.poll(Duration.ofSeconds(10)).records("source-of-truth")
////        val c = b.map { objectMapper.readValue(it.value(), BaseEvent::class.java) }
//        val c = b.map { genson.deserialize(it.value(), BaseEvent::class.java) }

        val config = Properties().apply {
            kafkaProperties().forEach { this[it.key] = it.value }
            this["application.id"] = "qwertyu"
        }
        val builder = StreamsBuilder()
//        val table = builder.table<Any, Any>(topic, Materialized.`as`("my-store-name"))
//                .filter { key, _ -> key.toString() == id.id() }
        builder.stream<Any, Any>(topic)
                .groupByKey()
                .aggregate(factory,
                        { _: Any, event: Any, aggregate: A ->
                            aggregate.process(genson.deserialize(String(event as ByteArray), BaseEvent::class.java).event)
                            aggregate
                        },
                        Materialized.`as`("my-store-name"))
                .mapValues { readOnlyKey, value -> genson.serialize(value) }
        val stream = KafkaStreams(builder.build(), config)
        stream.start()
        Thread.sleep(2000)
        val events = stream.store("my-store-name", QueryableStoreTypes.keyValueStore<String, String>())
                .all().asSequence().toList()
        TODO()
    }

    override fun append(aggregate: Aggregate, eventSequence: Int, event: Event) {
        val serializedEvent = genson.serialize(event.toBaseEvent(eventSequence))
//        val serializedEvent2 = event.toBaseEvent(eventSequence).asJson()
        ProducerRecord("source-of-truth", aggregate.id().id(), serializedEvent)
                .let(publisher::send)
                .get()
    }

    private fun kafkaProducer() =
            KafkaProducer<String, String>(kafkaProperties().toMap())

    private fun kafkaProperties() =
            mutableMapOf("bootstrap.servers" to "127.0.0.1:9092",
                    "topic" to "source-of-truth",
                    "key.serializer" to StringSerializer::class.java.canonicalName,
                    "key.deserializer" to StringDeserializer::class.java.canonicalName,
                    "value.serializer" to StringSerializer::class.java.canonicalName,
                    "value.deserializer" to StringDeserializer::class.java.canonicalName)

    private data class BaseEvent(val sequenceNumber: Int, val type: String, val event: Event)

    private fun Event.toBaseEvent(sequenceNumber: Int) =
            BaseEvent(sequenceNumber, this.javaClass.kotlin.simpleName!!, this)

    private fun BaseEvent.asJson() = objectMapper.writeValueAsString(this)
}

