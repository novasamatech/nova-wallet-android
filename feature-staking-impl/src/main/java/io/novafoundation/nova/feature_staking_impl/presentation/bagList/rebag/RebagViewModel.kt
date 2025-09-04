package io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.RebagInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.RebagMovement
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations.RebagValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations.RebagValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations.handleRebagValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag.model.RebagMovementModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanksRange
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RebagViewModel(
    private val interactor: RebagInteractor,
    private val stakingInteractor: StakingInteractor,
    private val stakingSharedState: StakingSharedState,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val walletUiUseCase: WalletUiUseCase,
    private val router: StakingRouter,
    private val externalActions: ExternalActions.Presentation,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: RebagValidationSystem,
    private val resourceManager: ResourceManager,
    private val iconGenerator: AddressIconGenerator,
    private val resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
) : BaseViewModel(),
    WithFeeLoaderMixin,
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val accountStakingFlow = stakingInteractor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .shareInBackground()

    private val stashAsset = accountStakingFlow.flatMapLatest {
        stakingInteractor.assetFlow(it.stashAddress)
    }.shareInBackground()

    override val originFeeMixin = feeLoaderMixinFactory.create(stashAsset)

    val walletModel = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val originAddressModelFlow = accountStakingFlow.map {
        iconGenerator.createAccountAddressModel(stakingSharedState.chain(), it.stashAddress)
    }
        .shareInBackground()

    val hintsMixin = resourcesHintsMixinFactory.create(
        coroutineScope = viewModelScope,
        hintsRes = listOf(R.string.staking_alert_rebag_message)
    )

    val rebagMovementModel = accountStakingFlow.flatMapLatest(interactor::rebagMovementFlow)
        .map { rebagMovement ->
            val chainAsset = stakingSharedState.chainAsset()
            mapRebagMovementToUi(rebagMovement, chainAsset)
        }
        .shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    init {
        loadFee()
    }

    fun confirmClicked() {
        rebagIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun accountClicked() = launch {
        val addressModel = originAddressModelFlow.first()

        externalActions.showAddressActions(addressModel.address, stakingSharedState.chain())
    }

    private fun rebagIfValid() {
        launch {
            _showNextProgress.value = true

            val validationPayload = RebagValidationPayload(
                fee = originFeeMixin.awaitFee(),
                asset = stashAsset.first()
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = validationPayload,
                progressConsumer = _showNextProgress.progressConsumer(),
                validationFailureTransformer = { handleRebagValidationFailure(it, resourceManager) }
            ) {
                rebag()
            }
        }
    }

    private fun rebag() = launch {
        val result = withContext(Dispatchers.Default) {
            interactor.rebag(accountStakingFlow.first())
        }

        result.onSuccess {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))

            router.back()
        }
            .onFailure(::showError)

        _showNextProgress.value = false
    }

    private fun mapRebagMovementToUi(rebagMovement: RebagMovement, chainAsset: Chain.Asset): RebagMovementModel {
        return RebagMovementModel(
            currentBag = rebagMovement.from.formatPlanksRange(chainAsset),
            newBag = rebagMovement.to.formatPlanksRange(chainAsset)
        )
    }

    private fun loadFee() {
        launch {
            originFeeMixin.loadFee(
                coroutineScope = this,
                feeConstructor = { interactor.calculateFee(accountStakingFlow.first()) },
                onRetryCancelled = {}
            )
        }
    }
}
