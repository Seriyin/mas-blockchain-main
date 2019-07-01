package pt.um.masb.ledger.storage.adapters


import pt.um.masb.common.database.NewInstanceSession
import pt.um.masb.common.database.StorageElement
import pt.um.masb.common.database.StorageType
import pt.um.masb.common.hash.Hash
import pt.um.masb.common.misc.byteEncodeToPublicKey
import pt.um.masb.common.results.Outcome
import pt.um.masb.common.results.mapFailure
import pt.um.masb.common.results.zip
import pt.um.masb.ledger.config.adapters.ChainIdStorageAdapter
import pt.um.masb.ledger.data.adapters.PhysicalDataStorageAdapter
import pt.um.masb.ledger.results.intoLoad
import pt.um.masb.ledger.results.tryOrLoadUnknownFailure
import pt.um.masb.ledger.service.handles.LedgerHandle
import pt.um.masb.ledger.service.results.LoadFailure
import pt.um.masb.ledger.storage.Transaction

object TransactionStorageAdapter : LedgerStorageAdapter<Transaction> {
    override val id: String
        get() = "Transaction"

    override val properties: Map<String, StorageType>
        get() = mapOf(
            "publicKey" to StorageType.BYTES,
            "chainId" to StorageType.LINK,
            "value" to StorageType.LINK,
            "signature" to StorageType.LINK,
            "hashId" to StorageType.HASH
        )

    override fun store(
        toStore: Transaction,
        session: NewInstanceSession
    ): StorageElement =
        session.newInstance(id).apply {
            setStorageProperty("publicKey", toStore.publicKey.encoded)
            setLinked(
                "chainId", ChainIdStorageAdapter,
                toStore.chainId, session
            )
            setLinked(
                "value", PhysicalDataStorageAdapter,
                toStore.data, session
            )
            setStorageBytes(
                "signature",
                session.newInstance(
                    toStore.signature
                )
            )
            setHashProperty("hashId", toStore.hashId)
        }

    override fun load(
        ledgerHash: Hash,
        element: StorageElement
    ): Outcome<Transaction, LoadFailure> =
        tryOrLoadUnknownFailure {
            val publicKey = byteEncodeToPublicKey(
                element.getStorageProperty("publicKey")
            )

            zip(
                ChainIdStorageAdapter.load(
                    ledgerHash, element.getLinked("chainId")
                ).mapFailure {
                    it.intoLoad()
                },
                PhysicalDataStorageAdapter.load(
                    ledgerHash,
                    element.getLinked("value")
                )
            ) { chainId, data ->
                val signature =
                    element.getStorageBytes("signature").bytes

                val hash =
                    element.getHashProperty("hashId")

                Transaction(
                    chainId,
                    publicKey,
                    data,
                    signature,
                    hash,
                    LedgerHandle.getHasher(ledgerHash)!!
                )
            }
        }
}