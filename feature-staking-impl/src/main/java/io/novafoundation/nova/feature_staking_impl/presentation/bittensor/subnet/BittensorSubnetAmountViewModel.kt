package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

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
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
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
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.initialBittensorValidatorAddress
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.maximumBittensorStake
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorMaxAmount
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLoadingValue
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorNeedsFunding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorStakeValidationFailureUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorSaleCostBasisFormatter
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
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
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetFactsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetFacts
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

class BittensorSubnetAmountViewModel(
    private val initialSubnetId: BittensorNetuid.Subnet?,
    private val quickPickOrder: BittensorQuickPickOrder?,
    private val quickPickInteractor: BittensorQuickPickInteractor,
    private val presetValidatorInteractor: BittensorPresetValidatorInteractor,
    validatorAddress: String?,
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
    subnetFactsInteractor: BittensorSubnetFactsInteractor
) : BaseViewModel(), Validatable by validationExecutor {

    val retryEvents = retryController.events

    val isQuickPick = quickPickOrder != null

    private val validating = MutableStateFlow(false)

    private val quoteRefresh = MutableStateFlow(0)

    private val selectedValidatorOverride = MutableStateFlow(validatorAddress)

    private val selectionRetry = MutableStateFlow(0)

    private val validatorRetry = MutableStateFlow(0)

    val subnetState = (
        if (quickPickOrder != null) {
            quickPickInteractor.observeSelection(quickPickOrder, selectionRetry)
                .onEach { if (it is ExtendedLoadingState.Error) logSelectionFailure(it.exception) }
        } else {
            retryController.observe(
                step = "subnet",
                transform = { it.withSafeLoading() }
            ) {
                flowOf { bittensorSubnetInteractor.subnet(requireNotNull(initialSubnetId)) }
            }
        }
        ).shareInBackground()

    val subnet = subnetState.map { it.dataOrNull }
        .shareInBackground()

    private val validatorsState = subnetState.flatMapLatest { state ->
        val subnet = state.dataOrNull
        if (subnet == null) {
            flowOf<ExtendedLoadingState<List<BittensorSubnetValidator>>> {
                if (state.isLoading()) ExtendedLoadingState.Loading else ExtendedLoadingState.Loaded(emptyList())
            }
        } else if (quickPickOrder != null) {
            combine(validatorRetry, selectedValidatorOverride) { _, explicitAddress -> explicitAddress }.flatMapLatest { explicitAddress ->
                flowOf {
                    if (explicitAddress == null) {
                        presetValidatorInteractor.validators(subnet.id)
                    } else {
                        val validators = bittensorSubnetStakingInteractor.validators(subnet.id)
                        require(validators.any { it.address == explicitAddress && it.canSelect }) { "Selected validator is unavailable" }
                        validators
                    }
                }.withSafeLoading()
                    .onEach { if (it is ExtendedLoadingState.Error) logSelectionFailure(it.exception) }
            }
        } else {
            retryController.observe(
                step = "subnet validators",
                transform = { it.withSafeLoading() }
            ) {
                flowOf {
                    if (existingPosition) {
                        listOf(bittensorSubnetStakingInteractor.existingPositionValidator(subnet.id, requireNotNull(validatorAddress)))
                    } else {
                        bittensorSubnetStakingInteractor.validators(subnet.id)
                    }
                }
            }
        }
    }
        .shareInBackground()

    private val selectedValidatorAddress = combine(validatorsState, selectedValidatorOverride) { state, selectedAddress ->
        initialBittensorValidatorAddress(selectedAddress, state.dataOrNull.orEmpty())
    }
        .shareInBackground()

    private val selectedValidator = combine(validatorsState, selectedValidatorAddress) { state, address ->
        address?.let { selectedAddress ->
            val validator = state.dataOrNull?.firstOrNull {
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

    private val quoteState = combine(
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
                    transform = { it.withSafeLoading() }
                ) {
                    flowOf { prepareQuote(request) }
                }
            }
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

    private val feeDraft = combine(estimateQuoteRequest, quoteState) { request, state ->
        state.dataOrNull?.takeIf { it.request == request }?.draft
    }
        .shareInBackground()

    private val yieldState = subnet.flatMapLatest { subnet ->
        if (subnet == null) {
            flowOf<ExtendedLoadingState<Map<AccountIdKey, BigDecimal>?>> { ExtendedLoadingState.Loaded(null) }
        } else {
            estimatesMixin.observeAlphaYields(walletContext.map { it.chain }, subnet.id)
        }
    }
        .shareInBackground()

    private val costBasisState = estimatesMixin.observeCostBasis(
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
        state.map { validators -> validators.firstOrNull { it.address == address }?.validator?.name?.takeIf { it.isNotBlank() } ?: address.orEmpty() }
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
    ) { state, quoteState, wallet, subnet ->
        val subnetId = subnet?.id?.value ?: return@combine ExtendedLoadingState.Loading
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
        subnet?.let { resources.getString(R.string.bittensor_average_price_description, it.symbol, it.name) }
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
        feeDraft,
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

    val continueButtonState = combine(
        quoteButtonState,
        fee,
        validating,
        needsFunding,
        reserveWarning
    ) { quoteButtonState, fee, validating, needsFunding, reserveWarning ->
        when {
            needsFunding -> DescriptiveButtonState.Disabled(resources.getString(R.string.common_continue))
            validating -> DescriptiveButtonState.Loading
            quoteButtonState !is DescriptiveButtonState.Enabled -> quoteButtonState
            fee is FeeStatus.Loading -> DescriptiveButtonState.Loading
            reserveWarning != null -> DescriptiveButtonState.Disabled(resources.getString(R.string.common_continue))
            fee.loadedFeeOrNull() == null -> DescriptiveButtonState.Disabled(resources.getString(R.string.common_continue))
            else -> quoteButtonState
        }
    }
        .shareInBackground()

    init {
        validatorCommunicator.responseFlow
            .onEach { response ->
                if (!existingPosition && response.subnetId == subnet.first()?.id?.value) {
                    selectedValidatorOverride.value = response.address
                }
            }
            .launchIn(this)

        slippageSettingsCommunicator.responseFlow
            .onEach { slippageSettings.set(BigDecimal.valueOf(it.fraction)) }
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
        slippageSettingsCommunicator.openRequest(slippageSettings.payload())
    }

    fun validatorClicked() = launchUnit {
        if (existingPosition || validating.value) return@launchUnit
        val subnetId = subnet.first()?.id ?: return@launchUnit
        val request = BittensorSelectValidatorCommunicator.Request(subnetId.value, selectedValidatorAddress.first())
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
            replaceAmount = true,
            validatorAddress = selectedValidatorAddress.first()
        )
        router.openBittensorSubnetDetails(payload)
    }

    fun chooseSubnetClicked() = router.openBittensorChooseSubnet(BittensorChooseSubnetPayload(replaceAmount = true))

    fun continueClicked() = launchUnit {
        if (validating.value) return@launchUnit
        val request = quoteRequest.first() ?: return@launchUnit
        val quote = currentQuote.first() ?: return@launchUnit
        if (quote.request != request) return@launchUnit
        val fee = fee.first()
            .loadedFeeOrNull()
            ?.takeIf { it.request == quote.draft } ?: return@launchUnit

        validating.value = true
        try {
            val payload = bittensorSubnetStakingInteractor.validationPayload(quote.draft, fee.fee)
            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                progressConsumer = validating.progressConsumer(),
                validationFailureTransformerCustom = { failure, actions ->
                    bittensorStakeValidationFailureUi(
                        failure,
                        actions,
                        resources
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

    private fun reportFailure(cause: Throwable) {
        logger.log("[BittensorSubnetAmount] Failed to calculate maximum stake")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }

    fun back() = router.back()

    private data class QuoteValidator(val address: String, val validator: BittensorValidator?)

    private data class QuoteRequest(
        val metaId: Long,
        val subnetId: BittensorNetuid.Subnet,
        val amount: BigInteger,
        val slippage: BigDecimal,
        val validatorAddress: String,
        val validator: BittensorValidator?
    )

    private data class QuoteResult(
        val request: QuoteRequest,
        val draft: BittensorSubnetStakeDraft
    ) {

        val quote: BittensorSubnetBuyQuote get() = draft.operation.quote
    }

    private companion object {

        const val QUOTE_DEBOUNCE_MILLIS = 350L
    }
}
