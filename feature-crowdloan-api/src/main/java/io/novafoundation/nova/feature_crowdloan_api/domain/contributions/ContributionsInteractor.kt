package io.novafoundation.nova.feature_crowdloan_api.domain.contributions

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface ContributionsInteractor {
    fun runUpdate(): Flow<Updater.SideEffect>

    fun observeTotalContributedByAssets(): Flow<Map<FullChainAssetId, BigInteger>>

    fun observeSelectedChainContributionsWithMetadata(): Flow<ContributionsWithTotalAmount<ContributionWithMetadata>>

    fun observeChainContributions(
        metaAccount: MetaAccount,
        chainId: ChainId,
        assetId: ChainAssetId
    ): Flow<ContributionsWithTotalAmount<Contribution>>
}
