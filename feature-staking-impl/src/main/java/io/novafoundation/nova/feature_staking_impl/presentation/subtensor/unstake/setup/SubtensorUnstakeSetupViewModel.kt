package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.setup

import android.graphics.drawable.Drawable
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createSubstrateAddressIcon
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.images.Icon as CommonIcon
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalToken
import kotlin.time.Duration.Companion.milliseconds
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.extrinsic.removeStakeLimit
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.BittensorDelegatesClient
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorValidatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.common.SubtensorNovaFee
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm.SubtensorUnstakeConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget.StakingTargetModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitOptionalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class SubtensorUnstakeSetupViewModel(
    private val router: StakingRouter,
    @Suppress("UnusedPrivateMember") private val dashboardRouter: StakingDashboardRouter,
    private val resourceManager: ResourceManager,
    private val stakingSharedState: StakingSharedState,
    private val extrinsicService: ExtrinsicService,
    private val delegatesClient: BittensorDelegatesClient,
    private val validatorProvider: SubtensorValidatorProvider,
    private val addressIconGenerator: AddressIconGenerator,
    private val assetIconProvider: AssetIconProvider,
    private val arbitraryTokenUseCase: ArbitraryTokenUseCase,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    val netuid: Int,
    val hotkeyAddress: String,
    val positionPlanks: BigInteger,
) : BaseViewModel() {

    val isRoot: Boolean = netuid == SubtensorStakingConstants.ROOT_NETUID

    /** "TAO" for root, "SN56" for subnets — used as the position/fee/max ticker. */
    val ticker: String = if (isRoot) "TAO" else "SN$netuid"

    /** Chain-asset icon (TAO logo) — populated once chainAsset() resolves and
     *  the relative filename ("TAO_bittensor.svg") is rebased through the
     *  AssetIconProvider to a full URL. iOS uses the TAO logo for subnet
     *  positions too; mirroring that here. */
    private val _tokenIcon = MutableStateFlow<CommonIcon?>(null)
    val tokenIcon: StateFlow<CommonIcon?> = _tokenIcon.asStateFlow()

    /** Validator hotkey identicon (the polkadot-style multi-coloured circle).
     *  Generated once on init, regardless of whether identity resolves. */
    private val _identicon = MutableStateFlow<Drawable?>(null)

    val titleText: StateFlow<String> = MutableStateFlow(
        // Mirrors iOS `SubtensorUnstakeSetupViewLayout.title`: "Unstake — SN56"
        // (or "Unstake — TAO"). The em-dash matches iOS exactly.
        resourceManager.getString(R.string.subtensor_unstake) + " — " + ticker
    ).asStateFlow()

    /** Resolved once chainAsset() returns. Default 9 (Bittensor mainnet TAO). */
    private val precision = MutableStateFlow(9)

    /** Resolved from delegates registry on init; null while loading or if unknown. */
    private val _identity = MutableStateFlow<String?>(null)
    val identity: StateFlow<String?> = _identity.asStateFlow()

    val validatorTargetModel: StateFlow<StakingTargetModel> = combine(_identity, _identicon) { identity, identicon ->
        buildTargetModel(identity, identicon)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, buildTargetModel(null, null))

    val positionLabel: StateFlow<String> = precision
        .map { formatPosition(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, formatPosition(9))

    val maxLabel: StateFlow<String> = precision
        .map { formatMaxLabel(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, formatMaxLabel(9))

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _amountInPlanks = MutableStateFlow(BigInteger.ZERO)

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    val toastEvents = androidx.lifecycle.MutableLiveData<Event<String>>()

    val unstakeAllPromptEvent = androidx.lifecycle.MutableLiveData<Event<Unit>>()

    private val selectedChainAsset = MutableStateFlow<Chain.Asset?>(null)

    /** Fee row mixin — wired to amount via [connectWith] in init. */
    val feeMixin = feeLoaderMixinFactory.createDefault(
        scope = this,
        selectedChainAssetFlow = selectedChainAsset.filterNotNull().shareInBackground(),
    )

    /** True only when the Nova fee applies (subnet + recipient set). Drives the
     *  fee row + caption visibility. Inert (false) today since the recipient is null. */
    val novaFeeApplies: Boolean = SubtensorNovaFee.feeApplies(netuid)

    /**
     * Nova service-fee row — live 0.3% of the entered amount, shown in the
     * screen's (alpha) asset units (e.g. "SN56"), mirroring iOS which shows the
     * unstake-setup fee in the subnet token. Sits beside [feeMixin]'s
     * network-fee row. Emits [FeeStatus.NoFee] (hidden) when the fee doesn't
     * apply. Isolated from the network-fee mixin.
     */
    val novaFeeStatusFlow: StateFlow<FeeStatus<*, FeeDisplay>> = combine(
        _amountInPlanks,
        precision,
    ) { amountPlanks, precision ->
        SubtensorNovaFee.alphaFeeStatus(
            netuid = netuid,
            grossPlanks = amountPlanks,
            precision = precision,
            ticker = ticker,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FeeStatus.NoFee)

    val canContinue: StateFlow<Boolean> = combine(_amount, _submitting) { amt, submitting ->
        !submitting && amt.isNotBlank() && (amt.toBigDecimalOrNull()?.signum() ?: 0) > 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** HistoricalToken built off the priceId directly (skips the symbol-keyed
     *  `tokens` table that conflates Fusotao and Bittensor under the symbol
     *  "TAO" — last write wins, so the cached rate may be Fusotao's). */
    private val _historicalToken = MutableStateFlow<HistoricalToken?>(null)

    /** "$X.XX" string for the typed amount. Subnet-alpha positions have no
     *  priceId of their own — iOS suppresses the fiat row for them rather
     *  than mis-pricing alpha at TAO's rate. Mirroring that here: only show
     *  fiat for root (where the typed amount is TAO and the price is TAO's). */
    val fiatAmount: StateFlow<CharSequence?> = combine(_historicalToken, _amount) { token, raw ->
        if (!isRoot) return@combine null
        val amount = raw.toBigDecimalOrNull() ?: return@combine null
        if (token == null) return@combine null
        token.amountToFiat(amount).formatAsCurrency(token.currency)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val asset = runCatching { stakingSharedState.chainAsset() }.getOrNull()
            precision.value = asset?.precision?.value?.toInt() ?: 9
            selectedChainAsset.value = asset
            // Resolve the relative filename ("TAO_bittensor.svg") to a full
            // URL via AssetIconProvider — mirrors how Nova wallet rows and
            // ChooseAmountView load asset icons everywhere else.
            _tokenIcon.value = assetIconProvider.getAssetIconOrFallback(asset?.icon)
        }
        // Fiat source: ArbitraryTokenUseCase.historicalToken(asset, now) —
        // fetches CoinPrice by the asset's *priceId* (e.g. "bittensor")
        // directly, bypassing the symbol-keyed `tokens` cache that conflates
        // Fusotao and Bittensor under symbol "TAO". The returned
        // HistoricalToken carries currency + rate together, so we can format
        // fiat with the same one-liner the rest of Nova uses.
        viewModelScope.launch {
            val asset = runCatching { stakingSharedState.chainAsset() }.getOrNull() ?: return@launch
            val now = System.currentTimeMillis().milliseconds
            _historicalToken.value = runCatching {
                withContext(Dispatchers.IO) { arbitraryTokenUseCase.historicalToken(asset, now) }
            }.getOrNull()
        }
        // Identicon — generated locally from the SS58 hotkey. Doesn't need a
        // network round-trip and works even when identity resolution fails,
        // matching iOS where the picker, confirm, and unstake screens all
        // show the same identicon for a given hotkey.
        viewModelScope.launch {
            _identicon.value = runCatching {
                addressIconGenerator.createSubstrateAddressIcon(hotkeyAddress, AddressIconGenerator.SIZE_MEDIUM)
            }.getOrNull()
        }
        // Identity lookup — go through the same provider the validator picker
        // uses so we get TaoStats names (which include "P2P.org" and other
        // operators not in the bittensor-delegates GitHub registry) merged
        // with the delegates registry. Falls back to delegates-only on
        // TaoStats failure. Mirrors iOS — picker and unstake share one
        // identity source.
        viewModelScope.launch {
            val prefix: Short = runCatching {
                stakingSharedState.chain().addressPrefix.toShort()
            }.getOrDefault(42)
            val resolved = runCatching {
                withContext(Dispatchers.IO) { validatorProvider.fetchValidators(netuid) }
                    .firstOrNull { row ->
                        runCatching { row.hotkey.toAddress(prefix) }.getOrNull() == hotkeyAddress
                    }?.identity?.takeIf { it.isNotBlank() }
            }.getOrNull()
                ?: runCatching {
                    withContext(Dispatchers.IO) { delegatesClient.fetchDelegates() }
                }.getOrElse { delegatesClient.cachedDelegates() }
                    .get(hotkeyAddress)?.name
                    ?.takeIf { it.isNotBlank() }
            _identity.value = resolved
        }

        listenFee()
    }

    fun amountChanged(text: String) {
        _amount.value = text
        _amountInPlanks.value = text.toBigDecimalOrNull()
            ?.multiply(BigDecimal.TEN.pow(precision.value))
            ?.toBigInteger()
            ?: BigInteger.ZERO
    }

    fun maxClicked() {
        val tao = positionPlanks.toBigDecimal().movePointLeft(precision.value).stripTrailingZeros().toPlainString()
        _amount.value = tao
        _amountInPlanks.value = positionPlanks
    }

    fun continueClicked() {
        if (_submitting.value) return
        val amt = _amount.value.toBigDecimalOrNull() ?: return
        if (amt <= BigDecimal.ZERO) return

        val planks = amt.multiply(BigDecimal.TEN.pow(precision.value)).toBigInteger()
        if (planks > positionPlanks) {
            toastEvents.value = Event(resourceManager.getString(R.string.subtensor_unstake_amount_exceeds))
            return
        }

        // Mirror iOS dust-remainder UX: prompt to unstake all when the
        // partial unstake would leave less than the chain minimum.
        val remainder = positionPlanks - planks
        val belowMin = remainder.signum() > 0 &&
            remainder < SubtensorStakingConstants.MIN_NOMINATOR_STAKE_RAO
        if (belowMin) {
            unstakeAllPromptEvent.value = Event(Unit)
            return
        }

        openConfirm(planks)
    }

    fun confirmUnstakeAll() {
        if (_submitting.value) return
        val tao = positionPlanks.toBigDecimal().movePointLeft(precision.value)
        _amount.value = tao.stripTrailingZeros().toPlainString()
        openConfirm(positionPlanks)
    }

    fun backClicked() {
        if (_submitting.value) return
        router.back()
    }

    private fun listenFee() {
        feeMixin.connectWith(
            inputSource1 = _amountInPlanks,
        ) { _, planks ->
            val realAmount = planks.takeIf { it > BigInteger.ZERO } ?: BigInteger.valueOf(1_000_000_000L)
            val hotkeyBytes = runCatching { hotkeyAddress.toAccountId() }.getOrNull() ?: ByteArray(32)
            extrinsicService.estimateFee(
                chain = stakingSharedState.chain(),
                origin = TransactionOrigin.SelectedWallet,
            ) {
                removeStakeLimit(hotkey = hotkeyBytes, netuid = netuid, amount = realAmount)
            }
        }
    }

    private fun openConfirm(planks: BigInteger) {
        _submitting.value = true
        viewModelScope.launch {
            val fee = feeMixin.awaitOptionalFee()
            if (fee == null) {
                _submitting.value = false
                toastEvents.value = Event(resourceManager.getString(R.string.subtensor_setup_submit_failed))
                return@launch
            }
            _submitting.value = false
            router.openSubtensorUnstakeConfirm(
                SubtensorUnstakeConfirmPayload(
                    netuid = netuid,
                    hotkeyAddress = hotkeyAddress,
                    validatorIdentity = _identity.value,
                    amountInPlanks = planks,
                    positionPlanks = positionPlanks,
                    fee = mapFeeToParcel(fee),
                )
            )
        }
    }

    private fun buildTargetModel(identity: String?, identicon: Drawable?): StakingTargetModel {
        val short = if (hotkeyAddress.length <= 10) hotkeyAddress
        else "${hotkeyAddress.take(6)}...${hotkeyAddress.takeLast(4)}"
        val name = identity?.takeIf { it.isNotBlank() }
        // iOS: when identity resolves, the subtitle drops away — only the
        // name + identicon remain. The hotkey is still discoverable via the
        // chevron tap on the row. Match that here.
        val title = name ?: short
        val iconModel = identicon?.let { StakingTargetModel.TargetIcon.Icon(it.asIcon()) }
        return StakingTargetModel(title = title, subtitle = null, icon = iconModel)
    }

    private fun formatPosition(precision: Int): String {
        val tao = positionPlanks.toBigDecimal().movePointLeft(precision)
        // iOS uses the subnet identifier ("SN56") as the alpha-token ticker,
        // not the alpha glyph (α). Switching here aligns Position / Max
        // labels with the chip in the amount input.
        return "%.4f %s".format(tao, ticker)
    }

    private fun formatMaxLabel(precision: Int): String {
        // Returns just the value ("0.4659 SN56") — MaxAmountView prepends a
        // blue "Max:" label of its own, matching the canonical Nova flows.
        val tao = positionPlanks.toBigDecimal().movePointLeft(precision)
        return "%.4f %s".format(tao, ticker)
    }
}
