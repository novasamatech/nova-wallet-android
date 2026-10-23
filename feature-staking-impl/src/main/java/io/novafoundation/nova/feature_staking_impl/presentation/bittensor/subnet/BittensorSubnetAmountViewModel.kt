package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorStakeAlertFormat
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.title
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorAnalyticsEvent
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingValidationStage
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.analyticsKind
import io.novafoundation.nova.analytics.StakingBlockedReason
import io.novafoundation.nova.analytics.StakingAnalyticsSelectionMethod
import io.novafoundation.nova.analytics.SubnetDetailsSource
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingMode
import io.novafoundation.nova.analytics.StakingValidatorSource
import io.novafoundation.nova.analytics.SlippageBucket
import io.novafoundation.nova.analytics.AnalyticsService

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetStakeOperation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPresetValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSlippage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakeDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorMonthlyProjection
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.maximumBittensorStake
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorMaxAmount
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorPositionCostBasis
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLoadingValue
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorNeedsFunding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorStakeValidationFailureUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorSaleCostBasisFormatter
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.actualAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmountInput
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetFactsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetFacts
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn

class BittensorSubnetAmountViewModel(
    private val analytics: AnalyticsService,
    private val initialSubnetId: BittensorNetuid.Subnet?,
    private val quickPickOrder: BittensorQuickPickOrder?,
    private val quickPickInteractor: BittensorQuickPickInteractor,
    private val presetValidatorInteractor: BittensorPresetValidatorInteractor,
    validatorAddress: String?,
    validatorIsPreset: Boolean,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val tokenFormatter: TokenFormatter,
    private val logger: Logger,
    private val bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
    private val fiatFormatter: FiatFormatter,
    private val retryController: BittensorRetryController,
    private val bittensorAmountFormatter: BittensorAmountFormatter,
    private val amountChooserFactory: AmountChooserMixin.Factory,
    private val validationSystem: BittensorStakePreflightSystem,
    private val validationExecutor: ValidationExecutor,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    val existingPosition: Boolean,
    private val costBasisFormatter: BittensorSaleCostBasisFormatter,
    private val estimatesMixin: BittensorEstimatesMixin,
    private val slippageSettings: BittensorSlippageSettings,
    private val slippageSettingsCommunicator: SlippageSettingsCommunicator,
    private val validatorCommunicator: BittensorSelectValidatorCommunicator,
    getAssetOptionsMixinFactory: GetAssetOptionsMixin.Factory,
    subnetFactsInteractor: BittensorSubnetFactsInteractor,
    chainStateRepository: ChainStateRepository,
    initialSelectionState: BittensorSelectionState = BittensorSelectionState()
) : BaseViewModel(), Validatable by validationExecutor {

    private var selectionState = initialSelectionState

    private var isNoBalanceTracked = false

    private var isTypeSelectedTracked = false

    private val subnetsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.SUBNETS, BittensorScreen.SETUP)
    private val validatorsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.VALIDATORS, BittensorScreen.SETUP)
    private val chainRpcReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CHAIN_RPC, BittensorScreen.SETUP)

    val retryEvents = retryController.events

    val isQuickPick = quickPickOrder != null

    /** Buy more keeps the slippage chosen when the position was opened, so it offers no settings (as on iOS). */
    val showsSlippageSettings = !existingPosition

    val subnetSectionTitle = resources.getString(
        when {
            existingPosition -> R.string.bittensor_your_subnet
            quickPickOrder != null -> quickPickOrder.sectionTitleRes()
            else -> R.string.bittensor_your_pick
        }
    )

    private val validating = MutableStateFlow(false)

    private val quoteRefresh = MutableStateFlow(0)

    private val reviewShown = MutableStateFlow(false)

    private val reviewClosed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // As on iOS: every new block re-quotes unless the review is on top, and coming back from it re-quotes at once.
    private val requotes = merge(
        chainStateRepository.bittensorNewBlocks().filter { !reviewShown.value },
        reviewClosed
    )
        .inBackground()
        .shareIn(this, SharingStarted.Eagerly)

    // A preset handed over by subnet details stays a preset: it is not the user's choice (analytics, re-preset on details).
    private val selectedValidatorOverride = MutableStateFlow(validatorAddress.takeUnless { validatorIsPreset })

    private val selectionRetry = MutableStateFlow(0)

    private val validatorRetry = MutableStateFlow(0)

    val subnetState = (
        if (quickPickOrder != null) {
            quickPickInteractor.observeSelection(quickPickOrder, selectionRetry)
                .onEach { if (it is ExtendedLoadingState.Error) logSelectionFailure(it.exception) }
        } else {
            retryController.observe(
                step = "subnet",
                analytics = subnetsReads,
                transform = { it.withSafeLoading() }
            ) {
                flowOf { bittensorSubnetInteractor.subnet(requireNotNull(initialSubnetId)) }
            }
        }
        ).shareInBackground()

    val subnet = subnetState.map { it.dataOrNull }
        .shareInBackground()

    /** "Buy more <subnet>" for an existing position; new positions keep the screen's default title. */
    val buyMoreTitle = subnetState.mapNotNull { state ->
        // Waits for the catalogue so the title does not flash "Subnet N" before the name arrives.
        if (state.isLoading()) return@mapNotNull null
        val subnet = state.dataOrNull
        val name = subnet?.title(resources)
            ?: resources.getString(R.string.bittensor_subnet_number, initialSubnetId?.value ?: 0)
        resources.getString(R.string.bittensor_buy_more_subnet, name)
    }
        .shareInBackground()

    private val validatorsState = subnetState.flatMapLatest { state ->
        val subnet = state.dataOrNull
        if (subnet == null) {
            flowOf<ExtendedLoadingState<SubnetValidators>> {
                if (state.isLoading()) ExtendedLoadingState.Loading else ExtendedLoadingState.Loaded(SubnetValidators(emptyList(), null))
            }
        } else if (quickPickOrder != null) {
            combine(validatorRetry, selectedValidatorOverride) { _, explicitAddress -> explicitAddress }.flatMapLatest { explicitAddress ->
                flowOf {
                    if (explicitAddress == null) {
                        // As on iOS, a subnet without a qualifying preset asks for a validator instead of failing the pick.
                        presetValidatorInteractor.preset(subnet.id).let { SubnetValidators(it.directory, it.preset?.address) }
                    } else {
                        val validators = bittensorSubnetStakingInteractor.validators(subnet.id)
                        require(validators.any { it.address == explicitAddress && it.canSelect }) { "Selected validator is unavailable" }
                        SubnetValidators(validators, null)
                    }
                }.withSafeLoading()
                    .onEach { if (it is ExtendedLoadingState.Error) logSelectionFailure(it.exception) }
            }
        } else {
            retryController.observe(
                step = "subnet validators",
                analytics = validatorsReads,
                transform = { it.withSafeLoading() }
            ) {
                flowOf {
                    when {
                        existingPosition -> {
                            val validator = bittensorSubnetStakingInteractor.existingPositionValidator(subnet.id, requireNotNull(validatorAddress))
                            SubnetValidators(listOf(validator), validator.address)
                        }
                        // An address chosen on subnet details or in the validator list is never replaced by the preset.
                        validatorAddress != null -> SubnetValidators(
                            bittensorSubnetStakingInteractor.validators(subnet.id),
                            presetAddress = validatorAddress.takeIf { validatorIsPreset }
                        )
                        else -> presetValidatorInteractor.preset(subnet.id).let { SubnetValidators(it.directory, it.preset?.address) }
                    }
                }
            }
        }
    }
        .shareInBackground()

    private val selectedValidatorAddress = combine(validatorsState, selectedValidatorOverride) { state, selectedAddress ->
        selectedAddress ?: state.dataOrNull?.presetAddress
    }
        .shareInBackground()

    private val selectedValidator = combine(validatorsState, selectedValidatorAddress) { state, address ->
        address?.let { selectedAddress ->
            val validator = state.dataOrNull?.validators?.firstOrNull {
                it.address == selectedAddress && (if (existingPosition) it.canAddToExistingPosition else it.canSelect)
            }?.validator
            QuoteValidator(selectedAddress, validator)
        }
    }.shareInBackground()

    val selectionLoading = subnetState.map { it.isLoading() }.shareInBackground()

    val selectionError = combine(subnetState, validatorsState) { subnet, validators ->
        if (quickPickOrder == null) {
            null
        } else {
            when {
                subnet.isError() -> resources.getString(R.string.bittensor_quick_selection_failed)
                validators.isError() -> resources.getString(R.string.bittensor_quick_validator_failed)
                else -> null
            }
        }
    }.shareInBackground()

    private val selectedSlippage = slippageSettings.observe()
        .shareInBackground()

    private val walletState = retryController.observe(
        step = "staking wallet",
        transform = { it.withSafeLoading() },
        source = bittensorSubnetInteractor::observeWallet
    )
        .shareInBackground()

    private val walletContext = walletState.mapNotNull { it.dataOrNull }
        .shareInBackground()

    private val feeContext = walletContext
        .map { it.asset.token.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorSubnetStakeDraft>>(
        scope = this,
        feeContext = feeContext
    )

    private val maxActionProvider = MaxActionProvider.create(this) {
        walletContext.map { it.asset }
            .providingMaxOf { maximumBittensorStake(it.transferableInPlanks, BigInteger.ZERO) }
            .deductFee(feeLoaderMixin)
    }

    private val amountChooser = amountChooserFactory.create(
        scope = this,
        assetFlow = walletContext.map { it.asset },
        maxActionProvider = maxActionProvider
    )

    val amount = amountChooser.inputState.map { it.value }
        .shareInBackground()

    val amountModel = amountChooser.assetModel

    val fiatAmount = combine(
        walletContext,
        amount
    ) { wallet, amount ->
        val fiat = amount.toBigDecimalOrNull()
            ?.let(wallet.asset.token::toFiatOrNull)
        fiat?.let { fiatFormatter.formatFiat(it, wallet.asset.token.currency) }
    }
        .shareInBackground()

    private val estimateQuoteRequest = combine(
        walletState,
        subnetState,
        amount,
        selectedSlippage,
        selectedValidator
    ) { walletState, subnetState, amount, slippage, validator ->
        val wallet = walletState.dataOrNull ?: return@combine null
        val subnet = subnetState.dataOrNull ?: return@combine null
        val selected = validator ?: return@combine null
        if (selected.validator == null) return@combine null
        val decimalAmount = amount.toBigDecimalOrNull()
            ?.takeIf { it.isPositive } ?: BigDecimal.ONE
        val planks = wallet.asset.token.configuration.planksFromAmount(decimalAmount)
        if (!planks.isPositive() || planks.bitLength() > 64) return@combine null
        QuoteRequest(wallet.metaId, subnet.id, planks, slippage, selected.address, selected.validator)
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val quoteRequest = combine(estimateQuoteRequest, walletContext, amount) { request, wallet, input ->
        val enteredAmount = input.toBigDecimalOrNull()?.takeIf { it.isPositive } ?: return@combine null
        val planks = wallet.asset.token.configuration.planksFromAmount(enteredAmount)
        request?.takeIf { it.amount == planks }
    }
        .shareInBackground()

    private val preparedQuoteState = combine(
        estimateQuoteRequest,
        quoteRefresh
    ) { request, _ -> request }
        .flatMapLatest { request ->
            if (request == null) {
                flowOf<QuoteResult?> { null }
                    .withSafeLoading()
            } else {
                retryController.observe(
                    step = "subnet quote",
                    analytics = chainRpcReads,
                    transform = { it.withSafeLoading() }
                ) {
                    flowOf { prepareQuote(request) }
                }
            }
        }
        .shareInBackground()

    private val quoteState = preparedQuoteState.flatMapLatest { state ->
        state.dataOrNull?.let(::requotedEachBlock) ?: flowOf { state }
    }
        .shareInBackground()

    private val enteredQuoteState = combine(
        quoteRequest,
        quoteState
    ) { request, state ->
        if (request == null) ExtendedLoadingState.Loaded(null) else state.map { it?.takeIf { it.request == request } }
    }
        .shareInBackground()

    private val currentQuote = enteredQuoteState.map { it.dataOrNull }
        .shareInBackground()

    private val draft = currentQuote.map { it?.draft }
        .shareInBackground()

    private val estimateDraft = combine(estimateQuoteRequest, quoteState) { request, state ->
        state.dataOrNull?.takeIf { it.request == request }?.draft
    }
        .shareInBackground()

    private val feeDraft = combine(estimateQuoteRequest, preparedQuoteState) { request, state ->
        state.dataOrNull?.takeIf { it.request == request }?.feeDraft
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val yieldState = subnet.flatMapLatest { subnet ->
        if (subnet == null) {
            flowOf<ExtendedLoadingState<Map<AccountIdKey, BigDecimal>?>> { ExtendedLoadingState.Loaded(null) }
        } else {
            estimatesMixin.observeAlphaYields(walletContext.map { it.chain }, subnet.id)
        }
    }
        .shareInBackground()

    private val costBasisState = estimatesMixin.observePositionCostBasis(
        walletContext.map { wallet ->
            if (existingPosition) BittensorEstimateContext(wallet.metaId, wallet.chain) else null
        }
    )
        .shareInBackground()

    val annualYield = combine(
        yieldState,
        walletContext,
        selectedValidatorAddress
    ) { state, walletContext, validatorAddress ->
        val key = validatorAddress?.let { runCatching { AccountIdKey(walletContext.chain.accountIdOf(it)) }.getOrNull() }
        key?.let { state.dataOrNull?.get(it) }
    }
        .shareInBackground()

    val validator = combine(validatorsState, selectedValidatorAddress) { state, address ->
        state.map { loaded ->
            if (address == null) return@map resources.getString(R.string.bittensor_select_validator)
            loaded.validators.firstOrNull { it.address == address }?.validator?.name?.takeIf { it.isNotBlank() } ?: address
        }
    }
        .shareInBackground()

    val apy = annualYield.map { it?.formatAsPercentage() }
        .shareInBackground()

    val monthlyEstimate = combine(
        enteredQuoteState,
        annualYield,
        subnet,
        yieldState
    ) { quoteState, annual, subnet, yields ->
        if (!quoteState.isLoading() && quoteState.dataOrNull == null) return@combine ExtendedLoadingState.Loaded("—")
        bittensorLoadingValue(yields, quoteState) {
            val quote = quoteState.dataOrNull?.quote
            if (quote == null || annual == null || subnet == null) {
                "—"
            } else {
                val alpha = bittensorMonthlyProjection(
                    BigDecimal(quote.alphaOutInPlanks, BittensorProtocolUnits.ALPHA_PRECISION),
                    annual
                )
                "≈ " + tokenFormatter.formatToken(
                    alpha,
                    TokenSymbol(subnet.symbol.ifBlank { "—" })
                )
            }
        }
    }
        .shareInBackground()

    val projectedAveragePrice = combine(
        costBasisState,
        enteredQuoteState,
        walletContext,
        subnet
    ) { costBasis, quoteState, wallet, subnet ->
        val subnetId = subnet?.id?.value ?: return@combine ExtendedLoadingState.Loading
        val state = costBasis.buyMoreCostBasis(subnetId)
        if (state.dataOrNull?.forSubnet(subnetId) == null || quoteState.isError()) {
            return@combine state.map { basis ->
                costBasisFormatter.projectedAveragePrice(basis, null, subnetId, wallet.asset.token, subnet.symbol)
            }
        }
        bittensorLoadingValue(state, quoteState) {
            costBasisFormatter.projectedAveragePrice(state.dataOrNull, quoteState.dataOrNull?.quote, subnetId, wallet.asset.token, subnet.symbol)
        }
    }
        .shareInBackground()

    val averagePriceDescription = subnet.map { subnet ->
        subnet?.let { resources.getString(R.string.bittensor_average_price_description, it.symbol, it.title(resources)) }
    }
        .shareInBackground()

    val fee = combine(
        feeDraft,
        quoteState,
        feeLoaderMixin.fee
    ) { draft, quote, status ->
        if (quote.isLoading()) FeeStatus.Loading(visibleDuringProgress = true) else status.forRequest(draft)
    }
        .shareInBackground()

    val needsFunding = combine(walletState, fee) { state, fee ->
        val wallet = state.dataOrNull
        val networkFee = wallet?.let { fee.loadedFeeOrNull()?.fee?.maxAmountDeductionFor(it.asset.token.configuration) }
        bittensorNeedsFunding(wallet?.asset?.transferableInPlanks, networkFee, existingPosition)
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val maximumAmount = maxActionProvider.maxAvailableBalance
        .map { BittensorMaxAmount(it.actualAmount, it.chainAsset.symbol) }
        .shareInBackground()

    val maxAmount = maximumAmount.map { tokenFormatter.formatToken(it.amount, it.symbol).toString() }
        .shareInBackground()

    val getAssetOptionsMixin = getAssetOptionsMixinFactory.create(
        assetFlow = walletState.map { it.dataOrNull?.asset?.token?.configuration },
        scope = this,
        additionalButtonFilter = needsFunding,
        showWhenNoOptions = true
    )

    private val subnetFacts = subnet.flatMapLatest { selectedSubnet ->
        if (selectedSubnet == null) {
            flowOf<BittensorSubnetFacts?> { null }
        } else {
            retryController.observe(
                step = "subnet facts",
                analytics = subnetsReads,
                transform = { source ->
                    source.onStart { emit(null) }.catch {
                        if (it is CancellationException) throw it
                        emit(null)
                    }
                }
            ) {
                flowOf<BittensorSubnetFacts?> { subnetFactsInteractor.facts(selectedSubnet) }
            }
        }
    }
        .shareInBackground()

    val subnetChips = combine(subnetFacts, needsFunding) { facts, funding ->
        if (funding || facts == null) {
            emptyList()
        } else {
            buildList {
                facts.monthsOld?.let { add(resources.getString(R.string.bittensor_chip_age_months, it)) }
                if (facts.deepPool) add(resources.getString(R.string.bittensor_chip_deep_pool))
                if (facts.steady) add(resources.getString(R.string.bittensor_chip_steady))
            }
        }
    }
        .distinctUntilChanged()
        .shareInBackground()

    val fundingMessage = walletContext.map { wallet ->
        val balance = tokenFormatter.formatToken(wallet.asset.transferable, wallet.asset.token.configuration.symbol)
        resources.getString(R.string.bittensor_get_asset_message, balance, wallet.asset.token.configuration.symbol.value)
    }
        .shareInBackground()

    val reserveWarning = combine(walletContext, amount, fee, needsFunding) { wallet, input, status, funding ->
        if (funding) null else bittensorAmountFormatter.feeReserveWarning(input, wallet.asset, status.loadedFeeOrNull())
    }
        .shareInBackground()

    val alphaAmount = combine(
        quoteState,
        subnet,
        currentQuote
    ) { state, subnet, result ->
        state.map {
            if (subnet == null || result == null) return@map "—"
            val amount = BigDecimal(result.quote.alphaOutInPlanks, BITTENSOR_ALPHA_DECIMALS)
            "≈ \u200e" + tokenFormatter.formatToken(
                amount,
                TokenSymbol(subnet.symbol.ifBlank { "—" })
            )
        }
    }
        .shareInBackground()

    val rate = combine(
        quoteState,
        subnet,
        estimateDraft,
        walletContext
    ) { state, subnet, result, wallet ->
        state.map {
            if (subnet == null || result == null) "—" else bittensorAmountFormatter.exchangeRate(result.operation.quote, subnet, wallet.asset.token)
        }
    }
        .shareInBackground()

    private val quoteButtonState = combine(
        quoteRequest,
        currentQuote,
        walletState,
        quoteState
    ) {
            request, quote, wallet, quoteState ->
        val label = resources.getString(R.string.common_continue)
        when {
            wallet.isLoading() -> DescriptiveButtonState.Loading
            request == null || wallet.isError() || quoteState.isError() -> DescriptiveButtonState.Disabled(label)
            wallet.dataOrNull?.let { request.amount > it.asset.transferableInPlanks } == true -> DescriptiveButtonState.Disabled(label)
            quote == null -> DescriptiveButtonState.Loading
            else -> DescriptiveButtonState.Enabled(label)
        }
    }
        .shareInBackground()

    private val quotedContinueButtonState = combine(
        quoteButtonState,
        fee,
        reserveWarning
    ) { quoteButtonState, fee, reserveWarning ->
        when {
            quoteButtonState !is DescriptiveButtonState.Enabled -> quoteButtonState
            fee is FeeStatus.Loading -> DescriptiveButtonState.Loading
            reserveWarning != null -> DescriptiveButtonState.Disabled(resources.getString(R.string.common_continue))
            fee.loadedFeeOrNull() == null -> DescriptiveButtonState.Disabled(resources.getString(R.string.common_continue))
            else -> quoteButtonState
        }
    }

    /** As on iOS, a new position left without a validator (no qualifying preset, or the preset failed) picks one from the button. */
    private val needsValidatorPick = combine(subnetState, validatorsState, selectedValidatorAddress) { subnet, validators, address ->
        !existingPosition && subnet.dataOrNull != null && !validators.isLoading() && address == null
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val hasAmount = combine(walletState, amount) { wallet, input ->
        val value = input.toBigDecimalOrNull()?.takeIf { it.isPositive } ?: return@combine false
        wallet.dataOrNull?.asset?.token?.configuration?.planksFromAmount(value)?.isPositive() ?: true
    }
        .distinctUntilChanged()

    val continueButtonState = combine(
        quotedContinueButtonState,
        validating,
        needsFunding,
        needsValidatorPick,
        hasAmount
    ) { quotedButtonState, validating, needsFunding, needsValidatorPick, hasAmount ->
        when {
            needsFunding -> DescriptiveButtonState.Disabled(resources.getString(R.string.common_continue))
            validating -> DescriptiveButtonState.Loading
            needsValidatorPick -> DescriptiveButtonState.Enabled(resources.getString(R.string.bittensor_select_validator_title))
            !hasAmount -> DescriptiveButtonState.Disabled(resources.getString(R.string.common_enter_amount))
            else -> quotedButtonState
        }
    }
        .shareInBackground()

    init {
        needsFunding.onEach { funding ->
            if (funding && !isNoBalanceTracked) {
                isNoBalanceTracked = true
                analytics.track(
                    BittensorAnalyticsEvent.Blocked(
                        StakingFlow.STAKE,
                        StakingBlockedReason.NO_BALANCE,
                        null,
                        StakingAnalyticsTarget.SUBNET
                    )
                )
            }
        }.launchIn(this)
        if (quickPickOrder != null) {
            subnet.onEach { selected ->
                if (selected != null && !isTypeSelectedTracked) {
                    val method = when (quickPickOrder) {
                        BittensorQuickPickOrder.POOL_SIZE -> StakingAnalyticsSelectionMethod.QUICK_PICK_POOL_SIZE
                        BittensorQuickPickOrder.AGE -> StakingAnalyticsSelectionMethod.QUICK_PICK_AGE
                        BittensorQuickPickOrder.APY -> StakingAnalyticsSelectionMethod.QUICK_PICK_APY
                    }
                    isTypeSelectedTracked = true
                    analytics.track(
                        BittensorAnalyticsEvent.TypeSelected(
                            BittensorAnalyticsEvent.NETWORK,
                            StakingAnalyticsTarget.SUBNET,
                            method,
                            selected.id.value
                        )
                    )
                }
            }.launchIn(this)
        }
        validatorCommunicator.responseFlow
            .onEach { response ->
                if (!selectionState.accepts(response)) return@onEach
                selectionState = selectionState.withResponse(response)
                if (!existingPosition && response.address != null && response.subnetId == subnet.first()?.id?.value) {
                    response.address?.let { selectedValidatorOverride.value = it }
                }
            }
            .launchIn(this)

        slippageSettingsCommunicator.responseFlow
            .onEach {
                slippageSettings.set(BigDecimal.valueOf(it.fraction))
                analytics.track(it.bittensorAnalyticsEvent())
            }
            .launchIn(this)

        feeLoaderMixin.connectWith(feeDraft) { _, draft ->
            draft?.let {
                val fee = bittensorSubnetStakingInteractor.estimateFee(it)
                BittensorFeeEstimate(it, fee)
            }
        }
    }

    fun amountChanged(value: String) {
        if (amountChooser.inputState.value.value == value) return
        amountChooser.setAmountInput(value, initiatedByUser = true)
    }

    fun onResume() {
        if (reviewShown.compareAndSet(expect = true, update = false)) reviewClosed.tryEmit(Unit)
    }

    fun retrySelection() = launchUnit {
        if (subnetState.first().isError()) {
            selectionRetry.value += 1
        } else if (validatorsState.first().isError()) {
            validatorRetry.value += 1
        }
    }

    private fun logSelectionFailure(cause: Throwable) {
        if (cause is CancellationException) throw cause
        logger.log("[BittensorSelection] Automatic setup unavailable")
        logger.log(cause)
    }

    fun slippageClicked() {
        if (!showsSlippageSettings) return
        slippageSettingsCommunicator.openRequest(slippageSettings.payload())
    }

    fun validatorClicked() = launchUnit {
        if (existingPosition || validating.value) return@launchUnit
        openValidatorPicker()
    }

    private suspend fun openValidatorPicker() {
        val subnetId = subnet.first()?.id ?: return
        val request = BittensorSelectValidatorCommunicator.Request(subnetId.value, selectedValidatorAddress.first(), selectionState)
        validatorCommunicator.openRequest(request)
    }

    fun maxClicked() = launchUnit {
        if (validating.value) return@launchUnit
        amountChooser.maxAction.maxClick.first()?.invoke()
    }

    fun subnetClicked() = launchUnit {
        if (existingPosition || validating.value) return@launchUnit
        val selectedSubnet = subnet.first() ?: return@launchUnit
        val payload = BittensorSubnetDetailsPayload(
            subnetId = selectedSubnet.id.value,
            analyticsSource = SubnetDetailsSource.SETUP,
            replaceAmount = true,
            // Only a user choice travels; details recomputes the same preset otherwise.
            validatorAddress = selectedValidatorOverride.value,
            selectionState = selectionState
        )
        router.openBittensorSubnetDetails(payload)
    }

    fun chooseSubnetClicked() {
        val browseState = selectionState.browseState ?: BittensorSubnetBrowseState.fromQuickPick(quickPickOrder)
        val payload = BittensorChooseSubnetPayload(replaceAmount = true, browseState = browseState, selectionState = selectionState)
        router.openBittensorChooseSubnet(payload)
    }

    fun continueClicked() = launchUnit {
        if (validating.value) return@launchUnit
        if (needsValidatorPick.first()) {
            openValidatorPicker()
            return@launchUnit
        }
        val request = quoteRequest.first() ?: return@launchUnit
        val quote = currentQuote.first() ?: return@launchUnit
        if (quote.request != request) return@launchUnit
        val fee = fee.first()
            .loadedFeeOrNull()
            ?.takeIf { it.request == quote.feeDraft } ?: return@launchUnit

        val token = walletContext.first().asset.token
        val selectedSubnet = subnet.first()
        validating.value = true
        try {
            val payload = bittensorSubnetStakingInteractor.validationPayload(quote.draft, fee.fee)
                .copy(slippage = BittensorSlippage(request.slippage))
            val alertFormat = tokenFormatter.bittensorStakeAlertFormat(token) { quote ->
                selectedSubnet?.let { bittensorAmountFormatter.exchangeRate(quote, it, token) } ?: "—"
            }
            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                progressConsumer = validating.progressConsumer(),
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
                                        StakingValidationStage.SETUP,
                                        outcome,
                                        StakingAnalyticsTarget.SUBNET
                                    )
                                )
                            }
                        },
                        // As on iOS, confirming the new rate continues to the review.
                        acceptRate = { actions.resumeFlow() }
                    ) { quoteRefresh.value += 1 }
                }
            ) { validated -> openReview(request, validated.inactiveValidatorAccepted) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            validating.value = false
        }
    }

    private fun openReview(request: QuoteRequest, inactiveValidatorAccepted: Boolean) = launchUnit {
        validating.value = false
        if (quoteRequest.first() != request) return@launchUnit
        reviewShown.value = true
        analytics.track(
            BittensorAnalyticsEvent.Initiated(
                BittensorAnalyticsEvent.NETWORK,
                StakingAnalyticsTarget.SUBNET,
                if (existingPosition) StakingMode.BUY_MORE else StakingMode.NEW_POSITION,
                bittensorStakeValidatorSource(
                    existingPosition,
                    request.validatorAddress,
                    validatorsState.replayCache.lastOrNull()?.dataOrNull?.presetAddress
                ),
                SlippageBucket.from(request.slippage.movePointRight(2)),
                null
            )
        )
        router.openBittensorSubnetReview(
            BittensorSubnetReviewPayload(
                request.subnetId.value,
                request.amount,
                request.metaId,
                request.validatorAddress,
                request.slippage,
                existingPosition = existingPosition,
                inactiveValidatorAccepted = inactiveValidatorAccepted
            )
        )
    }

    private suspend fun prepareQuote(request: QuoteRequest): QuoteResult {
        delay(QUOTE_DEBOUNCE_MILLIS)
        val quote = bittensorSubnetInteractor.quote(request.subnetId, request.amount, BittensorSlippage(request.slippage))
        val draft = if (quickPickOrder != null) {
            bittensorSubnetStakingInteractor.previewDraft(request.metaId, quote, requireNotNull(request.validator))
        } else {
            bittensorSubnetStakingInteractor.prepareDraft(request.metaId, quote, request.validatorAddress, existingPosition)
        }
        return QuoteResult(request, draft)
    }

    /** As on iOS, the shown quote stays until a block re-quote replaces it, and a failed re-quote clears it until the next block. */
    private fun requotedEachBlock(initial: QuoteResult): Flow<ExtendedLoadingState<QuoteResult>> = requotes
        .mapLatest<Unit, ExtendedLoadingState<QuoteResult>> {
            try {
                val request = initial.request
                val quote = bittensorSubnetInteractor.quote(request.subnetId, request.amount, BittensorSlippage(request.slippage))
                ExtendedLoadingState.Loaded(initial.requoted(quote))
            } catch (cause: Exception) {
                if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
                logger.log("[BittensorSubnetAmount] Block re-quote failed")
                logger.log(cause)
                chainRpcReads.failed(cause)
                ExtendedLoadingState.Error(cause)
            }
        }
        .onStart { emit(ExtendedLoadingState.Loaded(initial)) }

    private fun reportFailure(cause: Throwable) {
        logger.log("[BittensorSubnetAmount] Failed to calculate maximum stake")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }

    fun back() {
        validatorCommunicator.respond(BittensorSelectValidatorCommunicator.Response(null, null, selectionState))
        router.back()
    }

    private fun BittensorQuickPickOrder.sectionTitleRes(): Int = when (this) {
        BittensorQuickPickOrder.POOL_SIZE -> R.string.bittensor_quick_first_pool
        BittensorQuickPickOrder.AGE -> R.string.bittensor_quick_first_age
        BittensorQuickPickOrder.APY -> R.string.bittensor_quick_first_apy
    }

    /** [presetAddress] is the automatic choice; null when the user picked the validator or nothing qualifies. */
    private class SubnetValidators(val validators: List<BittensorSubnetValidator>, val presetAddress: String?)

    private data class QuoteValidator(val address: String, val validator: BittensorValidator?)

    private data class QuoteRequest(
        val metaId: Long,
        val subnetId: BittensorNetuid.Subnet,
        val amount: BigInteger,
        val slippage: BigDecimal,
        val validatorAddress: String,
        val validator: BittensorValidator?
    )

    /** [feeDraft] is the first draft of [request]: block re-quotes move only the limit price, which leaves the fee unchanged. */
    private data class QuoteResult(
        val request: QuoteRequest,
        val draft: BittensorSubnetStakeDraft,
        val feeDraft: BittensorSubnetStakeDraft = draft
    ) {

        val quote: BittensorSubnetBuyQuote get() = draft.operation.quote

        fun requoted(quote: BittensorSubnetBuyQuote) = copy(draft = draft.requoted(quote))
    }

    private companion object {

        const val QUOTE_DEBOUNCE_MILLIS = 350L
    }
}

