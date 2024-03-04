package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.utils.system
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger

typealias BlockNumber = BigInteger

typealias BlockHash = String

fun bindBlockNumber(scale: String, runtime: RuntimeSnapshot): BlockNumber {
    val type = runtime.metadata.system().storage("Number").returnType()

    val dynamicInstance = type.fromHexOrIncompatible(scale, runtime)

    return bindNumber(dynamicInstance)
}

fun bindBlockNumber(dynamic: Any?) = bindNumber(dynamic)
