package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.withSafeLoading
import kotlinx.coroutines.flow.flatMapLatest
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmountInput
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class BittensorSubnetAmountViewModel(
    private val initialSubnetId: BittensorSubnetId,
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
    private val amountChooserFactory: AmountChooserMixin.Factory
) : BaseViewModel() {

    val retryEvents = retryController.events

    private val calculatingMax = MutableStateFlow(false)

    private val walletState = retryController.observe("staking wallet", bittensorSubnetInteractor::observeWallet).shareInBackground()

    private val walletContext = walletState.mapNotNull { it.dataOrNull }.shareInBackground()

    private val amountChooser = amountChooserFactory.create(scope = this, assetFlow = walletContext.map { it.asset }, maxActionProvider = null)

    val amount = amountChooser.inputState.map { it.value }.shareInBackground()

    private val subnetState = retryController.observe("subnet") {
        flowOf { bittensorSubnetInteractor.subnet(initialSubnetId) }
    }.shareInBackground()

    val subnet = subnetState.map { it.dataOrNull }.shareInBackground()

    val amountModel = amountChooser.assetModel

    val fiatAmount = combine(walletContext, amount) { wallet, amount ->
        val fiat = amount.toBigDecimalOrNull()?.let(wallet.asset.token::toFiatOrNull)
        fiat?.let { fiatFormatter.formatFiat(it, wallet.asset.token.currency) }
    }.shareInBackground()

    val maxAmount = walletContext.map { wallet ->
        tokenFormatter.formatToken(wallet.asset.transferable, wallet.asset.token.configuration.symbol).toString()
    }.shareInBackground()

    private val quoteRequest = combine(walletState, subnetState, amount) { walletState, subnetState, amount ->
        val wallet = walletState.dataOrNull ?: return@combine null
        val subnet = subnetState.dataOrNull ?: return@combine null
        val decimalAmount = amount.toBigDecimalOrNull()?.takeIf { it.isPositive } ?: return@combine null
        val planks = wallet.asset.token.configuration.planksFromAmount(decimalAmount)
        if (!planks.isPositive() || planks.bitLength() > 64 || planks > wallet.asset.transferableInPlanks) return@combine null
        QuoteRequest(wallet.metaId, subnet.id, planks)
    }.distinctUntilChanged().shareInBackground()

    private val quoteState = quoteRequest.flatMapLatest { request ->
        if (request == null) {
            flowOf<QuoteResult?> { null }.withSafeLoading()
        } else {
            retryController.observe("subnet quote") {
                flowOf {
                    delay(QUOTE_DEBOUNCE_MILLIS)
                    QuoteResult(request, bittensorSubnetInteractor.quote(request.subnetId, request.amount))
                }
            }
        }
    }.shareInBackground()

    private val currentQuote = combine(quoteRequest, quoteState) { request, state ->
        state.dataOrNull?.takeIf { it.request == request }
    }.shareInBackground()

    val slippageDescription = resources.getString(
        R.string.bittensor_slippage_fixed,
        bittensorSubnetInteractor.slippage.fraction.movePointRight(2).toStripTrailingZerosString()
    )

    val feeCaption = combine(currentQuote, flowOf { bittensorSubnetInteractor.commissionRate() }) { quote, configuredRate ->
        val rate = quote?.quote?.serviceFeeRate ?: configuredRate
        resources.getString(R.string.bittensor_subnet_fee_caption, rate.movePointRight(2).toStripTrailingZerosString())
    }.shareInBackground()

    val alphaAmount = combine(subnet, currentQuote) { subnet, result ->
        if (subnet == null || result == null) return@combine "—"
        val amount = BigDecimal(result.quote.alphaOutInPlanks, BITTENSOR_ALPHA_DECIMALS)
        "≈ \u200e" + tokenFormatter.formatToken(amount, TokenSymbol(subnet.symbol.ifBlank { "—" }))
    }.shareInBackground()

    val rate = combine(subnet, currentQuote, walletContext) { subnet, result, wallet ->
        if (subnet == null || result == null) "—" else bittensorAmountFormatter.exchangeRate(result.quote, subnet, wallet.asset.token)
    }.shareInBackground()

    val continueButtonState = combine(quoteRequest, currentQuote, walletState, quoteState, calculatingMax) {
            request, quote, wallet, quoteState, calculatingMax ->
        val label = resources.getString(R.string.common_continue)
        when {
            calculatingMax || wallet.isLoading() -> DescriptiveButtonState.Loading
            request == null || wallet.isError() || quoteState.isError() -> DescriptiveButtonState.Disabled(label)
            quote == null -> DescriptiveButtonState.Loading
            else -> DescriptiveButtonState.Enabled(label)
        }
    }.shareInBackground()

    fun amountChanged(value: String) {
        amountChooser.setAmountInput(value, initiatedByUser = true)
    }

    fun maxClicked() = launchUnit {
        if (calculatingMax.value) return@launchUnit
        calculatingMax.value = true
        try {
            val wallet = walletContext.first()
            val maximum = bittensorSubnetStakingInteractor.maximumStake(wallet.metaId, initialSubnetId, validatorAddress)
            amountChooser.setAmountInput(wallet.asset.token.amountFromPlanks(maximum).toStripTrailingZerosString())
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
        if (calculatingMax.value) return@launchUnit
        val request = quoteRequest.first() ?: return@launchUnit
        val quote = currentQuote.first() ?: return@launchUnit
        if (quote.request != request) return@launchUnit
        router.openBittensorSubnetReview(
            BittensorSubnetReviewPayload(request.subnetId.value, request.amount, request.metaId, validatorAddress)
        )
    }

    private fun reportFailure(cause: Throwable) {
        logger.log("[BittensorSubnetAmount] Failed to calculate maximum stake")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }

    fun back() = router.back()

    private data class QuoteRequest(val metaId: Long, val subnetId: BittensorSubnetId, val amount: BigInteger)

    private data class QuoteResult(val request: QuoteRequest, val quote: BittensorSubnetBuyQuote)

    private companion object {

        const val QUOTE_DEBOUNCE_MILLIS = 350L
    }
}
