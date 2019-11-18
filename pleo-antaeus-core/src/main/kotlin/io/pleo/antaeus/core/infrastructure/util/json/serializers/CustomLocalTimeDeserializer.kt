package io.pleo.antaeus.core.infrastructure.util.json.serializers

import java.io.IOException
import java.time.LocalTime

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class CustomLocalTimeDeserializer : JsonDeserializer<LocalTime>() {
    @Throws(IOException::class)
    override fun deserialize(parser: JsonParser, context: DeserializationContext): LocalTime {
        return LocalTime.parse(parser.text)
    }
}
