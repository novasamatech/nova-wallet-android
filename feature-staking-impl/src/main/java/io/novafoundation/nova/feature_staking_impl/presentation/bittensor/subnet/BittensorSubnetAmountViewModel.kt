package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakeDraft
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorStakeValidationFailureUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmountInput
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRepository
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount

class BittensorSubnetAmountViewModel(
    private val initialSubnetId: BittensorNetuid.Subnet,
    private val validatorAddress: String,
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
    private val estimates: BittensorEstimatesRepository
) : BaseViewModel(), Validatable by validationExecutor {

    val retryEvents = retryController.events

    private val calculatingMax = MutableStateFlow(false)

    private val validating = MutableStateFlow(false)

    private val quoteRefresh = MutableStateFlow(0)

    private val walletState = retryController.observe(
        step = "staking wallet",
        transform = { it.withSafeLoading() },
        source = bittensorSubnetInteractor::observeWallet
    )
        .shareInBackground()

    private val walletContext = walletState.mapNotNull { it.dataOrNull }
        .shareInBackground()

    private val amountChooser = amountChooserFactory.create(
        scope = this,
        assetFlow = walletContext.map { it.asset },
        maxActionProvider = null
    )

    val amount = amountChooser.inputState.map { it.value }
        .shareInBackground()

    private val subnetState = retryController.observe(
        step = "subnet",
        transform = { it.withSafeLoading() }
    ) {
        flowOf { bittensorSubnetInteractor.subnet(initialSubnetId) }
    }
        .shareInBackground()

    val subnet = subnetState.map { it.dataOrNull }
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

    val maxAmount = walletContext.map { wallet ->
        tokenFormatter.formatToken(wallet.asset.transferable, wallet.asset.token.configuration.symbol)
            .toString()
    }
        .shareInBackground()

    private val quoteRequest = combine(
        walletState,
        subnetState,
        amount
    ) { walletState, subnetState, amount ->
        val wallet = walletState.dataOrNull ?: return@combine null
        val subnet = subnetState.dataOrNull ?: return@combine null
        val decimalAmount = amount.toBigDecimalOrNull()
            ?.takeIf { it.isPositive } ?: return@combine null
        val planks = wallet.asset.token.configuration.planksFromAmount(decimalAmount)
        if (!planks.isPositive() || planks.bitLength() > 64 || planks > wallet.asset.transferableInPlanks) return@combine null
        QuoteRequest(wallet.metaId, subnet.id, planks)
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val quoteState = combine(
        quoteRequest,
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

    private val currentQuote = combine(
        quoteRequest,
        quoteState
    ) { request, state ->
        state.dataOrNull?.takeIf { it.request == request }
    }
        .shareInBackground()

    private val draft = currentQuote.map { it?.draft }
        .shareInBackground()

    private val yieldState = estimates.observeAlphaYields(walletContext.map { it.chain }, initialSubnetId).shareInBackground()

    private val costBasisState = estimates.observeCostBasis(walletContext.map { BittensorEstimateContext(it.metaId, it.chain) }).shareInBackground()

    val annualYield = combine(yieldState, walletContext) { state, walletContext ->
        val key = runCatching { AccountIdKey(walletContext.chain.accountIdOf(validatorAddress)) }.getOrNull()
        key?.let { state?.get(it) }
    }.shareInBackground()

    val validator = draft.map { it?.validator?.name ?: "—" }.shareInBackground()

    val apy = annualYield.map { it?.let { "≈ ${it.stripTrailingZeros().toPlainString()}%" } ?: "—" }.shareInBackground()

    val monthlyEstimate = combine(draft.map { it?.operation?.quote }, annualYield, subnet) { quote, annual, subnet ->
        if (quote == null || annual == null || subnet == null) {
            "—"
        } else {
            val alpha = io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorMonthlyProjection(
                BigDecimal(quote.alphaOutInPlanks, 9),
                annual
            )
            "≈ " + tokenFormatter.formatToken(alpha, TokenSymbol(subnet.symbol.ifBlank { "—" }))
        }
    }.shareInBackground()

    val projectedAveragePrice = combine(costBasisState, draft.map { it?.operation?.quote }, walletContext) { state, quote, walletContext ->
        state.map { basis ->
            val current = basis?.forSubnet(initialSubnetId.value)
            if (current == null || quote == null) {
                "—"
            } else {
                val projected = current.adding(quote.totalTaoInPlanks, quote.alphaOutInPlanks)
                "≈ " + tokenFormatter.formatToken(projected.averagePrice, walletContext.asset.token.configuration.symbol)
            }
        }
    }.shareInBackground()

    private val feeContext = walletContext
        .map { it.asset.token.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorSubnetStakeDraft>>(
        scope = this,
        feeContext = feeContext
    )

    val fee = combine(
        draft,
        feeLoaderMixin.fee
    ) { draft, status ->
        status.forRequest(draft)
    }
        .shareInBackground()

    val slippageDescription = resources.getString(
        R.string.bittensor_slippage_fixed,
        bittensorSubnetInteractor.slippage.fraction.movePointRight(2)
            .toStripTrailingZerosString()
    )

    val feeCaption = combine(
        currentQuote,
        flowOf { bittensorSubnetInteractor.commissionRate() }
    ) { quote, configuredRate ->
        val rate = quote?.quote?.serviceFeeRate ?: configuredRate
        val percentage = rate.movePointRight(2)
            .toStripTrailingZerosString()
        resources.getString(R.string.bittensor_subnet_fee_caption, percentage)
    }
        .shareInBackground()

    val alphaAmount = combine(
        subnet,
        currentQuote
    ) { subnet, result ->
        if (subnet == null || result == null) return@combine "—"
        val amount = BigDecimal(result.quote.alphaOutInPlanks, BITTENSOR_ALPHA_DECIMALS)
        "≈ \u200e" + tokenFormatter.formatToken(amount, TokenSymbol(subnet.symbol.ifBlank { "—" }))
    }
        .shareInBackground()

    val rate = combine(
        subnet,
        currentQuote,
        walletContext
    ) { subnet, result, wallet ->
        if (subnet == null || result == null) "—" else bittensorAmountFormatter.exchangeRate(result.quote, subnet, wallet.asset.token)
    }
        .shareInBackground()

    private val quoteButtonState = combine(
        quoteRequest,
        currentQuote,
        walletState,
        quoteState,
        calculatingMax
    ) {
            request, quote, wallet, quoteState, calculatingMax ->
        val label = resources.getString(R.string.common_continue)
        when {
            calculatingMax || wallet.isLoading() -> DescriptiveButtonState.Loading
            request == null || wallet.isError() || quoteState.isError() -> DescriptiveButtonState.Disabled(label)
            quote == null -> DescriptiveButtonState.Loading
            else -> DescriptiveButtonState.Enabled(label)
        }
    }
        .shareInBackground()

    val continueButtonState = combine(
        quoteButtonState,
        fee,
        validating
    ) { quoteButtonState, fee, validating ->
        when {
            validating -> DescriptiveButtonState.Loading
            quoteButtonState !is DescriptiveButtonState.Enabled -> quoteButtonState
            fee is FeeStatus.Loading -> DescriptiveButtonState.Loading
            fee.loadedFeeOrNull() == null -> DescriptiveButtonState.Disabled(resources.getString(R.string.common_continue))
            else -> quoteButtonState
        }
    }
        .shareInBackground()

    init {
        feeLoaderMixin.connectWith(draft) { _, draft ->
            draft?.let {
                val fee = bittensorSubnetStakingInteractor.estimateFee(it)
                BittensorFeeEstimate(it, fee)
            }
        }
    }

    fun amountChanged(value: String) {
        amountChooser.setAmountInput(value, initiatedByUser = true)
    }

    fun maxClicked() = launchUnit {
        if (calculatingMax.value || validating.value) return@launchUnit
        calculatingMax.value = true
        try {
            val wallet = walletContext.first()
            val maximum = bittensorSubnetStakingInteractor.maximumStake(wallet.metaId, initialSubnetId, validatorAddress)
            val maximumAmount = wallet.asset.token.amountFromPlanks(maximum)
                .toStripTrailingZerosString()
            amountChooser.setAmountInput(maximumAmount)
        } catch (e: TimeoutCancellationException) {
            reportFailure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportFailure(e)
        } finally {
            calculatingMax.value = false
        }
    }

    fun chooseSubnetClicked() = router.openBittensorChooseSubnet()

    fun continueClicked() = launchUnit {
        if (calculatingMax.value || validating.value) return@launchUnit
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
            ) { openReview(request) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            validating.value = false
        }
    }

    private fun openReview(request: QuoteRequest) = launchUnit {
        validating.value = false
        if (quoteRequest.first() != request) return@launchUnit
        router.openBittensorSubnetReview(
            BittensorSubnetReviewPayload(request.subnetId.value, request.amount, request.metaId, validatorAddress)
        )
    }

    private suspend fun prepareQuote(request: QuoteRequest): QuoteResult {
        delay(QUOTE_DEBOUNCE_MILLIS)
        val quote = bittensorSubnetInteractor.quote(request.subnetId, request.amount)
        val draft = bittensorSubnetStakingInteractor.prepareDraft(request.metaId, quote, validatorAddress)
        return QuoteResult(request, draft)
    }

    private fun reportFailure(cause: Throwable) {
        logger.log("[BittensorSubnetAmount] Failed to calculate maximum stake")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }

    fun back() = router.back()

    private data class QuoteRequest(val metaId: Long, val subnetId: BittensorNetuid.Subnet, val amount: BigInteger)

    private data class QuoteResult(val request: QuoteRequest, val draft: BittensorSubnetStakeDraft) {

        val quote: BittensorSubnetBuyQuote get() = draft.operation.quote
    }

    private companion object {

        const val QUOTE_DEBOUNCE_MILLIS = 350L
    }
}
