package org.knowledger.ledger.storage.coinbase.header

import org.knowledger.ledger.config.CoinbaseParams
import org.knowledger.ledger.crypto.Hash
import org.knowledger.ledger.data.Difficulty
import org.knowledger.ledger.data.Payout

internal data class HashedCoinbaseHeaderImpl(
    override val coinbaseParams: CoinbaseParams,
    private var _merkleRoot: Hash,
    private var _payout: Payout,
    private var _difficulty: Difficulty,
    private var _blockheight: Long,
    private var _extraNonce: Long,
    private var _hash: Hash
) : MutableHashedCoinbaseHeader {
    override val merkleRoot: Hash
        get() = _merkleRoot

    override val payout: Payout
        get() = _payout

    override val difficulty: Difficulty
        get() = _difficulty

    override val blockheight: Long
        get() = _blockheight

    override val extraNonce: Long
        get() = _extraNonce

    override val hash: Hash
        get() = _hash

    override fun newNonce() {
        _extraNonce++
    }

    override fun updatePayout(payoutToAdd: Payout) {
        _payout += payoutToAdd
    }

    override fun updateHash(hash: Hash) {
        _hash = hash
    }


    override fun markForMining(
        blockheight: Long, difficulty: Difficulty
    ) {
        _blockheight = blockheight
        _difficulty = difficulty
    }
}