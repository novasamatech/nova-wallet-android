package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.utils.system
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

class AccountData(
    val free: BigInteger,
    val reserved: BigInteger,
    val frozen: BigInteger,
)

class AccountInfo(
    val data: AccountData,
) {

    companion object {
        fun empty() = AccountInfo(
            data = AccountData(
                free = BigInteger.ZERO,
                reserved = BigInteger.ZERO,
                frozen = BigInteger.ZERO
            )
        )
    }
}

@HelperBinding
fun bindAccountData(dynamicInstance: Struct.Instance): AccountData {
    val frozen = if (hasSplitFrozen(dynamicInstance)) {
        val miscFrozen = bindNumber(dynamicInstance["miscFrozen"])
        val feeFrozen = bindNumber(dynamicInstance["feeFrozen"])

        miscFrozen.max(feeFrozen)
    } else {
        bindNumber(dynamicInstance["frozen"])
    }

    return AccountData(
        free = bindNumber(dynamicInstance["free"]),
        reserved = bindNumber(dynamicInstance["reserved"]),
        frozen = frozen
    )
}

private fun hasSplitFrozen(accountInfo: Struct.Instance): Boolean {
    return "miscFrozen" in accountInfo.mapping
}

@HelperBinding
fun bindNonce(dynamicInstance: Any?): BigInteger {
    return bindNumber(dynamicInstance)
}

@UseCaseBinding
fun bindAccountInfo(scale: String, runtime: RuntimeSnapshot): AccountInfo {
    val type = runtime.metadata.system().storage("Account").returnType()

    val dynamicInstance = type.fromHexOrNull(runtime, scale).cast<Struct.Instance>()

    return AccountInfo(
        data = bindAccountData(dynamicInstance.getTyped("data"))
    )
}
