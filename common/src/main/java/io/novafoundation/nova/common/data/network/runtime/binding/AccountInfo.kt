package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.domain.balance.EDCountingMode
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.system
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHexOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger

open class AccountBalance(
    val free: BigInteger,
    val reserved: BigInteger,
    val frozen: BigInteger
) {

    companion object {

        fun empty(): AccountBalance {
            return AccountBalance(
                free = BigInteger.ZERO,
                reserved = BigInteger.ZERO,
                frozen = BigInteger.ZERO,
            )
        }
    }
}

fun AccountBalance?.orEmpty(): AccountBalance = this ?: AccountBalance.empty()

class AccountData(
    free: BigInteger,
    reserved: BigInteger,
    frozen: BigInteger,
    val flags: AccountDataFlags,
) : AccountBalance(free, reserved, frozen)

@JvmInline
value class AccountDataFlags(val value: BigInteger) {

    companion object {

        fun default() = AccountDataFlags(BigInteger.ZERO)

        private val HOLD_AND_FREEZES_ENABLED_MASK: BigInteger = BigInteger("80000000000000000000000000000000", 16)
    }

    fun holdsAndFreezesEnabled(): Boolean {
        return flagEnabled(HOLD_AND_FREEZES_ENABLED_MASK)
    }

    @Suppress("SameParameterValue")
    private fun flagEnabled(flag: BigInteger) = value and flag == flag
}

fun AccountDataFlags.transferableMode(): TransferableMode {
    return if (holdsAndFreezesEnabled()) {
        TransferableMode.HOLDS_AND_FREEZES
    } else {
        TransferableMode.REGULAR
    }
}

fun AccountDataFlags.edCountingMode(): EDCountingMode {
    return if (holdsAndFreezesEnabled()) {
        EDCountingMode.FREE
    } else {
        EDCountingMode.TOTAL
    }
}

class AccountInfo(
    val consumers: BigInteger,
    val providers: BigInteger,
    val sufficients: BigInteger,
    val data: AccountData
) {

    companion object {
        fun empty() = AccountInfo(
            consumers = BigInteger.ZERO,
            providers = BigInteger.ZERO,
            sufficients = BigInteger.ZERO,
            data = AccountData(
                free = BigInteger.ZERO,
                reserved = BigInteger.ZERO,
                frozen = BigInteger.ZERO,
                flags = AccountDataFlags.default(),
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
        frozen = frozen,
        flags = bindAccountDataFlags(dynamicInstance["flags"])
    )
}

private fun hasSplitFrozen(accountInfo: Struct.Instance): Boolean {
    return "miscFrozen" in accountInfo.mapping
}

private fun bindAccountDataFlags(instance: Any?): AccountDataFlags {
    return if (instance != null) {
        AccountDataFlags(bindNumber(instance))
    } else {
        AccountDataFlags.default()
    }
}

@HelperBinding
fun bindNonce(dynamicInstance: Any?): BigInteger {
    return bindNumber(dynamicInstance)
}

@UseCaseBinding
fun bindAccountInfo(scale: String, runtime: RuntimeSnapshot): AccountInfo {
    val type = runtime.metadata.system().storage("Account").returnType()

    val dynamicInstance = type.fromHexOrNull(runtime, scale)

    return bindAccountInfo(dynamicInstance)
}

fun bindAccountInfo(decoded: Any?): AccountInfo {
    val dynamicInstance = decoded.cast<Struct.Instance>()

    return AccountInfo(
        consumers = dynamicInstance.getTyped<BigInteger?>("consumers").orZero(),
        providers = dynamicInstance.getTyped<BigInteger?>("providers").orZero(),
        sufficients = dynamicInstance.getTyped<BigInteger?>("sufficients").orZero(),
        data = bindAccountData(dynamicInstance.getTyped("data"))
    )
}

fun bindOrmlAccountBalanceOrEmpty(decoded: Any?): AccountBalance {
    return decoded?.let { bindOrmlAccountData(decoded) } ?: AccountBalance.empty()
}

fun bindOrmlAccountData(decoded: Any?): AccountBalance {
    val dynamicInstance = decoded.cast<Struct.Instance>()

    return AccountBalance(
        free = bindNumber(dynamicInstance["free"]),
        reserved = bindNumber(dynamicInstance["reserved"]),
        frozen = bindNumber(dynamicInstance["frozen"]),
    )
}
