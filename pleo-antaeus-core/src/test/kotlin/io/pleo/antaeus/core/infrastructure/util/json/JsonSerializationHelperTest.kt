package io.pleo.antaeus.core.infrastructure.util.json

import io.pleo.antaeus.core.infrastructure.util.json.exceptions.JsonSerializationException
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class JsonSerializationHelperTest {

    private var testObjectToSerialize: TestObject = TestObject(id, name, number, occurredAt, active)
    private var serializedJsonString: String = String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"number\":%s,\"occurredAt\":\"%s\",\"active\":%s}", id, name, number,
            occurredAt.toZonedDateTime().toInstant().atZone(ZoneId.of("UTC")).toOffsetDateTime(),
            active.toString())

    @Test
    @Throws(JsonSerializationException::class)
    fun `test serializing to json`() {
        assert(serializedJsonString == JsonSerializationHelper.serializeToJson(testObjectToSerialize))
    }

    @Test
    @Throws(JsonSerializationException::class)
    fun `test OffsetDateTime is ISO8601 formatted while serializing to json `() {
        val offsetDateTime = OffsetDateTime.now()
        val expected = String.format("\"%s\"", offsetDateTime.toZonedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
        assert(expected == JsonSerializationHelper.serializeToJson(offsetDateTime))
    }

    @Test
    @Throws(JsonSerializationException::class)
    fun `test exception are handled and wrapped in JsonSerializationException while serializing to json`() {
        assertThrows<JsonSerializationException> { JsonSerializationHelper.serializeToJson(Any()) }
    }

    @Test
    @Throws(JsonSerializationException::class)
    fun `test deserializing from json using object class`() {
        val deserializedObject = JsonSerializationHelper.deserializeFromJson(
                serializedJsonString, TestObject::class.java
        )
        assertDeserializedObject(testObjectToSerialize, deserializedObject)
    }

    private fun assertDeserializedObject(expectedObject: TestObject, deserializedTestObject: TestObject) {
        assert(expectedObject.id == deserializedTestObject.id)
        assert(expectedObject.name == deserializedTestObject.name)
        assert(expectedObject.number == deserializedTestObject.number)
        assert(
                expectedObject.occurredAt!!.toZonedDateTime().toInstant().atZone(ZoneId.of("UTC"))
                        .toOffsetDateTime() == deserializedTestObject.occurredAt)
        assert(expectedObject.isActive == deserializedTestObject.isActive)
    }

    companion object {
        private const val id = "f94b5928-0074-4419-801b-394d3b1310bc"
        private const val name = "test-name"
        private const val number = 1234
        private val occurredAt = OffsetDateTime.of(
                LocalDate.now(),
                LocalTime.of(7, 0, 1),
                ZoneOffset.UTC
        )
        private const val active = true
    }
}
