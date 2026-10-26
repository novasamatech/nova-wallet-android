package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetEmissionRow

/** Keep root, absent rows and duplicate IDs: they are evidence for the completeness calculation. */
internal fun bindBittensorEmissionRows(decoded: Any?): List<BittensorSubnetEmissionRow?> = bindList(decoded) { row ->
    row?.castToStruct()?.let { value ->
        BittensorSubnetEmissionRow(
            netuid = BittensorNetuid.from(bindBittensorU16(value["netuid"])),
            registrationBlock = value.get<Any?>("networkRegisteredAt")?.let(::bindBittensorU64),
            taoInEmission = value.get<Any?>("taoInEmission")?.let(::bindBittensorU64)
        )
    }
}
