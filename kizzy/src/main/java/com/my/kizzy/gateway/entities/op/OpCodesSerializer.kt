package com.my.kizzy.gateway.entities.op

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class OpCodeSerializer : KSerializer<OpCode> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("OpCode", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): OpCode {
        val opCode = decoder.decodeInt()
        return OpCode.values().firstOrNull { it.value == opCode } ?: throw IllegalArgumentException("Unknown OpCode $opCode")
    }

    override fun serialize(encoder: Encoder, value: OpCode) {
        encoder.encodeInt(value.value)
    }
}