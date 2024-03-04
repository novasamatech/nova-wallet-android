package io.novafoundation.nova.common.data.network.runtime.binding

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromByteArrayOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.module.Constant
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
