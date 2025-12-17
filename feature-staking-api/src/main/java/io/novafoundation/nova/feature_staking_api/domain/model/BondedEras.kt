package io.novafoundation.nova.feature_staking_api.domain.model

@JvmInline
value class BondedEras(val value: List<BondedEra>) {

    companion object
}

class BondedEra(val era: EraIndex, val startSessionIndex: SessionIndex) {

    companion object
}

fun BondedEras.findStartSessionIndexOf(era: EraIndex): SessionIndex? {
    return value.find { it.era == era }?.startSessionIndex
}
