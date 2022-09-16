package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.accumulateFlatten
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.firstNonEmpty
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_crowdloan_api.data.common.CrowdloanContribution
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.DirectContribution
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdateSystemFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.LeasePeriodToBlocksConverter
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.data.repository.getContributions
import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.supports
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionWithMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsWithTotalAmount
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.leasePeriodInMillis
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RealContributionsInteractor(
    private val externalContributionsSources: List<ExternalContributionSource>,
    private val crowdloanRepository: CrowdloanRepository,
    private val accountRepository: AccountRepository,
    private val selectedAssetState: SingleAssetSharedState, // bring outside
    private val chainStateRepository: ChainStateRepository,
    private val contributionsRepository: ContributionsRepository,
    private val contributionsUpdateSystemFactory: ContributionsUpdateSystemFactory
) : ContributionsInteractor {

    override fun runUpdate(): Flow<Updater.SideEffect> = flow {
        val sideEffectFlow = contributionsUpdateSystemFactory.create()
            .start()

        emitAll(sideEffectFlow)
    }

    override fun observeUserContributions(): Flow<ContributionsWithTotalAmount> = flow {
        val chain = selectedAssetState.chain()
        val metaAccount = accountRepository.getSelectedMetaAccount()

        if (crowdloanRepository.isCrowdloansAvailable(chain.id).not()) {
            return@flow
        }

        val parachainMetadatas = runCatching {
            crowdloanRepository.getParachainMetadata(chain)
        }.getOrDefault(emptyMap())

        val fundInfos = crowdloanRepository.allFundInfos(chain.id)

        val blocksPerLeasePeriod = crowdloanRepository.leasePeriodToBlocksConverter(chain.id)
        val currentBlockNumber = chainStateRepository.currentBlock(chain.id)
        val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chain.id)

        val directContributionsFlow = directContributionsFlow(chain, metaAccount, fundInfos)
            .map { directContributionsMap ->
                directContributionsMap.map {
                    mapDirectContribution(
                        chain,
                        it.value,
                        it.key,
                        fundInfos.getValue(it.key),
                        parachainMetadatas.getValue(it.key),
                        blocksPerLeasePeriod,
                        currentBlockNumber,
                        expectedBlockTime
                    )
                }
            }

        val externalContributionsFlow = externalContributionsFlow(chain, metaAccount)
            .mapList { source ->
                mapExternalContribution(
                    chain,
                    source,
                    fundInfos.getValue(source.paraId),
                    parachainMetadatas.getValue(source.paraId),
                    blocksPerLeasePeriod,
                    currentBlockNumber,
                    expectedBlockTime
                )
            }

        val allContributionsFlow = firstNonEmpty(directContributionsFlow, externalContributionsFlow)
            .map { sortContributionsByTimeLeft(it) }
            .map {
                val totalAmount = getTotalContributionAmount(it)
                ContributionsWithTotalAmount(totalAmount, it)
            }

        emitAll(allContributionsFlow)
    }

    private fun getTotalContributionAmount(contributions: List<ContributionWithMetadata>): BigInteger = contributions.sumOf { it.amountInPlanks }

    private fun directContributionsFlow(chain: Chain, account: MetaAccount, fundInfos: Map<ParaId, FundInfo>): Flow<Map<ParaId, DirectContribution>> = flowOf {
        val accountId = account.accountIdIn(chain)!!
        crowdloanRepository.getContributions(chain.id, accountId, fundInfos)
            .filterNotNull()
    }

    override fun externalContributionsFlow(chain: Chain, account: MetaAccount): Flow<List<ExternalContributionSource.ExternalContribution>> {
        val accountId = account.accountIdIn(chain) ?: return flowOf(emptyList())

        val externalContributionFlows = externalContributionsSources
            .filter { it.supports(chain) }
            .map { flowOf { it.getContributions(chain, accountId) } }

        if (externalContributionFlows.isEmpty()) {
            return flowOf { emptyList() }
        }

        return accumulateFlatten(*externalContributionFlows.toTypedArray())
    }

    override fun getTotalAmountOfContributions(
        crowdloanContributions: List<CrowdloanContribution>
    ): BigInteger {
        return crowdloanContributions.sumOf { it.amount }
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
            chain = chain,
            amount = directContribution.amount,
            paraId = paraId,
            sourceName = null,
            returnsIn = fundInfo.returnDuration(blocksPerLeasePeriod, currentBlockNumber, expectedBlockTime),
            type = Contribution.Type.DIRECT,
            fundInfo = fundInfo,
            parachainMetadata = parachainMetadata,
        )
    }

    private fun mapExternalContribution(
        chain: Chain,
        contribution: ExternalContributionSource.ExternalContribution,
        fundInfo: FundInfo,
        parachainMetadata: ParachainMetadata,
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): ContributionWithMetadata {
        return ContributionWithMetadata(
            chain = chain,
            amount = contribution.amount,
            sourceName = contribution.sourceName,
            paraId = contribution.paraId,
            returnsIn = fundInfo.returnDuration(blocksPerLeasePeriod, currentBlockNumber, expectedBlockTime),
            type = Contribution.Type.DIRECT,
            fundInfo = fundInfo,
            parachainMetadata = parachainMetadata,
        )
    }

    private fun sortContributionsByTimeLeft(contributions: List<ContributionWithMetadata>): List<ContributionWithMetadata> {
        return contributions.sortedBy { it.returnsIn.millis }
    }
}
