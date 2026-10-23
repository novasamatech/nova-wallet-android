package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
import java.math.BigDecimal

data class BittensorExitPosition(
    val metaId: Long,
    val chain: Chain,
    val coldkey: AccountIdKey,
    val hotkey: AccountIdKey,
    val validatorAddress: String,
    val netuid: BittensorNetuid,
    val asset: Asset,
    val subnet: BittensorSubnet?,
    val preflight: BittensorExitPreflight,
    val supportsFeeFromStake: Boolean,
    val existentialDeposit: BigInteger
) {

    val isRoot: Boolean get() = netuid.isRoot()

    val amountPrecision: Int get() = if (isRoot) asset.token.configuration.precision.value else BittensorProtocolUnits.ALPHA_PRECISION
}

data class BittensorExitQuote(
    val netuid: BittensorNetuid,
    val amount: BigInteger,
    val fullExit: Boolean,
    val estimatedTaoOut: BigInteger,
    val minimumTaoOut: BigInteger,
    val serviceFee: BigInteger,
    val limitPrice: BigInteger?,
    val exitHotkeys: List<AccountIdKey> = emptyList(),
    val plan: BittensorExitPlan? = null,
    val spotPrice: BigInteger? = null,
    val simulatedInput: BigInteger? = null,
    val slippageFraction: BigDecimal = BigDecimal("0.005"),
    val groupHotkeys: Set<AccountIdKey> = emptySet(),
    val serviceFeeRate: BigDecimal? = null
) {

    val postTradePrice: BigInteger?
        get() {
            val input = simulatedInput?.takeIf { it.isPositive() } ?: return null
            val spot = spotPrice?.takeIf { it.isPositive() } ?: return null
            val average = estimatedTaoOut * BittensorProtocolUnits.RAO_PER_TAO / input
            return average * average / spot
        }

    /** iOS `isFillable(atLimit:)`: the pool price the sale leaves stays above [atLimit]. */
    fun isFillable(atLimit: BigInteger): Boolean = postTradePrice?.let { it > atLimit } ?: false

    /** iOS `tighterLimit(than:)`: a sale keeps the higher of the two limit prices. */
    fun tighterLimit(than: BigInteger): BigInteger = requireNotNull(limitPrice).max(than)

    /** The quote signed at [limit], as iOS signs a verified quote at its signing limit. */
    fun signedAt(limit: BigInteger): BittensorExitQuote = copy(
        limitPrice = limit,
        minimumTaoOut = requireNotNull(simulatedInput) * limit / BittensorProtocolUnits.RAO_PER_TAO
    )

    init {
        require(slippageFraction.isNonNegative && slippageFraction < BigDecimal.ONE)
        require(fullExit || exitHotkeys.isEmpty())
        require(plan == null || plan.amount == amount)
        require(exitHotkeys.distinct().size == exitHotkeys.size)
        require(exitHotkeys.all { it.value.size == 32 })
        require(groupHotkeys.all { it.value.size == 32 })
        require(amount.isPositive() && amount.bitLength() <= 64)
        require(serviceFee.isNonNegative && minimumTaoOut > serviceFee)
        if (netuid.isRoot()) {
            require(limitPrice == null && serviceFee == BigInteger.ZERO && estimatedTaoOut == amount)
        } else {
            require(limitPrice != null && limitPrice.isPositive() && limitPrice.bitLength() <= 64)
        }
    }
}

data class BittensorExitDraft(
    val position: BittensorExitPosition,
    val quote: BittensorExitQuote,
    private val createOperation: () -> BittensorExitOperation,
    val fee: Fee,
    val feeFromStake: Boolean
) {

    // Presentation and validation need the reviewed terms even when the fee exceeds the
    // withdrawal. Construct the immutable call only after validation permits submission.
    val operation: BittensorExitOperation by lazy(createOperation)

    val networkFee: BigInteger get() = fee.maxAmountDeductionFor(position.asset.token.configuration)

    // Simulation output already includes the swap's alpha fee; deduct service and network fee once.
    val netReceive: BigInteger get() = (quote.estimatedTaoOut - quote.serviceFee - networkFee).max(BigInteger.ZERO)
}

data class BittensorExitRequest(val position: BittensorExitPosition, val quote: BittensorExitQuote)
