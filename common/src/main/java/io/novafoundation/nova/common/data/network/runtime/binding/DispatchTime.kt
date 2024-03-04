package io.novafoundation.nova.common.data.network.runtime.binding

import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import java.math.BigInteger

sealed class DispatchTime {

    class At(val block: BlockNumber) : DispatchTime()

    class After(val block: BlockNumber) : DispatchTime()
}

fun bindDispatchTime(decoded: DictEnum.Entry<*>): DispatchTime {
    return when (decoded.name) {
        "At" -> DispatchTime.At(block = bindBlockNumber(decoded.value))
        "After" -> DispatchTime.After(block = bindBlockNumber(decoded.value))
        else -> incompatible()
    }
}

val DispatchTime.minimumRequiredBlock
    get() = when (this) {
        is DispatchTime.After -> block + BigInteger.ONE
        is DispatchTime.At -> block
    }
