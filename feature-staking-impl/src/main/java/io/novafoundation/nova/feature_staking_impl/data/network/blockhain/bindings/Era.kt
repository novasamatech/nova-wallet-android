package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.storageReturnType
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import java.math.BigInteger

/*
"ActiveEraInfo": {
  "type": "struct",
  "type_mapping": [
    [
      "index",
      "EraIndex"
    ],
    [
      "start",
      "Option<Moment>"
    ]
  ]
}
 */
@UseCaseBinding
fun bindActiveEra(
    scale: String,
    runtime: RuntimeSnapshot
): BigInteger {
    val returnType = runtime.metadata.storageReturnType("Staking", "ActiveEra")
    val decoded = returnType.fromHexOrNull(runtime, scale)

    return bindActiveEra(decoded)
}

fun bindActiveEra(decoded: Any?): BigInteger {
    return bindEraIndex(decoded.castToStruct().getTyped("index"))
}

/*
EraIndex
 */
@UseCaseBinding
fun bindCurrentEra(
    scale: String,
    runtime: RuntimeSnapshot
): BigInteger {
    val returnType = runtime.metadata.storageReturnType("Staking", "CurrentEra")

    return bindEraIndex(returnType.fromHexOrNull(runtime, scale))
}

@UseCaseBinding
fun bindCurrentIndex(
    scale: String,
    runtime: RuntimeSnapshot
): BigInteger {
    val returnType = runtime.metadata.storageReturnType("Session", "CurrentIndex")

    return bindSessionIndex(returnType.fromHexOrNull(runtime, scale))
}

@UseCaseBinding
fun bindCurrentSlot(
    scale: String,
    runtime: RuntimeSnapshot
): BigInteger {
    val returnType = runtime.metadata.storageReturnType("Babe", "CurrentSlot")

    return bindSlot(returnType.fromHexOrNull(runtime, scale))
}

@UseCaseBinding
fun bindErasStartSessionIndex(
    scale: String,
    runtime: RuntimeSnapshot
): BigInteger {
    val returnType = runtime.metadata.storageReturnType("Staking", "ErasStartSessionIndex")
    val decoded = returnType.fromHexOrNull(runtime, scale)

    return bindSessionIndex(decoded)
}

@HelperBinding
fun bindEraIndex(dynamicInstance: Any?): EraIndex = bindNumber(dynamicInstance)

@HelperBinding
fun bindSessionIndex(dynamicInstance: Any?): BigInteger = bindNumber(dynamicInstance)

@HelperBinding
fun bindSlot(dynamicInstance: Any?): BigInteger = bindNumber(dynamicInstance)
