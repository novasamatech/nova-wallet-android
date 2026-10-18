package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.removeBittensorRootStake
import io.novafoundation.nova.feature_staking_impl.data.bittensor.removeBittensorSubnetStake
import io.novafoundation.nova.feature_staking_impl.data.bittensor.removeFullBittensorStake
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.TransferMode
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder

class BittensorExitOperation(
    val quote: BittensorExitQuote,
    val hotkey: AccountIdKey,
    private val beneficiary: AccountIdKey
) {

    init {
        require(hotkey.value.size == 32 && beneficiary.value.size == 32)
    }

    fun build(builder: ExtrinsicBuilder) = with(builder) {
        when {
            quote.fullExit -> removeFullBittensorStake(hotkey.value, quote.netuid, quote.limitPrice)
            quote.netuid.isRoot() -> removeBittensorRootStake(hotkey.value, quote.amount)
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
