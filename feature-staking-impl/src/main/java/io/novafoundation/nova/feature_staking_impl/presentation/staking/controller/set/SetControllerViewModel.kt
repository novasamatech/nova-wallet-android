package io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.set

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mediatorLiveData
import io.novafoundation.nova.common.utils.updateFrom
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet.Payload
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.controller.ControllerInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.SetControllerValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.controller.SetControllerValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.confirm.ConfirmSetControllerPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.requireFee
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SetControllerViewModel(
    private val interactor: ControllerInteractor,
    private val stakingInteractor: StakingInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val router: StakingRouter,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val appLinksProvider: AppLinksProvider,
    private val resourceManager: ResourceManager,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: SetControllerValidationSystem,
    private val selectedAssetState: SingleAssetSharedState,
) : BaseViewModel(),
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val accountStakingFlow = stakingInteractor.selectedAccountStakingStateFlow()
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    val showNotStashAccountWarning = accountStakingFlow.map { stakingState ->
        stakingState.accountAddress != stakingState.stashAddress
    }.asLiveData()

    val stashAccountModel = accountStakingFlow.map {
        generateIcon(it.stashAddress)
    }.asLiveData()

    private val assetFlow = stakingInteractor.currentAssetFlow()
        .inBackground()
        .share()

    private val _controllerAccountModel = MutableLiveData<AddressModel>()
    val controllerAccountModel: LiveData<AddressModel> = _controllerAccountModel

    override val openBrowserEvent = mediatorLiveData<Event<String>> {
        updateFrom(externalActions.openBrowserEvent)
    }

    private val _showControllerChooserEvent = MutableLiveData<Event<Payload<AddressModel>>>()
    val showControllerChooserEvent: LiveData<Event<Payload<AddressModel>>> = _showControllerChooserEvent

    val isContinueButtonAvailable = combine(
        controllerAccountModel,
        accountStakingFlow.asLiveData(),
        showNotStashAccountWarning
    ) { (selectedController: AddressModel, stakingState: StakingState.Stash, warningShown: Boolean) ->
        selectedController.address != stakingState.controllerAddress && // The user selected account that was not the controller already
            warningShown.not() // The account is stash, so we don't have warning
    }

    fun onMoreClicked() {
        openBrowserEvent.value = Event(appLinksProvider.setControllerLearnMore)
    }

    fun openExternalActions() {
        viewModelScope.launch {
            externalActions.showExternalActions(ExternalActions.Type.Address(stashAddress()), selectedAssetState.chain())
        }
    }

    fun openAccounts() {
        viewModelScope.launch {
            val accountsInNetwork = accountsInCurrentNetwork()

            _showControllerChooserEvent.value = Event(Payload(accountsInNetwork))
        }
    }

    init {
        loadFee()

        viewModelScope.launch {
            _controllerAccountModel.value = accountStakingFlow.map {
                generateIcon(it.controllerAddress)
            }.first()
        }
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = { interactor.estimateFee(controllerAddress()) },
            onRetryCancelled = ::backClicked
        )
    }

    fun payoutControllerChanged(newController: AddressModel) {
        _controllerAccountModel.value = newController
    }

    fun backClicked() {
        router.back()
    }

    fun continueClicked() {
        maybeGoToConfirm()
    }

    private suspend fun stashAddress() = accountStakingFlow.first().stashAddress

    private suspend fun controllerAddress() = accountStakingFlow.first().controllerAddress

    private suspend fun accountsInCurrentNetwork(): List<AddressModel> {
        return stakingInteractor.getAccountProjectionsInSelectedChains()
            .map { generateDestinationModel(it) }
    }

    private suspend fun generateDestinationModel(account: StakingAccount): AddressModel {
        return addressIconGenerator.createAddressModel(account.address, AddressIconGenerator.SIZE_SMALL, account.name)
    }

    private suspend fun generateIcon(address: String): AddressModel {
        val name = addressDisplayUseCase(address)
        return addressIconGenerator
            .createAddressModel(
                address,
                AddressIconGenerator.SIZE_SMALL,
                name
            )
    }

    private fun maybeGoToConfirm() = feeLoaderMixin.requireFee(this) { fee ->
        launch {
            val controllerAddress = controllerAccountModel.value?.address ?: return@launch

            val payload = SetControllerValidationPayload(
                stashAddress = stashAddress(),
                controllerAddress = controllerAddress,
                fee = fee,
                transferable = assetFlow.first().transferable
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { bondSetControllerValidationFailure(it, resourceManager) }
            ) {
                openConfirm(
                    ConfirmSetControllerPayload(
                        fee = fee,
                        stashAddress = payload.stashAddress,
                        controllerAddress = payload.controllerAddress,
                        transferable = payload.transferable
                    )
                )
            }
        }
    }

    private fun openConfirm(payload: ConfirmSetControllerPayload) {
        router.openConfirmSetController(payload)
    }
}
