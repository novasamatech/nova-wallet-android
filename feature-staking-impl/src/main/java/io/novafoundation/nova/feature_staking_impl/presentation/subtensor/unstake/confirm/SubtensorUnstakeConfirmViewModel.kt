package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm

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
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorSubnetFetcher
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.SubtensorStakeSubmitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.common.SubtensorNovaFee
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class SubtensorUnstakeConfirmViewModel(
    private val router: StakingRouter,
    private val dashboardRouter: StakingDashboardRouter,
    private val resourceManager: ResourceManager,
    private val stakingSharedState: StakingSharedState,
    private val accountRepository: AccountRepository,
    private val submitInteractor: SubtensorStakeSubmitInteractor,
    private val assetUseCase: AssetUseCase,
    private val subnetFetcher: SubtensorSubnetFetcher,
    private val addressIconGenerator: AddressIconGenerator,
    private val walletUiUseCase: WalletUiUseCase,
    private val amountFormatter: AmountFormatter,
    private val externalActions: ExternalActions.Presentation,
    private val payload: SubtensorUnstakeConfirmPayload,
) : BaseViewModel(),
    ExternalActions by externalActions {

    val isRoot: Boolean = payload.netuid == SubtensorStakingConstants.ROOT_NETUID

    val titleText: StateFlow<String> = MutableStateFlow(resourceManager.getString(R.string.subtensor_unstake)).asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    val toastEvents = androidx.lifecycle.MutableLiveData<Event<String>>()

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val assetFlow = assetUseCase.currentAssetFlow().shareInBackground()

    /** Big-amount header. For root the alpha amount IS TAO, so we use the
     *  canonical formatter (TAO symbol + TAO fiat). For subnet alpha we
     *  build the AmountModel manually:
     *    - symbol = "SN<netuid>" (the asset chip ticker, not "TAO")
     *    - fiat   = null (subnet alpha has no priceId — same as iOS, which
     *      suppresses fiat for these rows rather than mis-pricing alpha at
     *      TAO's rate via the staking-shared-state asset). */
    val amountModelFlow = assetFlow.map { asset ->
        val amountTokens = asset.token.amountFromPlanks(payload.amountInPlanks)
        if (isRoot) {
            amountFormatter.formatAmountToAmountModel(amountTokens, asset)
        } else {
            io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel(
                token = "%.5f SN${payload.netuid}".format(amountTokens),
                fiat = null,
            )
        }
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

    val validatorLabel: StateFlow<String> = MutableStateFlow(formatValidator()).asStateFlow()

    /**
     * "You'll receive" estimate. For root TAO, omitted (1:1 unstake — the
     * amount is already in TAO). For subnet positions, derived from live
     * AMM reserves: tao_received ≈ alpha_amount × spot_price (TAO per alpha).
     * Mirrors iOS `SubtensorUnstakeConfirmPresenter.didReceiveAMMPrice`.
     */
    private val _expectedReceiveLabel = MutableStateFlow<String?>(null)
    val expectedReceiveLabel: StateFlow<String?> = _expectedReceiveLabel.asStateFlow()

    /** True only when the Nova fee applies (subnet + recipient set). Drives the
     *  fee row + caption visibility. Inert (false) today since the recipient is null. */
    val novaFeeApplies: Boolean = SubtensorNovaFee.feeApplies(payload.netuid)

    /**
     * Nova service-fee row — 0.3% of the minimum TAO received, in TAO. Computed
     * in [fetchAmmEstimate] from the same live AMM reserves the "You'll receive"
     * estimate uses: minTaoOut = amount × spotPrice × (1 - DEFAULT_SLIPPAGE).
     * Starts as [FeeStatus.NoFee] (hidden) until reserves arrive / when the fee
     * doesn't apply. Isolated from the network-fee flow.
     */
    private val _novaFeeStatus = MutableStateFlow<FeeStatus<*, FeeDisplay>>(FeeStatus.NoFee)
    val novaFeeStatusFlow: StateFlow<FeeStatus<*, FeeDisplay>> = _novaFeeStatus.asStateFlow()

    /**
     * Single source of truth for the unstake fee. [fetchAmmEstimate] resolves the
     * spot price ONCE, uses it for the displayed fee, and stores it here so the
     * submit path charges the fee from the exact same reserve read (no second
     * fetch → no displayed-vs-charged drift). [ammResolved] gates confirm until
     * that estimate settles, mirroring iOS deferring confirm until the commission
     * resolves. Root has no AMM, so it resolves immediately with a null price.
     */
    private var resolvedSpotPrice: Double? = null
    private val ammResolved = CompletableDeferred<Unit>()

    init {
        if (!isRoot) {
            viewModelScope.launch { fetchAmmEstimate() }
        } else {
            ammResolved.complete(Unit)
        }
    }

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

    private suspend fun fetchAmmEstimate() {
        try {
            val reserves = runCatching {
                withContext(Dispatchers.IO) { subnetFetcher.fetchReserves(payload.netuid) }
            }.getOrNull() ?: return
            val spotPrice = reserves.spotPrice
            if (spotPrice <= 0.0) return

            // Resolved once here; reused verbatim by the submit path (see
            // [resolvedSpotPrice]) so the charged fee matches this displayed one.
            resolvedSpotPrice = spotPrice

            // Convert alpha planks → whole tokens with the asset's own precision
            // (alpha shares TAO's 9 decimals on Bittensor); use the SAME precision
            // going back to planks below so display and charge can't diverge.
            val asset = assetFlow.first()
            val precision = asset.token.configuration.precision.value.toInt()

            // alpha_amount × spot_price → tao_received (whole tokens; BigDecimal
            // for precision, formatted to 4 decimals for the "You'll receive" row).
            val alphaAmount = payload.amountInPlanks
                .toBigDecimal()
                .movePointLeft(precision)
            val taoReceived = alphaAmount.multiply(BigDecimal.valueOf(spotPrice))
            _expectedReceiveLabel.value = "≈ %.4f TAO".format(taoReceived)

            // Nova fee = 0.3% of the MINIMUM TAO out (received minus slippage), in
            // TAO — mirrors iOS, which charges the fee on minTaoOut so it can never
            // exceed what's actually received. minTaoOut → planks via the same precision.
            val minTaoOut = taoReceived.multiply(
                BigDecimal.valueOf(1.0 - SubtensorStakingConstants.DEFAULT_SLIPPAGE)
            )
            val minTaoOutPlanks = minTaoOut.movePointRight(precision).toBigInteger().max(BigInteger.ZERO)
            _novaFeeStatus.value = SubtensorNovaFee.nativeFeeStatus(
                netuid = payload.netuid,
                grossPlanks = minTaoOutPlanks,
                token = asset.token,
                amountFormatter = amountFormatter,
            )
        } finally {
            // Release a pending confirm() on every path — success, empty reserves,
            // or non-positive spot — so the user is never stuck behind the gate.
            ammResolved.complete(Unit)
        }
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

            // Gate on the AMM estimate so the fee charged comes from the SAME
            // reserve read the screen displayed (iOS defers confirm likewise).
            // Root has no AMM and already resolved in init.
            if (!isRoot) ammResolved.await()

            val result = submitInteractor.submitUnstake(
                netuid = payload.netuid,
                hotkey = hotkeyBytes,
                amountInPlanks = payload.amountInPlanks,
                spotPriceTaoPerAlpha = resolvedSpotPrice,
            )
            _submitting.value = false
            result.fold(
                onSuccess = {
                    // Match the canonical Nova pattern: plain "Transaction
                    // submitted" toast on success, then return to dashboard.
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

    private fun formatValidator(): String {
        val short = if (payload.hotkeyAddress.length <= 10) payload.hotkeyAddress
        else "${payload.hotkeyAddress.take(6)}...${payload.hotkeyAddress.takeLast(4)}"
        val identity = payload.validatorIdentity?.takeIf { it.isNotBlank() }
        return if (identity != null) "$identity ($short)" else short
    }
}
