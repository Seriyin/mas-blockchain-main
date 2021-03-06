package org.knowledger.ledger.storage.transaction.factory

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.ExperimentalSerializationApi
import org.knowledger.ledger.crypto.EncodedPrivateKey
import org.knowledger.ledger.crypto.EncodedPublicKey
import org.knowledger.ledger.crypto.EncodedSignature
import org.knowledger.ledger.crypto.Hash
import org.knowledger.ledger.crypto.hash.Hashers
import org.knowledger.ledger.storage.MutableTransaction
import org.knowledger.ledger.storage.PhysicalData
import org.knowledger.ledger.storage.Transaction
import org.knowledger.ledger.storage.transaction.SignedTransaction
import org.knowledger.ledger.storage.transaction.StorageAwareTransactionImpl
import org.knowledger.ledger.storage.transaction.Transaction as RawTransaction

@OptIn(ExperimentalSerializationApi::class)
internal class StorageAwareTransactionFactory(
    private val transactionFactory: TransactionFactory = HashedTransactionFactory()
) : TransactionFactory {
    private fun createSA(mutableTransaction: MutableTransaction): StorageAwareTransactionImpl =
        StorageAwareTransactionImpl(mutableTransaction)

    override fun create(
        privateKey: EncodedPrivateKey, publicKey: EncodedPublicKey,
        data: PhysicalData, hashers: Hashers, encoder: BinaryFormat, index: Int,
    ): StorageAwareTransactionImpl =
        createSA(transactionFactory.create(privateKey, publicKey, data, hashers, encoder, index))

    override fun create(
        publicKey: EncodedPublicKey, data: PhysicalData,
        signature: EncodedSignature, hash: Hash, size: Int, index: Int,
    ): StorageAwareTransactionImpl =
        createSA(transactionFactory.create(publicKey, data, signature, hash, size, index))

    override fun create(
        publicKey: EncodedPublicKey, data: PhysicalData, signature: EncodedSignature,
        hasher: Hashers, encoder: BinaryFormat, index: Int,
    ): StorageAwareTransactionImpl =
        createSA(transactionFactory.create(publicKey, data, signature, hasher, encoder, index))

    override fun create(
        transaction: SignedTransaction, hasher: Hashers, encoder: BinaryFormat, index: Int,
    ): StorageAwareTransactionImpl =
        createSA(transactionFactory.create(transaction, hasher, encoder, index))

    override fun create(
        privateKey: EncodedPrivateKey, transaction: RawTransaction,
        hasher: Hashers, encoder: BinaryFormat, index: Int,
    ): StorageAwareTransactionImpl =
        createSA(transactionFactory.create(privateKey, transaction, hasher, encoder, index))

    override fun create(transaction: Transaction): StorageAwareTransactionImpl =
        createSA(transactionFactory.create(transaction))

    override fun create(other: MutableTransaction): StorageAwareTransactionImpl =
        createSA(transactionFactory.create(other))
}