package io.novafoundation.nova.feature_ledger_core

import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface LedgerCoreDependencies {

    val chainRegistry: ChainRegistry

    val metadataShortenerService: MetadataShortenerService
}
