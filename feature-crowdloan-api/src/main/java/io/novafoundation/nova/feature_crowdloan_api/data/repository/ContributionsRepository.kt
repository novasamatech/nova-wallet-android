package io.novafoundation.nova.feature_crowdloan_api.data.repository

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow

interface ContributionsRepository {

    fun loadContributionsGraduallyFlow(
        chain: Chain,
        accountId: ByteArray,
    ): Flow<Pair<String, Result<List<Contribution>>>>

    fun observeContributions(metaAccount: MetaAccount): Flow<List<Contribution>>

    fun observeContributions(metaAccount: MetaAccount, chain: Chain, asset: Chain.Asset): Flow<List<Contribution>>

    suspend fun getDirectContributions(chain: Chain, asset: Chain.Asset, accountId: ByteArray): Result<List<Contribution>>

    suspend fun deleteContributions(assetIds: List<FullChainAssetId>)
}
