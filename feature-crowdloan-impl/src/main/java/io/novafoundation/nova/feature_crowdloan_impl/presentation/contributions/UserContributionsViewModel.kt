package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionWithMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.model.ContributionModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.model.generateCrowdloanIcon
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class UserContributionsViewModel(
    private val interactor: ContributionsInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val selectedAssetState: SingleAssetSharedState,
    private val resourceManager: ResourceManager,
    private val router: CrowdloanRouter,
    private val tokenUseCase: TokenUseCase
) : BaseViewModel() {

    private val tokenFlow = tokenUseCase.currentTokenFlow()
        .shareInBackground()

    private val contributionsWitTotalAmountFlow = interactor.observeSelectedChainContributionsWithMetadata()
        .shareInBackground()

    private val contributionsFlow = contributionsWitTotalAmountFlow
        .map { it.contributions }
        .shareInBackground()

    val contributionModelsFlow = combine(tokenFlow, contributionsFlow) { token, contributions ->
        val chain = selectedAssetState.chain()
        contributions.map { mapCrowdloanToContributionModel(it, chain, token) }
    }
        .withLoading()
        .shareInBackground()

    val totalContributedAmountFlow = combine(contributionsWitTotalAmountFlow, tokenFlow) { contributionsWitTotalAmount, token ->
        mapAmountToAmountModel(contributionsWitTotalAmount.totalContributed, token)
    }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    private suspend fun mapCrowdloanToContributionModel(
        contributionWithMetadata: ContributionWithMetadata,
        chain: Chain,
        token: Token,
    ): ContributionModel {
        val depositorAddress = chain.addressOf(contributionWithMetadata.metadata.fundInfo.depositor)
        val contributionTitle = mapContributionTitle(contributionWithMetadata)

        return ContributionModel(
            title = contributionTitle,
            icon = generateCrowdloanIcon(contributionWithMetadata.metadata.parachainMetadata, depositorAddress, iconGenerator),
            amount = mapAmountToAmountModel(contributionWithMetadata.contribution.amountInPlanks, token),
            returnsIn = contributionWithMetadata.metadata.returnsIn
        )
    }

    private fun mapContributionTitle(contributionWithMetadata: ContributionWithMetadata): String {
        val parachainName = contributionWithMetadata.metadata.parachainMetadata?.name
            ?: contributionWithMetadata.contribution.paraId.toString()

        val sourceName = when (contributionWithMetadata.contribution.sourceId) {
            Contribution.DIRECT_SOURCE_ID -> null
            Contribution.LIQUID_SOURCE_ID -> resourceManager.getString(R.string.crowdloan_contributions_liquid_source)
            Contribution.PARALLEL_SOURCE_ID -> resourceManager.getString(R.string.crowdloan_contributions_parallel_source)
            else -> contributionWithMetadata.contribution.sourceId.capitalize()
        }

        return if (sourceName == null) {
            parachainName
        } else {
            resourceManager.getString(
                R.string.crowdloan_contributions_with_source,
                parachainName,
                sourceName
            )
        }
    }
}
