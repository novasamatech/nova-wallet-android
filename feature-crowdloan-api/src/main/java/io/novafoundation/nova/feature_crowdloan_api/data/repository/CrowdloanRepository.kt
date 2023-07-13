package io.novafoundation.nova.feature_crowdloan_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

interface CrowdloanRepository {

    suspend fun isCrowdloansAvailable(chainId: ChainId): Boolean

    suspend fun allFundInfos(chainId: ChainId): Map<ParaId, FundInfo>

    suspend fun getWinnerInfo(chainId: ChainId, funds: Map<ParaId, FundInfo>): Map<ParaId, Boolean>

    suspend fun getParachainMetadata(chain: Chain): Map<ParaId, ParachainMetadata>

    suspend fun leasePeriodToBlocksConverter(chainId: ChainId): LeasePeriodToBlocksConverter

    fun fundInfoFlow(chainId: ChainId, parachainId: ParaId): Flow<FundInfo>

    suspend fun minContribution(chainId: ChainId): BigInteger
}

class ParachainMetadata(
    val paraId: ParaId,
    val movedToParaId: ParaId?,
    val iconLink: String,
    val name: String,
    val description: String,
    val rewardRate: BigDecimal?,
    val website: String,
    val customFlow: String?,
    val token: String,
    val extras: Map<String, String>,
)

fun ParachainMetadata.getExtra(key: String) = extras[key] ?: throw IllegalArgumentException("No key $key found in parachain metadata extras for $name")
