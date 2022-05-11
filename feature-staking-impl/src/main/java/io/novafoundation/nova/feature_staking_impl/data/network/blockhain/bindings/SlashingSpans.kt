package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getList
import io.novafoundation.nova.common.data.network.runtime.binding.storageReturnType
import io.novafoundation.nova.feature_staking_api.domain.model.SlashingSpans
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.Type
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull

fun bindSlashingSpans(
    decoded: Any?,
): SlashingSpans {
    val asStruct = decoded.castToStruct()

    return SlashingSpans(
        lastNonZeroSlash = bindEraIndex(asStruct["lastNonzeroSlash"]),
        prior = asStruct.getList("prior").map(::bindEraIndex)
    )
}

@UseCaseBinding
fun bindSlashingSpans(
    scale: String,
    runtime: RuntimeSnapshot,
    returnType: Type<*> = runtime.metadata.storageReturnType("Staking", "SlashingSpans")
): SlashingSpans {
    val decoded = returnType.fromHexOrNull(runtime, scale)

    return bindSlashingSpans(decoded)
}
