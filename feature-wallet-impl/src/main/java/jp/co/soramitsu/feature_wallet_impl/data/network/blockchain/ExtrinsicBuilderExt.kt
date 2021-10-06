package jp.co.soramitsu.feature_wallet_impl.data.network.blockchain

import jp.co.soramitsu.common.data.network.runtime.binding.MultiAddress
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

fun ExtrinsicBuilder.transfer(multiAddress: MultiAddress, amount: BigInteger): ExtrinsicBuilder {
    return call(
        moduleName = "Balances",
        callName = "transfer",
        arguments = mapOf(
            "dest" to multiAddress,
            "value" to amount
        )
    )
}
