package io.novafoundation.nova.feature_staking_impl.domain.bagList

import io.novafoundation.nova.feature_staking_impl.domain.model.BagListNode
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

interface BagListScoreConverter {

    companion object {

        fun U128(totalIssuance: Balance): BagListScoreConverter = U128BagListScoreConverter(totalIssuance)
    }

    fun scoreOf(stake: Balance): BagListNode.Score

    fun balanceOf(score: BagListNode.Score): Balance
}

private val U64_MAX = BigInteger("18446744073709551615")

private class U128BagListScoreConverter(
    private val totalIssuance: Balance
) : BagListScoreConverter {

    private val factor = (totalIssuance / U64_MAX).max(BigInteger.ONE)

    override fun scoreOf(stake: Balance): BagListNode.Score {
        return BagListNode.Score(stake / factor)
    }

    override fun balanceOf(score: BagListNode.Score): Balance {
        return score.value * factor
    }
}
