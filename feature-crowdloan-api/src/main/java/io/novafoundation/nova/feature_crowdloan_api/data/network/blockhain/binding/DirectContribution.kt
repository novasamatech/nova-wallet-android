package io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindString
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.feature_crowdloan_api.data.common.CrowdloanContribution
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import java.math.BigInteger

class DirectContribution(
    override val amount: BigInteger,
    val memo: String,
) : CrowdloanContribution

fun bindContribution(scale: String, runtime: RuntimeSnapshot): DirectContribution {
    val type = runtime.typeRegistry["(Balance, Vec<u8>)"] ?: incompatible()

    val dynamicInstance = type.fromHex(runtime, scale).cast<List<*>>()

    return DirectContribution(
        amount = bindNumber(dynamicInstance[0]),
        memo = bindString(dynamicInstance[1])
    )
}
