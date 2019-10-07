package org.knowledger.ledger.serial

import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.SerialClassDescImpl
import org.knowledger.ledger.config.CoinbaseParams
import org.knowledger.ledger.core.hash.Hash
import org.knowledger.ledger.core.misc.hashFromHexString
import kotlin.properties.Delegates

@Serializer(forClass = CoinbaseParams::class)
object CoinbaseParamsSerializer : KSerializer<CoinbaseParams> {
    override val descriptor: SerialDescriptor =
        object : SerialClassDescImpl("CoinbaseParams") {
            init {
                addElement("timeIncentive")
                addElement("valueIncentive")
                addElement("baseIncentive")
                addElement("dividingThreshold")
                addElement("formula")
            }
        }

    override fun deserialize(decoder: Decoder): CoinbaseParams =
        with(decoder.beginStructure(descriptor)) {
            var timeIncentive by Delegates.notNull<Long>()
            var valueIncentive by Delegates.notNull<Long>()
            var baseIncentive by Delegates.notNull<Long>()
            var dividingThreshold by Delegates.notNull<Long>()
            lateinit var formula: Hash
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.READ_DONE -> break@loop
                    0 -> timeIncentive = decodeLongElement(
                        descriptor, i
                    )
                    1 -> valueIncentive = decodeLongElement(
                        descriptor, i
                    )
                    2 -> baseIncentive = decodeLongElement(
                        descriptor, i
                    )
                    3 -> dividingThreshold = decodeLongElement(
                        descriptor, i
                    )
                    4 -> formula = decodeStringElement(
                        descriptor, i
                    ).hashFromHexString()
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            endStructure(descriptor)
            CoinbaseParams(
                timeIncentive = timeIncentive,
                valueIncentive = valueIncentive,
                baseIncentive = baseIncentive,
                dividingThreshold = dividingThreshold,
                formula = formula
            )
        }

    override fun serialize(encoder: Encoder, obj: CoinbaseParams) {
        with(encoder.beginStructure(descriptor)) {
            encodeLongElement(
                descriptor, 0, obj.timeIncentive
            )
            encodeLongElement(
                descriptor, 1, obj.valueIncentive
            )
            encodeLongElement(
                descriptor, 2, obj.baseIncentive
            )
            encodeLongElement(
                descriptor, 3, obj.dividingThreshold
            )
            encodeStringElement(
                descriptor, 4, obj.formula.print
            )
            endStructure(descriptor)
        }
    }
}