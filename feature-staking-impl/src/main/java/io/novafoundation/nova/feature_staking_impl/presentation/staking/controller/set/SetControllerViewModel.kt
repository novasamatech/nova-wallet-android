package io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.set

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.selectingOneOf
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.mediatorLiveData
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.updateFrom
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
) : BaseViewModel(),
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val accountStakingFlow = stakingInteractor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .shareInBackground()

    val ableToChangeController = accountStakingFlow.map { stakingState ->
        stakingState.accountAddress == stakingState.stashAddress
    }
        .share()

    val stashAccountModel = accountStakingFlow.map {
        createAccountAddressModel(it.stashAddress)
    }.shareInBackground()

    private val assetFlow = stakingInteractor.currentAssetFlow()
        .shareInBackground()

    private val _controllerAccountModel = singleReplaySharedFlow<AddressModel>()
    val controllerAccountModel: Flow<AddressModel> = _controllerAccountModel

    override val openBrowserEvent = mediatorLiveData<Event<String>> {
        updateFrom(externalActions.openBrowserEvent)
    }

    val chooseControllerAction = actionAwaitableMixinFactory.selectingOneOf<AddressModel>()

    private val validationInProgress = MutableStateFlow(false)

    val continueButtonState = combine(
        controllerAccountModel,
        accountStakingFlow,
        ableToChangeController,
        validationInProgress
    ) { selectedController, stakingState, ableToChangeController, validationInProgress ->
        when {
            validationInProgress -> ButtonState.PROGRESS
            // The user selected account that was not the controller already and we able to change it
            selectedController.address != stakingState.controllerAddress && ableToChangeController -> ButtonState.NORMAL
            else -> ButtonState.GONE
        }
    }

    init {
        loadFee()

        setInitialController()
    }

    fun onMoreClicked() {
        openBrowserEvent.value = Event(appLinksProvider.setControllerLearnMore)
    }

    fun stashClicked() {
        viewModelScope.launch {
            externalActions.showExternalActions(ExternalActions.Type.Address(stashAddress()), selectedAssetState.chain())
        }
    }

    fun controllerClicked() = launch {
        val accountsInNetwork = accountsInCurrentNetwork()
        val currentController = controllerAccountModel.first()
        val payload = DynamicListBottomSheet.Payload(accountsInNetwork, currentController)

        val newController = chooseControllerAction.awaitAction(payload)

        _controllerAccountModel.emit(newController)
    }

    private fun setInitialController() = launch {
        val initialController = createAccountAddressModel(controllerAddress())

        _controllerAccountModel.emit(initialController)
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = { interactor.estimateFee(controllerAddress()) },
            onRetryCancelled = ::backClicked
        )
    }

    fun backClicked() {
        router.back()
    }

    fun continueClicked() {
        maybeGoToConfirm()
    }

    private suspend fun accountsInCurrentNetwork(): List<AddressModel> {
        return stakingInteractor.getAccountProjectionsInSelectedChains()
            .map { addressModelForStakingAccount(it) }
    }

    private suspend fun stashAddress() = accountStakingFlow.first().stashAddress

    private suspend fun controllerAddress() = accountStakingFlow.first().controllerAddress

    private fun maybeGoToConfirm() = feeLoaderMixin.requireFee(this) { fee ->
        launch {
            val controllerAddress = controllerAccountModel.first().address

            val payload = SetControllerValidationPayload(
                stashAddress = stashAddress(),
                controllerAddress = controllerAddress,
                fee = fee,
                transferable = assetFlow.first().transferable
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                progressConsumer = validationInProgress.progressConsumer(),
                validationFailureTransformer = { bondSetControllerValidationFailure(it, resourceManager) }
            ) {
                validationInProgress.value = false

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

    private suspend fun addressModelForStakingAccount(account: StakingAccount): AddressModel {
        return addressIconGenerator.createAccountAddressModel(
            chain = selectedAssetState.chain(),
            address = account.address,
            name = account.name
        )
    }

    private suspend fun createAccountAddressModel(address: String): AddressModel {
        return addressIconGenerator.createAccountAddressModel(
            chain = selectedAssetState.chain(),
            address = address,
            addressDisplayUseCase = addressDisplayUseCase
        )
    }

    private fun openConfirm(payload: ConfirmSetControllerPayload) {
        router.openConfirmSetController(payload)
    }
}
