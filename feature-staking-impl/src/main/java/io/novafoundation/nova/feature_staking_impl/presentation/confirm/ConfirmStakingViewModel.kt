package io.novafoundation.nova.feature_staking_impl.presentation.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.setup.BondPayload
import io.novafoundation.nova.feature_staking_impl.domain.setup.SetupStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit.Payload
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.validation.stakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.confirm.hints.ConfirmStakeHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ConfirmStakingViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val resourceManager: ResourceManager,
    private val validationSystem: ValidationSystem<SetupStakingPayload, SetupStakingValidationFailure>,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val setupStakingInteractor: SetupStakingInteractor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    walletUiUseCase: WalletUiUseCase,
    hintsMixinFactory: ConfirmStakeHintsMixinFactory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions {

    private val currentProcessState = setupStakingSharedState.get<SetupStakingProcess.ReadyToSubmit>()
    private val payload = currentProcessState.payload

    val hintsMixin = hintsMixinFactory.create(coroutineScope = this, payload)

    private val bondPayload = when (payload) {
        is Payload.Full -> BondPayload(payload.amount, payload.rewardDestination)
        else -> null
    }

    private val stashFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val stashAddressFlow = flowOf {
        currentAddressForFullFlowOr(StakingState.Stash::stashAddress)
    }
        .shareInBackground()

    private val controllerAddressFlow = flowOf {
        currentAddressForFullFlowOr(StakingState.Stash::controllerAddress)
    }
        .shareInBackground()

    private val controllerAssetFlow = controllerAddressFlow
        .flatMapLatest(interactor::assetFlow)
        .shareInBackground()

    private val stashAssetFlow = stashAddressFlow
        .flatMapLatest(interactor::assetFlow)
        .shareInBackground()

    val title = flowOf {
        when (payload) {
            is Payload.Full -> resourceManager.getString(R.string.staking_start_title)
            is Payload.Validators -> resourceManager.getString(R.string.staking_change_validators)
        }
    }
        .inBackground()
        .share()

    val amountModel = controllerAssetFlow.map { asset ->
        bondPayload?.let {
            mapAmountToAmountModel(it.amount, asset)
        }
    }
        .inBackground()
        .share()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .inBackground()
        .share()

    val currentAccountModelFlow = controllerAddressFlow.map {
        generateDestinationModel(it, name = null)
    }
        .inBackground()
        .share()

    val nominationsFlow = flowOf {
        val selectedCount = payload.validators.size
        val maxValidatorsPerNominator = interactor.maxValidatorsPerNominator()

        resourceManager.getString(R.string.staking_confirm_nominations, selectedCount, maxValidatorsPerNominator)
    }

    val rewardDestinationFlow = flowOf {
        val rewardDestination = when (payload) {
            is Payload.Full -> payload.rewardDestination
            else -> null
        }

        rewardDestination?.let { mapRewardDestinationToRewardDestinationModel(it) }
    }
        .inBackground()
        .share()

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

        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }

    fun payoutAccountClicked() = launch {
        val payoutDestination = rewardDestinationFlow.first() as? RewardDestinationModel.Payout ?: return@launch

        val type = ExternalActions.Type.Address(payoutDestination.destination.address)
        externalActions.showExternalActions(type, selectedAssetState.chain())
    }

    fun nominationsClicked() {
        router.openConfirmNominations()
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            viewModelScope,
            feeConstructor = {
                val token = controllerAssetFlow.first().token

                setupStakingInteractor.calculateSetupStakingFee(
                    controllerAddress = controllerAddressFlow.first(),
                    validatorAccountIds = prepareNominations(),
                    bondPayload = bondPayload
                )
            },
            onRetryCancelled = ::backClicked
        )
    }

    private suspend fun mapRewardDestinationToRewardDestinationModel(
        rewardDestination: RewardDestination,
    ): RewardDestinationModel {
        return when (rewardDestination) {
            is RewardDestination.Restake -> RewardDestinationModel.Restake
            is RewardDestination.Payout -> {
                val chain = selectedAssetState.chain()
                val address = chain.addressOf(rewardDestination.targetAccountId)
                val name = addressDisplayUseCase(address)

                val addressModel = generateDestinationModel(address, name)

                RewardDestinationModel.Payout(addressModel)
            }
        }
    }

    private fun prepareNominations() = payload.validators.map(Validator::accountIdHex)

    private fun sendTransactionIfValid() = requireFee { fee ->
        launch {
            val payload = SetupStakingPayload(
                maxFee = fee,
                bondAmount = bondPayload?.amount,
                stashAsset = stashAssetFlow.first(),
                controllerAsset = controllerAssetFlow.first()
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { stakingValidationFailure(payload, it, resourceManager) },
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                sendTransaction()
            }
        }
    }

    private fun sendTransaction() = launch {
        val setupResult = setupStakingInteractor.setupStaking(
            controllerAddress = controllerAddressFlow.first(),
            validatorAccountIds = prepareNominations(),
            bondPayload = bondPayload
        )

        _showNextProgress.value = false

        if (setupResult.isSuccess) {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))

            setupStakingSharedState.set(currentProcessState.finish())

            if (currentProcessState.payload is Payload.Validators) {
                router.returnToCurrentValidators()
            } else {
                router.returnToStakingMain()
            }
        } else {
            showError(setupResult.requireException())
        }
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )

    private suspend fun generateDestinationModel(address: String, name: String?): AddressModel {
        return addressIconGenerator.createAddressModel(
            accountAddress = address,
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            accountName = name,
            background = AddressIconGenerator.BACKGROUND_TRANSPARENT
        )
    }

    private suspend inline fun currentAddressForFullFlowOr(address: (StakingState.Stash) -> String): String {
        return when (payload) {
            is Payload.Full -> payload.currentAccountAddress
            else -> address(stashFlow.first())
        }
    }
}
