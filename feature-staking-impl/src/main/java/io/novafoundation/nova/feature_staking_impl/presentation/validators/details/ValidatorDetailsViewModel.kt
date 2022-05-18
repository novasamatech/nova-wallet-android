package io.novafoundation.nova.feature_staking_impl.presentation.validators.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapStakeTargetDetailsToErrors
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorDetailsParcelToValidatorDetailsModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetStakeParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakerParcelModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ValidatorDetailsViewModel(
    private val assetUseCase: AssetUseCase,
    private val router: StakingRouter,
    private val payload: StakeTargetDetailsPayload,
    private val iconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val appLinksProvider: AppLinksProvider,
    private val resourceManager: ResourceManager,
    private val selectedAssetState: SingleAssetSharedState,
) : BaseViewModel(), ExternalActions.Presentation by externalActions {

    private val stakeTarget = payload.stakeTarget
    val displayConfig = payload.displayConfig

    private val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    val stakeTargetDetails = assetFlow.map { asset ->
        mapValidatorDetailsParcelToValidatorDetailsModel(
            chain = selectedAssetState.chain(),
            validator = stakeTarget,
            asset = asset,
            displayConfig = payload.displayConfig,
            iconGenerator = iconGenerator,
            resourceManager = resourceManager
        )
    }
        .shareInBackground()

    val errorFlow = flowOf { mapStakeTargetDetailsToErrors(stakeTarget) }
        .inBackground()
        .share()

    private val _openEmailEvent = MutableLiveData<Event<String>>()
    val openEmailEvent: LiveData<Event<String>> = _openEmailEvent

    private val _totalStakeEvent = MutableLiveData<Event<ValidatorStakeBottomSheet.Payload>>()
    val totalStakeEvent: LiveData<Event<ValidatorStakeBottomSheet.Payload>> = _totalStakeEvent

    fun backClicked() {
        router.back()
    }

    fun totalStakeClicked() = launch {
        val validatorStake = stakeTarget.stake
        val asset = assetFlow.first()
        val payload = calculatePayload(asset, validatorStake)

        _totalStakeEvent.value = Event(payload)
    }

    private suspend fun calculatePayload(asset: Asset, stakeTargetStake: StakeTargetStakeParcelModel) = withContext(Dispatchers.Default) {
        require(stakeTargetStake is StakeTargetStakeParcelModel.Active)

        val nominatorsStake = stakeTargetStake.stakers.sumByBigInteger(StakerParcelModel::value)

        ValidatorStakeBottomSheet.Payload(
            own = mapAmountToAmountModel(stakeTargetStake.ownStake, asset),
            stakers = mapAmountToAmountModel(nominatorsStake, asset),
            total = mapAmountToAmountModel(stakeTargetStake.totalStake, asset),
            stakersLabel = payload.displayConfig.stakersLabelRes
        )
    }

    fun webClicked() {
        stakeTarget.identity?.web?.let {
            showBrowser(it)
        }
    }

    fun emailClicked() {
        stakeTarget.identity?.email?.let {
            _openEmailEvent.value = Event(it)
        }
    }

    fun twitterClicked() {
        stakeTarget.identity?.twitter?.let {
            showBrowser(appLinksProvider.getTwitterAccountUrl(it))
        }
    }

    fun accountActionsClicked() = launch {
        val address = stakeTargetDetails.first().addressModel.address
        val chain = selectedAssetState.chain()

        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
    }
}
