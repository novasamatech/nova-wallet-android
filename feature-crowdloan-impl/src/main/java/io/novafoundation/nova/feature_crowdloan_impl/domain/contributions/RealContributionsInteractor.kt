package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.formatting.toTimerValue
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdateSystemFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionWithMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsWithTotalAmount
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimatorFlow
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import kotlin.time.Duration.Companion.days

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

    override fun observeSelectedChainContributionsWithMetadata(): Flow<ContributionsWithTotalAmount<ContributionWithMetadata>> {
        val metaAccountFlow = accountRepository.selectedMetaAccountFlow()
        val chainFlow = selectedAssetCrowdloanState.selectedChainFlow()
        return combineToPair(metaAccountFlow, chainFlow)
            .flatMapLatest { (metaAccount, chain) ->
                observeChainContributionsWithMetadata(metaAccount, chain, chain.utilityAsset)
            }
    }

    override fun observeChainContributions(
        metaAccount: MetaAccount,
        chainId: ChainId,
        assetId: ChainAssetId
    ): Flow<ContributionsWithTotalAmount<Contribution>> {
        return flow {
            val (chain, asset) = chainRegistry.chainWithAsset(chainId, assetId)

            emitAll(contributionsRepository.observeContributions(metaAccount, chain, asset))
        }.map { contributions ->
            contributions.totalContributions { it.amountInPlanks }
        }
    }

    private suspend fun getParachainMetadata(chain: Chain): Map<ParaId, ParachainMetadata> {
        return runCatching {
            crowdloanRepository.getParachainMetadata(chain)
        }.getOrDefault(emptyMap())
    }

    private fun List<ContributionWithMetadata>.sortByTimeLeft(): List<ContributionWithMetadata> {
        return sortedBy { it.metadata.returnsIn.millis }
    }

    private suspend fun observeChainContributionsWithMetadata(
        metaAccount: MetaAccount,
        chain: Chain,
        asset: Chain.Asset
    ): Flow<ContributionsWithTotalAmount<ContributionWithMetadata>> {
        val parachainMetadatas = getParachainMetadata(chain)

        return combine(
            chainStateRepository.blockDurationEstimatorFlow(chain.timelineChainIdOrSelf()),
            contributionsRepository.observeContributions(metaAccount, chain, asset)
        ) { blockDurationEstimator, contributions ->
            contributions.map { contribution ->
                val parachainMetadata = parachainMetadatas[contribution.paraId]
                // TODO test code
                val returnsIn = (blockDurationEstimator.durationUntil(contribution.unlockBlock) - 10.days).toTimerValue()
//                val returnsIn = blockDurationEstimator.timerUntil(contribution.unlockBlock)

                ContributionWithMetadata(
                    contribution = contribution,
                    metadata = ContributionMetadata(
                        returnsIn = returnsIn,
                        parachainMetadata = parachainMetadata,
                    )
                )
            }
                .sortByTimeLeft()
                .totalContributions { it.contribution.amountInPlanks }
        }
    }

    private fun <T> List<T>.totalContributions(amount: (T) -> BigInteger): ContributionsWithTotalAmount<T> {
        return ContributionsWithTotalAmount(
            totalContributed = sumByBigInteger(amount),
            contributions = this
        )
    }
}
