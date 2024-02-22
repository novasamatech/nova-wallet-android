package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.getList
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.data.network.runtime.binding.requireType
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHexOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.storage

@UseCaseBinding
fun bindNominations(scale: String, runtime: RuntimeSnapshot): Nominations {
    val type = runtime.metadata.staking().storage("Nominators").returnType()

    val dynamicInstance = type.fromHexOrNull(runtime, scale) ?: incompatible()

    return bindNominations(dynamicInstance)
}

fun bindNominations(dynamicInstance: Any): Nominations {
    requireType<Struct.Instance>(dynamicInstance)

    return Nominations(
        targets = dynamicInstance.getList("targets").map { it as AccountId },
        submittedInEra = bindEraIndex(dynamicInstance["submittedIn"]),
        suppressed = dynamicInstance.getTyped("suppressed")
    )
}

fun bindNominationsOrNull(dynamicInstance: Any?): Nominations? {
    return dynamicInstance?.let(::bindNominations)
}
