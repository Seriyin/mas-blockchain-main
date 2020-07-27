package org.knowledger.ledger.storage.serial

import kotlinx.serialization.Encoder
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerializationStrategy
import org.knowledger.ledger.storage.BlockParams
import org.knowledger.ledger.storage.config.block.ImmutableBlockParams
import org.knowledger.ledger.storage.immutableCopy

object BlockParamsSerializationStrategy : SerializationStrategy<BlockParams> {
    private val serializer = ImmutableBlockParams.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: BlockParams) {
        encoder.encodeSerializableValue(
            serializer, value as? ImmutableBlockParams ?: value.immutableCopy()
        )
    }
}