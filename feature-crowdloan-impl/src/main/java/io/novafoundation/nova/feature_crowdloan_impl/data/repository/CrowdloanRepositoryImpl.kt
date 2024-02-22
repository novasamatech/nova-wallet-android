package io.novafoundation.nova.feature_crowdloan_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.crowdloan
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.slots
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.LeaseEntry
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.bindFundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.bindLeases
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.LeasePeriodToBlocksConverter
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parachain.ParachainMetadataApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parachain.mapParachainMetadataRemoteToParachainMetadata
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigInteger

class CrowdloanRepositoryImpl(
    private val remoteStorage: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val parachainMetadataApi: ParachainMetadataApi
) : CrowdloanRepository {

    override suspend fun isCrowdloansAvailable(chainId: ChainId): Boolean {
        return runtimeFor(chainId).metadata.hasModule(Modules.CROWDLOAN)
    }

    override suspend fun allFundInfos(chainId: ChainId): Map<ParaId, FundInfo> {
        return remoteStorage.query(chainId) {
            runtime.metadata.crowdloan().storage("Funds").entries(
                keyExtractor = { (paraId: BigInteger) -> paraId },
                binding = { instance, paraId -> bindFundInfo(instance, runtime, paraId) }
            )
        }
    }

    override suspend fun getWinnerInfo(chainId: ChainId, funds: Map<ParaId, FundInfo>): Map<ParaId, Boolean> {
        return remoteStorage.query(chainId) {
            runtime.metadata.slots().storage("Leases").singleArgumentEntries(
                keysArguments = funds.keys,
                binding = { decoded, paraId ->
                    val leases = decoded?.let { bindLeases(it) }
                    val fund = funds.getValue(paraId)

                    leases?.let { isWinner(leases, fund) } ?: false
                }
            )
        }
    }

    private fun isWinner(leases: List<LeaseEntry?>, fundInfo: FundInfo): Boolean {
        return leases.any { it.isOwnedBy(fundInfo.bidderAccountId) || it.isOwnedBy(fundInfo.pre9180BidderAccountId) }
    }

    private fun LeaseEntry?.isOwnedBy(accountId: AccountId): Boolean = this?.accountId.contentEquals(accountId)

    override suspend fun getParachainMetadata(chain: Chain): Map<ParaId, ParachainMetadata> {
        return withContext(Dispatchers.Default) {
            chain.externalApi<Chain.ExternalApi.Crowdloans>()?.let { section ->
                parachainMetadataApi.getParachainMetadata(section.url)
                    .associateBy { it.paraid }
                    .mapValues { (_, remoteMetadata) -> mapParachainMetadataRemoteToParachainMetadata(remoteMetadata) }
            } ?: emptyMap()
        }
    }

    override suspend fun leasePeriodToBlocksConverter(chainId: ChainId): LeasePeriodToBlocksConverter {
        val runtime = runtimeFor(chainId)
        val slots = runtime.metadata.slots()

        return LeasePeriodToBlocksConverter(
            blocksPerLease = slots.numberConstant("LeasePeriod", runtime),
            blocksOffset = slots.numberConstant("LeaseOffset", runtime)
        )
    }

    override fun fundInfoFlow(chainId: ChainId, parachainId: ParaId): Flow<FundInfo> {
        return remoteStorage.observe(
            keyBuilder = { it.metadata.crowdloan().storage("Funds").storageKey(it, parachainId) },
            binder = { scale, runtime -> bindFundInfo(scale!!, runtime, parachainId) },
            chainId = chainId
        )
    }

    override suspend fun minContribution(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.crowdloan().numberConstant("MinContribution", runtime)
    }

    private suspend fun runtimeFor(chainId: String) = chainRegistry.getRuntime(chainId)
}
