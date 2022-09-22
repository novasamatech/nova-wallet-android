package io.novafoundation.nova.feature_crowdloan_api.domain.contributions

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface ContributionsInteractor {
    fun runUpdate(): Flow<Updater.SideEffect>

    fun observeChainContributions(): Flow<ContributionsWithTotalAmount>

    fun observeChainContributions(chainId: ChainId, assetId: ChainAssetId): Flow<ContributionsWithTotalAmount>
}
