package io.novafoundation.nova.feature_crowdloan_impl.presentation.main

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressIcon
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.list.toValueList
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.formatTimeLeft
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.mapParachainMetadataToParcel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.model.CrowdloanModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.model.CrowdloanStatusModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.WithAssetSelector
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

private const val ICON_SIZE_DP = 40

class CrowdloanViewModel(
    private val interactor: CrowdloanInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val crowdloanSharedState: CrowdloanSharedState,
    private val router: CrowdloanRouter,
    private val sharedState: CrowdloanSharedState,
    private val crowdloanUpdateSystem: UpdateSystem,
    assetSelectorFactory: MixinFactory<AssetSelectorMixin.Presentation>,
) : BaseViewModel(), WithAssetSelector {

    override val assetSelectorMixin = assetSelectorFactory.create(scope = this)

    val mainDescription = assetSelectorMixin.selectedAssetFlow.map {
        resourceManager.getString(R.string.crowdloan_main_description, it.token.configuration.symbol)
    }

    private val selectedChain = sharedState.selectedChainFlow()
        .share()

    private val groupedCrowdloansFlow = selectedChain.withLoading {
        interactor.crowdloansFlow(it)
    }
        .inBackground()
        .share()

    private val crowdloansFlow = groupedCrowdloansFlow
        .mapLoading { it.toValueList() }
        .inBackground()
        .share()

    val crowdloanModelsFlow = groupedCrowdloansFlow.mapLoading { groupedCrowdloans ->
        val asset = assetSelectorMixin.selectedAssetFlow.first()
        val chain = crowdloanSharedState.chain()

        groupedCrowdloans
            .mapKeys { (statusClass, values) -> mapCrowdloanStatusToUi(statusClass, values.size) }
            .mapValues { (_, crowdloans) -> crowdloans.map { mapCrowdloanToCrowdloanModel(chain, it, asset) } }
            .toListWithHeaders()
    }
        .inBackground()
        .share()

    init {
        crowdloanUpdateSystem.start()
            .launchIn(this)
    }

    private fun mapCrowdloanStatusToUi(statusClass: KClass<out Crowdloan.State>, statusCount: Int): CrowdloanStatusModel {
        return when (statusClass) {
            Crowdloan.State.Finished::class -> CrowdloanStatusModel(
                text = resourceManager.getString(R.string.common_completed_with_count, statusCount),
                textColorRes = R.color.black1
            )
            Crowdloan.State.Active::class -> CrowdloanStatusModel(
                text = resourceManager.getString(R.string.crowdloan_active_section_format, statusCount),
                textColorRes = R.color.green
            )
            else -> throw IllegalArgumentException("Unsupported crowdloan status type: ${statusClass.simpleName}")
        }
    }

    private suspend fun mapCrowdloanToCrowdloanModel(
        chain: Chain,
        crowdloan: Crowdloan,
        asset: Asset
    ): CrowdloanModel {
        val token = asset.token

        val raisedDisplay = token.amountFromPlanks(crowdloan.fundInfo.raised).format()
        val capDisplay = token.amountFromPlanks(crowdloan.fundInfo.cap).formatTokenAmount(token.configuration)

        val depositorAddress = chain.addressOf(crowdloan.fundInfo.depositor)

        val icon = if (crowdloan.parachainMetadata != null) {
            CrowdloanModel.Icon.FromLink(crowdloan.parachainMetadata.iconLink)
        } else {
            generateDepositorIcon(depositorAddress)
        }

        val stateFormatted = when (val state = crowdloan.state) {
            Crowdloan.State.Finished -> CrowdloanModel.State.Finished

            is Crowdloan.State.Active -> {
                CrowdloanModel.State.Active(
                    timeRemaining = resourceManager.formatTimeLeft(state.remainingTimeInMillis)
                )
            }
        }

        val myContributionDisplay = crowdloan.myContribution?.let {
            val myContributionFormatted = token.amountFromPlanks(it.amount).formatTokenAmount(token.configuration)

            resourceManager.getString(R.string.crowdloan_contribution_format, myContributionFormatted)
        }

        return CrowdloanModel(
            relaychainId = chain.id,
            parachainId = crowdloan.parachainId,
            title = crowdloan.parachainMetadata?.name ?: crowdloan.parachainId.toString(),
            description = crowdloan.parachainMetadata?.description ?: depositorAddress,
            icon = icon,
            raised = resourceManager.getString(R.string.crownloans_raised_format, raisedDisplay, capDisplay),
            myContribution = myContributionDisplay,
            state = stateFormatted,
        )
    }

    private suspend fun generateDepositorIcon(depositorAddress: String): CrowdloanModel.Icon {
        val icon = iconGenerator.createAddressIcon(depositorAddress, ICON_SIZE_DP)

        return CrowdloanModel.Icon.FromDrawable(icon)
    }

    fun crowdloanClicked(paraId: ParaId) {
        launch {
            val crowdloans = crowdloansFlow.first() as? LoadingState.Loaded ?: return@launch
            val crowdloan = crowdloans.data.firstOrNull { it.parachainId == paraId } ?: return@launch

            val payload = ContributePayload(
                paraId = crowdloan.parachainId,
                parachainMetadata = crowdloan.parachainMetadata?.let(::mapParachainMetadataToParcel)
            )

            router.openContribute(payload)
        }
    }
}
