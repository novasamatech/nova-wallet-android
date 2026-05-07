package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.main

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.BittensorDelegatesClient
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorPositionCache
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorValidatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.first
import io.novafoundation.nova.common.utils.UNIFIED_ADDRESS_PREFIX
import io.novasama.substrate_sdk_android.ss58.SS58Encoder
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

/**
 * Backs the Bittensor staking detail screen. Reads the user's positions
 * via the shared [SubtensorPositionCache] (same source the multistaking
 * dashboard uses, so the data is consistent between screens), filters to
 * the entry netuid, and surfaces the result as three view-model blocks:
 * total, validator rows, info card.
 *
 * Mirrors the iOS `SubtensorStakingPresenter`. Stake/unstake actions are
 * out of scope for the first pass — the screen is read-only until the
 * setup VIPER flow lands.
 */
class SubtensorStakingViewModel(
    private val stakingSharedState: StakingSharedState,
    private val accountRepository: AccountRepository,
    private val positionCache: SubtensorPositionCache,
    private val delegatesClient: BittensorDelegatesClient,
    private val resourceManager: ResourceManager,
    private val router: StakingDashboardRouter,
    private val stakingRouter: StakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val walletRepository: WalletRepository,
    private val validatorProvider: SubtensorValidatorProvider,
    private val arbitraryTokenUseCase: ArbitraryTokenUseCase,
    private val appLinksProvider: AppLinksProvider,
) : BaseViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Headline percentage for the gold landing — TaoStats-derived peak APR
     *  among the top 20 validators by total stake. Falls back to the iOS
     *  default `subtensor_landing_default_max_apy` until the fetch lands or
     *  if it fails. Mirrors `StartStakingInfoSubtensorPresenter` Phase B. */
    private val _maxApy = MutableStateFlow(
        resourceManager.getString(R.string.subtensor_landing_default_max_apy)
    )
    val maxApy: StateFlow<String> = _maxApy.asStateFlow()

    /** Available balance label formatted as "0.0373 TAO" (no fiat — keeping
     *  scope tight so we don't wire a second price-source path). Null until
     *  the wallet asset resolves. */
    private val _availableBalance = MutableStateFlow<String?>(null)
    val availableBalance: StateFlow<String?> = _availableBalance.asStateFlow()

    /** Fired when the user taps the Nova Wiki link in the landing footer. */
    val openBrowserEvent = androidx.lifecycle.MutableLiveData<Event<String>>()

    /**
     * One-shot error events. Fragment observes via LiveData and shows a
     * Snackbar — keeps the existing loaded content on screen, like iOS
     * `wireframe.showError(from:message:)`.
     */
    val errorEvents = androidx.lifecycle.MutableLiveData<Event<String>>()

    init {
        launch { load(showLoadingIndicator = true) }
        // Landing data — runs in parallel with the position load. Cheap when
        // the user already has a stake (results are unused), but if they
        // land on the Empty state we want the gold screen populated by then.
        launch { loadLandingData() }
    }

    fun backClicked() {
        router.returnToStakingDashboard()
    }

    /** Mirrors iOS `SubtensorStakingPresenter.didTapStakeMore()`. Also used
     *  by the empty-state "Start staking" CTA on the gold landing. */
    fun stakeMore() {
        stakingRouter.openSubtensorStakeType()
    }

    fun novaWikiClicked() {
        openBrowserEvent.value = Event(appLinksProvider.wikiBase)
    }

    sealed interface UnstakeAction {
        object Empty : UnstakeAction
        data class Single(val netuid: Int, val hotkeyAddress: String, val amount: BigInteger) : UnstakeAction
        data class Pick(val options: List<Single>, val labels: List<String>) : UnstakeAction
    }

    val unstakeRequest = androidx.lifecycle.MutableLiveData<Event<UnstakeAction>>()

    /**
     * Mirrors iOS `SubtensorStakingPresenter.didTapUnstake()`. Loads
     * positions for this netuid; if there is exactly one we route directly
     * to UnstakeSetup, otherwise emit a picker request the Fragment
     * resolves with an AlertDialog.
     */
    fun unstake() {
        launch {
            val chain = stakingSharedState.chain()
            val coldkey = accountRepository.getSelectedMetaAccount().substrateAccountId
            if (coldkey == null) {
                errorEvents.value = Event(resourceManager.getString(R.string.subtensor_unstake_no_positions))
                return@launch
            }
            val entryNetuid = stakingSharedState.selectedOption.first()
                .additional
                .subtensorEntryNetuid
                ?: SubtensorStakingConstants.ROOT_NETUID

            val positions = runCatching {
                withContext(Dispatchers.IO) { positionCache.positions(chain.id, coldkey) }
            }.getOrDefault(emptyList())
                .filter { it.netuid == entryNetuid && it.amount.signum() > 0 }

            when (positions.size) {
                0 -> unstakeRequest.value = Event(UnstakeAction.Empty)
                1 -> {
                    val pos = positions[0]
                    unstakeRequest.value = Event(
                        UnstakeAction.Single(
                            netuid = pos.netuid,
                            hotkeyAddress = pos.hotkey.toAddress(chain.addressPrefix.toShort()),
                            amount = pos.amount,
                        )
                    )
                }
                else -> {
                    val identityByHotkey = runCatching {
                        withContext(Dispatchers.IO) { delegatesClient.fetchDelegates() }
                    }.getOrDefault(emptyMap())
                    val prefix = chain.addressPrefix.toShort()
                    val singles = positions.map { p ->
                        UnstakeAction.Single(
                            netuid = p.netuid,
                            hotkeyAddress = p.hotkey.toAddress(prefix),
                            amount = p.amount,
                        )
                    }
                    val labels = singles.map { single ->
                        identityByHotkey[single.hotkeyAddress]?.name?.takeIf { it.isNotBlank() }
                            ?: shortenAddress(single.hotkeyAddress)
                    }
                    unstakeRequest.value = Event(UnstakeAction.Pick(options = singles, labels = labels))
                }
            }
        }
    }

    fun unstakeOptionPicked(option: UnstakeAction.Single) {
        stakingRouter.openSubtensorUnstakeSetup(
            netuid = option.netuid,
            hotkeyAddress = option.hotkeyAddress,
            positionPlanks = option.amount,
        )
    }

    private fun shortenAddress(address: String): String =
        if (address.length <= 10) address else "${address.take(6)}…${address.takeLast(4)}"

    /**
     * Mirrors iOS `SubtensorStakingPresenter.refresh()` — deliberately keeps
     * the existing loaded content on screen while a fresh fetch runs in the
     * background. The screen flickering to a spinner on every nav return
     * felt jumpy (matches iOS comment).
     */
    fun refresh() {
        launch { load(showLoadingIndicator = false) }
    }

    private suspend fun load(showLoadingIndicator: Boolean) {
        if (showLoadingIndicator) {
            _state.value = UiState.Loading
        }

        val chain = stakingSharedState.chain()
        val chainAsset = stakingSharedState.chainAsset()

        // Entry netuid arrives via the shared-state additional data — set by
        // the dashboard ViewModel from the originally tapped row before
        // normalising chainAsset to TAO. Defaults to root for safety.
        // Mirrors iOS `SubtensorStakingViewFactory.createView(... entryNetuid:)`.
        val entryNetuid = stakingSharedState.selectedOption.first()
            .additional
            .subtensorEntryNetuid
            ?: SubtensorStakingConstants.ROOT_NETUID

        val coldkey = accountRepository.getSelectedMetaAccount().substrateAccountId
        if (coldkey == null) {
            _state.value = UiState.Empty
            return
        }

        val positions = runCatching {
            withContext(Dispatchers.IO) { positionCache.positions(chain.id, coldkey) }
        }.getOrElse { e ->
            // Mirrors iOS `SubtensorStakingPresenter.didReceive(error:)`:
            // surface the error via a side-channel without destroying the
            // existing loaded content. If we never had loaded content
            // (initial fetch failed), fall through to Empty so the screen
            // doesn't stick on the loading spinner.
            errorEvents.value = Event(e.message.orEmpty())
            if (_state.value !is UiState.Loaded) {
                _state.value = UiState.Empty
            }
            return
        }

        val scoped = positions.filter { it.netuid == entryNetuid }
        if (scoped.isEmpty()) {
            _state.value = UiState.Empty
            return
        }

        val total = scoped.fold(BigInteger.ZERO) { acc, p -> acc + p.amount }
        val isRoot = entryNetuid == SubtensorStakingConstants.ROOT_NETUID
        val precision = chainAsset.precision.value.toInt()
        val totalText = formatSubtensorAmount(total, precision, isRoot)

        // Best-effort identity merge — if the registry fetch fails we just
        // show truncated hotkeys. Mirrors iOS behaviour.
        val identityByHotkey = runCatching {
            withContext(Dispatchers.IO) { delegatesClient.fetchDelegates() }
        }.getOrDefault(emptyMap())

        // Render validator hotkeys with the unified-display prefix (0 / Polkadot)
        // for cross-chain consistency, matching iOS `toAddressWithDefaultConversion`
        // → `multichainDisplayPrefix = 0`. The chain's actual prefix (42 for
        // Bittensor) still drives the registry identity lookup.
        val displayPrefix = SS58Encoder.UNIFIED_ADDRESS_PREFIX
        val identityLookupPrefix = chain.addressPrefix.toShort()
        val rows = scoped.map { position ->
            val identityKey = position.hotkey.toAddress(identityLookupPrefix)
            val displayAddress = position.hotkey.toAddress(displayPrefix)
            // Identicon mirrors iOS `PolkadotIconGenerator().generateFromAccountId`
            // — the colourful blockie next to "Opentensor Foundation".
            val identicon = runCatching {
                withContext(Dispatchers.Default) {
                    addressIconGenerator.createAddressIcon(position.hotkey, ICON_SIZE_DP)
                }
            }.getOrNull()
            ValidatorRow(
                hotkeyShort = displayAddress.shortHotkey(),
                identityName = identityByHotkey[identityKey]?.name,
                amountText = formatSubtensorAmount(position.amount, precision, isRoot),
                identicon = identicon,
            )
        }

        _state.value = UiState.Loaded(
            totalAmountText = totalText,
            netuidBadge = if (entryNetuid == SubtensorStakingConstants.ROOT_NETUID) null else "SN$entryNetuid",
            validatorsTitle = resourceManager.getString(
                if (rows.size == 1) R.string.subtensor_your_validator else R.string.subtensor_your_validators
            ),
            validators = rows,
            minStakeText = resourceManager.getString(R.string.subtensor_minimum_stake_value),
            unstakingPeriodText = resourceManager.getString(R.string.subtensor_unstaking_period_instant),
        )
    }

    /**
     * Populates the gold-landing flows used by the empty state — peak APR
     * and available balance. Mirrors iOS `StartStakingInfoSubtensorPresenter`:
     * top-N validators by stake → max APR (replaces hardcoded fallback).
     * Both fetches are best-effort; the screen renders fine without them.
     */
    private suspend fun loadLandingData() {
        val chainAsset = stakingSharedState.chainAsset()
        val isRoot = (stakingSharedState.selectedOption.first().additional.subtensorEntryNetuid
            ?: SubtensorStakingConstants.ROOT_NETUID) == SubtensorStakingConstants.ROOT_NETUID
        // Available balance — through the wallet repository, same path the
        // assets screen uses. Asset symbol clash with Fusotao only affects
        // the *price* lookup, not the balance.
        val coldkey = accountRepository.getSelectedMetaAccount()
        runCatching {
            val asset = withContext(Dispatchers.IO) {
                walletRepository.assetFlowOrNull(coldkey.id, chainAsset).first()
            } ?: return@runCatching
            val transferable = asset.token.configuration.let {
                asset.transferableInPlanks
            }
            _availableBalance.value = formatSubtensorAmount(transferable, chainAsset.precision.value.toInt(), isRoot)
        }
        // Peak APR — top 20 by total stake, take max. Only meaningful for
        // root (subnet APRs vary per netuid and are out of scope for v1).
        runCatching {
            val rows = withContext(Dispatchers.IO) {
                validatorProvider.fetchValidators(SubtensorStakingConstants.ROOT_NETUID)
            }
            val topByStake = rows
                .sortedByDescending { it.totalStake }
                .take(MAX_APY_SAMPLE_SIZE)
            val maxApr = topByStake.mapNotNull { it.apr }.maxOrNull()
            if (maxApr != null && maxApr > 0.0) {
                _maxApy.value = "%.2f%%".format(maxApr * 100.0)
            }
        }
    }

    /**
     * Mirrors iOS `SubtensorPositionViewModel.shorten` — prefix(6) + "…" +
     * suffix(4). Same character widths as iOS so identical hotkeys render
     * identically on both platforms.
     */
    private fun String.shortHotkey(): String =
        if (length <= 10) this else "${take(6)}…${takeLast(4)}"

    /**
     * Formats a substrate balance with 4 decimals max + the right unit
     * suffix: `TAO` for root (netuid 0), `α` for subnet alpha. Mirrors iOS
     * `SubtensorPositionViewModel.formatAmount(_:precision:isRoot:)`.
     */
    private fun formatSubtensorAmount(amount: BigInteger, precision: Int, isRoot: Boolean): String {
        val symbol = if (isRoot) "TAO" else "α"
        if (precision <= 0) return "$amount $symbol"
        val divisor = BigInteger.TEN.pow(precision)
        val whole = amount / divisor
        val fractional = amount % divisor
        val decimalsToShow = minOf(4, precision)
        val fracScale = BigInteger.TEN.pow(precision - decimalsToShow)
        val fracPart = fractional / fracScale
        val fracStr = fracPart.toString().padStart(decimalsToShow, '0')
        return "$whole.$fracStr $symbol"
    }

    sealed interface UiState {
        object Loading : UiState
        object Empty : UiState
        // Error state was removed in favour of one-shot `errorEvents` so a
        // failed refresh doesn't blow away existing loaded content. Mirrors
        // iOS `wireframe.showError(from:message:)`.
        data class Loaded(
            val totalAmountText: String,
            val netuidBadge: String?,
            val validatorsTitle: String,
            val validators: List<ValidatorRow>,
            val minStakeText: String,
            val unstakingPeriodText: String,
        ) : UiState
    }

    data class ValidatorRow(
        val hotkeyShort: String,
        val identityName: String?,
        val amountText: String,
        val identicon: Drawable?,
    )

    companion object {
        /** dp size for the validator-row identicon, matches iOS `PolkadotIconView` (28pt). */
        private const val ICON_SIZE_DP = 28

        /** Top-N validators sampled when computing the headline "Earn up
         *  to X%" peak APR. Anchors the figure on serious operators —
         *  micro-stake validators with one recent reward can show APRs in
         *  the hundreds of percent and skew the headline. Mirrors iOS
         *  `StartStakingInfoSubtensorPresenter.headlineSampleSize`. */
        private const val MAX_APY_SAMPLE_SIZE = 20
    }
}
