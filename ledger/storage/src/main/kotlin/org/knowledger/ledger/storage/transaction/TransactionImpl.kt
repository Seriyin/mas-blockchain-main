package org.knowledger.ledger.storage.transaction

import org.knowledger.ledger.crypto.EncodedPublicKey
import org.knowledger.ledger.storage.PhysicalData

internal data class TransactionImpl(
    // Agent's pub key.
    override val publicKey: EncodedPublicKey,
    override val data: PhysicalData,
) : Transaction {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transaction) return false

        if (publicKey != other.publicKey) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }


}
