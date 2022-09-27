package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdateSystemFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.LeasePeriodToBlocksConverter
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionWithMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsWithTotalAmount
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.totalContributionAmount
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.leasePeriodInMillis
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class RealContributionsInteractor(
    private val crowdloanRepository: CrowdloanRepository,
    private val accountRepository: AccountRepository,
    private val selectedAssetCrowdloanState: SingleAssetSharedState,
    private val chainStateRepository: ChainStateRepository,
    private val contributionsRepository: ContributionsRepository,
    private val contributionsUpdateSystemFactory: ContributionsUpdateSystemFactory,
    private val chainRegistry: ChainRegistry,
) : ContributionsInteractor {

    override fun runUpdate(): Flow<Updater.SideEffect> {
        return contributionsUpdateSystemFactory.create()
            .start()
    }

    override fun observeTotalContributedByAssets(): Flow<Map<ChainAssetId, BigInteger>> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            contributionsRepository.observeContributions(metaAccount)
        }.map {
            it.groupBy { it.asset.id }
                .mapValues { entry -> entry.value.sumOf { it.amountInPlanks } }
        }
    }

    override fun observeSelectedChainContributions(): Flow<ContributionsWithTotalAmount> {
        val metaAccountFlow = accountRepository.selectedMetaAccountFlow()
        val chainFlow = selectedAssetCrowdloanState.assetWithChain.map { it.chain }
        return combineToPair(metaAccountFlow, chainFlow)
            .filter { (_, chain) -> crowdloanRepository.isCrowdloansAvailable(chain.id) }
            .flatMapLatest { (metaAccount, chain) ->
                observeChainContributions(metaAccount, chain, chain.utilityAsset)
            }
    }

    override fun observeChainContributions(chainId: ChainId, assetId: ChainAssetId): Flow<ContributionsWithTotalAmount> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest {
            val (chain, asset) = chainRegistry.chainWithAsset(chainId, assetId)
            if (chain.hasCrowdloans) {
                observeChainContributions(it, chain, asset)
            } else {
                emptyFlow()
            }
        }
    }

    private suspend fun getParachainMetadata(chain: Chain): Map<ParaId, ParachainMetadata> {
        return runCatching {
            crowdloanRepository.getParachainMetadata(chain)
        }.getOrDefault(emptyMap())
    }

    private fun FundInfo.returnDuration(
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): TimerValue {
        val millis = leasePeriodInMillis(
            leasePeriodToBlocksConverter = blocksPerLeasePeriod,
            currentBlockNumber = currentBlockNumber,
            endingLeasePeriod = lastSlot,
            expectedBlockTimeInMillis = expectedBlockTime,
        )

        return TimerValue.fromCurrentTime(millis)
    }

    private fun getMetadata(
        fundInfo: FundInfo,
        parachainMetadata: ParachainMetadata,
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): ContributionMetadata {
        return ContributionMetadata(
            returnsIn = fundInfo.returnDuration(blocksPerLeasePeriod, currentBlockNumber, expectedBlockTime),
            fundInfo = fundInfo,
            parachainMetadata = parachainMetadata,
        )
    }

    private fun sortContributionsByTimeLeft(contributions: List<ContributionWithMetadata>): List<ContributionWithMetadata> {
        return contributions.sortedBy { it.metadata.returnsIn.millis }
    }

    private suspend fun observeChainContributions(metaAccount: MetaAccount, chain: Chain, asset: Chain.Asset): Flow<ContributionsWithTotalAmount> {
        val parachainMetadatas = getParachainMetadata(chain)
        val fundInfos = crowdloanRepository.allFundInfos(chain.id)
        val blocksPerLeasePeriod = crowdloanRepository.leasePeriodToBlocksConverter(chain.id)
        val currentBlockNumber = chainStateRepository.currentBlock(chain.id)
        val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chain.id)

        return contributionsRepository.observeContributions(metaAccount, chain, asset)
            .mapList { contribution ->
                ContributionWithMetadata(
                    contribution,
                    getMetadata(
                        fundInfos.getValue(contribution.paraId),
                        parachainMetadatas.getValue(contribution.paraId),
                        blocksPerLeasePeriod,
                        currentBlockNumber,
                        expectedBlockTime
                    )
                )
            }.map {
                ContributionsWithTotalAmount(
                    it.totalContributionAmount(),
                    sortContributionsByTimeLeft(it)
                )
            }
    }
}
