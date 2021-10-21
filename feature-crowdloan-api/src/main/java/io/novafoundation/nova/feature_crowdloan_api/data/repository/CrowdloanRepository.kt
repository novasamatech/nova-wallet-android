package io.novafoundation.nova.feature_crowdloan_api.data.repository

import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.Contribution
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.TrieIndex
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.BigInteger

interface CrowdloanRepository {

    suspend fun isCrowdloansAvailable(chainId: ChainId): Boolean

    suspend fun allFundInfos(chainId: ChainId): Map<ParaId, FundInfo>

    suspend fun getWinnerInfo(chainId: ChainId, funds: Map<ParaId, FundInfo>): Map<ParaId, Boolean>

    suspend fun getParachainMetadata(chain: Chain): Map<ParaId, ParachainMetadata>

    suspend fun getContribution(chainId: ChainId, accountId: AccountId, paraId: ParaId, trieIndex: TrieIndex): Contribution?

    suspend fun blocksPerLeasePeriod(chainId: ChainId): BigInteger

    fun fundInfoFlow(chainId: ChainId, parachainId: ParaId): Flow<FundInfo>

    suspend fun minContribution(chainId: ChainId): BigInteger
}

class ParachainMetadata(
    val iconLink: String,
    val name: String,
    val description: String,
    val rewardRate: BigDecimal?,
    val website: String,
    val customFlow: String?,
    val token: String
)
