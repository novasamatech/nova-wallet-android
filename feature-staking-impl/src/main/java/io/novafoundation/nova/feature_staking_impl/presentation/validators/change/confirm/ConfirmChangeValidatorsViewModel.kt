package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createSubstrateAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.setup.ChangeValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.common.validation.stakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.activeStake
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.hints.ConfirmStakeHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.reset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmChangeValidatorsViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val validationSystem: ValidationSystem<SetupStakingPayload, SetupStakingValidationFailure>,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val changeValidatorsInteractor: ChangeValidatorsInteractor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    walletUiUseCase: WalletUiUseCase,
    hintsMixinFactory: ConfirmStakeHintsMixinFactory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val maxValidatorsPerNominator by lazyAsync {
        interactor.maxValidatorsPerNominator(setupStakingSharedState.activeStake())
    }

    private val currentProcessState = setupStakingSharedState.get<SetupStakingProcess.ReadyToSubmit>()

    val hintsMixin = hintsMixinFactory.create(coroutineScope = this)

    private val stashFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val controllerAddressFlow = stashFlow.map { it.controllerAddress }
        .shareInBackground()

    private val controllerAssetFlow = controllerAddressFlow
        .flatMapLatest(interactor::assetFlow)
        .shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .inBackground()
        .share()

    val currentAccountModelFlow = controllerAddressFlow.map {
        generateDestinationModel(it, name = null)
    }
        .inBackground()
        .share()

    val nominationsFlow = flowOf {
        val selectedCount = currentProcessState.newValidators.size
        val maxValidatorsPerNominator = maxValidatorsPerNominator()

        resourceManager.getString(R.string.staking_confirm_nominations, selectedCount, maxValidatorsPerNominator)
    }

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    init {
        loadFee()
    }

    fun confirmClicked() {
        sendTransactionIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = currentAccountModelFlow.first().address

        externalActions.showAddressActions(address, selectedAssetState.chain())
    }

    fun nominationsClicked() {
        router.openConfirmNominations()
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = { changeValidatorsInteractor.estimateFee(prepareNominations(), stashFlow.first()) },
            onRetryCancelled = ::backClicked
        )
    }

    private fun prepareNominations() = currentProcessState.newValidators.map(Validator::accountIdHex)

    private fun sendTransactionIfValid() = launch {
        _showNextProgress.value = true

        val payload = SetupStakingPayload(
            maxFee = feeLoaderMixin.awaitFee(),
            controllerAsset = controllerAssetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { stakingValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction()
        }
    }

    private fun sendTransaction() = launch {
        changeValidatorsInteractor.changeValidators(
            stakingState = stashFlow.first(),
            validatorAccountIds = prepareNominations(),
        ).onSuccess {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))

            setupStakingSharedState.reset()

            startNavigation(it.submissionHierarchy) { router.returnToCurrentValidators() }
        }.onFailure {
            showError(it)
        }

        _showNextProgress.value = false
    }

    private suspend fun generateDestinationModel(address: String, name: String?): AddressModel {
        return addressIconGenerator.createSubstrateAddressModel(
            accountAddress = address,
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            accountName = name,
            background = AddressIconGenerator.BACKGROUND_TRANSPARENT
        )
    }
}
