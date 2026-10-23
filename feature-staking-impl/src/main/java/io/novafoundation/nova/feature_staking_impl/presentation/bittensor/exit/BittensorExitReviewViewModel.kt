package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.title
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
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorPositionCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitRequest
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
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
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
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitSelection
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.rootHoldBlocks
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorExitFailureValues
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novafoundation.nova.analytics.StakingValidationKind
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
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

/** One validator of a sale spread over several; [detail] marks a full exit. */
data class BittensorExitLegUi(val name: String, val amount: String, val detail: String?)

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
    private val validatorInteractor: BittensorValidatorInteractor,
    private val logger: Logger,
    private val durationFormatter: DurationFormatter
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    val isRoot = payload.position.netuid().isRoot()

    private companion object {

        const val BLOCKS_RESUBSCRIBE_DELAY_MILLIS = 5_000L
    }

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

    // As on iOS confirm, the rows show the latest block re-quote; the fee and the limit Confirm checks against stay with the acknowledged quote.
    private val quotes = MutableStateFlow(BittensorExitReviewQuotes(payload.quote()))

    private val acknowledgedQuote = quotes.map { it.acknowledged }
        .distinctUntilChanged()
        .shareInBackground()

    private val retryRequotes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val priceMoveBanner = BittensorExitPriceMoveBanner { outcome ->
        analytics.track(
            BittensorAnalyticsEvent.ValidationShown(
                StakingFlow.UNSTAKE,
                StakingValidationKind.PRICE_MOVED,
                StakingValidationStage.CONFIRM,
                outcome,
                StakingAnalyticsTarget.SUBNET
            )
        )
    }

    private val positionReload = MutableStateFlow(0)

    private val positionState = positionReload.flatMapLatest {
        retryController.observe(
            step = "exit confirmation",
            analytics = chainRpcReads,
            transform = { it.withSafeLoading() }
        ) {
            flowOf { bittensorExitInteractor.load(payload.position.metaId, payload.position.netuid(), payload.position.hotkeyAddress) }
        }
    }
        .shareInBackground()

    private val requestState = combine(positionState, acknowledgedQuote) { state, quote -> state.map { BittensorExitRequest(it, quote) } }
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
        feeStatus,
        quotes
    ) { state, status, reviewed ->
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
                    reviewed.displayed,
                    estimate,
                    fromStake
                )
                ExtendedLoadingState.Loaded(draft)
            }
        }
    }
        .shareInBackground()

    val isPriceMoved = quotes.map { it.isPriceMoved }
        .distinctUntilChanged()
        .shareInBackground()

    val position = prepared.map { it.dataOrNull?.position }
        .shareInBackground()

    val title = resources.getString(if (isRoot) R.string.bittensor_exit_root_title else R.string.bittensor_exit_review_sale)

    val primaryAmount = prepared.map {
        it.dataOrNull?.let { draft ->
            exitFormatter.amount(draft.position, draft.quote.amount, !isRoot, draft.quote).let {
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
                    exitFormatter.amount(draft.position, actualAlpha ?: draft.quote.amount, true, draft.quote).let {
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

    // iOS `createStakeAfter`: the ticker only after the arrow.
    val stakeAfter = prepared.mapLoading {
        it?.let { draft ->
            val before = tokenFormatter.formatToken(
                draft.position.asset.token.amountFromPlanks(draft.position.preflight.totalStake),
                draft.position.asset.token.configuration.symbol,
                TokenConfig(includeAssetTicker = false)
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
    // As on iOS confirm, it reads as loading until the position arrives, never as unknown.
    private val saleCostBasis = estimatesMixin.observePositionCostBasis(
        request.filterNotNull().map {
            it.position.takeUnless { it.isRoot }
                ?.let { BittensorEstimateContext(it.metaId, it.chain) }
        }.distinctUntilChanged()
    )
        .onStart { emit(BittensorPositionCostBasis(ExtendedLoadingState.Loading, isSettling = false)) }
        .map { resolveSaleCostBasis(it, payload.position.netuid, carried = payload.costBasis?.totals()) }
        .shareInBackground()

    val averagePrice = combine(
        saleCostBasis,
        request
    ) { state, request ->
        state.map { sale ->
            val alphaSymbol = bittensorAlphaSymbol(payload.position.netuid, request?.position?.subnet?.symbol)
            saleCostBasisFormatter.averagePrice(sale, request?.position?.asset?.token, alphaSymbol)
        }
    }
        .shareInBackground()

    val earned = combine(
        saleCostBasis,
        requestState,
        quotes
    ) { state, request, reviewed ->
        bittensorLoadingValue(state, request) {
            val exit = request.dataOrNull
            saleCostBasisFormatter.profit(state.dataOrNull, exit?.let { reviewed.displayed }, exit?.position?.asset?.token)
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

    /** iOS confirm and result: a plan over several validators lists them instead of naming one. */
    val isMultiValidator = acknowledgedQuote.map { quote -> quote.plan?.slices.orEmpty().map { it.hotkey }.distinct().size > 1 }
        .shareInBackground()

    private val legNames = acknowledgedQuote.map { quote -> quote.plan?.slices.orEmpty().map { it.hotkey }.toSet() }
        .distinctUntilChanged()
        .flatMapLatest { hotkeys -> flowOf { validatorInteractor.validatorNames(payload.position.netuid(), hotkeys) }.withSafeLoading() }
        .map { it.dataOrNull.orEmpty() }
        .shareInBackground()

    val legs = combine(request, isMultiValidator, legNames) { request, multiValidator, names ->
        if (request == null || !multiValidator) return@combine emptyList()
        request.quote.plan?.slices.orEmpty().map { slice ->
            BittensorExitLegUi(
                name = names[slice.hotkey] ?: request.position.chain.addressOf(slice.hotkey.value).ellipsizeAddress(),
                amount = exitFormatter.amount(request.position, slice.amount, !isRoot).token.toString(),
                detail = resources.getString(R.string.bittensor_exit_full_exit).takeIf { slice.fullExit }
            )
        }
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
            draft.position.subnet?.title(resources) ?: resources.getString(R.string.bittensor_subnet_number, payload.position.netuid),
            percentage
        )
    }
        .shareInBackground()

    val averagePriceDescription = position.map { position ->
        position?.subnet?.let {
            resources.getString(R.string.bittensor_average_price_description, bittensorAlphaSymbol(it.id.value, it.symbol), it.title(resources))
        }
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
                position?.subnet?.title(resources)
                    ?: resources.getString(R.string.bittensor_subnet_number, payload.position.netuid),
                position?.asset?.token?.configuration?.symbol?.value.orEmpty()
            )
        }
    }
        .shareInBackground()

    val confirmButtonState = combine(
        prepared,
        execution,
        validating,
        isPriceMoved
    ) { prepared, state, validating, priceMoved ->
        val label = resources.getString(if (priceMoved) R.string.bittensor_exit_confirm_new_rate else R.string.common_confirm)
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
        execution
    ) { prepared, state ->
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
                        ),
                        actions = resultNavigation.landingActions(draft.quote.fullExit)
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

        if (!isRoot) {
            // As on iOS, every new block re-quotes the sale until it is handed to submission, and a retry re-quotes at once.
            merge(bittensorExitInteractor.newBlocks(ChainGeneses.BITTENSOR, flowOf { true }), retryRequotes)
                .retryWhen { cause, _ ->
                    if (cause is CancellationException) throw cause
                    delay(BLOCKS_RESUBSCRIBE_DELAY_MILLIS)
                    true
                }
                .filter { execution.value is ExitExecution.Idle }
                .mapLatest { requote() }
                .inBackground()
                .launchIn(this)

            quotes.map { it.isPriceMoved }
                .distinctUntilChanged()
                .onEach(priceMoveBanner::sync)
                .launchIn(this)
        }
    }

    override fun onCleared() {
        priceMoveBanner.leave()
        super.onCleared()
    }

    fun remainingExecutionTime(): Duration {
        val running = execution.value as? ExitExecution.Running ?: return Duration.ZERO
        val elapsed = (SystemClock.elapsedRealtime() - running.startedAt).milliseconds
        return (running.estimatedDuration - elapsed).coerceAtLeast(Duration.ZERO)
    }

    /** iOS `didRequestRetry`: Try again returns to the review with the position, fee and quote refreshed; Confirm signs again. */
    fun retryClicked() {
        val failure = execution.value as? ExitExecution.Failure ?: return
        if (!failure.model.canRetry) return
        execution.value = ExitExecution.Idle
        positionReload.update { it + 1 }
        retryRequotes.tryEmit(Unit)
    }

    fun confirmClicked(): Unit = launchUnit {
        if (!confirmMutex.tryLock()) return@launchUnit
        try {
            priceMoveBanner.beginTap()
            if (confirmButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
            val loaded = prepared.first().dataOrNull ?: return@launchUnit
            // iOS `applyTapGate`: Confirm at the new rate acknowledges the latest quote; a crossing the banner does not show yet only raises it.
            val reviewed = when {
                quotes.value.isPriceMoved -> {
                    priceMoveBanner.acknowledge()
                    quotes.updateAndGet { it.acknowledgeLatest() }
                }
                quotes.value.latestCrossesAcknowledgedLimit -> {
                    quotes.update { it.copy(isPriceMoved = true) }
                    return@launchUnit
                }
                else -> quotes.value
            }
            val acknowledged = reviewed.acknowledged
            val draft = bittensorExitInteractor.prepare(loaded.position, reviewed.displayed, loaded.fee, loaded.feeFromStake)
            validating.value = true
            val estimatedDuration = chainStateRepository.expectedBlockTime(ChainGeneses.BITTENSOR)
            val validationPayload = bittensorExitInteractor.validationPayload(
                draft.position,
                draft.quote,
                draft.fee,
                draft.feeFromStake
            )
                .copy(acknowledgedLimit = acknowledged.limitPrice)
            val failureValues = failureValues(validationPayload, draft.position)
            validationExecutor.requireValid(
                validationSystem = exitValidationSystem,
                payload = validationPayload,
                validationFailureTransformerCustom = { failure, actions ->
                    // iOS `applyMovedQuote`: the moved price shows in the banner and the next Confirm takes it.
                    val moved = validationPayload.latestQuote?.takeIf { failure.reason == BittensorExitValidationFailure.QUOTE_CHANGED }
                    moved?.let { latest -> quotes.update { if (it.acknowledged == acknowledged) it.withMovedPrice(latest) else it } }
                    bittensorExitValidationFailureUi(
                        failure.reason,
                        resources,
                        isRoot = isRoot,
                        values = failureValues,
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
                        // iOS confirm warns about a swept remainder again, with Proceed and Cancel only.
                        unstakeAll = null,
                        // As on iOS, an accepted warning continues the same validation.
                        acceptRemainder = { actions.revalidate { it.copy(remainderSweepAccepted = true) } },
                        acceptRate = null,
                        acceptImpact = { actions.revalidate { it.copy(priceImpactAccepted = true) } }
                    )
                }
            ) { validated ->
                val state = execution.value
                val handedOff = state is ExitExecution.Running || state is ExitExecution.Success || state is ExitExecution.Unconfirmed
                val current = validated.latestQuote?.let { verified ->
                    quotes.updateAndGet { if (it.acknowledged == acknowledged) it.withLatest(verified) else it }
                } ?: quotes.value
                // iOS hands off only while no block re-quote has moved the price past the acknowledged limit since the tap.
                if (current.acknowledged == acknowledged && !current.isPriceMoved && !handedOff) {
                    priceMoveBanner.takeAcknowledgement()
                    execution.value = ExitExecution.Running(SystemClock.elapsedRealtime(), estimatedDuration)
                    launchUnit { submit(signedDraft(draft, validated)) }
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

    /** iOS signs the verified re-read quote, with its service fee, at the tighter of its own and the acknowledged limit. */
    private fun signedDraft(draft: BittensorExitDraft, validated: BittensorExitValidationPayload): BittensorExitDraft {
        val verified = validated.latestQuote ?: return draft
        val signed = verified.signedAt(verified.tighterLimit(than = requireNotNull(validated.acknowledgedLimit)))
        return bittensorExitInteractor.prepare(draft.position, signed, draft.fee, draft.feeFromStake)
    }

    /** Re-quotes the acknowledged sale on a block (iOS `refreshQuote`); after a failed one the review shows the acknowledged quote. */
    private suspend fun requote() {
        val current = request.filterNotNull().first()
        val latest = try {
            bittensorExitInteractor.refreshQuote(current.position, current.quote)
        } catch (cause: Exception) {
            if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
            logger.log("[BittensorExitReview] Block re-quote failed")
            logger.log(cause)
            chainRpcReads.failed(cause)
            null
        }
        quotes.update { state ->
            when {
                state.acknowledged != current.quote -> state
                latest == null -> state.withoutLatest()
                else -> state.withLatest(latest)
            }
        }
    }

    private fun reviewedSelection(position: BittensorExitPosition): BittensorExitSelection {
        return if (payload.allValidators) BittensorExitSelection.All else BittensorExitSelection.Validator(position.hotkey)
    }

    /** The values the alerts name, from the chain state the validation read. */
    private suspend fun failureValues(validationPayload: BittensorExitValidationPayload, position: BittensorExitPosition): BittensorExitFailureValues {
        val validated = position.copy(preflight = validationPayload.preflight)
        val maximum = bittensorExitInteractor.maximumAmount(validated, reviewedSelection(position), validationPayload.quote.slippageFraction)
        val holdEta = validationPayload.rootHoldBlocks().takeIf { it.isPositive() }
            ?.let { durationFormatter.format(bittensorExitInteractor.blocksDuration(position, it)) }
        return exitFormatter.failureValues(validationPayload, position, maximum, holdEta)
    }

    private suspend fun submit(draft: BittensorExitDraft) {
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
                    val model = result.mapResult(resources, resultNavigation.landingActions(draft.quote.fullExit))
                    execution.value = when (model) {
                        is BittensorStakeResultUi.Failure -> ExitExecution.Failure(model)
                        is BittensorStakeResultUi.Unconfirmed -> ExitExecution.Unconfirmed(model, draft)
                        else -> error("Unexpected failure presentation")
                    }
                }
                is BittensorOperationResult.Unknown ->
                    execution.value = ExitExecution.Unconfirmed(result.mapResult(resources, resultNavigation.landingActions(draft.quote.fullExit)), draft)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val result = e.mapBittensorSubmissionError()
            trackOutcome(result, draft)
            execution.value = ExitExecution.Unconfirmed(result.mapResult(resources, resultNavigation.landingActions(draft.quote.fullExit)), draft)
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

    fun validatorClicked() {
        router.openBittensorValidatorInfo(BittensorValidatorInfoPayload(payload.position.netuid, validator))
    }

    /** A root result sheet button, as iOS `activate(action:)`; Try again returns to the refreshed review. */
    fun resultActionClicked(action: BittensorResultAction) {
        if (!isRoot || action !in shownResultActions()) return

        resultNavigation.activate(action, finishedEmptiesPosition() ?: false) { retryClicked() }
    }

    /** The subnet failure page button, as iOS `createPageAction`: Try again when a retry can succeed, otherwise Close leaves the flow. */
    fun failureActionClicked() {
        val failure = execution.value as? ExitExecution.Failure ?: return
        val action = if (failure.model.canRetry) BittensorResultAction.TRY_AGAIN else BittensorResultAction.CLOSE

        resultNavigation.activate(action) { retryClicked() }
    }

    fun doneClicked() {
        finishedEmptiesPosition()?.let(resultNavigation::done)
    }

    fun back() {
        when (execution.value) {
            is ExitExecution.Running -> Unit
            is ExitExecution.Success, is ExitExecution.Unconfirmed -> resultNavigation.leave(finishedEmptiesPosition() ?: false)
            else -> router.back()
        }
    }

    private fun shownResultActions(): List<BittensorResultAction> = when (val state = execution.value) {
        is ExitExecution.Success -> resultNavigation.landingActions(state.draft.quote.fullExit)
        is ExitExecution.Unconfirmed -> state.model.actions
        is ExitExecution.Failure -> state.model.actions
        else -> emptyList()
    }

    /** Whether the finished exit emptied the position (iOS `emptiesPosition`); null while nothing has finished. */
    private fun finishedEmptiesPosition(): Boolean? = when (val state = execution.value) {
        is ExitExecution.Success -> state.draft.quote.fullExit
        is ExitExecution.Unconfirmed -> state.draft.quote.fullExit
        else -> null
    }
}
