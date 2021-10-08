package jp.co.soramitsu.feature_wallet_impl.data.network.blockchain

import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

fun ExtrinsicBuilder.transfer(accountId: AccountId, amount: BigInteger, keepAlive: Boolean = false): ExtrinsicBuilder {
    val callName = if (keepAlive) "transfer_keep_alive" else "transfer"

    return call(
        moduleName = "Balances",
        callName = callName,
        arguments = mapOf(
            "dest" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId),
            "value" to amount
        )
    )
}
