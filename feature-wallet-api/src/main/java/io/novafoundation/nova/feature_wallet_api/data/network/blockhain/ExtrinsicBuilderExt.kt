package io.novafoundation.nova.feature_wallet_api.data.network.blockhain

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.common.utils.firstExistingCallName
import io.novafoundation.nova.common.utils.hasCall
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

enum class TransferMode {
    KEEP_ALIVE, ALLOW_DEATH, ALL
}

fun ExtrinsicBuilder.nativeTransfer(accountId: AccountId, amount: BigInteger, mode: TransferMode = TransferMode.ALLOW_DEATH): ExtrinsicBuilder {
    when (mode) {
        TransferMode.KEEP_ALIVE -> transferKeepAlive(accountId, amount)
        TransferMode.ALLOW_DEATH -> transferAllowDeath(accountId, amount)
        TransferMode.ALL -> transferAll(accountId, amount)
    }

    return this
}

private fun ExtrinsicBuilder.transferKeepAlive(accountId: AccountId, amount: BigInteger) {
    call(
        moduleName = Modules.BALANCES,
        callName = "transfer_keep_alive",
        arguments = mapOf(
            "dest" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId),
            "value" to amount
        )
    )
}

private fun ExtrinsicBuilder.transferAllowDeath(accountId: AccountId, amount: BigInteger) {
    val callName = runtime.metadata.balances().firstExistingCallName("transfer_allow_death", "transfer")

    call(
        moduleName = Modules.BALANCES,
        callName = callName,
        arguments = mapOf(
            "dest" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId),
            "value" to amount
        )
    )
}

private fun ExtrinsicBuilder.transferAll(accountId: AccountId, amount: BigInteger) {
    val transferAllPresent = runtime.metadata.balances().hasCall("transfer_all")

    if (transferAllPresent) {
        call(
            moduleName = Modules.BALANCES,
            callName = "transfer_all",
            arguments = mapOf(
                "dest" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId),
                "keep_alive" to false
            )
        )
    } else {
        transferAllowDeath(accountId, amount)
    }
}
