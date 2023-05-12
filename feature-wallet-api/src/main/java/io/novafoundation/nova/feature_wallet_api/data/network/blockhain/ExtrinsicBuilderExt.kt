package io.novafoundation.nova.feature_wallet_api.data.network.blockhain

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.common.utils.firstExistingCallName
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

fun ExtrinsicBuilder.nativeTransfer(accountId: AccountId, amount: BigInteger, keepAlive: Boolean = false): ExtrinsicBuilder {
    val callName = if (keepAlive) "transfer_keep_alive" else runtime.metadata.balances().firstExistingCallName("transfer_allow_death", "transfer")

    return call(
        moduleName = Modules.BALANCES,
        callName = callName,
        arguments = mapOf(
            "dest" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId),
            "value" to amount
        )
    )
}
