package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountInputModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat

class BittensorSubnetAmountViewModel(
    private val initialSubnetId: BittensorSubnetId,
    private val validatorAddress: String,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val icons: AssetIconProvider,
    private val tokenFormatter: TokenFormatter,
    private val logger: Logger,
    private val bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor
) : BaseViewModel() {

    private val input = MutableStateFlow("")

    val amount = input.asStateFlow()

    private val failed = MutableStateFlow(false)

    private val calculatingMax = MutableStateFlow(false)

    val hasError = failed.asStateFlow()

    private val portfolio = bittensorSubnetInteractor.observePortfolio().catch { reportFailure(it) }.shareInBackground()

    val subnet = flowOf<BittensorSubnet?> { bittensorSubnetInteractor.subnet(initialSubnetId) }
        .onStart { emit(null) }
        .catch { reportFailure(it) }
        .shareInBackground()

    val amountModel = portfolio.map {
        val token = it.asset.token.configuration
        ChooseAmountModel(ChooseAmountInputModel(tokenSymbol = token.symbol.value, tokenIcon = icons.getAssetIconOrFallback(token)))
    }.shareInBackground()

    val fiatAmount = combine(portfolio, amount) { portfolio, amount ->
        amount.toBigDecimalOrNull()?.let(portfolio.asset.token::toFiatOrNull)?.let { value ->
            NumberFormat.getCurrencyInstance().apply {
                currency = java.util.Currency.getInstance(portfolio.asset.token.currency.code)
            }.format(value)
        }
    }.shareInBackground()

    val maxAmount = portfolio.map {
        tokenFormatter.formatToken(it.asset.transferable, it.asset.token.configuration.symbol).toString()
    }
        .shareInBackground()

    private val request = combine(portfolio, amount, subnet) { portfolio, amount, subnet ->
        val planks = amount.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
            ?.let(portfolio.asset.token.configuration::planksFromAmount)
            ?.takeIf { it.signum() > 0 && it.bitLength() <= 64 && it <= portfolio.asset.transferableInPlanks }
        QuoteRequest(portfolio.metaId, subnet?.id, planks)
    }.distinctUntilChanged().shareInBackground()

    private val quote = request.debounce(350L).transformLatest { request ->
        emit(null)
        val amount = request.amount ?: return@transformLatest
        val subnetId = request.subnetId ?: return@transformLatest
        failed.value = false
        try {
            emit(QuoteResult(request, bittensorSubnetInteractor.quote(subnetId, amount)))
        } catch (e: TimeoutCancellationException) {
            reportFailure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportFailure(e)
        }
    }.shareInBackground()

    val alphaAmount = combine(subnet.filterNotNull(), quote) { subnet, quote ->
        quote?.takeIf { it.request.subnetId == subnet.id }?.let {
            "≈ \u200e" + tokenFormatter.formatToken(BigDecimal(it.quote.alphaOutInPlanks, 9), TokenSymbol(subnet.symbol.ifBlank { "—" }))
        } ?: "—"
    }.shareInBackground()

    val rate = combine(subnet.filterNotNull(), quote, portfolio) { subnet, quote, portfolio ->
        quote?.takeIf { it.request.subnetId == subnet.id }?.let {
            val output = BigDecimal(it.quote.alphaOutInPlanks, 9)
            val input = portfolio.asset.token.amountFromPlanks(it.quote.totalTaoInPlanks)
            tokenFormatter.formatToken(BigDecimal.ONE, portfolio.asset.token.configuration.symbol).toString() + " ≈ \u200e" + tokenFormatter.formatToken(
                output.divide(input, 9, java.math.RoundingMode.DOWN),
                TokenSymbol(subnet.symbol.ifBlank { "—" })
            )
        } ?: "—"
    }.shareInBackground()

    val continueButtonState = combine(request, quote, subnet, hasError, calculatingMax) { request, quote, subnet, error, calculatingMax ->
        val label = resources.getString(R.string.common_continue)
        when {
            calculatingMax -> DescriptiveButtonState.Loading
            error || subnet == null || request.amount == null -> DescriptiveButtonState.Disabled(label)
            quote?.request != request -> DescriptiveButtonState.Loading
            else -> DescriptiveButtonState.Enabled(label)
        }
    }.shareInBackground()

    fun amountChanged(value: String) {
        input.value = value
    }

    fun maxClicked() = launchUnit {
        if (calculatingMax.value) return@launchUnit
        calculatingMax.value = true
        try {
            val portfolio = portfolio.first()
            val maximum = bittensorSubnetStakingInteractor.maximumStake(portfolio.metaId, initialSubnetId, validatorAddress)
            input.value = portfolio.asset.token.amountFromPlanks(maximum).toStripTrailingZerosString()
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
        val request = request.first()
        val quoted = quote.first() ?: return@launchUnit
        if (quoted.request != request || subnet.first() == null || failed.value) return@launchUnit
        router.openBittensorSubnetReview(
            BittensorSubnetReviewPayload(requireNotNull(request.subnetId).value, requireNotNull(request.amount), request.metaId, validatorAddress)
        )
    }

    private fun reportFailure(cause: Throwable) {
        logger.log("[BittensorSubnetAmount] Failed to prepare amount")
        logger.log(cause)
        if (!failed.value) showError(resources.getString(R.string.common_retry_message))
        failed.value = true
    }

    fun back() = router.back()

    private data class QuoteRequest(val metaId: Long, val subnetId: BittensorSubnetId?, val amount: BigInteger?)
    private data class QuoteResult(val request: QuoteRequest, val quote: BittensorSubnetBuyQuote)
}
