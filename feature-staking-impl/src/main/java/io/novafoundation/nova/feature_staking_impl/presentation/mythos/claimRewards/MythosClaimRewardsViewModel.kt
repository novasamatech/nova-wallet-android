package io.novafoundation.nova.feature_staking_impl.presentation.mythos.claimRewards

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.MythosClaimRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.validations.MythosClaimRewardsValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.validations.MythosClaimRewardsValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MythosClaimRewardsViewModel(
    private val router: MythosStakingRouter,
    private val interactor: MythosClaimRewardsInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: MythosClaimRewardsValidationSystem,
    private val validationFailureFormatter: MythosStakingValidationFailureFormatter,
    private val stakingSharedState: StakingSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    selectedAccountUseCase: SelectedAccountUseCase,
    walletUiUseCase: WalletUiUseCase,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    assetUseCase: AssetUseCase,
    private val amountFormatter: AmountFormatter
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val pendingRewardsFlow = interactor.pendingRewardsFlow()
        .shareInBackground()

    val shouldRestakeFlow = MutableStateFlow(true)

    val pendingRewardsAmountModel = combine(pendingRewardsFlow, assetFlow) { pendingRewards, asset ->
        amountFormatter.formatAmountToAmountModel(pendingRewards, asset)
    }
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefault(
        scope = viewModelScope,
        selectedChainAssetFlow = stakingSharedState.selectedAssetFlow(),
    )

    val originAddressModelFlow = selectedAccountUseCase.selectedAddressModelFlow { stakingSharedState.chain() }
        .shareInBackground()

    init {
        setupFee()

        setDefaultRestakeSetting()
    }

    fun confirmClicked() {
        claimRewardsIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = originAddressModelFlow.first().address
        val chain = stakingSharedState.chain()

        externalActions.showAddressActions(address, chain)
    }

    private fun setupFee() {
        feeLoaderMixin.connectWith(pendingRewardsFlow, shouldRestakeFlow) { _, pendingRewards, shouldRestake ->
            interactor.estimateFee(pendingRewards, shouldRestake)
        }
    }

    private fun claimRewardsIfValid() = launchUnit {
        _showNextProgress.value = true

        val payload = MythosClaimRewardsValidationPayload(
            fee = feeLoaderMixin.awaitFee(),
            pendingRewardsPlanks = pendingRewardsFlow.first(),
            asset = assetFlow.first(),
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = validationFailureFormatter::formatClaimRewards,
            progressConsumer = _showNextProgress.progressConsumer(),
            block = { sendTransaction() }
        )
    }

    private fun sendTransaction() = launchUnit {
        val pendingRewards = pendingRewardsFlow.first()
        val shouldRestake = shouldRestakeFlow.value

        interactor.claimRewards(pendingRewards, shouldRestake)
            .onSuccess {
                showToast(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(it.submissionHierarchy) { router.returnToStakingMain() }
            }
            .onFailure(::showError)

        _showNextProgress.value = false
    }

    private fun setDefaultRestakeSetting() = launchUnit {
        shouldRestakeFlow.value = interactor.initialShouldRestakeSetting()
    }
}
