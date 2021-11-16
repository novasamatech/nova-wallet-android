package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_impl.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.model.ContributionModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.model.generateCrowdloanIcon
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chainAndAsset
import kotlinx.coroutines.flow.map

class UserContributionsViewModel(
    private val interactor: ContributionsInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val selectedAssetState: SingleAssetSharedState,
    private val resourceManager: ResourceManager,
    private val router: CrowdloanRouter,
) : BaseViewModel() {

    val userContributionsFlow = flowOf { interactor.getUserContributions() }
        .map { contributions ->
            val (chain, chainAsset) = selectedAssetState.chainAndAsset()

            contributions.map { mapCrowdloanToContributionModel(it, chain, chainAsset) }
        }
        .withLoading()
        .inBackground()
        .share()

    fun backClicked() {
        router.back()
    }

    private suspend fun mapCrowdloanToContributionModel(
        contribution: Contribution,
        chain: Chain,
        chainAsset: Chain.Asset,
    ): ContributionModel {
        val depositorAddress = chain.addressOf(contribution.fundInfo.depositor)
        val parachainName = contribution.parachainMetadata?.name ?: contribution.paraId.toString()

        val contributionTitle = if (contribution.sourceName != null) {
            resourceManager.getString(R.string.crowdloan_contributions_with_source, parachainName, contribution.sourceName)
        } else {
            parachainName
        }

        return ContributionModel(
            title = contributionTitle,
            icon = generateCrowdloanIcon(contribution.parachainMetadata, depositorAddress, iconGenerator),
            amount = chainAsset.amountFromPlanks(contribution.amount).formatTokenAmount(chainAsset)
        )
    }
}
