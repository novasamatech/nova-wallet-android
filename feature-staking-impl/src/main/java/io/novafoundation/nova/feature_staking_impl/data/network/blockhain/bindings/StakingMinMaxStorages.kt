package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.fromHexOrIncompatible
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.Type
import java.math.BigInteger

fun bindMinBond(scale: String, runtimeSnapshot: RuntimeSnapshot, type: Type<*>): BigInteger {
    return bindNumber(scale, runtimeSnapshot, type)
}

fun bindMaxNominators(scale: String, runtimeSnapshot: RuntimeSnapshot, type: Type<*>): BigInteger {
    return bindNumber(scale, runtimeSnapshot, type)
}

fun bindNominatorsCount(scale: String, runtimeSnapshot: RuntimeSnapshot, type: Type<*>): BigInteger {
    return bindNumber(scale, runtimeSnapshot, type)
}

private fun bindNumber(scale: String, runtimeSnapshot: RuntimeSnapshot, type: Type<*>): BigInteger {
    return bindNumber(type.fromHexOrIncompatible(scale, runtimeSnapshot))
}
