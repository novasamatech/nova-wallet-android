package io.novafoundation.nova.feature_crowdloan_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

interface ContributionsRepository {
    fun loadContributionsGraduallyFlow(
        chain: Chain,
        accountId: ByteArray,
        fundInfos: Map<ParaId, FundInfo>,
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): Flow<Pair<Contribution.Type, List<Contribution>>>

    suspend fun isCrowdloansAvailable(chain: Chain): Boolean

    fun observeContributions(metaAccount: MetaAccount): Flow<List<Contribution>>

    fun observeContributions(metaAccount: MetaAccount, chain: Chain): Flow<List<Contribution>>
}
