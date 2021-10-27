package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.model.ContributionModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.model.generateCrowdloanIcon
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.map

class UserContributionsViewModel(
    private val interactor: CrowdloanInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val router: CrowdloanRouter,
    sharedState: CrowdloanSharedState,
) : BaseViewModel() {

    val userContributionsFlow = sharedState.assetWithChain.withLoading { (chain, asset) ->
        interactor.contributedCrowdloansFlow(chain).map { crowdloans ->
            crowdloans.map { mapCrowdloanToContributionModel(it, chain, asset) }
        }
    }
        .inBackground()
        .share()

    fun backClicked() {
        router.back()
    }

    private suspend fun mapCrowdloanToContributionModel(
        crowdloan: Crowdloan,
        chain: Chain,
        chainAsset: Chain.Asset,
    ): ContributionModel {
        val depositorAddress = chain.addressOf(crowdloan.fundInfo.depositor)

        return ContributionModel(
            name = crowdloan.parachainMetadata?.name ?: crowdloan.parachainId.toString(),
            icon = generateCrowdloanIcon(crowdloan, depositorAddress, iconGenerator),
            amount = chainAsset.amountFromPlanks(crowdloan.myContribution!!.amount).formatTokenAmount(chainAsset)
        )
    }
}
