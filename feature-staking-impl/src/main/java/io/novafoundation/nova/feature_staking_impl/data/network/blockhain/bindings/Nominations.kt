package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.getList
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.data.network.runtime.binding.requireType
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull

@UseCaseBinding
fun bindNominations(scale: String, runtime: RuntimeSnapshot): Nominations {
    val type = runtime.typeRegistry["Nominations"] ?: incompatible()

    val dynamicInstance = type.fromHexOrNull(runtime, scale)
    requireType<Struct.Instance>(dynamicInstance)

    return Nominations(
        targets = dynamicInstance.getList("targets").map { it as AccountId },
        submittedInEra = bindEraIndex(dynamicInstance["submittedIn"]),
        suppressed = dynamicInstance.getTyped("suppressed")
    )
}
