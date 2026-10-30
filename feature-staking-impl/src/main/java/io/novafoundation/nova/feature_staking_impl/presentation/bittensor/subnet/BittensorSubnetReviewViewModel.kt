package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevReview
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevReviewFee
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorMevFeeRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorMevSwitchState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forMevSelection
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorStakeAlertFormat
import kotlinx.coroutines.flow.onStart
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorValidatorRowUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLeadingApy
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorYieldColor
import io.novafoundation.nova.common.utils.ltrIsolated
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.title
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingValidationStage
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.analyticsKind
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.reportWatchOnlyStake
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingMode
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorOperationAnalytics
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorResultNavigation

import android.os.SystemClock
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLoadingValue
import io.novafoundation.nova.common.view.ExecutionTimerView
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSlippage
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.actualBuyPay
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatMevFailureFee
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatNetworkFee
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatBittensorNativeResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mapBittensorPreSubmissionError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mapBittensorSubmissionError
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.mapResult
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.toSubmissionState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPreparedSubnetStake
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakeDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorMonthlyProjection
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorStakeValidationFailureUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.mapDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetsView
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class BittensorSubnetReviewViewModel(
    private val analytics: AnalyticsService,
    private val operationAnalytics: BittensorOperationAnalytics,
    private val subnetId: BittensorNetuid.Subnet,
    private val totalInPlanks: BigInteger,
    private val metaId: Long,
    private val validatorAddress: String,
    private val router: StakingRouter,
    chainRegistry: ChainRegistry,
    walletUiUseCase: WalletUiUseCase,
    accountRepository: AccountRepository,
    private val bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
    private val resources: ResourceManager,
    private val icons: AssetIconProvider,
    private val externalActions: ExternalActions.Presentation,
    private val iconGenerator: AddressIconGenerator,
    private val retryController: BittensorRetryController,
    private val bittensorAmountFormatter: BittensorAmountFormatter,
    private val validationSystem: BittensorStakePreflightSystem,
    private val validationExecutor: ValidationExecutor,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val mevReview: BittensorMevReview,
    private val estimatesMixin: BittensorEstimatesMixin,
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    private val chainStateRepository: ChainStateRepository,
    private val selectedSlippage: BigDecimal,
    val existingPosition: Boolean = false,
    private val inactiveValidatorAccepted: Boolean = false
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    private val chainRpcReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CHAIN_RPC, BittensorScreen.CONFIRM)

    /** Set once the stake is confirmed, also when it then fails. */
    private var flowContinued = false

    val retryEvents = retryController.events

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }

    val wallet = walletUiUseCase.walletUiFlow(metaId)
        .shareInBackground()

    val account = accountRepository.metaAccountFlow(metaId).map { walletUiUseCase.walletAddressModel(it, chain.await(), AddressIconGenerator.SIZE_MEDIUM) }
        .shareInBackground()

    private val submission = MutableStateFlow<BittensorSubmissionState>(BittensorSubmissionState.Ready)

    val executionResult = submission.map { it.result }
        .shareInBackground()

    private val walletState = retryController.observe(
        step = "staking wallet",
        transform = { it.withSafeLoading() }
    ) {
        bittensorSubnetStakingInteractor.observeWallet(
            metaId
        )
    }
        .shareInBackground()

    private val walletContext = walletState.filterLoaded()
        .shareInBackground()

    private val preparationRefresh = MutableStateFlow(0)

    private val validating = MutableStateFlow(false)

    val canChangeMevProtection = combine(submission, validating) { submission, validating -> submission.canSubmit && !validating }
        .shareInBackground()

    private val retryRequotes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Every new block re-quotes until the stake is handed to submission, and a retry re-quotes at once.
    private val requotes = bittensorSubnetStakingInteractor.requotes(
        active = submission.map { it is BittensorSubmissionState.Ready },
        immediate = retryRequotes
    )
        .inBackground()
        .shareIn(this, SharingStarted.Eagerly)

    private val preparedStake = retryController.observe(
        step = "stake preparation",
        analytics = chainRpcReads,
        transform = { it.withSafeLoading() }
    ) {
        preparationRefresh.flatMapLatest {
            flowOf {
                bittensorSubnetStakingInteractor.prepare(
                    metaId,
                    subnetId,
                    totalInPlanks,
                    validatorAddress,
                    BittensorSlippage(selectedSlippage),
                    existingPosition = existingPosition
                )
            }
        }
    }
        .shareInBackground()

    private val preparationState = preparedStake.flatMapLatest { state ->
        state.dataOrNull?.let(::requotedEachBlock) ?: flowOf { state }
    }
        .shareInBackground()

    private val draft = preparationState.map { it.dataOrNull?.draft }
        .shareInBackground()

    // Block re-quotes only update what is shown. The fee and the confirm check keep the prepared (acknowledged) quote,
    // so a price move past its limit asks for the updated quote before anything is signed.
    private val acknowledgedDraft = preparedStake.map { it.dataOrNull?.draft }
        .distinctUntilChanged()
        .shareInBackground()

    private val quote = draft.map { it?.operation?.quote }
        .shareInBackground()

    val feeCaption = quote.map { quote ->
        val serviceFeeRate = quote?.serviceFeeRate ?: return@map ""
        val percentage = serviceFeeRate.movePointRight(2).toStripTrailingZerosString()
        resources.getString(R.string.bittensor_subnet_fee_caption, percentage)
    }
        .shareInBackground()

    val swapRateDescription = combine(
        preparationState,
        walletContext
    ) { preparation, wallet ->
        preparation.dataOrNull?.let { prepared ->
            resources.getString(
                R.string.bittensor_swap_rate_buy_description,
                prepared.subnet.title(resources),
                wallet.asset.token.configuration.symbol.value,
                prepared.draft.operation.quote.serviceFeeRate.movePointRight(2).toStripTrailingZerosString()
            )
        }
    }
        .shareInBackground()

    val networkFeeDescription = walletContext.map { wallet ->
        resources.getString(R.string.bittensor_network_fee_buy_description, wallet.asset.token.configuration.symbol.value)
    }
        .shareInBackground()

    val averagePriceDescription = preparationState.map { preparation ->
        preparation.dataOrNull?.subnet?.let { subnet ->
            resources.getString(R.string.bittensor_average_price_description, subnet.symbol, subnet.title(resources))
        }
    }
        .shareInBackground()

    private val hasError = combine(
        walletState,
        preparationState
    ) { walletContext, preparation -> walletContext.isError() || preparation.isError() }
        .shareInBackground()

    private val yieldState = estimatesMixin.observeAlphaYields(
        walletContext.map { it.chain },
        subnetId
    )
        .shareInBackground()

    private val costBasisState = estimatesMixin.observePositionCostBasis(
        walletContext.map {
            it.takeIf { existingPosition && it.metaId == metaId }
                ?.let { BittensorEstimateContext(metaId, it.chain) }
        }
    )
        .shareInBackground()

    val monthlyEstimate = combine(
        yieldState,
        walletContext,
        quote,
        preparationState
    ) { state, walletContext, quote, preparation ->
        bittensorLoadingValue(state, preparation) {
            val subnet = preparation.dataOrNull?.subnet
            val key = runCatching { AccountIdKey(walletContext.chain.accountIdOf(validatorAddress)) }
                .getOrNull()
            val annual = key?.let { state.dataOrNull?.get(it) }
            if (quote == null || annual == null || subnet == null) {
                "—"
            } else {
                val alpha = bittensorMonthlyProjection(BigDecimal(quote.alphaOutInPlanks, BittensorProtocolUnits.ALPHA_PRECISION), annual)
                ("≈ " + tokenFormatter.formatToken(alpha, TokenSymbol(subnet.symbol.ifBlank { "—" }))).ltrIsolated()
            }
        }
    }
        .shareInBackground()

    val projectedAveragePrice = combine(
        costBasisState,
        draft.map { it?.operation?.quote },
        walletContext,
        preparationState
    ) { costBasis, quote, walletContext, quoteLoading ->
        val state = costBasis.buyMoreCostBasis(subnetId.value)
        bittensorLoadingValue(state, quoteLoading) {
            val basis = state.dataOrNull
            val current = basis?.forSubnet(subnetId.value)
            if (current == null || quote == null) {
                if (basis?.canEstimate(subnetId.value) == true && current == null) resources.getString(R.string.common_not_available) else "—"
            } else {
                val projected = current.adding(quote.totalTaoInPlanks, quote.alphaOutInPlanks)
                "≈ " + tokenFormatter.formatToken(projected.averagePrice, walletContext.asset.token.configuration.symbol)
            }
        }
    }
        .shareInBackground()

    private val feeContext = walletContext
        .map { it.asset.token.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorSubnetStakeDraft>>(
        scope = this,
        feeContext = feeContext
    )

    private val mevEnabled = MutableStateFlow(true)

    private val mevSupported = flowOf { mevReview.supports(metaId) }
        .shareInBackground()

    val fee = combine(
        acknowledgedDraft,
        feeLoaderMixin.fee,
        mevEnabled
    ) { draft, status, enabled ->
        status.forRequest(draft).forMevSelection(enabled)
    }
        .shareInBackground()

    val mevProtection = combine(mevSupported, mevEnabled, fee) { supported, enabled, fee ->
        bittensorMevSwitchState(supported, enabled, fee.loadedFeeOrNull()?.fee)
    }
        .shareInBackground()

    val validator = draft.map { it?.validator?.name ?: resources.getString(R.string.bittensor_select_validator) }
        .shareInBackground()

    private val nativeToken = walletContext.map { it.asset.token }
        .shareInBackground()

    val assets = combine(
        nativeToken,
        preparationState
    ) { token, preparation -> mapAssets(token, preparation.dataOrNull) }
        .shareInBackground()

    private var executionStartedAt = 0L

    private var executionPeriodMultiplier = 1

    private var executionCompletedAt = 0L

    private val duration = flowOf { chainStateRepository.expectedBlockTime(ChainGeneses.BITTENSOR) }
        .catch { emit(Duration.ZERO) }
        .shareInBackground()

    val isExecuting = submission.map { it is BittensorSubmissionState.Submitting }
        .shareInBackground()

    val showExecution = submission.map { it !is BittensorSubmissionState.Ready }
        .shareInBackground()

    val executionSubtitle = combine(
        preparationState,
        submission
    ) { prepared, state ->
        if (state is BittensorSubmissionState.Completed || state is BittensorSubmissionState.Failed || state is BittensorSubmissionState.Unconfirmed) {
            resources.formatDateTime(executionCompletedAt)
        } else {
            resources.getString(
                R.string.bittensor_subnet_execution_buying,
                prepared.dataOrNull?.subnet.title(subnetId.value, resources),
                prepared.dataOrNull?.subnet?.symbol.orEmpty()
            )
        }
    }
        .shareInBackground()

    val executionAssets = combine(assets, submission, preparationState, nativeToken) { assets, state, preparation, token ->
        val output = assets.assetOut
        val result = state.result as? BittensorStakeResultUi.Success
        val outcome = result?.outcome
        val subnet = preparation.dataOrNull?.subnet
        val actualAlpha = outcome?.alpha?.let { BigDecimal(it, BittensorProtocolUnits.ALPHA_PRECISION) }
        val actualOutput = actualAlpha?.let { alpha ->
            subnet?.symbol?.let { tokenFormatter.formatToken(alpha, TokenSymbol(it)) }
        }
        // As on iOS, the done page keeps the fiat value under both amounts.
        val paidIn = outcome?.actualBuyPay(preparation.dataOrNull?.draft?.operation?.quote?.serviceFeeTaoInPlanks ?: BigInteger.ZERO) ?: totalInPlanks
        val inputFiat = formatFiat(token, token.toFiatOrNull(token.amountFromPlanks(paidIn)))
        val outputFiat = if (actualAlpha == null) {
            output.amount.fiat
        } else {
            formatFiat(token, subnet?.priceInRao?.let(token::amountFromPlanks)?.let { token.toFiatOrNull(actualAlpha * it) })
        }
        SwapAssetsView.Model(
            assetIn = SwapAssetView.Model(
                assetIcon = assets.assetIn.assetIcon,
                amount = (result?.amount ?: formatBittensorNativeResult(null, totalInPlanks, chain.await(), tokenFormatter)).copy(fiat = inputFiat),
                iconSizeDp = assets.assetIn.iconSizeDp
            ),
            assetOut = SwapAssetView.Model(
                assetIcon = output.assetIcon,
                amount = output.amount.copy(token = actualOutput?.ltrIsolated() ?: ("≈ " + output.amount.token).ltrIsolated(), fiat = outputFiat),
                iconSizeDp = output.iconSizeDp,
                clipIconToOutline = output.clipIconToOutline
            )
        )
    }
        .shareInBackground()

    val executionFee = combine(fee, submission) { status, state ->
        val resultFee = when (val result = state.result) {
            is BittensorStakeResultUi.Success -> result.fee
            is BittensorStakeResultUi.Failure -> result.fee
            else -> null
        }
        if (resultFee != null) {
            FeeStatus.Loaded(FeeModel(Unit, FeeDisplay(resultFee.amount, resultFee.fiatAmount)))
        } else {
            status.mapDisplay { display -> FeeDisplay("≈ " + display.title, null) }
        }
    }
        .shareInBackground()

    val rate = combine(
        nativeToken,
        preparationState
    ) { token, preparation -> preparation.map { formatRate(token, it) } }
        .shareInBackground()

    val slippage = selectedSlippage.multiply(BigDecimal(100)).toStripTrailingZerosString() + "%"

    val slippageDescription = resources.getString(R.string.bittensor_slippage_description, slippage)

    val validatorIcon = draft.map { prepared ->
        prepared?.validator?.let {
            iconGenerator.createAddressIcon(it.hotkey.value, AddressIconGenerator.SIZE_MEDIUM, AddressIconGenerator.BACKGROUND_TRANSPARENT)
        }
    }
        .shareInBackground()

    private val validatorAnnualYield = combine(yieldState, walletContext) { state, walletContext ->
        val key = runCatching { AccountIdKey(walletContext.chain.accountIdOf(validatorAddress)) }.getOrNull()
        key?.let { state.dataOrNull?.get(it) }
    }
        .onStart { emit(null) }

    /** As on iOS, the review shows the validator with "Est. 33.99% APY" under its name. */
    val validatorRow = combine(validator, validatorIcon, validatorAnnualYield) { name, icon, annual ->
        BittensorValidatorRowUi(
            name = name,
            icon = icon?.let(Icon::FromDrawable),
            rate = resources.bittensorLeadingApy(annual),
            rateColorRes = bittensorYieldColor(annual)
        )
    }
        .shareInBackground()

    val confirmButtonState = combine(
        draft,
        validating,
        submission,
        hasError,
        fee
    ) { draft, validating, submission, failed, fee ->
        val label = resources.getString(R.string.common_confirm)
        val canConfirm = draft != null && !validating && submission.canSubmit && !failed && fee.loadedFeeOrNull() != null
        if (canConfirm) DescriptiveButtonState.Enabled(label) else DescriptiveButtonState.Disabled(label)
    }
        .shareInBackground()

    init {
        val feeRequests = combine(acknowledgedDraft, mevEnabled) { draft, enabled -> BittensorMevFeeRequest(draft, enabled) }
        feeLoaderMixin.connectWith(feeRequests) { _, request ->
            request.request?.let { draft ->
                val fee = bittensorSubnetStakingInteractor.estimateReviewFee(draft, request.enabled, mevReview)
                BittensorFeeEstimate(draft, fee)
            }
        }
        requotes.onEach {
            if (mevSupported.first() && !validating.value && feeLoaderMixin.fee.value is FeeStatus.Loaded) {
                val draft = acknowledgedDraft.first() ?: return@onEach
                val enabled = mevEnabled.value
                feeLoaderMixin.loadFee {
                    BittensorFeeEstimate(draft, bittensorSubnetStakingInteractor.estimateReviewFee(draft, enabled, mevReview))
                }
            }
        }.launchIn(this)
        launch {
            // A watch-only wallet is reported blocked once the review opens, and leaving it is not an abandonment.
            if (accountRepository.getMetaAccount(metaId).type == LightMetaAccount.Type.WATCH_ONLY) {
                flowContinued = true
                analytics.reportWatchOnlyStake(StakingAnalyticsTarget.SUBNET)
            }
        }
    }

    fun mevProtectionChanged(enabled: Boolean) {
        if (submission.value.canSubmit && !validating.value) mevEnabled.value = enabled
    }

    fun executionTimerState(): ExecutionTimerView.State.CountdownTimer {
        val repeat = duration.replayCache.lastOrNull()?.takeIf { executionPeriodMultiplier == 4 && it.isPositive() }
        return ExecutionTimerView.State.CountdownTimer(remainingExecutionTime(), repeat)
    }

    fun remainingExecutionTime(): Duration = (
        (duration.replayCache.lastOrNull() ?: Duration.ZERO) * executionPeriodMultiplier -
            (SystemClock.elapsedRealtime() - executionStartedAt).milliseconds
        )
        .coerceAtLeast(Duration.ZERO)

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(
            account.first().address,
            chain.await()
        )
    }

    fun validatorClicked() {
        router.openBittensorValidatorInfo(BittensorValidatorInfoPayload(subnetId.value, validatorAddress))
    }

    fun confirmClicked() = launchUnit {
        if (!submission.value.canSubmit || hasError.first() || validating.value) return@launchUnit
        val prepared = acknowledgedDraft.first() ?: return@launchUnit
        val fee = fee.first()
            .loadedFeeOrNull()
            ?.takeIf { it.request == prepared } ?: return@launchUnit
        val token = walletContext.first().asset.token
        val subnet = preparedStake.first().dataOrNull?.subnet
        validating.value = true
        try {
            val payload = bittensorSubnetStakingInteractor.validationPayload(prepared, fee.fee)
                .copy(inactiveValidatorAccepted = inactiveValidatorAccepted, slippage = BittensorSlippage(selectedSlippage))
            val alertFormat = tokenFormatter.bittensorStakeAlertFormat(token) { quote ->
                subnet?.let { bittensorAmountFormatter.exchangeRate(quote, it, token) } ?: "—"
            }
            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                errorDisplayer = ::showConfirmError,
                progressConsumer = validating.progressConsumer(),
                scope = this@BittensorSubnetReviewViewModel,
                validationFailureTransformerCustom = { failure, actions ->
                    bittensorStakeValidationFailureUi(
                        failure,
                        actions,
                        resources,
                        alertFormat,
                        onOutcome = { outcome ->
                            failure.reason.analyticsKind()?.let { kind ->
                                analytics.track(
                                    BittensorAnalyticsEvent.ValidationShown(
                                        StakingFlow.STAKE,
                                        kind,
                                        StakingValidationStage.CONFIRM,
                                        outcome,
                                        StakingAnalyticsTarget.SUBNET
                                    )
                                )
                            }
                        },
                        // A moved price is acknowledged before anything is signed: Confirm prepares the stake at the new rate.
                        acceptRate = { preparationRefresh.value += 1 }
                    ) { preparationRefresh.value += 1 }
                }
            ) { validated ->
                executeStake(prepared.copy(inactiveValidatorAccepted = validated.inactiveValidatorAccepted), fee.fee)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            executionCompletedAt = System.currentTimeMillis()
            submission.value = e.mapBittensorPreSubmissionError().mapResult(resources).toSubmissionState()
        } finally {
            validating.value = false
        }
    }

    private val resultNavigation = BittensorResultNavigation(
        router,
        analytics,
        StakingFlow.STAKE,
        StakingAnalyticsTarget.SUBNET,
        newPosition = !existingPosition
    )

    private val analyticsMode get() = if (existingPosition) StakingMode.BUY_MORE else StakingMode.NEW_POSITION

    private fun executeStake(prepared: BittensorSubnetStakeDraft, fee: Fee) = launchUnit {
        if (!submission.value.canSubmit) return@launchUnit
        executionStartedAt = SystemClock.elapsedRealtime()
        executionPeriodMultiplier = if ((fee as? BittensorMevReviewFee)?.isProtected == true) 4 else 1
        submission.value = BittensorSubmissionState.Submitting(mapProgress())
        try {
            operationAnalytics.startOperation()
            flowContinued = true
            analytics.track(
                BittensorAnalyticsEvent.Confirmed(
                    BittensorAnalyticsEvent.NETWORK,
                    StakingAnalyticsTarget.SUBNET,
                    analyticsMode,
                    false,
                    null
                )
            )
            val result = bittensorSubnetStakingInteractor.execute(prepared, (fee as? BittensorMevReviewFee)?.isProtected == true) { hash ->
                executionCompletedAt = System.currentTimeMillis()
                submission.value = BittensorOperationResult.Unknown(hash).mapResult(resources).toSubmissionState()
            }
            operationAnalytics.outcome(result, StakingAnalyticsTarget.SUBNET, StakingFlow.STAKE, mode = analyticsMode)
            executionCompletedAt = System.currentTimeMillis()
            when (result) {
                is BittensorOperationResult.Success -> {
                    executionCompletedAt = System.currentTimeMillis()
                    submission.value = BittensorSubmissionState.Completed(mapSuccess(prepared, fee, result.outcome))
                }
                is BittensorOperationResult.Failure -> {
                    val mapped = result.toSubmissionState(resources)
                    val failure = mapped.result as? BittensorStakeResultUi.Failure
                    submission.value = if (failure != null) {
                        BittensorSubmissionState.Failed(failure.copy(fee = result.formatMevFailureFee(fee, chain.await(), tokenFormatter)))
                    } else {
                        mapped
                    }
                }
                is BittensorOperationResult.Unknown -> submission.value = result.mapResult(resources).toSubmissionState()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            executionCompletedAt = System.currentTimeMillis()
            val result = e.mapBittensorSubmissionError()
            operationAnalytics.outcome(result, StakingAnalyticsTarget.SUBNET, StakingFlow.STAKE, mode = analyticsMode)
            submission.value = result.mapResult(resources).toSubmissionState()
        } finally {
            validating.value = false
        }
    }

    private fun mapProgress(): BittensorStakeResultUi.Progress = BittensorStakeResultUi.Progress(
        resources.getString(R.string.bittensor_staking_progress_title),
        resources.getString(R.string.bittensor_subnet_staking_progress, subnetId.value)
    )

    private suspend fun mapSuccess(draft: BittensorSubnetStakeDraft, fee: Fee, outcome: BittensorOperationOutcome?): BittensorStakeResultUi.Success {
        return BittensorStakeResultUi.Success(
            resources.getString(R.string.bittensor_subnet_staking_success, subnetId.value),
            // The done-state pill reads "1 operation" as on iOS; the progress state keeps "1 of 1 operation".
            resources.getString(R.string.bittensor_exit_completed_operation),
            draft.validator.name,
            outcome.formatNetworkFee(fee, chain.await(), resources, tokenFormatter),
            outcome,
            amount = formatBittensorNativeResult(
                outcome?.actualBuyPay(draft.operation.quote.serviceFeeTaoInPlanks),
                draft.operation.quote.totalTaoInPlanks,
                chain.await(),
                tokenFormatter
            )
        )
    }

    private fun mapAssets(token: TokenBase, prepared: BittensorPreparedSubnetStake?): SwapAssetsView.Model = SwapAssetsView.Model(
        assetIn = mapInputAsset(token),
        assetOut = mapOutputAsset(token, prepared)
    )

    private fun mapInputAsset(token: TokenBase): SwapAssetView.Model {
        val input = token.amountFromPlanks(totalInPlanks)
        return SwapAssetView.Model(
            assetIcon = icons.getAssetIconOrFallback(token.configuration),
            amount = bittensorAmountFormatter.amount(
                input,
                token.configuration.symbol,
                token.toFiatOrNull(input),
                token.currency
            ),
            iconSizeDp = 48
        )
    }

    private fun mapOutputAsset(token: TokenBase, prepared: BittensorPreparedSubnetStake?): SwapAssetView.Model = SwapAssetView.Model(
        assetIcon = prepared?.subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon,
        amount = mapOutputAmount(token, prepared),
        iconSizeDp = 48,
        clipIconToOutline = true
    )

    private fun mapOutputAmount(token: TokenBase, prepared: BittensorPreparedSubnetStake?): AmountModel {
        if (prepared == null) return AmountModel("—", null)

        val output = BigDecimal(prepared.draft.operation.quote.alphaOutInPlanks, BITTENSOR_ALPHA_DECIMALS)
        val alphaPrice = prepared.subnet.priceInRao?.let(token::amountFromPlanks)
        val outputFiat = alphaPrice?.let { token.toFiatOrNull(output * it) }
        val symbol = TokenSymbol(prepared.subnet.symbol.ifBlank { "—" })
        val amount = bittensorAmountFormatter.amount(
            output,
            symbol,
            outputFiat,
            token.currency
        )
        return amount.copy(token = amount.token.ltrIsolated())
    }

    private fun formatFiat(token: TokenBase, fiat: BigDecimal?): CharSequence? {
        return fiat?.let { fiatFormatter.formatFiat(it, token.currency) }
    }

    private fun formatRate(token: TokenBase, prepared: BittensorPreparedSubnetStake?): String {
        if (prepared == null) return "—"
        return bittensorAmountFormatter.exchangeRate(prepared.draft.operation.quote, prepared.subnet, token)
    }

    /** A failed re-quote falls back to the prepared stake, so the acknowledged quote stays shown. */
    private fun requotedEachBlock(prepared: BittensorPreparedSubnetStake): Flow<ExtendedLoadingState<BittensorPreparedSubnetStake>> =
        bittensorSubnetStakingInteractor.requoted(prepared.draft, BittensorSlippage(selectedSlippage), requotes)
            .onEach { requote -> requote.onFailure(chainRpcReads::failed) }
            .map { requote -> ExtendedLoadingState.Loaded(prepared.copy(draft = requote.getOrDefault(prepared.draft))) }
            .onStart { emit(ExtendedLoadingState.Loaded(prepared)) }

    // A slow node gets the same Retry / Skip dialog as a failed read, not the raw "<read> timed out" text.
    private fun showConfirmError(error: Throwable) {
        if (!retryController.reportSlowAction("subnet stake review", error, ::confirmClicked)) showError(error)
    }

    fun retryClicked() {
        val failure = submission.value.result as? BittensorStakeResultUi.Failure ?: return
        if (!failure.canRetry) return
        submission.value = BittensorSubmissionState.Ready
        retryRequotes.tryEmit(Unit)
    }

    fun failureActionClicked() {
        val failure = submission.value.result as? BittensorStakeResultUi.Failure ?: return
        val action = if (failure.canRetry) BittensorResultAction.TRY_AGAIN else BittensorResultAction.CLOSE

        resultNavigation.activate(action) { retryClicked() }
    }

    fun viewPositionClicked() {
        if (submission.value.isFinished) resultNavigation.done()
    }

    fun back() {
        when {
            submission.value is BittensorSubmissionState.Submitting -> Unit
            submission.value.isFinished -> resultNavigation.leave()
            else -> {
                // Leaving the review of a new position before confirming reports it abandoned.
                if (!flowContinued && !existingPosition) {
                    analytics.track(BittensorAnalyticsEvent.Abandoned(BittensorScreen.CONFIRM, StakingAnalyticsTarget.SUBNET))
                }
                router.back()
            }
        }
    }
}
