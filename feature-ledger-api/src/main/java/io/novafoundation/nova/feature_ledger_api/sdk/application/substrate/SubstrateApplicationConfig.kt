package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SubstrateApplicationConfig(
    val chainId: String,
    val coin: Int,
    val cla: UByte
) {
    companion object {

        fun all() = listOf(
            SubstrateApplicationConfig(
                chainId = Chain.Geneses.POLKADOT,
                coin = 354,
                cla = 0x90u,
            ),
            SubstrateApplicationConfig(
                chainId = Chain.Geneses.KUSAMA,
                coin = 434,
                cla = 0x99u
            )
        )
    }
}
