package io.pleo.antaeus.core.infrastructure.util.json.serializers

import java.io.IOException
import java.time.OffsetDateTime

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * Class to deserialize a string into an [OffsetDateTime] class.
 */
class CustomOffsetDateTimeDeserializer : JsonDeserializer<OffsetDateTime>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): OffsetDateTime {
        return OffsetDateTime.parse(jsonParser.text)
    }
}
