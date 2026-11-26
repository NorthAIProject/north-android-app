package com.fernandocorreia.khepri.core.api

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Reads RFC 3339 with or without fractions (up to nanoseconds) and with any offset; writes UTC with milliseconds. */
object FlexibleInstantSerializer : KSerializer<Instant> {
    private val writer = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

    override val descriptor = PrimitiveSerialDescriptor("FlexibleInstant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant =
        OffsetDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(writer.format(value.truncatedTo(ChronoUnit.MILLIS)))
    }
}
