package io.novafoundation.nova.feature_staking_impl.data.dashboard.repository

import io.novafoundation.nova.common.utils.associateWithIndex
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface TotalStakeChainComparatorProvider {

    suspend fun getTotalStakeComparator(): Comparator<Chain>
}

class RealTotalStakeChainComparatorProvider: TotalStakeChainComparatorProvider {

    private val positionByGenesisHash by lazy {
        listOf(
            Chain.Geneses.POLKADOT,
            Chain.Geneses.KUSAMA,
            Chain.Geneses.ALEPH_ZERO,
            Chain.Geneses.MOONBEAM,
            Chain.Geneses.MOONRIVER,
            Chain.Geneses.TERNOA,
            Chain.Geneses.POLKADEX,
            Chain.Geneses.CALAMARI,
            Chain.Geneses.ZEITGEIST,
            Chain.Geneses.TURING
        ).associateWithIndex()
    }

    override suspend fun getTotalStakeComparator(): Comparator<Chain> {
        return compareBy {
            positionByGenesisHash[it.id] ?: Int.MAX_VALUE
        }
    }
}
