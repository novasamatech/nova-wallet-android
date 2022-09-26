package io.novafoundation.nova.feature_crowdloan_api.domain.contributions

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface ContributionsInteractor {
    fun runUpdate(): Flow<Updater.SideEffect>

    fun observeTotalContributedByAssets(): Flow<Map<ChainAssetId, BigInteger>>

    fun observeSelectedChainContributions(): Flow<ContributionsWithTotalAmount>

    fun observeChainContributions(chainId: ChainId, assetId: ChainAssetId): Flow<ContributionsWithTotalAmount>
}
