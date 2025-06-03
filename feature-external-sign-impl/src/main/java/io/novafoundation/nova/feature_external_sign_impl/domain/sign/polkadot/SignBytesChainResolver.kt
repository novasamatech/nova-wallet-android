package io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findChainIds
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.addressPrefix
import javax.inject.Inject

interface SignBytesChainResolver {

    suspend fun resolveChainId(address: String): ChainId?
}

@FeatureScope
class RealSignBytesChainResolver @Inject constructor(
    private val chainRegistry: ChainRegistry,
) : SignBytesChainResolver {

    override suspend fun resolveChainId(address: String): ChainId? {
        return runCatching {
            val ss58Prefix = address.addressPrefix()
            detectChainIdFromSs58Prefix(ss58Prefix.toInt())
        }.getOrNull()
    }

    private suspend fun detectChainIdFromSs58Prefix(prefix: Int): ChainId? {
        val chains = chainRegistry.findChainIds { it.addressPrefix == prefix }

        // This mapping is targeted to provide better detection for UAF and Polkadot Vault derivations (PV has Polkadot and Kusama derivations by default)
        return when {
            chains.isEmpty() -> null

            Chain.Geneses.POLKADOT in chains -> Chain.Geneses.POLKADOT
            Chain.Geneses.KUSAMA in chains -> Chain.Geneses.KUSAMA

            chains.size == 1 -> chains.single()

            else -> Chain.Geneses.POLKADOT
        }
    }
}
