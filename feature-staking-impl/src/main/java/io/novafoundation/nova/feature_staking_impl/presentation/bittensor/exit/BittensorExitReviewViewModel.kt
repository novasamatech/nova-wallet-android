package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingValidationStage
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.analyticsKind
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorResultNavigation
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.UnstakeMode
import io.novafoundation.nova.analytics.SlippageBucket
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorOperationAnalytics

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isSigningCancelled

import android.os.SystemClock
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLoadingValue
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatNetworkFee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.actualSellReceive
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.actualRootStakeMoved
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.mapDisplay
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatBittensorNativeResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mapBittensorSubmissionError
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.mapResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorExitValidationFailureUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetsView
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitExceedsStakeException
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.message
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex

private sealed interface ExitExecution {
    data object Idle : ExitExecution
    data class Running(val startedAt: Long, val estimatedDuration: Duration) : ExitExecution
    data class Success(val at: Long, val outcome: BittensorOperationOutcome?, val draft: BittensorExitDraft) : ExitExecution
    data class Failure(val model: BittensorStakeResultUi.Failure, val at: Long = System.currentTimeMillis()) : ExitExecution
    data class Unconfirmed(
        val model: BittensorStakeResultUi.Unconfirmed,
        val draft: BittensorExitDraft,
        val at: Long = System.currentTimeMillis()
    ) : ExitExecution
}

