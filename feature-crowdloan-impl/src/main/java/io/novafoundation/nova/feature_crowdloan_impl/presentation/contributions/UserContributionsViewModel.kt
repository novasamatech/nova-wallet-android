package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionClaimStatus
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionWithMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.model.ContributionModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.model.generateCrowdloanIcon
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class UserContributionsViewModel(
    private val interactor: ContributionsInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val selectedAssetState: SingleAssetSharedState,
    private val resourceManager: ResourceManager,
    private val router: CrowdloanRouter,
    private val tokenUseCase: TokenUseCase,
    private val amountFormatter: AmountFormatter
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
        amountFormatter.formatAmountToAmountModel(contributionsWitTotalAmount.totalContributed, token)
    }
        .shareInBackground()

    val claimContributionsVisible = contributionsWitTotalAmountFlow.map {
        it.contributions.any { it.metadata.claimStatus is ContributionClaimStatus.Claimable }
    }
        .onStart { emit(false) }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun claimClicked() {
        router.openClaimContribution()
    }

    private suspend fun mapCrowdloanToContributionModel(
        contributionWithMetadata: ContributionWithMetadata,
        chain: Chain,
        token: Token,
    ): ContributionModel {
        val depositorAddress = chain.addressOf(contributionWithMetadata.contribution.leaseDepositor)
        val contributionTitle = mapContributionTitle(contributionWithMetadata)

        val claimStatus: ContributionModel.ClaimStatus
        val claimStatusColorRes: Int

        when (val status = contributionWithMetadata.metadata.claimStatus) {
            ContributionClaimStatus.Claimable -> {
                claimStatus = ContributionModel.ClaimStatus.Text(resourceManager.getString(R.string.crowdloan_contribution_claimable))
                claimStatusColorRes = R.color.text_positive
            }
            is ContributionClaimStatus.ReturnsIn -> {
                claimStatus = ContributionModel.ClaimStatus.Timer(status.timer)
                claimStatusColorRes = R.color.text_secondary
            }
        }

        return ContributionModel(
            title = contributionTitle,
            icon = generateCrowdloanIcon(contributionWithMetadata.metadata.parachainMetadata, depositorAddress, iconGenerator),
            amount = amountFormatter.formatAmountToAmountModel(contributionWithMetadata.contribution.amountInPlanks, token),
            claimStatus = claimStatus,
            claimStatusColorRes = claimStatusColorRes
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
