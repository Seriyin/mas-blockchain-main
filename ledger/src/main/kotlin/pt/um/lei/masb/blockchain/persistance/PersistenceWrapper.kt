package pt.um.lei.masb.blockchain.persistance

import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.record.OElement
import mu.KLogging
import pt.um.lei.masb.blockchain.data.BlockChainData
import pt.um.lei.masb.blockchain.data.Loadable
import pt.um.lei.masb.blockchain.ledger.*
import pt.um.lei.masb.blockchain.persistance.loaders.ChainLoadable
import pt.um.lei.masb.blockchain.persistance.loaders.DefaultLoadable
import pt.um.lei.masb.blockchain.persistance.loaders.LoaderManager
import pt.um.lei.masb.blockchain.persistance.query.ClusterSelect
import pt.um.lei.masb.blockchain.persistance.query.Filters
import pt.um.lei.masb.blockchain.persistance.query.GenericQuery
import pt.um.lei.masb.blockchain.persistance.schema.PreConfiguredSchemas
import pt.um.lei.masb.blockchain.persistance.schema.SchemaProvider
import pt.um.lei.masb.blockchain.service.results.LedgerResult
import java.security.PublicKey


/**
 * A Thread-safe wrapper into a DB context
 * for the blockchain library.
 */
internal class PersistenceWrapper(
    private val db: ManagedSession
) {

    private val dbSchema = db.session.metadata.schema

    init {
        registerDefaultSchemas()
    }

    private fun registerDefaultSchemas(
    ) {
        PreConfiguredSchemas
            .chainSchemas
            .forEach {
                registerSchema(
                    it
                )
            }
    }

    fun registerSchema(
        schemaProvider: SchemaProvider
    ) {
        if (!dbSchema.existsClass(schemaProvider.id)) {
            createSchema(
                dbSchema,
                schemaProvider
            )
        } else {
            replaceSchema(
                dbSchema,
                schemaProvider
            )
        }
    }

    private fun createSchema(
        schema: OSchema,
        provider: SchemaProvider
    ) {
        val cl = schema.createClass(provider.id)
        provider.properties.forEach {
            cl.createProperty(it.key, it.value)
        }
    }


    private fun replaceSchema(schema: OSchema, provider: SchemaProvider) {
        val cl = schema.getClass(provider.id)
        val (propsIn, propsNotIn) = cl
            .declaredProperties()
            .partition {
                it.name in provider.properties.keys
            }

        if (propsNotIn.isNotEmpty()) {
            //Drop properties that no longer exist in provider.
            propsNotIn.forEach {
                cl.dropProperty(it.name)
            }
        }

        if (propsIn.size != provider.properties.keys.size) {
            //New properties are those in provider that are not already present.
            val toAdd = provider.properties.keys.filter { elem ->
                elem !in propsIn.map { it.name }
            }
            toAdd.forEach {
                cl.createProperty(
                    it,
                    provider.properties[it]
                )
            }
        }
    }

    internal fun registerDefaultClusters(
        blockChainId: Hash
    ) =
    //For all schemas except Ident add a new chain-specific cluster.
        PreConfiguredSchemas.chainSchemas.forEach {
            dbSchema.getClass(
                it.id
            ).addCluster(
                "${it.id}${blockChainId.truncated()}"
            )
        }


    @Synchronized
    internal fun executeWithSuccessInCurrentSession(
        executable: (
            ODatabaseDocument
        ) -> Boolean
    ): Boolean =
        let {
            db.reOpenIfNecessary()
            executable(db.session)
        }


    @Synchronized
    internal fun executeInCurrentSession(
        executable: (
            ODatabaseDocument
        ) -> Unit
    ): PersistenceWrapper =
        apply {
            db.reOpenIfNecessary()
            executable(db.session)
        }


    @Synchronized
    internal fun <R> executeInSessionAndReturn(
        function: (
            ODatabaseDocument
        ) -> R
    ): R =
        let {
            db.reOpenIfNecessary()
            function(db.session)
        }


    @Synchronized
    internal fun closeCurrentSession(): PersistenceWrapper =
        apply {
            db.session.close()
        }

    fun getInstanceSession(): NewInstanceSession =
        NewInstanceSession(db.session)

    /**
     * Not to be used directly.
     * Requires knowledge of inner workings of DB.
     *
     * Requires:
     * - The [session] in which to execute the query.
     * - A [query] with the command to execute
     * and it's arguments.
     *
     * *Note:* Two arguments are required for any query
     * - A [LedgerId]'s hash
     * - A type id [String], the same as used to register
     * that class of data as per [Storable] and [SchemaProvider].
     *
     * Returns an optional result as [T]?
     */
    private fun <T : BlockChainData> getUniqueResult(
        session: ODatabaseDocument,
        query: GenericQuery,
        loader: Loadable<T>
    ): T? =
        try {
            val rs = session.query(
                query.query,
                query.params
            )
            if (rs.hasNext()) {
                (loader.load)(rs.next().toElement())
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn(e) {}
            null
        }

    /**
     * Not to be used directly.
     * Requires knowledge of inner workings of DB.
     *
     * Requires:
     * - The [session] in which to execute the query.
     * - A [query] with the command to execute
     * and it's arguments.
     * - A [DefaultLoadable]<[T]> to load a [T]
     * type object from the a database element.
     * [DefaultLoadable]s apply *exclusively*
     * to [LedgerContract] classes.
     *
     *
     * *Note:* One extra argument is required for any query
     * over a [DefaultLoadable]:
     * - A [LedgerId]'s hash
     *
     *
     * Returns an optional result as [T]?
     */
    private fun <T : LedgerContract> getUniqueResult(
        session: ODatabaseDocument,
        blockChainId: Hash,
        query: GenericQuery,
        loader: DefaultLoadable<T>
    ): T? =
        try {
            val rs = session.query(
                query.query,
                query.params
            )
            if (rs.hasNext()) {
                (loader.load)(
                    blockChainId,
                    rs.next().toElement()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn(e) {}
            null
        }


    /**
     * Not to be used directly.
     * Requires knowledge of inner workings of DB.
     *
     * Requires:
     * - The [session] in which to execute the query.
     * - A [query] with the command to execute
     * and it's arguments.
     * - A [DefaultLoadable]<[T]> to load a [T]
     * type object from the a database element.
     * [DefaultLoadable]s apply *exclusively*
     * to [LedgerContract] classes.
     *
     *
     * *Note:* One extra argument is required for any query
     * over a [DefaultLoadable]:
     * - A [LedgerId]'s hash
     *
     *
     * Returns an optional result as [T]?
     */
    private fun <T : LedgerContract> getUniqueResult(
        session: ODatabaseDocument,
        query: GenericQuery,
        loader: ChainLoadable<T>
    ): T? =
        try {
            val rs = session.query(
                query.query,
                query.params
            )
            if (rs.hasNext()) {
                (loader.load)(
                    this,
                    rs.next().toElement()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn(e) {}
            null
        }


    /**
     * Not to be used directly.
     * Requires knowledge of inner workings of DB.
     *
     * Requires:
     * - The [session] in which to execute the query.
     * - A [query] with the command to execute
     * and it's arguments.
     *
     *
     * Returns an optional result as a potentially empty
     * [List] of [T]
     */
    private fun <T : BlockChainData> getResults(
        session: ODatabaseDocument,
        query: GenericQuery,
        loader: Loadable<T>
    ): List<T> =
        try {
            val ls = mutableListOf<T>()
            session.query(
                query.query,
                query.params
            ).forEach { r ->
                r.element.map {
                    (loader.load)(it)
                }.ifPresent {
                    ls.add(it)
                }
            }
            ls
        } catch (e: Exception) {
            logger.warn(e) {}
            emptyList()
        }

    /**
     * Not to be used directly.
     * Requires knowledge of inner workings of DB.
     *
     * Requires:
     * - The [session] in which to execute the query.
     * - A [query] with the command to execute
     * and it's arguments.
     *
     * *Note:* One extra argument is required for any query
     * over a [DefaultLoadable]:
     * - A [LedgerId]'s hash
     *
     * Returns an optional result as a potentially empty
     * [List] of [T]
     */
    private fun <T : LedgerContract> getResults(
        session: ODatabaseDocument,
        blockChainId: Hash,
        query: GenericQuery,
        loader: DefaultLoadable<T>
    ): List<T> =
        try {
            val ls = mutableListOf<T>()
            session.query(
                query.query,
                query.params
            ).forEach { r ->
                r.element.map {
                    (loader.load)(blockChainId, it)
                }.ifPresent {
                    ls.add(it)
                }
            }
            ls
        } catch (e: Exception) {
            logger.warn(e) {}
            emptyList()
        }


    /**
     * Requires an active [session], the [element] to
     * persist and the [logger] to write errors.
     *
     * Returns whether the element was successfully saved.
     */
    private fun persistEntity(
        session: ODatabaseDocument,
        element: OElement
    ): Boolean =
        try {
            val r = session.save<OElement>(element)
            r == element
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }


    /**
     * Should be called when querying is finished to reset the persistence context.
     */
    internal fun clearTransactionsContext() =
        closeCurrentSession()

    private fun <R : LedgerContract> queryUniqueResult(
        id: String,
        blockChainId: Hash,
        query: GenericQuery
    ): R? =
        executeInSessionAndReturn {
            getUniqueResult(
                it,
                blockChainId,
                query,
                LoaderManager.getFromDefault(id)
            )
        }

    private fun <R : LedgerContract> queryUniqueResult(
        id: String,
        query: GenericQuery
    ): R? =
        executeInSessionAndReturn {
            getUniqueResult(
                it,
                query,
                LoaderManager.getFromChains(id)
            )
        }


    private fun <R : LedgerContract> queryResults(
        id: String,
        blockChainId: Hash,
        query: GenericQuery
    ): List<R> =
        executeInSessionAndReturn {
            getResults(
                it,
                blockChainId,
                query,
                LoaderManager.getFromDefault(id)
            )
        }


    internal fun persistEntity(
        element: Storable
    ): Boolean =
        executeWithSuccessInCurrentSession {
            persistEntity(
                db.session,
                element.store(
                    NewInstanceSession(db.session)
                )
            )
        }


// ------------------------------
// Blockchain transactions.
//
// ------------------------------

    /**
     *
     */
    internal fun getBlockChain(
        blockChainId: LedgerId
    ): LedgerResult =
        "LedgerHandle".let {
            queryUniqueResult(
                it,
                ClusterSelect(
                    it,
                    blockChainId.hash
                ).withSimpleFilter(
                    Filters.WHERE,
                    "ledgerId.hash",
                    "hash",
                    blockChainId.hash
                )
            )
        }

    internal fun getBlockChain(
        hash: Hash
    ): LedgerResult =
        "LedgerHandle".let {
            queryUniqueResult(
                it,
                ClusterSelect(
                    it,
                    hash
                ).withSimpleFilter(
                    Filters.WHERE,
                    "ledgerId.hash",
                    "hash",
                    hash
                )
            )
        }

    // ------------------------------
// Blockheader transactions.
//
// ------------------------------
    internal fun getBlockHeaderByHash(
        blockChainId: Hash,
        hash: Hash
    ): BlockHeader? =
        "BlockHeader".let {
            queryUniqueResult(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.WHERE,
                    "hash",
                    "hash",
                    hash
                )
            )
        }


    internal fun getBlockHeaderByBlockHeight(
        blockChainId: Hash,
        height: Long
    ): BlockHeader? =
        "BlockHeader".let {
            queryUniqueResult(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.WHERE, "blockheight",
                    "blockheight",
                    height
                )
            )
        }


    internal fun getBlockHeaderByPrevHeaderHash(
        blockChainId: Hash,
        hash: Hash
    ): BlockHeader? =
        "BlockHeader".let {
            queryUniqueResult(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.WHERE,
                    "previousHash",
                    "hash",
                    hash
                )
            )

        }

    internal fun getLatestBlockHeader(
        blockChainId: Hash
    ): BlockHeader? =
        "BlockHeader".let {
            queryUniqueResult(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId,
                    "max(blockheight), *"
                )
            )
        }

    // ------------------------------
// Block transactions.
//
// ------------------------------
    internal fun getBlockByBlockHeight(
        blockChainId: Hash,
        blockheight: Long
    ): Block? =
        "Block".let {
            queryUniqueResult(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.WHERE,
                    "header.blockheight",
                    "blockheight",
                    blockheight
                )
            )
        }


    internal fun getBlockByHeaderHash(
        blockChainId: Hash,
        hash: Hash
    ): Block? =
        "Block".let {
            queryUniqueResult(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.WHERE,
                    "header.hash",
                    "hash",
                    hash
                )
            )
        }


    internal fun getBlockByPrevHeaderHash(
        blockChainId: Hash,
        hash: Hash
    ): Block? =
        "Block".let {
            queryUniqueResult(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.WHERE,
                    "header.previousHash",
                    "hash",
                    hash
                )
            )
        }


    internal fun getLatestBlock(
        blockChainId: Hash
    ): Block? =
        "Block".let {
            queryUniqueResult(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withProjection(
                    "max(header.blockheight), *"
                )
            )
        }


    // ------------------------------
    // Ident transaction.
    //
    // ------------------------------
    internal fun getIdent(id: String): OElement? =
        executeInSessionAndReturn {
            val rs = it.query(
                "SELECT * FROM ident WHERE id = :id",
                id
            )
            if (rs.hasNext()) {
                rs.next().toElement()
            } else {
                null
            }
        }


    // ------------------------------
