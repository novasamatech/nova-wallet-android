package io.novafoundation.nova.feature_crowdloan_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.TrieIndex
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface ContributionsRepository {

    fun loadContributionsGraduallyFlow(
        chain: Chain,
        accountId: ByteArray,
        fundInfos: Map<ParaId, FundInfo>
    ): Flow<Pair<String, Result<List<Contribution>>>>

    fun observeContributions(metaAccount: MetaAccount): Flow<List<Contribution>>

    fun observeContributions(metaAccount: MetaAccount, chain: Chain, asset: Chain.Asset): Flow<List<Contribution>>

    suspend fun getDirectContributions(chain: Chain, asset: Chain.Asset, accountId: ByteArray, fundInfos: Map<ParaId, FundInfo>): List<Contribution>

    suspend fun getDirectContribution(chain: Chain, asset: Chain.Asset, accountId: ByteArray, paraId: ParaId, trieIndex: TrieIndex): Contribution?
}
