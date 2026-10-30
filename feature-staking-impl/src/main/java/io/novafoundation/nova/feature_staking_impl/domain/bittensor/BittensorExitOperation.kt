package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_impl.data.bittensor.removeBittensorRootStake
import io.novafoundation.nova.feature_staking_impl.data.bittensor.removeBittensorSubnetStake
import io.novafoundation.nova.feature_staking_impl.data.bittensor.removeFullBittensorStake
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.TransferMode
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import java.math.BigInteger

class BittensorExitOperation(
    val quote: BittensorExitQuote,
    val hotkey: AccountIdKey,
    private val beneficiary: AccountIdKey,
    private val rootFeeFromStake: BigInteger = BigInteger.ZERO
) {

    init {
        require(hotkey.value.size == 32 && beneficiary.value.size == 32)
        require(rootFeeFromStake.isNonNegative && rootFeeFromStake < quote.amount)
        require(rootFeeFromStake.isZero || quote.netuid.isRoot() && quote.plan?.isBatched != true && quote.exitHotkeys.size <= 1)
    }

    fun outcomeTarget(): BittensorOutcomeTarget {
        val hotkeys = quote.plan?.slices?.map { it.hotkey } ?: quote.exitHotkeys.ifEmpty { listOf(hotkey) }
        return BittensorOutcomeTarget(BittensorOperationKind.UNSTAKE, quote.netuid, hotkeys.toSet(), beneficiary)
    }

    fun build(builder: ExtrinsicBuilder) = with(builder) {
        when {
            quote.plan != null -> quote.plan.slices.forEach { slice ->
                when {
                    slice.fullExit -> removeFullBittensorStake(slice.hotkey.value, quote.netuid, quote.limitPrice)
                    quote.netuid.isRoot() -> removeBittensorRootStake(slice.hotkey.value, slice.amount - rootFeeFromStake)
                    else -> removeBittensorSubnetStake(
                        BittensorNetuid.Subnet(quote.netuid.value),
                        slice.hotkey.value,
                        slice.amount,
                        requireNotNull(quote.limitPrice)
                    )
                }
            }
            // Legacy restored payloads predate slices. Newly prepared operations always carry a plan;
            // retaining this decoder path avoids inventing per-hotkey amounts for an older full exit.
            quote.fullExit -> {
                val exitHotkeys = quote.exitHotkeys.ifEmpty { listOf(hotkey) }
                exitHotkeys.forEach { removeFullBittensorStake(it.value, quote.netuid, quote.limitPrice) }
            }
            quote.netuid.isRoot() -> removeBittensorRootStake(hotkey.value, quote.amount - rootFeeFromStake)
            else -> removeBittensorSubnetStake(
                BittensorNetuid.Subnet(quote.netuid.value),
                hotkey.value,
                quote.amount,
                requireNotNull(quote.limitPrice)
            )
        }
        if (quote.serviceFee.isPositive()) nativeTransfer(beneficiary.value, quote.serviceFee, TransferMode.KEEP_ALIVE)
        Unit
    }
}
