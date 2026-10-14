package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.isNonNegative

internal fun bindBittensorU16(decoded: Any?): Int = bindBittensorUnsignedInt(decoded, 16)

internal fun bindBittensorU8(decoded: Any?): Int = bindBittensorUnsignedInt(decoded, 8)

private fun bindBittensorUnsignedInt(decoded: Any?, bits: Int): Int {
    val value = bindNumber(decoded)
    require(value.isNonNegative && value.bitLength() <= bits) { "Invalid Bittensor u$bits value" }
    return value.toInt()
}
