package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import kotlinx.coroutines.flow.first
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountInputModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import kotlinx.coroutines.CancellationException
import io.novafoundation.nova.common.utils.TokenSymbol

class BittensorStakeAmountViewModel(
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val interactor: BittensorStakingInteractor,
    private val assetIconProvider: AssetIconProvider,
    private val iconGenerator: AddressIconGenerator,
    private val validatorCommunicator: BittensorSelectValidatorCommunicator
) : BaseViewModel() {

    private val loadingFailed = MutableStateFlow(false)
    private val stakingDetails = interactor.observeRootStakingDetails()
        .catch {
            loadingFailed.value = true
            showError(it)
        }
        .shareInBackground()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()
    private val preparing = MutableStateFlow(false)

    private val amountInPlanks = combine(stakingDetails, amount) { details, amount ->
        amount.toBigDecimalOrNull()?.let(details.asset.token.configuration::planksFromAmount)
    }.distinctUntilChanged().shareInBackground()

    private val feeResult = amountInPlanks
        .debounce(FEE_DEBOUNCE_MILLIS)
        .mapLatest { amount ->
            amount?.takeIf { it.isPositive() }?.let {
                val result = try {
                    Result.success(interactor.estimateRootStakeFee(it))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    showError(e)
                    Result.failure(e)
                }
                FeeEstimate(it, result)
            }
        }
        .shareInBackground()

    val amountModel = stakingDetails
        .map(::mapAmountModel)
        .shareInBackground()

    val fiatAmount = combine(stakingDetails, amount) { details, amount ->
        amount.toBigDecimalOrNull()
            ?.let(details.asset.token::toFiatOrNull)
            ?.let { formatFiat(it, details) }
    }.shareInBackground()

    val currentStake = stakingDetails.map { details ->
        if (details.dataStatus !is io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus.Fresh) {
            "—"
        } else {
            formatToken(details.asset.token.amountFromPlanks(details.position?.stakeInPlanks ?: BigInteger.ZERO), details.asset.token.configuration.symbol)
        }
    }.shareInBackground()

    val maxAmount = combine(stakingDetails, feeResult) { details, fee ->
        val available = maxAvailableInPlanks(details, fee?.result?.getOrNull())
        formatToken(details.asset.token.amountFromPlanks(available), details.asset.token.configuration.symbol)
    }.shareInBackground()

    val validator = stakingDetails
        .map {
            BittensorValidatorUi(
                it.validator.name,
                Icon.FromDrawable(
                    iconGenerator.createAddressIcon(
                        it.validator.hotkey.value,
                        AddressIconGenerator.SIZE_MEDIUM,
                        AddressIconGenerator.BACKGROUND_TRANSPARENT
                    )
                )
            )
        }
        .shareInBackground()

    val apy = stakingDetails
        .map(::formatApy)
        .shareInBackground()

    val monthlyEstimate = combine(stakingDetails, amount) { details, text ->
        val annual = details.annualPercentageYield ?: return@combine "—"
        val principal = text.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return@combine "—"
        val estimate = io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorMonthlyProjection(principal, annual)
        "≈ " + formatToken(estimate, details.asset.token.configuration.symbol)
    }.shareInBackground()

    val fee = combine(stakingDetails, feeResult) { details, result ->
        result?.result?.getOrNull()?.let { mapFee(it, details) } ?: BittensorFeeUi("—", "—")
    }.shareInBackground()

    private val loadedContinueButtonState = combine(stakingDetails, amountInPlanks, feeResult, preparing) { details, amount, fee, preparing ->
        if (preparing) DescriptiveButtonState.Loading else continueButtonState(details, amount, fee)
    }

    val continueButtonState = merge(
        loadedContinueButtonState,
        loadingFailed.filter { it }.map { DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_continue)) }
    )
        .onStart { emit(DescriptiveButtonState.Loading) }
        .shareInBackground()

    init {
        validatorCommunicator.responseFlow.onEach { response ->
            if (response.subnetId == 0) {
                if (preparing.value) return@onEach
                preparing.value = true
                try {
                    kotlinx.coroutines.withTimeout(60_000) {
                        interactor.selectRootValidator(response.address)
                        stakingDetails.first { it.chain.addressOf(it.validator.hotkey.value) == response.address }
                    }
                } catch (e: CancellationException) {
                    if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                    showError(resourceManager.getString(R.string.common_retry_message))
                } catch (e: Exception) {
                    showError(e)
                } finally {
                    preparing.value = false
                }
            }
        }.launchIn(viewModelScope)
        stakingDetails
            .onEach(::setInitialAmount)
            .launchIn(viewModelScope)
    }

    fun back() {
        interactor.clearDraft()
        router.back()
    }

    fun amountChanged(amount: String) {
        if (this.amount.value != amount) {
            _amount.value = amount
        }
    }

    fun maxClicked() {
        val details = stakingDetails.replayCache.firstOrNull() ?: return
        val fee = feeResult.replayCache.firstOrNull()?.result?.getOrNull()
        val max = details.asset.token.amountFromPlanks(maxAvailableInPlanks(details, fee))

        _amount.value = max.toStripTrailingZerosString()
    }

    fun validatorClicked() = launchUnit {
        if (preparing.value) return@launchUnit
        val details = stakingDetails.first()
        validatorCommunicator.openRequest(BittensorSelectValidatorCommunicator.Request(0, details.chain.addressOf(details.validator.hotkey.value)))
    }

    fun continueClicked() = launchUnit {
        if (preparing.value || loadingFailed.value) return@launchUnit
        preparing.value = true
        try {
            val amount = requireNotNull(amountInPlanks.replayCache.firstOrNull())
            interactor.prepareRootStake(amount)
            router.openBittensorStakeConfirm()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            preparing.value = false
        }
    }

    private fun mapAmountModel(details: BittensorRootStakingDetails): ChooseAmountModel {
        val token = details.asset.token.configuration

        return ChooseAmountModel(
            input = ChooseAmountInputModel(
                tokenSymbol = token.symbol.value,
                tokenIcon = assetIconProvider.getAssetIconOrFallback(token)
            )
        )
    }

    private fun formatApy(details: BittensorRootStakingDetails): String {
        val apy = details.annualPercentageYield?.toStripTrailingZerosString() ?: return "—"

        return resourceManager.getString(R.string.bittensor_apy_paid_in, "$apy%", details.asset.token.configuration.symbol)
    }

    private fun mapFee(fee: Fee, details: BittensorRootStakingDetails): BittensorFeeUi {
        return BittensorFeeUi(
            amount = formatToken(fee.decimalAmount, fee.asset.symbol),
            fiatAmount = details.asset.token.toFiatOrNull(fee.decimalAmount)?.let { formatFiat(it, details) } ?: "—"
        )
    }

    private fun setInitialAmount(details: BittensorRootStakingDetails) {
        if (_amount.value.isNotEmpty()) return

        val suggested = minOf(SUGGESTED_AMOUNT, details.asset.transferable)
        _amount.value = suggested.toStripTrailingZerosString()
    }

    private fun continueButtonState(
        details: BittensorRootStakingDetails,
        amount: BigInteger?,
        fee: FeeEstimate?
    ): DescriptiveButtonState {
        val label = resourceManager.getString(R.string.common_continue)
        if (amount == null || !amount.isPositive()) {
            return DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
        }

        if (fee?.amount != amount) return DescriptiveButtonState.Loading
        val loadedFee = fee.result.getOrNull() ?: return DescriptiveButtonState.Disabled(label)
        val enoughBalance = amount <= maxAvailableInPlanks(details, loadedFee)

        return if (enoughBalance) DescriptiveButtonState.Enabled(label) else DescriptiveButtonState.Disabled(label)
    }

    private fun maxAvailableInPlanks(details: BittensorRootStakingDetails, fee: Fee?): BigInteger {
        val feeInPlanks = fee?.maxAmountDeductionFor(details.asset.token.configuration) ?: BigInteger.ZERO
        return (details.asset.transferableInPlanks - feeInPlanks).coerceAtLeast(BigInteger.ZERO)
    }

    private fun formatToken(amount: BigDecimal, symbol: TokenSymbol): String =
        "${amount.toStripTrailingZerosString()} ${symbol.value}"

    private fun formatFiat(amount: BigDecimal, details: BittensorRootStakingDetails): String {
        return NumberFormat.getCurrencyInstance().apply {
            currency = java.util.Currency.getInstance(details.asset.token.currency.code)
        }.format(amount)
    }

    private fun BigInteger.isPositive(): Boolean = signum() > 0

    private data class FeeEstimate(val amount: BigInteger, val result: Result<Fee>)

    private companion object {
        const val FEE_DEBOUNCE_MILLIS = 350L
        val SUGGESTED_AMOUNT: BigDecimal = BigDecimal("5")
    }
}
