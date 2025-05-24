// File: com/example/campusbites/data/util/DateSerializer.kt (Crea este archivo si no existe)

package com.example.campusbites.data.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Date


object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Date {
        return Date(decoder.decodeLong())
    }

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }
}