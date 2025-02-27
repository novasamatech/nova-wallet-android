package io.novafoundation.nova.feature_staking_impl.presentation.mythos.claimRewards

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
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
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    selectedAccountUseCase: SelectedAccountUseCase,
    walletUiUseCase: WalletUiUseCase,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    assetUseCase: AssetUseCase,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val pendingRewards = interactor.pendingRewardsFlow()
        .shareInBackground()

    val pendingRewardsAmountModel = combine(pendingRewards, assetFlow, ::mapAmountToAmountModel)
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefault<SubmissionFee>(
        scope = viewModelScope,
        selectedChainAssetFlow = assetFlow.map { it.token.configuration }.distinctUntilChangedBy { it.fullId },
    )

    val originAddressModelFlow = selectedAccountUseCase.selectedAddressModelFlow { stakingSharedState.chain() }
        .shareInBackground()

    init {
        loadFee()
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

    private fun loadFee() = launchUnit {
        feeLoaderMixin.loadFee {
            interactor.estimateFee()
        }
    }

    private fun claimRewardsIfValid() = launchUnit {
        _showNextProgress.value = true

        val payload = MythosClaimRewardsValidationPayload(
            fee = feeLoaderMixin.awaitFee(),
            pendingRewardsPlanks = pendingRewards.first(),
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
        interactor.claimRewards()
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.returnToStakingMain()
            }
            .onFailure(::showError)

        _showNextProgress.value = false
    }
}
