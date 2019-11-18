package io.pleo.antaeus.core.infrastructure.util.json

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.pleo.antaeus.core.infrastructure.util.json.exceptions.JsonSerializationException
import io.pleo.antaeus.core.infrastructure.util.json.serializers.CustomLocalTimeDeserializer
import io.pleo.antaeus.core.infrastructure.util.json.serializers.CustomLocalTimeSerializer
import io.pleo.antaeus.core.infrastructure.util.json.serializers.CustomOffsetDateTimeDeserializer
import io.pleo.antaeus.core.infrastructure.util.json.serializers.CustomOffsetDateTimeSerializer
import java.time.LocalTime
import java.time.OffsetDateTime

object JsonSerializationHelper {
    /**
     * Method to serialize an Object into a JSON string.
     *
     * @param objectToSerialize  Object to serialize
     * @param modules an array of custom [Module] extensions to default functionality
     * @return String
     * @throws JsonSerializationException an exception used to wrap any caught [Throwable]
     */
    @Throws(JsonSerializationException::class)
    fun serializeToJson(objectToSerialize: Any, vararg modules: Module): String {
        try {
            return getObjectMapper(*modules).writeValueAsString(objectToSerialize)
        } catch (throwable: Throwable) {
            throw JsonSerializationException(throwable)
        }
    }

    /**
     * Method to deserialize a JSON string into an Object of specified type.
     *
     * @param jsonString    JSON string
     * @param objectClass   Object class
     * @param modules       an array of custom [Module] extensions to default functionality
     * @param <ObjectClass> object class
     * @return objectClass
     * @throws JsonSerializationException an exception used to wrap any caught [Throwable]
    </ObjectClass> */
    @Throws(JsonSerializationException::class)
    fun <ObjectClass> deserializeFromJson(
            jsonString: String,
            objectClass: Class<ObjectClass>,
            vararg modules: Module
    ): ObjectClass {
        try {
            return getObjectMapper(*modules).readValue(jsonString, objectClass)
        } catch (throwable: Throwable) {
            throw JsonSerializationException(throwable)
        }
    }

    private fun getObjectMapper(vararg modules: Module): ObjectMapper {
        val javaTimeModule = JavaTimeModule()

        // offset date time
        javaTimeModule.addDeserializer(OffsetDateTime::class.java, CustomOffsetDateTimeDeserializer())
        javaTimeModule.addSerializer(OffsetDateTime::class.java, CustomOffsetDateTimeSerializer())

        // local time
        javaTimeModule.addDeserializer(LocalTime::class.java, CustomLocalTimeDeserializer())
        javaTimeModule.addSerializer(LocalTime::class.java, CustomLocalTimeSerializer())

        val objectMapper = ObjectMapper()
        objectMapper.registerModule(javaTimeModule)
//        objectMapper.registerModules(modules)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return objectMapper
    }
}
