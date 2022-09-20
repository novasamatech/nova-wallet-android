package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.DirectContribution
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdateSystemFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.LeasePeriodToBlocksConverter
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionWithMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsWithTotalAmount
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.leasePeriodInMillis
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class RealContributionsInteractor(
    private val crowdloanRepository: CrowdloanRepository,
    private val accountRepository: AccountRepository,
    private val selectedAssetCrowdloanState: SingleAssetSharedState,
    private val chainStateRepository: ChainStateRepository,
    private val contributionsRepository: ContributionsRepository,
    private val contributionsUpdateSystemFactory: ContributionsUpdateSystemFactory
) : ContributionsInteractor {

    override fun runUpdate(): Flow<Updater.SideEffect> {
        return contributionsUpdateSystemFactory.create()
            .start()
    }

    override fun observeChainContributions(): Flow<ContributionsWithTotalAmount> {
        val metaAccountFlow = accountRepository.selectedMetaAccountFlow()
        val chainFlow = selectedAssetCrowdloanState.assetWithChain.map { it.chain }
        return combine(metaAccountFlow, chainFlow) { metaAccount, chain -> metaAccount to chain }
            .filter { (_, chain) -> crowdloanRepository.isCrowdloansAvailable(chain.id) }
            .flatMapLatest { (metaAccount, chain) ->

                val parachainMetadatas = getParachainMetadata(chain)
                val fundInfos = crowdloanRepository.allFundInfos(chain.id)
                val blocksPerLeasePeriod = crowdloanRepository.leasePeriodToBlocksConverter(chain.id)
                val currentBlockNumber = chainStateRepository.currentBlock(chain.id)
                val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chain.id)

                contributionsRepository.observeContributions(metaAccount, chain)
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
                            getTotalAmountOfContributions(it),
                            sortContributionsByTimeLeft(it)
                        )
                    }
            }
    }

    private suspend fun getParachainMetadata(chain: Chain): Map<ParaId, ParachainMetadata> {
        return runCatching {
            crowdloanRepository.getParachainMetadata(chain)
        }.getOrDefault(emptyMap())
    }

    private fun getTotalAmountOfContributions(
        crowdloanContributions: List<ContributionWithMetadata>
    ): BigInteger {
        return crowdloanContributions.sumOf { it.contribution.amountInPlanks }
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

        return TimerValue(millis, millisCalculatedAt = System.currentTimeMillis())
    }

    private fun mapDirectContribution(
        chain: Chain,
        directContribution: DirectContribution,
        paraId: ParaId,
        fundInfo: FundInfo,
        parachainMetadata: ParachainMetadata,
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): ContributionWithMetadata {
        return ContributionWithMetadata(
            contribution = Contribution(
                chain = chain,
                amountInPlanks = directContribution.amount,
                paraId = paraId,
                sourceId = directContribution.sourceId,
            ),
            metadata = ContributionMetadata(
                returnsIn = fundInfo.returnDuration(blocksPerLeasePeriod, currentBlockNumber, expectedBlockTime),
                fundInfo = fundInfo,
                parachainMetadata = parachainMetadata,
            )
        )
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
}
