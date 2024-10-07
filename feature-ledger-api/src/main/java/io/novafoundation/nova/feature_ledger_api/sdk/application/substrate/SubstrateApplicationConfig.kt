package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novafoundation.nova.feature_ledger_api.BuildConfig
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SubstrateApplicationConfig(
    val chainId: String,
    val coin: Int,
    val cla: UByte
) {

    companion object {

        private val ALL by lazy {
            listOfNotNull(
                SubstrateApplicationConfig(chainId = Chain.Geneses.POLKADOT, coin = 354, cla = 0x90u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.KUSAMA, coin = 434, cla = 0x99u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.STATEMINT, coin = 354, cla = 0x96u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.STATEMINE, coin = 434, cla = 0x97u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.EDGEWARE, coin = 523, cla = 0x94u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.KARURA, coin = 686, cla = 0x9au),
                SubstrateApplicationConfig(chainId = Chain.Geneses.ACALA, coin = 787, cla = 0x9bu),
                SubstrateApplicationConfig(chainId = Chain.Geneses.NODLE_PARACHAIN, coin = 1003, cla = 0x98u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.POLYMESH, coin = 595, cla = 0x91u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.XX_NETWORK, coin = 1955, cla = 0xa3u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.ASTAR, coin = 810, cla = 0xa9u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.ALEPH_ZERO, coin = 643, cla = 0xa4u),
                SubstrateApplicationConfig(chainId = Chain.Geneses.POLKADEX, coin = 799, cla = 0xa0u),

                novasamaLedgerTestnetFakeApp()
            )
        }

        fun all() = ALL

        private fun novasamaLedgerTestnetFakeApp(): SubstrateApplicationConfig? {
            return SubstrateApplicationConfig(chainId = "d67c91ca75c199ff1ee9555567dfad21b9033165c39977170ec8d3f6c1fa433c", coin = 434, cla = 0x90u)
                .takeIf { BuildConfig.DEBUG }
        }
    }
}

fun SubstrateApplicationConfig.Companion.supports(chainId: String): Boolean {
    return all().any { it.chainId == chainId }
}