internal fun bittensorStakeValidatorSource(existingPosition: Boolean, selectedAddress: String, presetAddress: String?): StakingValidatorSource = when {
    existingPosition -> StakingValidatorSource.EXISTING_HOTKEY
    selectedAddress != presetAddress -> StakingValidatorSource.MANUAL
    else -> StakingValidatorSource.RECOMMENDED
}

/** A history still settling an own trade may miss that purchase: without totals for [netuid] it is unknown, never "no purchases". */
internal fun BittensorPositionCostBasis.buyMoreCostBasis(netuid: Int): ExtendedLoadingState<BittensorEstimatedCostBasis?> =
    state.map { basis -> basis.takeUnless { isSettling && basis?.forSubnet(netuid) == null } }

/** Every block after the current one; a lost subscription is restored after a short pause. */
internal fun ChainStateRepository.bittensorNewBlocks(): Flow<Unit> = flowOfAll { currentRemoteBlockNumberFlow(ChainGeneses.BITTENSOR) }
    .drop(1)
    .map { Unit }
    .retryWhen { cause, _ ->
        if (cause is CancellationException) throw cause
        delay(BLOCKS_RESUBSCRIBE_DELAY_MILLIS)
        true
    }

internal fun BittensorSubnetStakeDraft.requoted(quote: BittensorSubnetBuyQuote) =
    copy(operation = BittensorSubnetStakeOperation(quote, operation.hotkey, operation.beneficiary))

private const val BLOCKS_RESUBSCRIBE_DELAY_MILLIS = 5_000L
