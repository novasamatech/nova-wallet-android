package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

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
    val supportsFeeFromStake: Boolean
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
    val limitPrice: BigInteger?
) {

    init {
        require(amount.isPositive() && amount.bitLength() <= 64)
        require(serviceFee.isNonNegative && minimumTaoOut > serviceFee)
        require(estimatedTaoOut >= minimumTaoOut)
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
    val operation: BittensorExitOperation,
    val fee: Fee,
    val feeFromStake: Boolean
)
