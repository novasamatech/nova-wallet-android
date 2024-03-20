package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.set

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.selectingOneOf
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mediatorLiveData
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.updateFrom
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.AdvertisementCardModel
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.accountIsStash
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.repository.ControllersDeprecationStage
import io.novafoundation.nova.feature_staking_impl.data.repository.ControllersDeprecationStage.DEPRECATED
import io.novafoundation.nova.feature_staking_impl.data.repository.ControllersDeprecationStage.NORMAL
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.controller.ControllerInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.ConfirmSetControllerPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
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
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
) : BaseViewModel(),
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val accountStakingFlow = stakingInteractor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .shareInBackground()

    private val controllerDeprecationStageFlow = flowOf { interactor.controllerDeprecationStage() }
        .shareInBackground()

    val stashAccountModel = accountStakingFlow.map {
        createAccountAddressModel(it.stashAddress)
    }.shareInBackground()

    private val assetFlow = stakingInteractor.currentAssetFlow()
        .shareInBackground()

    private val _controllerAccountModel = singleReplaySharedFlow<AddressModel>()
    val controllerAccountModel: Flow<AddressModel> = _controllerAccountModel

    override val openBrowserEvent = mediatorLiveData {
        updateFrom(externalActions.openBrowserEvent)
    }

    val chooseControllerAction = actionAwaitableMixinFactory.selectingOneOf<AddressModel>()

    private val validationInProgress = MutableStateFlow(false)

    val isControllerSelectorEnabled = combine(accountStakingFlow, controllerDeprecationStageFlow) { stakingState, controllerDeprecationStage ->
        stakingState.isUsingCorrectAccountToChangeController() && controllerDeprecationStage == NORMAL
    }
        .share()

    val showSwitchToStashWarning = combine(controllerDeprecationStageFlow, accountStakingFlow) { controllerDeprecationStage, stakingState ->
        val usingWrongAccountToChangeController = stakingState.isUsingWrongAccountToChangeController()

        when (controllerDeprecationStage) {
            NORMAL -> usingWrongAccountToChangeController

            // when controllers are deprecated, there is no point to show the switch to stash warning if user has not separate controller
            // switching to stash wont allow to change controller anyway
            DEPRECATED -> usingWrongAccountToChangeController && stakingState.hasSeparateController()
        }
    }
        .shareInBackground()

    val continueButtonState = combine(
        controllerAccountModel,
        accountStakingFlow,
        controllerDeprecationStageFlow,
        validationInProgress
    ) { selectedController, stakingState, controllerDeprecationStage, validationInProgress ->
        when {
            validationInProgress -> DescriptiveButtonState.Loading

            stakingState.isUsingWrongAccountToChangeController() -> DescriptiveButtonState.Gone

            // Let user to remove controller when controllers are deprecated
            controllerDeprecationStage == DEPRECATED && stakingState.hasSeparateController() -> {
                DescriptiveButtonState.Enabled(resourceManager.getString(R.string.staking_set_controller_deprecated_action))
            }

            // User cant do anything beneficial when controllers are deprecated and user doesn't have controller set up
            controllerDeprecationStage == DEPRECATED && stakingState.hasSeparateController().not() -> {
                DescriptiveButtonState.Gone
            }

            // The user selected account that was not the controller already
            selectedController.address != stakingState.controllerAddress -> {
                DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
            }

            else -> DescriptiveButtonState.Gone
        }
    }
        .shareInBackground()

    val advertisementCardModel = controllerDeprecationStageFlow.map(::createBannerContent)
        .shareInBackground()

    init {
        loadFee()

        setInitialController()
    }

    fun onMoreClicked() = launch {
        openBrowserEvent.value = Event(getLearnMoreUrl())
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
            feeConstructor = { interactor.estimateFee(controllerAddress(), accountStakingFlow.first()) },
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

    private fun maybeGoToConfirm() = launch {
        validationInProgress.value = true

        val controllerAddress = getNewControllerAddress()

        val payload = SetControllerValidationPayload(
            stashAddress = stashAddress(),
            controllerAddress = controllerAddress,
            fee = feeLoaderMixin.awaitDecimalFee(),
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
                    fee = mapFeeToParcel(it.fee),
                    stashAddress = payload.stashAddress,
                    controllerAddress = payload.controllerAddress,
                    transferable = payload.transferable
                )
            )
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

    private fun StakingState.Stash.isUsingCorrectAccountToChangeController(): Boolean {
        return accountIsStash()
    }

    private fun StakingState.Stash.isUsingWrongAccountToChangeController(): Boolean {
        return !isUsingCorrectAccountToChangeController()
    }

    private fun StakingState.Stash.hasSeparateController(): Boolean {
        return !controllerId.contentEquals(stashId)
    }

    private fun createBannerContent(deprecationStage: ControllersDeprecationStage): AdvertisementCardModel {
        return when (deprecationStage) {
            NORMAL -> AdvertisementCardModel(
                title = resourceManager.getString(R.string.staking_set_controller_title),
                subtitle = resourceManager.getString(R.string.staking_set_controller_subtitle),
                imageRes = R.drawable.shield,
                bannerBackgroundRes = R.drawable.ic_banner_grey_gradient
            )

            DEPRECATED -> AdvertisementCardModel(
                title = resourceManager.getString(R.string.staking_set_controller_deprecated_title),
                subtitle = resourceManager.getString(R.string.staking_set_controller_deprecated_subtitle),
                imageRes = R.drawable.ic_banner_warning,
                bannerBackgroundRes = R.drawable.ic_banner_yellow_gradient
            )
        }
    }

    private suspend fun getNewControllerAddress(): String {
        return when (controllerDeprecationStageFlow.first()) {
            NORMAL -> controllerAccountModel.first().address
            DEPRECATED -> accountStakingFlow.first().stashAddress
        }
    }

    private suspend fun getLearnMoreUrl(): String {
        return when (controllerDeprecationStageFlow.first()) {
            NORMAL -> appLinksProvider.setControllerLearnMore
            DEPRECATED -> appLinksProvider.setControllerDeprecatedLeanMore
        }
    }
}
