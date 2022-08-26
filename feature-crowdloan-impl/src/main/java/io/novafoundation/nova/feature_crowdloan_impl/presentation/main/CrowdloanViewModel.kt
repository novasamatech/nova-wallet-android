package io.novafoundation.nova.feature_crowdloan_impl.presentation.main

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.list.toValueList
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.formatTimeLeft
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.fractionToPercentage
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull.StatefulCrowdloanMixin
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.mapParachainMetadataToParcel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.model.CrowdloanModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.model.CrowdloanStatusModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.model.generateCrowdloanIcon
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.WithAssetSelector
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class CrowdloanViewModel(
    private val iconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val crowdloanSharedState: CrowdloanSharedState,
    private val router: CrowdloanRouter,
    private val customContributeManager: CustomContributeManager,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    crowdloanUpdateSystem: UpdateSystem,
    assetSelectorFactory: MixinFactory<AssetSelectorMixin.Presentation>,
    statefulCrowdloanMixinFactory: StatefulCrowdloanMixin.Factory,
    customDialogDisplayer: CustomDialogDisplayer,
) : BaseViewModel(),
    WithAssetSelector,
    CustomDialogDisplayer by customDialogDisplayer {

    override val assetSelectorMixin = assetSelectorFactory.create(scope = this)

    val mainDescription = assetSelectorMixin.selectedAssetFlow.map {
        resourceManager.getString(R.string.crowdloan_main_description_v2_2_0, it.token.configuration.symbol)
    }

    private val crowdloansMixin = statefulCrowdloanMixinFactory.create(scope = this)

    private val crowdloansListFlow = crowdloansMixin.groupedCrowdloansFlow
        .mapLoading { it.toValueList() }
        .inBackground()
        .share()

    val crowdloanModelsFlow = crowdloansMixin.groupedCrowdloansFlow.mapLoading { groupedCrowdloans ->
        val asset = assetSelectorMixin.selectedAssetFlow.first()
        val chain = crowdloanSharedState.chain()

        groupedCrowdloans
            .mapKeys { (statusClass, values) -> mapCrowdloanStatusToUi(statusClass, values.size) }
            .mapValues { (_, crowdloans) -> crowdloans.map { mapCrowdloanToCrowdloanModel(chain, it, asset) } }
            .toListWithHeaders()
    }
        .inBackground()
        .share()

    val myContributionsCount = crowdloansMixin.allUserContributions
        .mapLoading { it.format() }
        .inBackground()
        .share()

    val selectedWalletModel = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    init {
        crowdloanUpdateSystem.start()
            .launchIn(this)
    }

    private fun mapCrowdloanStatusToUi(statusClass: KClass<out Crowdloan.State>, statusCount: Int): CrowdloanStatusModel {
        return when (statusClass) {
            Crowdloan.State.Finished::class -> CrowdloanStatusModel(
                status = resourceManager.getString(R.string.crowdloan_completed_section),
                count = statusCount.toString()
            )
            Crowdloan.State.Active::class -> CrowdloanStatusModel(
                status = resourceManager.getString(R.string.crowdloan_active_section),
                count = statusCount.toString()
            )
            else -> throw IllegalArgumentException("Unsupported crowdloan status type: ${statusClass.simpleName}")
        }
    }

    private suspend fun mapCrowdloanToCrowdloanModel(
        chain: Chain,
        crowdloan: Crowdloan,
        asset: Asset,
    ): CrowdloanModel {
        val token = asset.token

        val raisedDisplay = token.amountFromPlanks(crowdloan.fundInfo.raised).format()
        val capDisplay = token.amountFromPlanks(crowdloan.fundInfo.cap).formatTokenAmount(token.configuration)

        val depositorAddress = chain.addressOf(crowdloan.fundInfo.depositor)

        val stateFormatted = when (val state = crowdloan.state) {
            Crowdloan.State.Finished -> CrowdloanModel.State.Finished

            is Crowdloan.State.Active -> {
                CrowdloanModel.State.Active(
                    timeRemaining = resourceManager.formatTimeLeft(state.remainingTimeInMillis)
                )
            }
        }

        val raisedPercentage = crowdloan.raisedFraction.fractionToPercentage()

        return CrowdloanModel(
            relaychainId = chain.id,
            parachainId = crowdloan.parachainId,
            title = crowdloan.parachainMetadata?.name ?: crowdloan.parachainId.toString(),
            description = crowdloan.parachainMetadata?.description ?: depositorAddress,
            icon = generateCrowdloanIcon(crowdloan.parachainMetadata, depositorAddress, iconGenerator),
            raised = CrowdloanModel.Raised(
                value = resourceManager.getString(R.string.crownloans_raised_format, raisedDisplay, capDisplay),
                percentage = raisedPercentage.toInt(),
                percentageDisplay = raisedPercentage.formatAsPercentage()
            ),
            state = stateFormatted,
        )
    }

    fun crowdloanClicked(paraId: ParaId) {
        launch {
            val crowdloans = crowdloansListFlow.first() as? LoadingState.Loaded ?: return@launch
            val crowdloan = crowdloans.data.firstOrNull { it.parachainId == paraId } ?: return@launch

            val payload = ContributePayload(
                paraId = crowdloan.parachainId,
                parachainMetadata = crowdloan.parachainMetadata?.let(::mapParachainMetadataToParcel)
            )

            val startFlowInterceptor = crowdloan.parachainMetadata?.customFlow?.let { customFlow ->
                customContributeManager.getFactoryOrNull(customFlow)?.startFlowInterceptor
            }

            if (startFlowInterceptor != null) {
                startFlowInterceptor.startFlow(payload)
            } else {
                router.openContribute(payload)
            }
        }
    }

    fun myContributionsClicked() {
        router.openUserContributions()
    }

    fun avatarClicked() {
        router.openSwitchWallet()
    }
}
