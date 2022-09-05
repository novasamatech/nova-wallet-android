package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.accumulateFlatten
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_crowdloan_api.data.common.CrowdloanContribution
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.DirectContribution
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.LeasePeriodToBlocksConverter
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.data.repository.getContributions
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.supports
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.leasePeriodInMillis
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import java.math.BigInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ContributionsInteractor(
    private val externalContributionsSources: List<ExternalContributionSource>,
    private val crowdloanRepository: CrowdloanRepository,
    private val accountRepository: AccountRepository,
    private val selectedAssetState: SingleAssetSharedState,
    private val chainStateRepository: ChainStateRepository,
) {

    // return model with count, amount and list of contributions
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeUserContributions(): Flow<ContributionsWithTotalAmount> = flow {
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
            .mapList {
                mapExternalContribution(
                    it,
                    fundInfos.getValue(it.paraId),
                    parachainMetadatas.getValue(it.paraId),
                    blocksPerLeasePeriod,
                    currentBlockNumber,
                    expectedBlockTime
                )
            }

        val allContributionsFlow = accumulateFlatten(directContributionsFlow, externalContributionsFlow)
            .map { sortContributionsByTimeLeft(it) }
            .map {
                val totalAmount = getTotalContributionAmount(it)
                ContributionsWithTotalAmount(totalAmount, it)
            }

        emitAll(allContributionsFlow)
    }

    fun getTotalContributionAmount(contributions: List<Contribution>): BigInteger = contributions.sumOf { it.amount }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun directContributionsFlow(chain: Chain, account: MetaAccount, fundInfos: Map<ParaId, FundInfo>): Flow<Map<ParaId, DirectContribution>> = flowOf {
        val accountId = account.accountIdIn(chain)!!
        crowdloanRepository.getContributions(chain.id, accountId, fundInfos)
            .filterNotNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun externalContributionsFlow(chain: Chain, account: MetaAccount): Flow<List<ExternalContributionSource.Contribution>> {
        val accountId = account.accountIdIn(chain)!!
        val externalContributionFlows = externalContributionsSources
            .filter { it.supports(chain) }
            .map { flowOf { it.getContributions(chain, accountId) } }

        if (externalContributionFlows.isEmpty()) {
            return flowOf { emptyList() }
        }

        return accumulateFlatten(*externalContributionFlows.toTypedArray())
    }

    fun getTotalAmountOfContributions(
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
        directContribution: DirectContribution,
        paraId: ParaId,
        fundInfo: FundInfo,
        parachainMetadata: ParachainMetadata,
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): Contribution {
        return Contribution(
            amount = directContribution.amount,
            paraId = paraId,
            fundInfo = fundInfo,
            sourceName = null,
            parachainMetadata = parachainMetadata,
            returnsIn = fundInfo.returnDuration(blocksPerLeasePeriod, currentBlockNumber, expectedBlockTime)
        )
    }

    private fun mapExternalContribution(
        contribution: ExternalContributionSource.Contribution,
        fundInfo: FundInfo,
        parachainMetadata: ParachainMetadata,
        blocksPerLeasePeriod: LeasePeriodToBlocksConverter,
        currentBlockNumber: BlockNumber,
        expectedBlockTime: BigInteger
    ): Contribution {
        return Contribution(
            amount = contribution.amount,
            parachainMetadata = parachainMetadata,
            sourceName = contribution.sourceName,
            fundInfo = fundInfo,
            paraId = contribution.paraId,
            returnsIn = fundInfo.returnDuration(blocksPerLeasePeriod, currentBlockNumber, expectedBlockTime)
        )
    }

    private fun sortContributionsByTimeLeft(contributions: List<Contribution>): List<Contribution> {
        return contributions.sortedBy { it.returnsIn.millis }
    }
}
