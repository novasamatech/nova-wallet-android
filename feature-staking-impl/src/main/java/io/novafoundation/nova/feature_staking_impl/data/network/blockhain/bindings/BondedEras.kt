package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.feature_staking_api.domain.model.BondedEra
import io.novafoundation.nova.feature_staking_api.domain.model.BondedEras

fun BondedEras.Companion.bind(decoded: Any?): BondedEras {
    val value = bindList(decoded, BondedEra.Companion::bind)
    return BondedEras(value)
}

private fun BondedEra.Companion.bind(decoded: Any?): BondedEra {
    val (eraIndex, sessionIndex) = decoded.castToList()

    return BondedEra(
        bindEraIndex(dynamicInstance = eraIndex),
        bindSessionIndex(sessionIndex)
    )
}
