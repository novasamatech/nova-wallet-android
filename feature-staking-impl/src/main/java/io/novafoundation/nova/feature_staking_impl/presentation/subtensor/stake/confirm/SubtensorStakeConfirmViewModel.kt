package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.SubtensorStakeSubmitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SubtensorStakeConfirmViewModel(
    private val router: StakingRouter,
    private val dashboardRouter: StakingDashboardRouter,
    private val resourceManager: ResourceManager,
    private val stakingSharedState: StakingSharedState,
    private val accountRepository: AccountRepository,
    private val submitInteractor: SubtensorStakeSubmitInteractor,
    private val assetUseCase: AssetUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val walletUiUseCase: WalletUiUseCase,
    private val amountFormatter: AmountFormatter,
    private val externalActions: ExternalActions.Presentation,
    private val payload: SubtensorStakeConfirmPayload,
) : BaseViewModel(),
    ExternalActions by externalActions {

    val isRoot: Boolean = payload.netuid == SubtensorStakingConstants.ROOT_NETUID

    val titleText: StateFlow<String> = MutableStateFlow(resolveTitle()).asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    val toastEvents = androidx.lifecycle.MutableLiveData<Event<String>>()

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val assetFlow = assetUseCase.currentAssetFlow().shareInBackground()

    /** Big-amount header at the top (token + fiat). */
    val amountModelFlow = assetFlow.map { asset ->
        val tao = asset.token.amountFromPlanks(payload.amountInPlanks)
        amountFormatter.formatAmountToAmountModel(tao, asset)
    }.shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow().shareInBackground()

    val originAddressModelFlow = flowOf {
        val chain = stakingSharedState.chain()
        val coldkey = accountRepository.getSelectedMetaAccount().requireAccountIdIn(chain)
        addressIconGenerator.createAccountAddressModel(chain, coldkey.toAddress(chain.addressPrefix.toShort()))
    }.shareInBackground()

    val feeStatusFlow = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(
            fee = decimalFee,
            token = asset.token,
            amountFormatter = amountFormatter,
        )
        FeeStatus.Loaded(feeModel)
    }.shareInBackground()

    /** Validator row label. Identity threading is part of the Validator Picker iOS-parity work. */
    val validatorLabel: StateFlow<String> = MutableStateFlow(formatValidator()).asStateFlow()

    fun confirmClicked() {
        if (_submitting.value) return
        sendTransaction()
    }

    fun backClicked() {
        if (_submitting.value) return
        router.back()
    }

    fun originAccountClicked() = launch {
        val chain = stakingSharedState.chain()
        val coldkey = accountRepository.getSelectedMetaAccount().requireAccountIdIn(chain)
        externalActions.showAddressActions(coldkey.toAddress(chain.addressPrefix.toShort()), chain)
    }

    fun validatorClicked() = launch {
        val chain = stakingSharedState.chain()
        externalActions.showAddressActions(payload.hotkeyAddress, chain)
    }

    private fun sendTransaction() {
        _submitting.value = true
        viewModelScope.launch {
            val hotkeyBytes = runCatching { payload.hotkeyAddress.toAccountId() }.getOrNull()
            if (hotkeyBytes == null) {
                _submitting.value = false
                toastEvents.value = Event(resourceManager.getString(R.string.subtensor_setup_invalid_validator))
                return@launch
            }

            val result = submitInteractor.submitStake(
                netuid = payload.netuid,
                hotkey = hotkeyBytes,
                amountInPlanks = payload.amountInPlanks,
            )
            _submitting.value = false
            result.fold(
                onSuccess = {
                    // Match the canonical Nova pattern (nomination-pools /
                    // parachain confirm flows): plain "Transaction submitted"
                    // toast on success, then navigate back to the dashboard.
                    showToast(resourceManager.getString(R.string.common_transaction_submitted))
                    dashboardRouter.returnToStakingDashboard()
                },
                onFailure = { error ->
                    val message = error.localizedMessage
                        ?: resourceManager.getString(R.string.subtensor_setup_submit_failed)
                    toastEvents.value = Event(message)
                },
            )
        }
    }

    private fun resolveTitle(): String = when {
        isRoot -> resourceManager.getString(R.string.common_confirm_title)
        !payload.subnetName.isNullOrBlank() -> resourceManager.getString(
            R.string.subtensor_confirm_subnet_title,
            payload.subnetName,
        )
        else -> resourceManager.getString(
            R.string.subtensor_confirm_subnet_title,
            "SN${payload.netuid}",
        )
    }

    private fun formatValidator(): String {
        val short = if (payload.hotkeyAddress.length <= 10) payload.hotkeyAddress
        else "${payload.hotkeyAddress.take(6)}...${payload.hotkeyAddress.takeLast(4)}"
        // iOS Confirm shows "Identity (5GgYg...XEm9zT)" when an identity is
        // known, falls back to the bare short hotkey otherwise.
        val identity = payload.validatorIdentity?.takeIf { it.isNotBlank() }
        return if (identity != null) "$identity ($short)" else short
    }
}