class BittensorExitReviewViewModel(
    private val analytics: AnalyticsService,
    private val operationAnalytics: BittensorOperationAnalytics,
    private val payload: BittensorExitReviewPayload,
    private val bittensorExitInteractor: BittensorExitInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    private val addressIconGenerator: AddressIconGenerator,
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    chainRegistry: ChainRegistry,
    walletUiUseCase: WalletUiUseCase,
    accountRepository: AccountRepository,
    private val chainStateRepository: ChainStateRepository,
    private val externalActions: ExternalActions.Presentation,
    estimatesMixin: BittensorEstimatesMixin,
    private val retryController: BittensorRetryController,
    private val validationExecutor: ValidationExecutor,
    private val exitValidationSystem: BittensorExitValidationSystem,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val exitFormatter: BittensorExitFormatter,
    private val saleCostBasisFormatter: BittensorSaleCostBasisFormatter,
    validatorInteractor: BittensorValidatorInteractor,
    private val logger: Logger
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    val isRoot = payload.position.netuid().isRoot()

    private val resultNavigation = BittensorResultNavigation(
        router,
        analytics,
        StakingFlow.UNSTAKE,
        if (isRoot) StakingAnalyticsTarget.ROOT else StakingAnalyticsTarget.SUBNET,
        newPosition = false
    )

    val slippage = payload.slippageFraction.toBigDecimal().movePointRight(2).toStripTrailingZerosString() + "%"

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }

    val wallet = walletUiUseCase.walletUiFlow(payload.position.metaId)
        .shareInBackground()

    val account = accountRepository.metaAccountFlow(payload.position.metaId)
        .map { walletUiUseCase.walletAddressModel(it, chain.await(), AddressIconGenerator.SIZE_MEDIUM) }
        .shareInBackground()

    private val confirmMutex = Mutex()

    private val chainRpcReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CHAIN_RPC, BittensorScreen.CONFIRM)

    val retryEvents = retryController.events

    private val validating = MutableStateFlow(false)

    private val execution = MutableStateFlow<ExitExecution>(ExitExecution.Idle)

    private val sheetDismissed = MutableStateFlow(false)

    private val reviewedQuote = MutableStateFlow(payload.quote())

    private val requestState = reviewedQuote.flatMapLatest { quote ->
        retryController.observe(
            step = "exit confirmation",
            analytics = chainRpcReads,
            transform = { it.withSafeLoading() }
        ) {
            flowOf {
                val position = bittensorExitInteractor.load(payload.position.metaId, payload.position.netuid(), payload.position.hotkeyAddress)
                BittensorExitRequest(position, quote)
            }
        }
    }
        .shareInBackground()

    private val request = requestState.map { it.dataOrNull }
        .shareInBackground()

    private val feeContext = request.mapNotNull { it?.position?.asset?.token?.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorExitRequest>>(
        scope = this,
        feeContext = feeContext
    )

    val feeStatus = combine(
        request,
        feeLoaderMixin.fee
    ) { request, status -> status.forRequest(request) }
        .shareInBackground()

    private val prepared = combine(
        requestState,
        feeStatus
    ) { state, status ->
        val request = state.dataOrNull
        val estimate = status.loadedFeeOrNull()
        when {
            request == null -> state.map { null }
            status is FeeStatus.Error || status is FeeStatus.NoFee -> ExtendedLoadingState.Loaded(null)
            estimate == null || estimate.request != request -> ExtendedLoadingState.Loading
            else -> {
                val fromStake = bittensorExitInteractor.feeFromStake(request.position, estimate)
                val draft = bittensorExitInteractor.prepare(
                    request.position,
                    request.quote,
                    estimate,
                    fromStake
                )
                ExtendedLoadingState.Loaded(draft)
            }
        }
    }
        .shareInBackground()

    val position = prepared.map { it.dataOrNull?.position }
        .shareInBackground()

    val title = resources.getString(if (isRoot) R.string.bittensor_exit_root_title else R.string.bittensor_exit_review_sale)

    val primaryAmount = prepared.map {
        it.dataOrNull?.let { draft ->
            exitFormatter.amount(draft.position, draft.quote.amount, !isRoot).let {
                if (draft.quote.fullExit) {
                    it.copy(
                        token = "≈ " + it.token
                    )
                } else {
                    it
                }
            }
        }
    }
        .shareInBackground()

    val assets = combine(prepared, execution) { prepared, state ->
        ((state as? ExitExecution.Success)?.draft ?: prepared.dataOrNull)?.let { draft ->
            val outcome = (state as? ExitExecution.Success)?.outcome
            val actualAlpha = outcome?.alpha
            val inIcon = draft.position.subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon
            val outIcon = assetIconProvider.getAssetIconOrFallback(draft.position.asset.token.configuration)
            val output = formatBittensorNativeResult(
                outcome?.actualSellReceive(draft.quote.serviceFee),
                draft.netReceive,
                draft.position.chain,
                tokenFormatter
            )
            SwapAssetsView.Model(
                SwapAssetView.Model(
                    inIcon,
                    exitFormatter.amount(draft.position, actualAlpha ?: draft.quote.amount, true).let {
                        if (actualAlpha == null) it.copy(token = "≈ " + it.token) else it.copy(fiat = null)
                    },
                    iconSizeDp = 48,
                    clipIconToOutline = true
                ),
                SwapAssetView.Model(outIcon, output, iconSizeDp = 48)
            )
        }
    }
        .shareInBackground()

    val stakeAfter = prepared.mapLoading {
        it?.let { draft ->
            val before = tokenFormatter.formatToken(
                draft.position.asset.token.amountFromPlanks(draft.position.preflight.totalStake),
                draft.position.asset.token.configuration.symbol
            )
            val after = if (draft.quote.fullExit) {
                BigInteger.ZERO
            } else {
                (draft.position.preflight.totalStake - draft.quote.amount).max(BigInteger.ZERO)
            }
            "$before → " + tokenFormatter.formatToken(draft.position.asset.token.amountFromPlanks(after), draft.position.asset.token.configuration.symbol)
        } ?: "—"
    }
        .shareInBackground()

    val rate = prepared.mapLoading { exitFormatter.exchangeRate(it) }
        .shareInBackground()

    // Keyed by the loaded position, not the fee-dependent draft, so a fee reload does not drop the estimate.
    private val saleCostBasis = estimatesMixin.observePositionCostBasis(
        request.map {
            it?.position?.takeUnless { it.isRoot }
                ?.let { BittensorEstimateContext(it.metaId, it.chain) }
        }.distinctUntilChanged()
    )
        .map { resolveSaleCostBasis(it, payload.position.netuid, carried = payload.costBasis?.totals()) }
        .shareInBackground()

    val averagePrice = combine(
        saleCostBasis,
        request
    ) { state, request ->
        state.map { sale ->
            saleCostBasisFormatter.averagePrice(sale, request?.position?.asset?.token, request?.position?.subnet?.symbol)
        }
    }
        .shareInBackground()

    val earned = combine(
        saleCostBasis,
        requestState
    ) { state, request ->
        bittensorLoadingValue(state, request) {
            val exit = request.dataOrNull
            saleCostBasisFormatter.profit(state.dataOrNull, exit?.quote, exit?.position?.asset?.token)
        }
    }
        .shareInBackground()

    val validator = payload.position.hotkeyAddress

    val validatorName = flowOf {
        validatorInteractor.validatorName(payload.position.netuid(), AccountIdKey(chain.await().accountIdOf(validator)))
    }
        .withSafeLoading()
        .onError { cause ->
            logger.log("[BittensorExitReview] Optional validator name unavailable")
            logger.log(cause)
        }
        .map { it.dataOrNull ?: validator.ellipsizeAddress() }
        .shareInBackground()

    val validatorIcon = position.map { position ->
        position?.let { addressIconGenerator.createAddressIcon(it.hotkey.value, 18, AddressIconGenerator.BACKGROUND_TRANSPARENT) }
    }
        .shareInBackground()

    val fee = feeStatus

    val executionFee = combine(fee, prepared, execution) { status, preparation, state ->
        val draft = (state as? ExitExecution.Success)?.draft ?: preparation.dataOrNull
        val outcome = (state as? ExitExecution.Success)?.outcome
        if (state is ExitExecution.Success && draft != null) {
            val fee = outcome.formatNetworkFee(draft.fee, draft.position.chain, resources, tokenFormatter)
            FeeStatus.Loaded(FeeModel(Unit, FeeDisplay(fee.amount, fee.fiatAmount)))
        } else {
            status.mapDisplay { display -> FeeDisplay("≈ " + display.title, null) }
        }
    }
        .shareInBackground()

    val commissionCaption = prepared.map { state ->
        state.dataOrNull?.let { draft ->
            val percentage = draft.quote.serviceFeeRate?.movePointRight(2)?.toStripTrailingZerosString()
                ?: return@map null
            resources.getString(R.string.bittensor_subnet_fee_caption, percentage)
        }
    }
        .shareInBackground()

    val feeCaption = prepared.map { state ->
        val draft = state.dataOrNull ?: return@map null
        val feeSource = resources.getString(
            if (draft.feeFromStake) R.string.bittensor_exit_fee_from_stake else R.string.bittensor_exit_fee_from_free,
            draft.position.asset.token.configuration.symbol.value
        )
        if (draft.quote.fullExit) resources.getString(R.string.bittensor_exit_full_position) + "\n" + feeSource else feeSource
    }
        .shareInBackground()

    val swapRateDescription = prepared.map { state ->
        val draft = state.dataOrNull ?: return@map null
        val percentage = draft.quote.serviceFeeRate?.movePointRight(2)?.toStripTrailingZerosString() ?: return@map null
        resources.getString(
            R.string.bittensor_swap_rate_sell_description,
            draft.position.asset.token.configuration.symbol.value,
            draft.position.subnet?.name.orEmpty(),
            percentage
        )
    }
        .shareInBackground()

    val averagePriceDescription = position.map { position ->
        position?.subnet?.let { resources.getString(R.string.bittensor_average_price_description, it.symbol, it.name) }
    }
        .shareInBackground()

    val profitDescription = position.map { position ->
        position?.let { resources.getString(R.string.bittensor_sale_profit_description, it.asset.token.configuration.symbol.value) }
    }
        .shareInBackground()

    val slippageDescription = resources.getString(R.string.bittensor_slippage_description, slippage)

    val isExecuting = execution.map { it is ExitExecution.Running }
        .shareInBackground()

    val isCompleted = execution.map { it is ExitExecution.Success }
        .shareInBackground()

    val canFinish = execution.map { it is ExitExecution.Success || it is ExitExecution.Unconfirmed }
        .shareInBackground()

    val executionUnconfirmed = execution.map { if (isRoot) null else (it as? ExitExecution.Unconfirmed)?.model }
        .shareInBackground()

    val showExecution = execution.map { !isRoot && it !is ExitExecution.Idle }
        .shareInBackground()

    val executionSubtitle = combine(
        position,
        execution
    ) { position, state ->
        if (state is ExitExecution.Success) {
            resources.formatDateTime(state.at)
        } else if (state is ExitExecution.Failure) {
            resources.formatDateTime(state.at)
        } else if (state is ExitExecution.Unconfirmed) {
            resources.formatDateTime(state.at)
        } else {
            resources.getString(
                R.string.bittensor_exit_selling_progress,
                position?.subnet?.name?.ifBlank { resources.getString(R.string.bittensor_subnet_number, payload.position.netuid) }
                    ?: resources.getString(R.string.bittensor_subnet_number, payload.position.netuid),
                position?.asset?.token?.configuration?.symbol?.value.orEmpty()
            )
        }
    }
        .shareInBackground()

    val confirmButtonState = combine(
        prepared,
        execution,
        validating
    ) { prepared, state, validating ->
        val label = resources.getString(R.string.common_confirm)
        val blocked = state is ExitExecution.Running || state is ExitExecution.Success || state is ExitExecution.Unconfirmed ||
            state is ExitExecution.Failure && !state.model.canRetry
        if (validating) {
            DescriptiveButtonState.Loading
        } else if (prepared.dataOrNull == null || blocked) {
            DescriptiveButtonState.Disabled(label)
        } else {
            DescriptiveButtonState.Enabled(label)
        }
    }
        .shareInBackground()

    val executionFailure = execution.map { state ->
        if (isRoot) null else (state as? ExitExecution.Failure)?.model
    }
        .shareInBackground()

    val resultState = combine(
        prepared,
        execution,
        sheetDismissed
    ) { prepared, state, dismissed ->
        if (dismissed) return@combine null
        when (state) {
            is ExitExecution.Unconfirmed -> if (isRoot) state.model else null
            is ExitExecution.Failure -> if (isRoot) state.model else null
            is ExitExecution.Running -> if (isRoot) {
                BittensorStakeResultUi.Progress(
                    resources.getString(R.string.bittensor_exit_unstaking_progress),
                    resources.getString(R.string.common_do_not_close_app)
                )
            } else {
                null
            }
            is ExitExecution.Success -> if (isRoot) {
                state.draft.let { draft ->
                    BittensorStakeResultUi.Success(
                        resources.getString(R.string.bittensor_exit_root_completed),
                        resources.getString(R.string.bittensor_exit_root_completed_description),
                        validator,
                        state.outcome.formatNetworkFee(draft.fee, draft.position.chain, resources, tokenFormatter),
                        amount = formatBittensorNativeResult(
                            state.outcome?.actualRootStakeMoved(),
                            draft.quote.amount,
                            draft.position.chain,
                            tokenFormatter
                        )
                    )
                }
            } else {
                null
            }
            ExitExecution.Idle -> null
        }
    }
        .shareInBackground()

    init {
        feeLoaderMixin.connectWith(request) { _, request ->
            request?.let {
                val fee = bittensorExitInteractor.estimateFee(it.position, it.quote)
                BittensorFeeEstimate(it, fee)
            }
        }
    }

    fun remainingExecutionTime(): Duration {
        val running = execution.value as? ExitExecution.Running ?: return Duration.ZERO
        val elapsed = (SystemClock.elapsedRealtime() - running.startedAt).milliseconds
        return (running.estimatedDuration - elapsed).coerceAtLeast(Duration.ZERO)
    }

    fun retryClicked() {
        val failure = execution.value as? ExitExecution.Failure ?: return
        if (!failure.model.canRetry) return
        execution.value = ExitExecution.Idle
    }

    fun confirmClicked(): Unit = confirmWith(acceptedImpactQuote = null, remainderSweepAccepted = false)

    private fun confirmWith(acceptedImpactQuote: BittensorExitQuote?, remainderSweepAccepted: Boolean): Unit = launchUnit {
        if (!confirmMutex.tryLock()) return@launchUnit
        try {
            if (confirmButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
            val draft = prepared.first().dataOrNull ?: return@launchUnit
            validating.value = true
            val estimatedDuration = chainStateRepository.expectedBlockTime(ChainGeneses.BITTENSOR)
            val freshPayload = bittensorExitInteractor.validationPayload(
                draft.position,
                draft.quote,
                draft.fee,
                draft.feeFromStake
            )
            val validationPayload = freshPayload.copy(
                priceImpactAccepted = acceptedImpactQuote != null && freshPayload.latestQuote == acceptedImpactQuote,
                remainderSweepAccepted = remainderSweepAccepted || payload.carriesRemainderSweepFor(draft.quote)
            )
            validationExecutor.requireValid(
                validationSystem = exitValidationSystem,
                payload = validationPayload,
                validationFailureTransformerCustom = { failure, _ ->
                    bittensorExitValidationFailureUi(
                        failure.reason,
                        resources,
                        isRoot = isRoot,
                        dustRemainder = exitFormatter.dustRemainder(validationPayload, draft.position),
                        onOutcome = { outcome ->
                            failure.reason.analyticsKind()?.let { kind ->
                                analytics.track(
                                    BittensorAnalyticsEvent.ValidationShown(
                                        StakingFlow.UNSTAKE,
                                        kind,
                                        StakingValidationStage.CONFIRM,
                                        outcome,
                                        if (payload.position.netuid == 0) StakingAnalyticsTarget.ROOT else StakingAnalyticsTarget.SUBNET
                                    )
                                )
                            }
                        },
                        unstakeAll = { refreshQuote(draft.quote, fullExit = true) },
                        acceptRemainder = {
                            val acceptedImpact = validationPayload.latestQuote.takeIf { validationPayload.priceImpactAccepted }
                            if (reviewedQuote.value == draft.quote) confirmWith(acceptedImpact, remainderSweepAccepted = true)
                        },
                        refreshQuote = { refreshQuote(draft.quote, fullExit = false) },
                        acceptImpact = {
                            if (reviewedQuote.value == draft.quote) confirmWith(validationPayload.latestQuote, validationPayload.remainderSweepAccepted)
                        }
                    )
                }
            ) {
                if (reviewedQuote.value == draft.quote) {
                    execution.value = ExitExecution.Running(SystemClock.elapsedRealtime(), estimatedDuration)
                    launchUnit { submit(draft) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            validating.value = false
            confirmMutex.unlock()
        }
    }

    private fun refreshQuote(expected: BittensorExitQuote, fullExit: Boolean) = launchUnit {
        if (!confirmMutex.tryLock()) return@launchUnit
        try {
            if (reviewedQuote.value != expected || execution.value !is ExitExecution.Idle) return@launchUnit
            validating.value = true
            val position = bittensorExitInteractor.load(payload.position.metaId, payload.position.netuid(), payload.position.hotkeyAddress)
            // Changing to Max is explicit; a market refresh preserves the reviewed participant plan.
            reviewedQuote.value = if (fullExit) {
                bittensorExitInteractor.quote(position, position.preflight.totalStake, fullExit = true, slippageFraction = expected.slippageFraction)
            } else {
                bittensorExitInteractor.refreshQuote(position, expected)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: BittensorExitExceedsStakeException) {
            showError(resources.getString(R.string.common_error_general_title), BittensorExitValidationFailure.INSUFFICIENT_STAKE.message(resources))
        } catch (e: Exception) {
            showError(e)
        } finally {
            validating.value = false
            confirmMutex.unlock()
        }
    }

    private suspend fun submit(draft: BittensorExitDraft) {
        sheetDismissed.value = false
        try {
            operationAnalytics.startOperation()
            val result = bittensorExitInteractor.execute(draft)
            trackOutcome(result, draft)
            when (result) {
                is BittensorOperationResult.Success -> execution.value = ExitExecution.Success(
                    System.currentTimeMillis(),
                    result.outcome,
                    draft
                )
                is BittensorOperationResult.Failure -> {
                    if (result.isSigningCancelled) {
                        execution.value = ExitExecution.Idle
                        return
                    }
                    val model = result.mapResult(resources)
                    execution.value = when (model) {
                        is BittensorStakeResultUi.Failure -> ExitExecution.Failure(model)
                        is BittensorStakeResultUi.Unconfirmed -> ExitExecution.Unconfirmed(model, draft)
                        else -> error("Unexpected failure presentation")
                    }
                }
                is BittensorOperationResult.Unknown -> execution.value = ExitExecution.Unconfirmed(result.mapResult(resources), draft)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val result = e.mapBittensorSubmissionError()
            trackOutcome(result, draft)
            execution.value = ExitExecution.Unconfirmed(result.mapResult(resources), draft)
        }
    }

    private fun trackOutcome(result: BittensorOperationResult, draft: BittensorExitDraft) {
        operationAnalytics.outcome(
            result,
            if (draft.position.isRoot) StakingAnalyticsTarget.ROOT else StakingAnalyticsTarget.SUBNET,
            StakingFlow.UNSTAKE,
            unstakeMode = if (draft.quote.fullExit) UnstakeMode.FULL else UnstakeMode.PARTIAL,
            hotkeys = draft.quote.groupHotkeys.size.takeIf { it > 0 } ?: 1,
            hasLocked = draft.position.preflight.availability.locked.signum() > 0,
            slippage = draft.quote.slippageFraction.takeUnless { draft.position.isRoot }?.let {
                SlippageBucket.from(it.movePointRight(2))
            }
        )
    }

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(
            account.first().address,
            chain.await()
        )
    }

    fun validatorClicked() = launchUnit {
        externalActions.showAddressActions(validator, chain.await())
    }

    fun resultDismissed() {
        sheetDismissed.value = true
        finishedEmptiesPosition()?.let(resultNavigation::leave)
    }

    fun doneClicked() {
        finishedEmptiesPosition()?.let(resultNavigation::done)
    }

    fun viewPositionClicked() {
        val state = execution.value as? ExitExecution.Success ?: return
        resultNavigation.viewPosition(state.draft.quote.fullExit)
    }

    fun back() {
        when (execution.value) {
            is ExitExecution.Running -> Unit
            is ExitExecution.Success, is ExitExecution.Unconfirmed -> resultNavigation.leave(finishedEmptiesPosition() ?: false)
            else -> router.back()
        }
    }

    /** Whether the finished exit emptied the position (iOS `emptiesPosition`); null while nothing has finished. */
    private fun finishedEmptiesPosition(): Boolean? = when (val state = execution.value) {
        is ExitExecution.Success -> state.draft.quote.fullExit
        is ExitExecution.Unconfirmed -> state.draft.quote.fullExit
        else -> null
    }
}
