package io.novafoundation.nova.runtime.ext

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

suspend fun ChainRegistry.polkadot() = getChain(ChainGeneses.POLKADOT)
