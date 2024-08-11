package io.novafoundation.nova.feature_swap_core.di

import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface SwapCoreDependencies {

    val chainRegistry: ChainRegistry

    val metadataShortenerService: MetadataShortenerService
}
