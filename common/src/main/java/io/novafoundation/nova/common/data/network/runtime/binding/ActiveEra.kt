package io.novafoundation.nova.common.data.network.runtime.binding

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
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
fun bindActiveEraIndex(
    scale: String,
    runtime: RuntimeSnapshot
): BigInteger {
    val returnType = runtime.metadata.storageReturnType("Staking", "ActiveEra")
    val decoded = returnType.fromHex(runtime, scale) as? Struct.Instance ?: incompatible()

    return decoded.get<BigInteger>("index") ?: incompatible()
}