// Transactions over transactions.
// Go figure.
//
// Execution must be runtime determined.
// ------------------------------
    fun getTransactionsFromAgent(
        blockChainId: Hash,
        publicKey: PublicKey
    ): List<Transaction> =
        "Transaction".let {
            queryResults(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.WHERE,
                    "publicKey",
                    "publicKey",
                    publicKey.encoded
                )
            )
        }

    fun getTransactionByHash(
        blockChainId: Hash,
        hash: Hash
    ): Transaction? =
        "Transaction".let {
            queryUniqueResult(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.WHERE,
                    "hashId",
                    "hash",
                    hash
                )
            )
        }


    //Execution must be runtime determined.
    fun getTransactionsOrderedByTimestamp(
        blockChainId: Hash
    ): List<Transaction> =
        "Transaction".let {
            queryResults(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.ORDER,
                    "data.seconds DESC, data.nanos DESC"
                )
            )
        }

    fun getTransactionsByClass(
        blockChainId: Hash,
        typeName: String
    ): List<Transaction> =
        "Transaction".let {
            queryResults(
                it,
                blockChainId,
                ClusterSelect(
                    it,
                    blockChainId
                ).withSimpleFilter(
                    Filters.WHERE,
                    "data.data.@class",
                    "typeName",
                    typeName
                )
            )
        }

    companion object : KLogging()
}