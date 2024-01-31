package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.utils.Percent
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromByteArrayOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Constant
import java.math.BigInteger

@HelperBinding
fun bindNumberConstant(
    constant: Constant,
    runtime: RuntimeSnapshot
): BigInteger = bindNullableNumberConstant(constant, runtime) ?: incompatible()

@HelperBinding
fun bindNullableNumberConstant(
    constant: Constant,
    runtime: RuntimeSnapshot
): BigInteger? {
    val decoded = constant.type?.fromByteArrayOrNull(runtime, constant.value) ?: incompatible()

    return decoded as BigInteger?
}

fun Constant.decodePercentOrThrow(runtime: RuntimeSnapshot): Percent {
    return decodeAsOrThrow(runtime) { Percent(bindNumber(it).toDouble()) }
}

fun <T> Constant.decodeAsOrThrow(runtime: RuntimeSnapshot, binding: (Any?) -> T): T {
    return binding(decodeOrThrow(runtime))
}

fun Constant.decodeOrThrow(runtime: RuntimeSnapshot): Any? {
    return requireNotNull(type).fromByteArrayOrIncompatible(value, runtime)
}
