package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SubstrateApplicationConfig(
    val chainId: String,
    val coin: Int,
    val cla: UByte
) {
    companion object {

        private val ALL by lazy {
            listOf(
                SubstrateApplicationConfig(chainId = Chain.Geneses.POLKADOT, coin = 354, cla = 0x90u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.KUSAMA, coin = 434, cla = 0x99u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.STATEMINT, coin = 354, cla = 0x96u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.STATEMINE, coin = 434, cla = 0x97u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.EDGEWARE, coin = 523, cla = 0x94u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.KARURA, coin = 686, cla =  0x9au)
            )
        }

        fun all() = ALL
    }
}

fun SubstrateApplicationConfig.Companion.supports(chainId: String): Boolean {
    return all().any { it.chainId == chainId }
}
